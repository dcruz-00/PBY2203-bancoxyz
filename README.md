# BancoXYZ — Proyecto Batch

Es un sistema de procesamiento batch para el BancoXYZ, desarrollado con **Spring Batch 6** sobre **Spring Boot**, que automatiza tres procesos clave del negocio bancario: transacciones diarias, cálculo de intereses mensuales y generación de estados de cuenta anuales.

## Tabla de contenidos

- [Tecnologías](#tecnologías)
- [Requisitos previos](#requisitos-previos)
- [Configuración del entorno](#configuración-del-entorno)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Jobs](#jobs)
- [Escalado y procesamiento paralelo](#escalado-y-procesamiento-paralelo)
- [Tolerancia a fallos](#tolerancia-a-fallos)
- [Cómo ejecutar los Jobs](#cómo-ejecutar-los-jobs)
- [Integrantes](#integrantes)

## Tecnologías

- Java
- Spring Boot
- Spring Batch 6
- PostgreSQL
- Maven

## Requisitos previos

- JDK instalado
- PostgreSQL instalado y corriendo localmente
- Maven Wrapper incluido en el proyecto (`mvnw` / `mvnw.cmd`), no requiere instalación aparte

## Configuración del entorno

### 1. Base de datos

Se utilizó PostgreSQL, para usarlo, se debe crear el usuario y la base de datos de forma local:

```sql
CREATE USER batch_user WITH PASSWORD 'postgres18';
CREATE DATABASE bancoxyz OWNER batch_user;
GRANT ALL PRIVILEGES ON DATABASE bancoxyz TO batch_user;
```

### 2. Tablas de metadata de Spring Batch

Conectarse a la base `bancoxyz` y ejecutar el script `schema-batch.sql` (tablas `BATCH_JOB_INSTANCE`, `BATCH_JOB_EXECUTION`, `BATCH_JOB_EXECUTION_PARAMS`, `BATCH_STEP_EXECUTION`, `BATCH_STEP_EXECUTION_CONTEXT`, `BATCH_JOB_EXECUTION_CONTEXT` y sus secuencias). Este esquema corresponde a Spring Batch 6, por lo que difiere del esquema usado en versiones anteriores del framework.

### 3. Tablas de negocio

Ejecutar los siguientes scripts sobre la base `bancoxyz`:

```sql
CREATE TABLE transacciones_diarias (
    id BIGINT PRIMARY KEY,
    fecha DATE NOT NULL,
    monto NUMERIC(12,2) NOT NULL,
    tipo VARCHAR(20) NOT NULL
);

CREATE TABLE intereses_calculados (
    cuenta_id BIGINT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    saldo NUMERIC(12,2) NOT NULL,
    edad INTEGER,
    tipo VARCHAR(20) NOT NULL,
    interes_generado NUMERIC(12,2),
    fecha_calculo DATE
);

CREATE TABLE cuentas_anuales (
    id SERIAL PRIMARY KEY,
    cuenta_id BIGINT NOT NULL,
    fecha DATE NOT NULL,
    transaccion VARCHAR(20) NOT NULL,
    monto NUMERIC(12,2) NOT NULL,
    descripcion VARCHAR(255)
);
```

### 4. `application.properties`

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/bancoxyz
spring.datasource.username=batch_user
spring.datasource.password=postgres18
spring.datasource.driver-class-name=org.postgresql.Driver

spring.batch.jdbc.initialize-schema=never

# Número de particiones para transaccionesJob (por defecto 3)
batch.transacciones.grid-size=3
```

### 5. Archivos de entrada (CSV)

Los archivos CSV que alimentan cada Job deben ubicarse en `src/main/resources/data/`:

- `data/transacciones.csv`
- `data/intereses.csv`
- `data/cuentas_anuales.csv`

## Estructura del proyecto

```
src/main/java/com/bancoxyz/batch/
├── config/      # Configuración de Spring Batch (JobRepository, TaskExecutor)
├── exception/   # Excepciones de negocio (validación de datos)
├── jobs/        # Configuración de cada Job y sus Steps
├── listeners/   # Listeners de Step (registro de ítems descartados)
├── model/       # Clases de dominio usadas por los Jobs
├── partition/   # Particionadores para escalado de Jobs
├── processors/  # Lógica de negocio aplicada a cada ítem
├── readers/     # Lectores de archivos de entrada (CSV)
└── writers/     # Escritores hacia la base de datos
```

## Jobs

| Job | Bean | Responsable | Descripción |
|---|---|---|---|
| Reporte de Transacciones Diarias | `transaccionesJob` | Diego | Procesa transacciones diarias para detectar anomalías y generar un resumen |
| Cálculo de Intereses Mensuales | `interesesMensualesJob` | Emilia | Aplica intereses sobre cuentas de ahorro y préstamo, y actualiza el saldo final en la base de datos |
| Generación de Estados de Cuenta Anuales | `cuentasAnualesJob` | Diego | Compila datos anuales por cuenta y genera un informe detallado para auditorías |

## Escalado y procesamiento paralelo

Cada Job aplica la técnica de escalado más adecuada según su volumen de datos:

| Job | Técnica | Justificación |
|---|---|---|
| `transaccionesJob` | Particionado (`Partitioner` + `TaskExecutorPartitionHandler`) | Mayor volumen de datos (procesamiento diario) |
| `interesesMensualesJob` | Multi-threading (`TaskExecutor`) | Volumen menor, ejecución periódica |
| `cuentasAnualesJob` | Multi-threading (`TaskExecutor`) | Volumen menor, ejecución periódica |

### Particionado en `transaccionesJob`

`transaccionesPartitionStep` divide el archivo `transacciones.csv` en `gridSize` rangos no solapados (clase `TransaccionesPartitioner`), y cada rango se procesa en paralelo por una instancia independiente de `transaccionesMinionStep`, en un hilo del `batchTaskExecutor`.

El número de particiones es configurable sin recompilar mediante:

```properties
batch.transacciones.grid-size=3
```

### Comparación de `gridSize` para encontrar la configuración óptima

Con `batchTaskExecutor` configurado con `corePoolSize=3`:

| gridSize | Threads usados | Tiempo del Job |
|---|---|---|
| 1 | 1 | 93ms |
| 2 | 2 | 92ms |
| 3 | 3 | **82ms** ✅ |
| 5 | 3 (pool saturado) | 110ms |

**Conclusión:** el óptimo es `gridSize = 3`, porque coincide con `corePoolSize` del `taskExecutor`. Pedir más particiones que hilos disponibles (`gridSize=5`) no mejora el rendimiento — las particiones sobrantes quedan en cola esperando un hilo libre, agregando overhead sin sumar paralelismo real.

## Tolerancia a fallos

Los tres Jobs aplican la misma política de `skip`/`retry` en su Step de procesamiento:

- **Skip** (se descarta el ítem y se continúa): `DatoInvalidoException` (regla de negocio incumplida) y `FlatFileParseException` (línea del CSV mal formada), con `skipLimit(100)`.
- **Retry** (se reintenta el ítem): `TransientDataAccessException` (fallos transitorios de base de datos), con `retryLimit(3)`.
- Cada ítem descartado por `skip` queda registrado por `RegistroDescartadoListener` (log vía SLF4J), indicando el motivo del descarte.
- Los 3 Steps también aplican `.noRollback(DatoInvalidoException.class)`. Sin esto, un dato inválido dentro de un chunk provoca el rollback y reescaneo (ítem por ítem) de todo el chunk; como los `ItemProcessor` mantienen un `Set` de deduplicación con estado, ese reescaneo volvía a invocar `process()` sobre ítems ya procesados correctamente, y estos quedaban descartados en silencio por "duplicados" al reencontrarse en el `Set`. `noRollback` evita el reescaneo para esta excepción, preservando el registro en el listener sin reprocesar los ítems ya válidos.

## Cómo ejecutar los Jobs

Cada Job puede lanzarse individualmente indicando su nombre como parámetro. Se debe agregar un parámetro `run.id` (sin `--`) distinto en cada corrida, ya que Spring Batch no permite reejecutar una instancia con los mismos parámetros identificadores:

```bash
./mvnw spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=<nombreDelJob> run.id=<valor único>"
```

Para `transaccionesJob`, el tamaño de partición puede ajustarse con `batch.transacciones.grid-size` (por defecto 3):

```bash
./mvnw spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=transaccionesJob --batch.transacciones.grid-size=3 run.id=<valor único>"
```

## Integrantes del Grupo 11 (S1 y S2)

- **Diego Cruz** — Reporte de Transacciones Diarias, Generación de Estados de Cuenta Anuales
- **Emilia Acevedo** — Cálculo de Intereses Mensuales