
-- =========================================================
-- LABORATORIO: DISEÑO DE BASE DE DATOS RELACIONAL
-- Consultas SQL sobre estaciones de servicio
-- Base de datos: actividad1
-- Esquema: laboratorio
-- =========================================================


-- 0. VERIFICACIÓN DE LA INGESTA
-- Comprobar el número de estaciones terrestres y marítimas.

SELECT
    id_tipo,
    COUNT(*) AS total_estaciones
FROM laboratorio.estacion_servicio
GROUP BY id_tipo
ORDER BY id_tipo;

-- Comprobar el número total de precios cargados.

SELECT
    COUNT(*) AS total_precios
FROM laboratorio.precio_carburante;


-- =========================================================
-- 1. EMPRESA CON MÁS ESTACIONES DE SERVICIO TERRESTRES
-- =========================================================

SELECT
    e.nombre_comercial AS empresa,
    COUNT(*) AS total_estaciones
FROM laboratorio.estacion_servicio s
         JOIN laboratorio.empresa e
              ON s.id_empresa = e.id_empresa
WHERE s.id_tipo = 'TERRESTRE'
GROUP BY e.id_empresa, e.nombre_comercial
ORDER BY total_estaciones DESC, empresa
    LIMIT 1;


-- =========================================================
-- 2. EMPRESA CON MÁS ESTACIONES DE SERVICIO MARÍTIMAS
-- =========================================================

SELECT
    e.nombre_comercial AS empresa,
    COUNT(*) AS total_estaciones
FROM laboratorio.estacion_servicio s
         JOIN laboratorio.empresa e
              ON s.id_empresa = e.id_empresa
WHERE s.id_tipo = 'MARITIMA'
GROUP BY e.id_empresa, e.nombre_comercial
ORDER BY total_estaciones DESC, empresa
    LIMIT 1;


-- =========================================================
-- 3. GASOLINA 95 E5 MÁS BARATA EN LA COMUNIDAD DE MADRID
-- =========================================================

SELECT
    s.provincia,
    s.municipio,
    s.localidad,
    s.codigo_postal,
    s.direccion,
    COALESCE(
            e.nombre_comercial,
            s.rotulo_original,
            'SIN RÓTULO'
    ) AS empresa,
    s.margen,
    p.precio AS precio_euros
FROM laboratorio.estacion_servicio s
         JOIN laboratorio.precio_carburante p
              ON s.id_estacion = p.id_estacion
         JOIN laboratorio.carburante c
              ON p.id_carburante = c.id_carburante
         LEFT JOIN laboratorio.empresa e
                   ON s.id_empresa = e.id_empresa
WHERE s.id_tipo = 'TERRESTRE'
  AND UPPER(TRIM(s.provincia)) = 'MADRID'
  AND c.nombre = 'gasolina 95 e5'
ORDER BY p.precio ASC, s.id_estacion
    LIMIT 1;


-- =========================================================
-- 4. GASÓLEO A MÁS BARATO A MENOS DE 10 KM
--    DEL CENTRO DE ALBACETE
-- =========================================================

-- Coordenadas aproximadas del centro de Albacete:
-- Latitud: 38.9943
-- Longitud: -1.8585
--
-- Se utiliza la fórmula Haversine para calcular
-- la distancia geográfica en línea recta.

WITH estaciones_cercanas AS (
    SELECT
        s.*,
        2 * 6371 * ASIN(
                LEAST(
                        1.0,
                        SQRT(
                                POWER(
                                        SIN(
                                                RADIANS(
                                                        (s.latitud::double precision - 38.9943) / 2
                                                )
                                        ), 2
                                )
                                    +
                                COS(RADIANS(38.9943))
                                    * COS(RADIANS(s.latitud::double precision))
                                    * POWER(
                                        SIN(
                                                RADIANS(
                                                        (s.longitud::double precision - (-1.8585)) / 2
                                                )
                                        ), 2
                                      )
                        )
                )
                   ) AS distancia_km
    FROM laboratorio.estacion_servicio s
    WHERE s.id_tipo = 'TERRESTRE'
      AND s.latitud IS NOT NULL
      AND s.longitud IS NOT NULL
)

SELECT
    s.provincia,
    s.municipio,
    s.localidad,
    s.direccion,
    COALESCE(
            e.nombre_comercial,
            s.rotulo_original,
            'SIN RÓTULO'
    ) AS empresa,
    s.margen,
    p.precio AS precio_euros,
    ROUND(s.distancia_km::numeric, 2) AS distancia_km
FROM estaciones_cercanas s
         JOIN laboratorio.precio_carburante p
              ON s.id_estacion = p.id_estacion
         JOIN laboratorio.carburante c
              ON p.id_carburante = c.id_carburante
         LEFT JOIN laboratorio.empresa e
                   ON s.id_empresa = e.id_empresa
WHERE s.distancia_km <= 10
  AND c.nombre = 'gasóleo a'
ORDER BY p.precio ASC, s.distancia_km ASC, s.id_estacion
    LIMIT 1;


-- =========================================================
-- 5. PROVINCIA DE LA ESTACIÓN MARÍTIMA CON
--    GASOLINA 95 E5 MÁS CARA
-- =========================================================

SELECT
    s.provincia,
    s.municipio,
    s.localidad,
    s.direccion,
    COALESCE(
            e.nombre_comercial,
            s.rotulo_original,
            'SIN RÓTULO'
    ) AS empresa,
    p.precio AS precio_euros
FROM laboratorio.estacion_servicio s
         JOIN laboratorio.precio_carburante p
              ON s.id_estacion = p.id_estacion
         JOIN laboratorio.carburante c
              ON p.id_carburante = c.id_carburante
         LEFT JOIN laboratorio.empresa e
                   ON s.id_empresa = e.id_empresa
WHERE s.id_tipo = 'MARITIMA'
  AND c.nombre = 'gasolina 95 e5'
ORDER BY p.precio DESC, s.id_estacion
    LIMIT 1;
