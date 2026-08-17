# BancoXYZ — Proyecto Batch

Es un sistema de procesamiento batch para el BancoXYZ, desarrollado con **Spring Batch 6** sobre **Spring Boot**, que automatiza tres procesos clave del negocio bancario: transacciones diarias, cálculo de intereses mensuales y generación de estados de cuenta anuales.

## Tabla de contenidos

- [Tecnologías](#tecnologías)
- [Requisitos previos](#requisitos-previos)
- [Configuración del entorno](#configuración-del-entorno)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Jobs](#jobs)
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

Se utilizó PostgreSQL, para usarli, se debe crear el usuario y la base de datos de forma local:

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
```

### 5. Archivos de entrada (CSV)

Los archivos CSV que alimentan cada Job deben ubicarse en `src/main/resources/data/`:

- `data/transacciones.csv`
- `data/intereses.csv`
- `data/cuentas_anuales.csv`

## Estructura del proyecto
```
src/main/java/com/bancoxyz/batch/
├── jobs/ # Configuración de cada Job y su Step
├── model/ # Clases de dominio usadas por los Jobs
├── processors/ # Lógica de negocio aplicada a cada ítem
├── readers/ # Lectores de archivos de entrada (CSV)
└── writers/ # Escritores hacia la base de datos
```
## Jobs

| Job | Bean | Responsable | Descripción |
|---|---|---|---|
| Reporte de Transacciones Diarias | `transaccionesJob` | Diego | Procesa transacciones diarias para detectar anomalías y generar un resumen |
| Cálculo de Intereses Mensuales | `intereesMensualesJob` | Emilia | Aplica intereses sobre cuentas de ahorro y préstamo, y actualiza el saldo final en la base de datos |
| Generación de Estados de Cuenta Anuales | `cuentasAnualesJob` | Diego | Compila datos anuales por cuenta y genera un informe detallado para auditorías |

## Cómo ejecutar los Jobs

Cada Job puede lanzarse individualmente indicando su nombre como parámetro:

```bash
./mvnw spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=<nombreDelJob>"
```

## Integrantes del Grupo 11

- **Diego Cruz** — Reporte de Transacciones Diarias, Generación de Estados de Cuenta Anuales
- **Emilia Acevedo** — Cálculo de Intereses Mensuales