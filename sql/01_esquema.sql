-- Laboratorio UNIR. Ejecutar en la base de datos actividad1.
CREATE SCHEMA IF NOT EXISTS laboratorio;
SET search_path TO laboratorio;

CREATE TABLE IF NOT EXISTS empresa (
 id_empresa BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 nombre_comercial VARCHAR(255) NOT NULL,
 nombre_normalizado VARCHAR(255) NOT NULL UNIQUE
);
CREATE TABLE IF NOT EXISTS tipo_estacion (
 id_tipo VARCHAR(10) PRIMARY KEY,
 descripcion VARCHAR(100) NOT NULL
);
INSERT INTO tipo_estacion VALUES ('TERRESTRE','Estación de servicio terrestre'),('MARITIMA','Poste marítimo') ON CONFLICT DO NOTHING;
CREATE TABLE IF NOT EXISTS estacion_servicio (
 id_estacion BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 id_empresa BIGINT REFERENCES empresa(id_empresa),
 id_tipo VARCHAR(10) NOT NULL REFERENCES tipo_estacion(id_tipo),
 fila_origen INTEGER NOT NULL CHECK(fila_origen >= 2),
 provincia VARCHAR(100), municipio VARCHAR(150), localidad VARCHAR(150),
 codigo_postal VARCHAR(10), direccion TEXT, margen VARCHAR(5),
 latitud NUMERIC(10,7) CHECK(latitud BETWEEN -90 AND 90),
 longitud NUMERIC(11,7) CHECK(longitud BETWEEN -180 AND 180),
 fecha_actualizacion TIMESTAMP NULL,
 horario TEXT, tipo_venta TEXT, rem TEXT, tipo_servicio TEXT,
 rotulo_original TEXT,
 UNIQUE(id_tipo,fila_origen),
 CHECK(id_tipo = 'TERRESTRE' OR margen IS NULL)
);
CREATE TABLE IF NOT EXISTS carburante (
 id_carburante BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 nombre VARCHAR(150) NOT NULL UNIQUE
);
CREATE TABLE IF NOT EXISTS precio_carburante (
 id_estacion BIGINT NOT NULL REFERENCES estacion_servicio(id_estacion) ON DELETE CASCADE,
 id_carburante BIGINT NOT NULL REFERENCES carburante(id_carburante),
 precio NUMERIC(10,3) NOT NULL CHECK(precio > 0),
 porcentaje_mezcla NUMERIC(6,2),
 PRIMARY KEY(id_estacion,id_carburante)
);
CREATE INDEX IF NOT EXISTS ix_estacion_empresa_tipo ON estacion_servicio(id_empresa,id_tipo);
CREATE INDEX IF NOT EXISTS ix_estacion_provincia ON estacion_servicio(provincia);
CREATE INDEX IF NOT EXISTS ix_estacion_coordenadas ON estacion_servicio(latitud,longitud);
CREATE INDEX IF NOT EXISTS ix_precio_carburante_valor ON precio_carburante(id_carburante,precio);
COMMENT ON COLUMN estacion_servicio.fila_origen IS 'Fila del Excel limpio, usada para trazabilidad y evitar colisiones entre estaciones en la misma dirección.';
COMMENT ON COLUMN estacion_servicio.fecha_actualizacion IS 'Fecha por estación disponible únicamente en el fichero terrestre; NULL en marítimas.';
COMMENT ON COLUMN empresa.nombre_comercial IS 'Rótulo comercial; no acredita la razón social del operador.';
