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
- [Arquitectura BFF (Backend for Frontend)](#arquitectura-bff-backend-for-frontend)
- [Cómo ejecutar los BFF](#cómo-ejecutar-los-bff)
- [Microservicios en la nube con Spring Cloud (Semana 6)](#microservicios-en-la-nube-con-spring-cloud-semana-6)
- [Integrantes](#integrantes-del-grupo-11-s1-s2-s4-y-s6)

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
src/main/java/com/bancoxyz/batch/
├── config/ # Configuración de Spring Batch (JobRepository, TaskExecutor)
├── exception/ # Excepciones de negocio (validación de datos)
├── jobs/ # Configuración de cada Job y sus Steps
├── listeners/ # Listeners de Step (registro de ítems descartados)
├── model/ # Clases de dominio usadas por los Jobs
├── partition/ # Particionadores para escalado de Jobs
├── processors/ # Lógica de negocio aplicada a cada ítem
├── readers/ # Lectores de archivos de entrada (CSV)
└── writers/ # Escritores hacia la base de datos


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

## Arquitectura BFF (Backend for Frontend)

A partir de esta entrega, el proyecto crece de una sola aplicación Spring Boot a un **repositorio multi-módulo de Maven**, donde cada servicio es una aplicación independiente con su propio `main()`, su propio puerto y su propio ciclo de vida:

PBY2203-bancoxyz/
├── pom.xml # aggregator (packaging=pom, declara los módulos)
├── batch-jobs/ # los 3 Jobs de Spring Batch (procesamiento offline)
├── core-api/ # fuente de verdad: expone los datos vía REST (puerto 8080)
├── bff-web/ # BFF para clientes Web (puerto 8081)
├── bff-movil/ # BFF para app móvil (puerto 8082)
├── bff-cajeros/ # BFF para cajeros automáticos (puerto 8083)
├── config-server/ # configuración centralizada, Spring Cloud Config (puerto 8888)
└── service-registry/ # Service Discovery, Eureka Server (puerto 8761)


### Decisión de diseño: microservicios separados en vez de un solo Spring Boot

Antes de repartir el trabajo, evaluamos dos alternativas:

1. **Un solo proyecto Spring Boot** con 3 sets de controllers (uno por canal: Web, Móvil, Cajeros).
2. **Módulos/microservicios separados**, cada uno como una aplicación Spring Boot independiente.

Elegimos la **opción 2** por estas razones:

- **Trabajo en paralelo sin conflictos:** al ser aplicaciones separadas (carpetas, `pom.xml` y procesos independientes), cada integrante pudo construir su parte sin tocar los archivos del otro.
- **Aislamiento real entre canales:** cada BFF tiene su propia autenticación, su propio puerto y su propio ciclo de despliegue. Un cambio o una caída en `bff-movil` no afecta a `bff-web` ni a `bff-cajeros`, algo que en un único proyecto con 3 sets de controllers sería más difícil de garantizar (comparten el mismo proceso y el mismo classpath).
- **Reglas de seguridad muy distintas por canal:** Cajeros necesita un PIN adicional por operación (`X-Pin`), Móvil y Web usan credenciales propias — mantenerlas en `SecurityConfig` separados por módulo es más claro que condicionar una sola configuración de seguridad según la ruta.
- **Se ajusta mejor al patrón BFF real:** el patrón Backend for Frontend nace justamente para que cada canal tenga su propio backend "a la medida", desplegable y escalable de forma independiente — un monolito con 3 sets de controllers es más bien un backend único con vistas distintas, no BFF real.

La contrapartida asumida: hay que levantar 4 procesos en paralelo para probar el sistema completo (`core-api` + los 3 BFF), en vez de uno solo. Se consideró un costo aceptable frente a los beneficios de aislamiento y trabajo paralelo.

### `core-api`

Expone los datos ya cargados por `batch-jobs` en Postgres:

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/cuentas` | Lista todas las cuentas con su interés calculado |
| GET | `/api/cuentas/{id}` | Detalle de una cuenta |
| GET | `/api/transacciones` | Lista todas las transacciones válidas |
| PATCH | `/api/cuentas/{id}/retiro` | Descuenta un monto del saldo (usado por Cajeros); responde `404` si la cuenta no existe y `409` si el saldo es insuficiente |

`core-api` **no es de acceso público**: un filtro (`InternalApiKeyFilter`) exige el header `X-Internal-Key` en cada request, con un valor compartido (`internal.api.key`) que solo conocen los 3 BFF. Esto evita que un cliente externo se salte los BFF y golpee la fuente de datos directamente.

### `bff-web` — canal Web (puerto 8081)

Expone datos completos (pensado para dashboards): `GET /web/cuentas`, `GET /web/cuentas/{id}`, `GET /web/transacciones`. Autenticación básica propia (`web-client` / `web-secret`, rol `WEB`).

### `bff-movil` — canal Móvil (puerto 8082)

Expone una versión liviana de la cuenta (solo `cuentaId`, `saldo`, `tipo` — sin los campos que la app no necesita) vía `GET /movil/cuentas/{id}`. Autenticación básica propia (`movil-client` / `movil-secret`, rol `MOVIL`).

### `bff-cajeros` — canal Cajeros Automáticos (puerto 8083)

Pensado para operaciones críticas: `GET /cajero/cuentas/{id}/saldo` y `PATCH /cajero/cuentas/{id}/retiro`. Además de la autenticación básica propia (`cajero-client` / `cajero-secret`, rol `CAJERO`), exige un PIN por operación mediante el header `X-Pin` (validado por `PinFilter` contra `atm.pin.esperado`) — una capa extra de seguridad acorde a que un cajero es un canal físico de alto riesgo.

## Cómo ejecutar los BFF

Se necesitan **PostgreSQL** en marcha y **6 terminales** en paralelo, una por servicio, **en este orden**:

```bash
cd config-server    && ../mvnw spring-boot:run   # puerto 8888 (debe ir primero)
cd core-api         && ../mvnw spring-boot:run   # puerto 8080
cd service-registry && ../mvnw spring-boot:run   # puerto 8761
cd bff-web          && ../mvnw spring-boot:run   # puerto 8081
cd bff-movil        && ../mvnw spring-boot:run   # puerto 8082
cd bff-cajeros      && ../mvnw spring-boot:run   # puerto 8083
```

- `core-api` obtiene su configuración (puerto, base de datos, SSL y clave interna) del Config Server, por lo que **no arranca si el Config Server no está levantado**.
- El Service Registry debe estar arriba antes que los BFF para que estos se registren al iniciar.
- `batch-jobs` no es necesario para esta parte: se ejecuta por separado (ver *Cómo ejecutar los Jobs*).

## Seguridad de transporte: HTTPS

A partir de esta entrega, los 4 servicios (`core-api`, `bff-web`, `bff-movil`, `bff-cajeros`) exponen sus endpoints únicamente por HTTPS, usando un certificado autofirmado compartido (`bancoxyz-keystore.p12`, alias `bancoxyz`, contraseña `bancoxyz2026`).

### Generación del certificado

El keystore ya está incluido en el repositorio (copiado en `src/main/resources/` de cada módulo). Si se necesitara regenerar:

```bash
keytool -genkeypair -alias bancoxyz -keyalg RSA -keysize 2048 -storetype PKCS12 \
  -keystore bancoxyz-keystore.p12 -validity 3650 \
  -dname "CN=localhost, OU=BancoXYZ, O=BancoXYZ, L=Santiago, ST=RM, C=CL" \
  -storepass bancoxyz2026
```

### Configuración por módulo

Cada `application.properties` incluye:

```properties
server.ssl.enabled=true
server.ssl.key-store=classpath:bancoxyz-keystore.p12
server.ssl.key-store-password=bancoxyz2026
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=bancoxyz
```

### Comunicación interna BFF → core-api

Como el certificado es autofirmado, el cliente HTTP por defecto de Java (usado por `RestClient`) rechaza la conexión por no reconocer una CA. Para resolverlo sin desactivar la validación por completo, cada BFF construye su `RestClient` sobre Apache HttpClient 5, configurado para confiar explícitamente en el mismo `bancoxyz-keystore.p12` como *trust store* (ver `RestClientConfig` en cada módulo).

### Pruebas con `curl`

Al ser un certificado autofirmado (no emitido por una CA pública), los clientes deben ignorar la validación de confianza explícitamente:

```bash
# core-api directo (requiere X-Internal-Key)
curl -k -H "X-Internal-Key: clave-interna-bancoxyz-2026" https://localhost:8080/api/cuentas

# bff-web
curl -k -u web-client:web-secret https://localhost:8081/web/cuentas

# bff-movil
curl -k -u movil-client:movil-secret https://localhost:8082/movil/cuentas/101

# bff-cajeros (requiere ademas el PIN)
curl -k -u cajero-client:cajero-secret -H "X-Pin: 1234" https://localhost:8083/cajero/cuentas/101/saldo
```

### Pruebas con Insomnia

Es necesario desactivar la validación SSL antes de probar estos endpoints, dado que el certificado no proviene de una CA reconocida por el sistema. En Insomnia: **Preferences → General → desactivar "Validate certificates"** (o el toggle equivalente según la versión).

### Pruebas con Postman

Postman valida certificados SSL por defecto. Para desactivarlo: **Settings (ícono de engranaje) → General → desactivar "SSL certificate verification"**. Alternativamente, puede desactivarse solo para este proyecto agregando la excepción del dominio en **Settings → Certificates**, sin afectar la verificación global de otras colecciones.

## Microservicios en la nube con Spring Cloud (Semana 6)

### Objetivo y propuesta técnica

Esta entrega lleva el proyecto a una arquitectura de microservicios preparada para la nube, agregando tres piezas de Spring Cloud sobre los servicios existentes (`core-api` y los 3 BFF):

| Necesidad | Solución | Módulo / componente |
|---|---|---|
| Configuración centralizada | Spring Cloud Config Server | `config-server` (puerto 8888) |
| Descubrimiento de servicios | Netflix Eureka | `service-registry` (puerto 8761) |
| Tolerancia a fallos | Circuit Breaker de Resilience4j con fallback | `bff-web`, `bff-movil`, `bff-cajeros` |
| Autenticación y autorización | Spring Security (ya existente, documentada más abajo) | los 3 BFF y `core-api` |

Los módulos de esta entrega que usan Spring Cloud (`config-server`, `service-registry`, `core-api` y los 3 BFF) usan **Spring Boot 4.1.0** y el tren de versiones **Spring Cloud 2025.1.3**, declarado en el `pom.xml` de cada módulo (propiedad `spring-cloud.version`).

```
 config-server (:8888) ── configuración ──────▶ core-api (:8080)
                                                ▲
 bff-web     (:8081) ─┐                         │
 bff-movil   (:8082) ─┼─ HTTPS + X-Internal-Key ┘
 bff-cajeros (:8083) ─┘   (Circuit Breaker en cada BFF)
        │
        └─ se registran en ─▶ service-registry / Eureka (:8761)
```

### Configuración centralizada: Config Server

- `config-server` es una aplicación Spring Boot con `spring-cloud-config-server`, anotada con `@EnableConfigServer`, en el puerto **8888**.
- Funciona en modo `native`: sirve la configuración desde una carpeta local del propio módulo, `config-server/src/main/resources/config-repo/`, en vez de un repositorio Git aparte. Cada microservicio tiene un archivo con el mismo nombre que su `spring.application.name`; hoy existe `core-api.properties`.
- `core-api` consume esa configuración: agrega la dependencia `spring-cloud-starter-config` y su `application.properties` local quedó reducido a su nombre, a `spring.config.import=configserver:http://localhost:8888` y a una propiedad de compatibilidad de Spring Cloud. Lo que se movió al Config Server: el puerto, la conexión a la base de datos, `internal.api.key` y la configuración SSL.
- Verificación: `GET http://localhost:8888/core-api/default` devuelve las propiedades, y en el log de arranque de `core-api` aparecen las líneas `Fetching config from server at : http://localhost:8888` y `Located environment: name=core-api, ...`.

### Service Discovery: Eureka

- `service-registry` es una aplicación Spring Boot con `spring-cloud-starter-netflix-eureka-server`, anotada con `@EnableEurekaServer`, en el puerto **8761**. Funciona como servidor único: `eureka.client.register-with-eureka=false` y `eureka.client.fetch-registry=false`.
- Los 3 BFF se registran como clientes con `spring-cloud-starter-netflix-eureka-client`. Cada uno declara la URL del registry (`eureka.client.service-url.defaultZone=http://localhost:8761/eureka/`) y, como los servicios solo atienden HTTPS, `eureka.instance.secure-port-enabled=true` y `eureka.instance.non-secure-port-enabled=false` (el cliente Eureka no deduce el puerto seguro a partir de `server.ssl.enabled`).
- Panel: `http://localhost:8761` muestra `BFF-WEB`, `BFF-MOVIL` y `BFF-CAJEROS` en estado UP.
- El aviso rojo *"EMERGENCY! EUREKA MAY BE INCORRECTLY CLAIMING INSTANCES ARE UP..."* del panel es el modo de auto-preservación de Eureka con pocas instancias registradas; no indica un error.
- Alcance: los BFF se registran, pero siguen llamando a `core-api` con una URL fija (`https://localhost:8080`); `core-api` no está registrado en Eureka (ver *Limitaciones conocidas*).

### Tolerancia a fallos en los BFF: Circuit Breaker (Resilience4j)

Cada BFF depende de `core-api`. Sin protección, si `core-api` cae, cada petición intentaría llamarlo y fallaría o esperaría en vano. Para evitarlo, los 3 BFF usan el patrón **Circuit Breaker** con **fallback**, mediante `spring-cloud-starter-circuitbreaker-resilience4j`:

- `ResilienceConfig` define el circuito `core-api` y sus parámetros.
- `RestClientConfig` registra un interceptor (`ClientHttpRequestInterceptor`) que envuelve con el Circuit Breaker el envío de cada petición a `core-api`, tanto los `GET` como el `PATCH` del retiro en `bff-cajeros`. Los controllers no cambian.

| Parámetro | Valor |
|---|---|
| Ventana de llamadas evaluadas | 5 |
| Mínimo de llamadas para evaluar | 3 |
| Umbral de fallos para abrir el circuito | 50 % |
| Tiempo en estado abierto | 15 s |
| Llamadas de prueba en estado semiabierto | 2 |
| Timeout por llamada | 5 s |

Estos valores están elegidos para poder demostrar el comportamiento; no son valores de producción. Los valores por defecto de Resilience4j (ventana y mínimo de 100 llamadas, 60 s en estado abierto, 1 s de timeout) hacen que el circuito casi nunca llegue a abrirse en una demostración.

**Qué cuenta como fallo:** los errores de conexión y los timeouts (más de 5 s sin respuesta). Las respuestas HTTP de `core-api`, incluidos los 4xx (por ejemplo, "cuenta no existe") y los 5xx, no cuentan como fallo del circuito, porque Spring aplica el manejo de códigos de estado después del interceptor.

**Fallback:** cuando la llamada falla o el circuito está abierto, el interceptor lanza una `ResourceAccessException` y el `RestClientExceptionHandler` de cada BFF la convierte en un **503 Service Unavailable** con uno de estos mensajes:

- `No fue posible comunicarse con core-api: sin respuesta (HttpHostConnectException)`: la llamada se intentó y falló.
- `No fue posible comunicarse con core-api: circuito abierto, llamada rechazada sin contactar al servicio`: el circuito está abierto y ni siquiera se intentó.

Decisión de diseño: el fallback **no inventa datos** (ni listas vacías ni saldos guardados). En un sistema bancario, una lista vacía es indistinguible de "no hay cuentas" y un saldo desactualizado es peligroso; y en el retiro del cajero nunca se simula un éxito.

**Ciclo del circuito:** cerrado → se abre cuando el porcentaje de fallos supera el umbral → permanece abierto 15 s (respondiendo de inmediato con el fallback) → pasa a semiabierto y deja pasar 2 llamadas de prueba → se cierra si tienen éxito, o vuelve a abrirse si fallan. Con la ventana de 5 llamadas y el mínimo de 3, el circuito se abre tras 2 o 3 fallos, según las llamadas previas registradas en la ventana.

**Cómo probarlo:** detener `core-api` y repetir una petición al BFF. Las primeras respuestas son 503 con `sin respuesta` y luego con `circuito abierto`. Al levantar `core-api` de nuevo y esperar 15 s, las peticiones vuelven a responder 200.

### Autenticación y autorización

Se usa Spring Security con `httpBasic`, como en el ejemplo de seguridad de la guía de la semana, y se protege el sistema en varias capas:

| Capa | Mecanismo | Si falla |
|---|---|---|
| Transporte | HTTPS con certificado autofirmado en `core-api` y los 3 BFF (ver *Seguridad de transporte: HTTPS*) | |
| Autenticación por canal | HTTP Basic en cada BFF, con un usuario en memoria por canal (`web-client`, `movil-client`, `cajero-client`) y contraseñas codificadas con `DelegatingPasswordEncoder` | 401 Unauthorized |
| Autorización por rol | Cada canal tiene su rol: `WEB` para `/web/**`, `MOVIL` para `/movil/**` y `CAJERO` para `/cajero/**`; el resto de las rutas exige estar autenticado | 401 / 403 |
| Segundo factor en cajeros | Cabecera `X-Pin` validada por un filtro sobre `/cajero/**` (valor esperado en `atm.pin.esperado`) | 403 con `{"error":"PIN invalido o ausente (X-Pin)"}` |
| Confianza entre servicios | Los BFF envían automáticamente la cabecera `X-Internal-Key` a `core-api`, que la valida con un filtro; el valor (`internal.api.key`) proviene del Config Server | 401 con `{"error":"Falta o es invalida la clave interna (X-Internal-Key)"}` |

Así, un cliente solo puede llegar a los datos a través del BFF de su canal y con sus credenciales, y `core-api` no puede consumirse directamente sin la clave interna. Las pruebas están en *Evidencia de ejecución* y en *Pruebas con `curl`*.

### Evidencia de ejecución

Las capturas están en el archivo `evidencia_backend_s6.pdf`, incluido en la carpeta de entrega, y se hicieron con Insomnia y con la terminal:

| Qué demuestra | Captura |
|---|---|
| Config Server | Log de arranque de `core-api` (`Fetching config from server at : http://localhost:8888`, `Located environment: name=core-api...`, Tomcat en el puerto 8080 con HTTPS) y `GET http://localhost:8888/core-api/default` con 200 |
| Cada canal responde | 200 en `bff-web`, `bff-movil` y `bff-cajeros` (este último con `X-Pin`) |
| Autenticación | 401 sin credenciales (`bff-web` y `bff-movil`) y 401 con clave incorrecta (`bff-web`) |
| Autorización por capas | 403 en cajeros sin PIN y 401 en `core-api` directo sin `X-Internal-Key` |
| Retiro | 200 con `core-api` arriba (el saldo de la cuenta 101 pasó de 4800 a 4700) y 503 con el circuito abierto |
| Circuit Breaker | En `bff-web` y `bff-movil`: 503 `sin respuesta` y luego 503 `circuito abierto` |
| Service Discovery | Panel de Eureka con `BFF-WEB`, `BFF-MOVIL` y `BFF-CAJEROS` en estado UP |

### Limitaciones conocidas

Decisiones y pendientes que se dejan documentados a propósito:

- **Config Server y Service Registry sin HTTPS ni autenticación.** Funcionan por HTTP en `localhost`, y el Config Server entrega en texto plano las contraseñas de la base de datos y del keystore y la clave interna. Esos valores además están versionados en este repositorio público. En un entorno real irían en un repositorio de configuración privado o en un gestor de secretos, y el Config Server estaría protegido.
- **Discovery parcial.** Los BFF se registran en Eureka, pero siguen llamando a `core-api` con una URL fija; `core-api` no está registrado en Eureka.
- **Circuit Breaker acotado.** Solo cuentan como fallo los errores de conexión y los timeouts: los 5xx de `core-api` no abren el circuito, y el cliente HTTP no tiene timeouts propios (solo el de 5 s del Circuit Breaker). En el retiro de cajeros, un timeout es ambiguo: la operación pudo haberse aplicado en `core-api` aunque el BFF responda 503.
- **Formato del error.** El fallback responde con un 503 en texto plano, no con un JSON estructurado.
- **Autenticación básica.** Los usuarios están en memoria y las credenciales, el PIN y la clave interna se guardan en archivos versionados. No se usan JWT ni OAuth 2.0.
- **Detalles del panel de Eureka.** Los BFF no exponen actuator, por lo que los enlaces de estado y salud del panel no funcionan, y el identificador de cada instancia incluye la IP de la red local.

## Integrantes del Grupo 11 (S1, S2, S4 y S6)

- **Diego Cruz** — Reporte de Transacciones Diarias, Generación de Estados de Cuenta Anuales, BFF Móvil, BFF Cajeros. S6: Service Registry (Eureka), registro de los 3 BFF en Eureka y Circuit Breaker.
- **Emilia Acevedo** — Cálculo de Intereses Mensuales, `core-api`, BFF Web. S6: Config Server y conexión de `core-api`.