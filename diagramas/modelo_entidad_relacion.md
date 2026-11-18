# Modelo entidad-relación propuesto

```mermaid
erDiagram
    EMPRESA o|--o{ ESTACION_SERVICIO : opera
    TIPO_ESTACION ||--o{ ESTACION_SERVICIO : clasifica
    ESTACION_SERVICIO ||--o{ PRECIO_CARBURANTE : ofrece
    CARBURANTE ||--o{ PRECIO_CARBURANTE : identifica

    EMPRESA {
        bigint id_empresa PK
        varchar nombre_comercial
        varchar nombre_normalizado UK
    }
    TIPO_ESTACION {
        varchar id_tipo PK
        varchar descripcion
    }
    ESTACION_SERVICIO {
        bigint id_estacion PK
        bigint id_empresa FK
        varchar id_tipo FK
        int fila_origen
        varchar provincia
        varchar municipio
        varchar localidad
        varchar codigo_postal
        text direccion
        varchar margen
        numeric latitud
        numeric longitud
        timestamp fecha_actualizacion
        text horario
        text tipo_venta
        text rem
        text tipo_servicio
        text rotulo_original
    }
    CARBURANTE {
        bigint id_carburante PK
        varchar nombre UK
    }
    PRECIO_CARBURANTE {
        bigint id_estacion PK,FK
        bigint id_carburante PK,FK
        numeric precio
        numeric porcentaje_mezcla
    }
```

## Justificación

- **Cinco tablas**: `EMPRESA`, `TIPO_ESTACION`, `ESTACION_SERVICIO`, `CARBURANTE` y `PRECIO_CARBURANTE`.
- Una empresa puede operar muchas estaciones; una estación puede carecer de empresa identificada.
- Cada estación es terrestre o marítima; los precios son registros separados por carburante.
- La clave compuesta de `PRECIO_CARBURANTE` impide dos precios simultáneos del mismo carburante en una estación.
- Provincia, municipio y localidad se conservan como atributos de estación para simplificar este laboratorio; son campos repetidos y podrían normalizarse más en un modelo ampliado.
- `fila_origen` conserva trazabilidad con los archivos depurados, sin deduplicar erróneamente estaciones que comparten dirección.
- `fecha_actualizacion` queda NULL en marítimas porque el fichero limpio no trae fecha por poste.
- `Rótulo` se interpreta como **nombre comercial**, no como razón social acreditada.
- La búsqueda de 10 km usa distancia geodésica aproximada, no itinerario por carretera.
