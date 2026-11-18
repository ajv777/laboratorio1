package es.unir.combustibles;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Ingesta completa de los dos Excel depurados, en una transacción. */
public class Ingesta {
    private static final String URL = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5433/actividad1");
    private static final String USER = System.getenv().getOrDefault("DB_USER", "actividad1");
    private static final String PASS = System.getenv().getOrDefault("DB_PASSWORD", "actividad1");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DataFormatter FORMAT = new DataFormatter(new Locale("es", "ES"));

    public static void main(String[] args) throws Exception {
        Path carpeta = Paths.get(args.length > 0 ? args[0] : "../datos");
        try (Connection db = DriverManager.getConnection(URL, USER, PASS)) {
            db.setAutoCommit(false);
            try {
                // Carga repetible: borra registros anteriores, pero conserva el esquema.
                try (Statement st = db.createStatement()) {
                    st.execute("TRUNCATE laboratorio.precio_carburante, laboratorio.estacion_servicio, laboratorio.carburante, laboratorio.empresa RESTART IDENTITY CASCADE");
                }
                int terrestres = cargar(db, carpeta.resolve("preciosEESS_es_limpio.xlsx"), "TERRESTRE");
                int maritimas = cargar(db, carpeta.resolve("embarcacionesPrecios_es_limpio.xlsx"), "MARITIMA");
                db.commit();
                System.out.printf("Carga confirmada: %d terrestres y %d marítimas (%d estaciones).%n", terrestres, maritimas, terrestres + maritimas);
            } catch (Exception e) {
                db.rollback();
                System.err.println("Error: se ha revertido toda la carga: " + e.getMessage());
                throw e;
            }
        }
    }

    private static int cargar(Connection db, Path archivo, String tipo) throws Exception {
        if (!Files.isRegularFile(archivo)) throw new FileNotFoundException(archivo.toString());
        int estaciones = 0, precios = 0;
        Map<String,Long> empresas = new HashMap<>(), carburantes = new HashMap<>();
        try (InputStream in = Files.newInputStream(archivo); Workbook libro = new XSSFWorkbook(in)) {
            Sheet hoja = libro.getSheetAt(0);
            Row cabecera = hoja.getRow(0);
            Map<String,Integer> columnas = new LinkedHashMap<>();
            for (Cell celda : cabecera) {
                String nombre = texto(celda).trim().replaceAll("\\s+", " ");
                if (!nombre.isEmpty()) columnas.put(nombre.toLowerCase(Locale.ROOT), celda.getColumnIndex());
            }
            for (String requerida : List.of("Provincia","Municipio","Localidad","Código postal","Dirección","Rótulo","Horario")) {
                if (!columnas.containsKey(requerida.toLowerCase(Locale.ROOT))) throw new IllegalArgumentException("Falta columna " + requerida + " en " + archivo);
            }
            for (int r = 1; r <= hoja.getLastRowNum(); r++) {
                Row fila = hoja.getRow(r);
                if (fila == null || campo(fila,columnas,"Provincia").isBlank()) continue;
                String rotulo = campo(fila,columnas,"Rótulo");
                Long empresaId = null;
                String normal = rotulo.trim().replaceAll("\\s+"," ").toUpperCase(Locale.ROOT);
                if (!normal.isBlank() && !Set.of("NINGUNO","NO HAY","POR RELLENAR","SIN RÓTULO","SIN ROTULO").contains(normal)) {
                    empresaId = empresas.get(normal);
                    if (empresaId == null) {
                        try (PreparedStatement ps = db.prepareStatement("INSERT INTO laboratorio.empresa(nombre_comercial,nombre_normalizado) VALUES (?,?) ON CONFLICT (nombre_normalizado) DO UPDATE SET nombre_comercial = laboratorio.empresa.nombre_comercial RETURNING id_empresa")) {
                            ps.setString(1,rotulo.trim()); ps.setString(2,normal);
                            try (ResultSet rs = ps.executeQuery()) {rs.next(); empresaId=rs.getLong(1);}
                        }
                        empresas.put(normal,empresaId);
                    }
                }
                long estacionId;
                try (PreparedStatement ps = db.prepareStatement("""
                    INSERT INTO laboratorio.estacion_servicio
                    (id_empresa,id_tipo,fila_origen,provincia,municipio,localidad,codigo_postal,direccion,margen,latitud,longitud,fecha_actualizacion,horario,tipo_venta,rem,tipo_servicio,rotulo_original)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING id_estacion
                    """)) {
                    if (empresaId == null) ps.setNull(1,Types.BIGINT); else ps.setLong(1,empresaId);
                    ps.setString(2,tipo);ps.setInt(3,r+1);
                    ps.setString(4,campo(fila,columnas,"Provincia"));
                    ps.setString(5,campo(fila,columnas,"Municipio"));
                    ps.setString(6,campo(fila,columnas,"Localidad"));
                    ps.setString(7,campo(fila,columnas,"Código postal"));
                    ps.setString(8,campo(fila,columnas,"Dirección"));
                    nullableString(ps,9,tipo.equals("TERRESTRE")?campo(fila,columnas,"Margen"):"");
                    decimalNullable(ps,10,campo(fila,columnas,"Latitud"));
                    decimalNullable(ps,11,campo(fila,columnas,"Longitud"));
                    String fecha = campo(fila,columnas,"Toma de datos");
                    if (fecha.isBlank()) ps.setNull(12,Types.TIMESTAMP);
                    else ps.setTimestamp(12,Timestamp.valueOf(LocalDateTime.parse(fecha,FECHA)));
                    nullableString(ps,13,campo(fila,columnas,"Horario"));
                    nullableString(ps,14,campo(fila,columnas,"Tipo venta"));
                    nullableString(ps,15,campo(fila,columnas,"Rem."));
                    nullableString(ps,16,campo(fila,columnas,"Tipo servicio"));
                    nullableString(ps,17,rotulo);
                    try (ResultSet rs=ps.executeQuery()){rs.next();estacionId=rs.getLong(1);}
                }
                estaciones++;
                for (Map.Entry<String,Integer> col : columnas.entrySet()) {
                    String nombre = col.getKey();
                    if (!nombre.startsWith("precio ")) continue;
                    String valor = texto(fila.getCell(col.getValue()));
                    BigDecimal precio = numero(valor);
                    if (precio == null || precio.signum() <= 0) continue;
                    String producto = nombre.substring(7).replaceAll("\\s+", " ").trim();
                    Long carburanteId = carburantes.get(producto);
                    if (carburanteId == null) {
                        try (PreparedStatement ps=db.prepareStatement("INSERT INTO laboratorio.carburante(nombre) VALUES (?) ON CONFLICT(nombre) DO UPDATE SET nombre=EXCLUDED.nombre RETURNING id_carburante")) {
                            ps.setString(1,producto);
                            try (ResultSet rs=ps.executeQuery()){rs.next();carburanteId=rs.getLong(1);}
                        }
                        carburantes.put(producto,carburanteId);
                    }
                    BigDecimal mezcla = null;
                    if (producto.equals("bioetanol")) mezcla = numero(campo(fila,columnas,"% bioalcohol"));
                    if (producto.equals("biodiésel")) mezcla = numero(campo(fila,columnas,"% éster metílico"));
                    try (PreparedStatement ps=db.prepareStatement("INSERT INTO laboratorio.precio_carburante(id_estacion,id_carburante,precio,porcentaje_mezcla) VALUES(?,?,?,?)")) {
                        ps.setLong(1,estacionId);ps.setLong(2,carburanteId);ps.setBigDecimal(3,precio);
                        if (mezcla==null) ps.setNull(4,Types.NUMERIC);else ps.setBigDecimal(4,mezcla);
                        ps.executeUpdate();precios++;
                    }
                }
            }
        }
        System.out.printf("%s: %d estaciones y %d precios.%n",tipo,estaciones,precios);
        return estaciones;
    }
    private static String texto(Cell c) { return c==null ? "" : FORMAT.formatCellValue(c).trim(); }
    private static String campo(Row fila,Map<String,Integer> columnas,String nombre) {
        Integer i=columnas.get(nombre.toLowerCase(Locale.ROOT));
        return i==null?"":texto(fila.getCell(i));
    }
    private static BigDecimal numero(String s) {
        if (s==null || s.isBlank()) return null;
        String n=s.trim().replace(" ","").replace("€","").replace("%","");
        if (n.contains(",")) n=n.replace(".","").replace(',','.');
        try {return new BigDecimal(n);}catch(NumberFormatException ex){throw new IllegalArgumentException("Número no válido: " + s,ex);}
    }
    private static void nullableString(PreparedStatement ps,int i,String s)throws SQLException {
        if(s==null||s.isBlank())ps.setNull(i,Types.VARCHAR);else ps.setString(i,s);
    }
    private static void decimalNullable(PreparedStatement ps,int i,String s)throws SQLException {
        BigDecimal n=numero(s);if(n==null)ps.setNull(i,Types.NUMERIC);else ps.setBigDecimal(i,n);
    }
}
