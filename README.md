# Laboratorio UNIR — Diseño de una base de datos relacional

## Descripción del proyecto

Este proyecto corresponde a la actividad de laboratorio de la asignatura **Bases de Datos Avanzadas** de la Universidad Internacional de La Rioja (UNIR).

El objetivo es diseñar e implementar una base de datos relacional que permita almacenar y consultar información sobre los precios de carburantes de las estaciones de servicio terrestres y marítimas de España, a partir de los datos publicados por el Ministerio para la Transición Ecológica y el Reto Demográfico.

Para su desarrollo se han utilizado las siguientes tecnologías:

- **Java 21:** desarrollo del proceso de ingesta de datos.
- **IntelliJ IDEA:** entorno de desarrollo.
- **Apache Maven:** gestión de dependencias.
- **Apache POI:** lectura y procesamiento de archivos Excel.
- **PostgreSQL 16:** sistema gestor de bases de datos relacional.
- **pgAdmin 4:** administración y ejecución de consultas SQL.
- **Docker Desktop:** despliegue de PostgreSQL y pgAdmin en contenedores.

El proyecto incluye el diseño entidad-relación, la creación del esquema mediante SQL, la implementación del proceso de carga en Java y la resolución de las cinco consultas solicitadas.

---

## 1. Estructura del proyecto

El proyecto está organizado de la siguiente manera:

```text
laboratorio1/
├── README.md
├── datos/
│   ├── preciosEESS_es_limpio.xlsx
│   └── embarcacionesPrecios_es_limpio.xlsx
├── diagramas/
│   └── modelo_entidad_relacion.md
├── docker/
│   └── docker-compose.yml
├── java/
│   ├── pom.xml
│   └── src/main/java/es/unir/combustibles/
│       └── Ingesta.java
├── sql/
│   ├── 01_esquema.sql
│   └── 02_consultas.sql
├── resultados/
│   └── INSTRUCCIONES.md
└── video/
    └── guion_videomemoria.md
```

Los archivos Excel contienen **12.073 estaciones terrestres y 136 estaciones marítimas**, lo que supone un total de **12.209 estaciones**.

Los datos originales se han preparado eliminando las tres primeras filas informativas de cada archivo y conservando los encabezados de las columnas. Los ficheros se han guardado en formato `.xlsx` para facilitar su procesamiento mediante Apache POI.

## 2. Diseño de la base de datos

Se ha desarrollado un modelo relacional compuesto por cinco tablas, con el objetivo de organizar la información y evitar redundancias innecesarias.

### 2.1. Entidades

**EMPRESA**

Almacena la información comercial de las empresas asociadas a las estaciones de servicio.

**TIPO_ESTACION**

Permite diferenciar las estaciones terrestres de las marítimas.

**ESTACION_SERVICIO**

Contiene la información general de cada instalación: provincia, municipio, localidad, código postal, dirección, coordenadas geográficas, margen, horario y fecha de actualización.

**CARBURANTE**

Contiene el catálogo de carburantes disponibles en los archivos de origen.

**PRECIO_CARBURANTE**

Relaciona las estaciones de servicio con los carburantes que ofrecen y sus precios correspondientes.

### 2.2. Relaciones

El modelo establece las siguientes relaciones:

- Una empresa puede estar asociada a varias estaciones de servicio.
- Cada estación pertenece a un tipo de estación.
- Una estación puede ofrecer varios carburantes.
- Un mismo carburante puede estar disponible en múltiples estaciones.

La relación entre estaciones y carburantes se resuelve mediante la tabla `precio_carburante`, que permite almacenar los precios sin crear una columna independiente para cada combustible.

Se utilizan claves primarias y foráneas para garantizar la integridad referencial.

El diagrama entidad-relación se encuentra en:

`diagramas/modelo_entidad_relacion.md`

## 3. Requisitos del entorno

Para ejecutar el proyecto es necesario disponer de:

- macOS con Docker Desktop instalado.
- IntelliJ IDEA.
- JDK 21.
- Maven, integrado en IntelliJ IDEA.
- Navegador web para acceder a pgAdmin.

La configuración de Docker utiliza los siguientes puertos:

| Servicio | Puerto local |
|---|---|
| PostgreSQL | 5433 |
| pgAdmin | 8081 |

## 4. Despliegue de PostgreSQL y pgAdmin

Abrir Docker Desktop desde Aplicaciones o mediante la terminal:

```bash
open -a Docker
```

Seleccionar el contexto de Docker Desktop:

```bash
docker context use desktop-linux
```

Desde la carpeta raíz del proyecto, ejecutar:

```bash
docker compose -f docker/docker-compose.yml up -d
```

Comprobar el estado de los contenedores:

```bash
docker compose -f docker/docker-compose.yml ps
```

El contenedor de PostgreSQL debe aparecer en estado `healthy`.

En el primer despliegue se crea la base de datos `actividad1` y se ejecuta el script de creación del esquema.

### 4.1. Acceso a pgAdmin

Abrir en el navegador:

http://localhost:8081

Credenciales del entorno local:

- Usuario: `admin@example.com`
- Contraseña: `admin`

### 4.2. Configuración de la conexión

En pgAdmin, registrar un servidor con los siguientes datos:

| Parámetro | Valor |
|---|---|
| Nombre | Actividad 1 |
| Host | db |
| Puerto | 5432 |
| Base de datos | actividad1 |
| Usuario | actividad1 |
| Contraseña | actividad1 |

Estas credenciales corresponden únicamente al entorno de desarrollo local.

Una vez establecida la conexión, acceder a:

`Databases > actividad1 > Schemas > laboratorio > Tables`

En el esquema deben aparecer las cinco tablas del modelo relacional.

## 5. Creación del esquema relacional

El archivo `sql/01_esquema.sql` contiene las instrucciones DDL necesarias para construir la base de datos.

Incluye:

- Creación del esquema `laboratorio`.
- Definición de las cinco tablas.
- Claves primarias.
- Claves foráneas.
- Restricciones de integridad.
- Índices para facilitar las consultas.

El script puede ejecutarse desde Query Tool de pgAdmin, seleccionando previamente la base de datos `actividad1`.

## 6. Desarrollo y ejecución de la ingesta en Java

### 6.1. Configuración de IntelliJ IDEA

1. Abrir IntelliJ IDEA.
2. Seleccionar **File > Open**.
3. Abrir la carpeta `java/` del proyecto.
4. Importar el proyecto Maven mediante `pom.xml`.
5. Configurar el SDK del proyecto con Java 21.
6. Esperar a que Maven descargue las dependencias necesarias.
7. Abrir la clase `Ingesta.java`.

El programa utiliza Apache POI para procesar los archivos Excel y JDBC para realizar la conexión con PostgreSQL.

### 6.2. Configuración de ejecución

En IntelliJ, acceder a:

**Run > Edit Configurations**

Seleccionar la configuración `Ingesta` y establecer el directorio de trabajo:

```text
/Users/anajv/Desktop/laboratorio1/java
```

El programa utiliza por defecto la carpeta `../datos`, donde se encuentran los dos archivos Excel.

La conexión JDBC está configurada mediante los siguientes parámetros:

```text
URL: jdbc:postgresql://localhost:5433/actividad1
Usuario: actividad1
Contraseña: actividad1
```

Estos valores pueden modificarse mediante las variables de entorno `DB_URL`, `DB_USER` y `DB_PASSWORD`.

### 6.3. Proceso de carga

La clase `Ingesta.java` realiza las siguientes operaciones:

1. Establece la conexión con PostgreSQL.
2. Inicia una transacción.
3. Prepara las tablas para una carga completa.
4. Lee los archivos Excel mediante Apache POI.
5. Identifica las columnas y procesa sus valores.
6. Inserta las empresas y los carburantes.
7. Inserta las estaciones terrestres y marítimas.
8. Relaciona cada estación con sus carburantes y precios.
9. Confirma la transacción cuando finaliza correctamente.

Durante el procesamiento se transforman los valores numéricos y las fechas al formato requerido por PostgreSQL.

Los valores vacíos se gestionan según el tipo de dato y las restricciones definidas en el esquema.

El programa permite repetir la ingesta. Antes de cada ejecución elimina los registros anteriores de las tablas de datos y vuelve a cargarlos, manteniendo la estructura relacional.

Si se produce un error durante la carga, se ejecuta un `rollback` para revertir la transacción.

### 6.4. Resultado de la ejecución

La ingesta se ha ejecutado correctamente desde IntelliJ IDEA, obteniendo los siguientes resultados:

```text
TERRESTRE: 12073 estaciones y 46378 precios.
MARITIMA: 136 estaciones y 254 precios.
Carga confirmada: 12073 terrestres y 136 marítimas
(12209 estaciones).

Process finished with exit code 0
```

Por tanto, se han cargado:

| Tipo | Estaciones | Precios |
|---|---:|---:|
| Terrestres | 12.073 | 46.378 |
| Marítimas | 136 | 254 |
| **Total** | **12.209** | **46.632** |

El código de salida `0` indica que el programa ha finalizado correctamente.

## 7. Verificación de los datos

Una vez completada la ingesta, se comprueba la información almacenada mediante consultas SQL desde pgAdmin.

### 7.1. Número de estaciones por tipo

```sql
SELECT
    id_tipo,
    COUNT(*) AS total_estaciones
FROM laboratorio.estacion_servicio
GROUP BY id_tipo
ORDER BY id_tipo;
```

El resultado esperado es de 12.073 estaciones terrestres y 136 marítimas.

### 7.2. Número de precios cargados

```sql
SELECT
    COUNT(*) AS total_precios
FROM laboratorio.precio_carburante;
```

El resultado esperado es de 46.632 registros de precios.

Estas comprobaciones permiten contrastar los registros almacenados con los datos procesados durante la ejecución de Java.

## 8. Consultas SQL de la actividad

El archivo `sql/02_consultas.sql` contiene las cinco consultas solicitadas en el enunciado:

1. Identificar la empresa con mayor número de estaciones terrestres.
2. Identificar la empresa con mayor número de estaciones marítimas.
3. Localizar la estación con el precio más bajo de Gasolina 95 E5 en la Comunidad de Madrid.
4. Localizar la estación con el Gasóleo A más barato a una distancia máxima de 10 km del centro de Albacete.
5. Identificar la provincia donde se encuentra la estación marítima con el precio más alto de Gasolina 95 E5.

Las consultas utilizan operaciones `JOIN`, filtros `WHERE`, agrupaciones `GROUP BY`, ordenaciones `ORDER BY` y limitación de resultados mediante `LIMIT`.

Para evitar problemas de resolución de esquemas, se utiliza el prefijo `laboratorio.` delante de los nombres de las tablas.

### 8.1. Criterios utilizados

**Comunidad de Madrid**

Se filtran las estaciones de la provincia de Madrid, ya que se trata de una comunidad autónoma uniprovincial.

**Distancia desde Albacete**

Se toma como referencia aproximada el punto geográfico:

- Latitud: 38.9943
- Longitud: -1.8585

Se utiliza la fórmula de Haversine para calcular la distancia geográfica entre dos puntos a partir de sus coordenadas.

Este cálculo representa la distancia en línea recta, no