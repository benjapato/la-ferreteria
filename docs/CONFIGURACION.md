# Configuración de la demo

Esta copia usa una base nueva llamada `laferreteria_demo`. No incluye registros del negocio, copias de seguridad ni contraseñas de la instalación original. El esquema se crea al iniciar; el único dato inicial automático es la cuenta administradora que configures.

## Requisitos

- JDK 21, con `JAVA_HOME` y `java` disponibles.
- Node.js 24 y npm disponibles. Vite admite Node 20.19 o Node 22.12 en adelante; Node 24 es una opción compatible para esta copia.
- PostgreSQL instalado y en ejecución. Se recomienda usar PostgreSQL 18, que es la versión contemplada por la instalación original.
- Acceso a internet en la primera compilación para descargar Gradle y dependencias. El proyecto incluye Gradle Wrapper 9.5.1 y fija pnpm 11.9.0; no hace falta instalar Gradle o pnpm globalmente.

Los comandos siguientes son para PowerShell en Windows. En macOS o Linux, exportá las mismas variables y usá `bash ./gradlew` en lugar de `.\gradlew.bat`.

## 1. Crear una base aislada

Abrí `psql` con una cuenta administradora de tu instalación PostgreSQL:

```powershell
psql -U postgres
```

En la consola de PostgreSQL, ejecutá:

```sql
CREATE ROLE laferreteria_demo LOGIN;
\password laferreteria_demo
CREATE DATABASE laferreteria_demo OWNER laferreteria_demo;
\q
```

El comando `\password` solicita una contraseña sin incluirla en el archivo ni en el historial de SQL. Usá esa contraseña al configurar `SPRING_DATASOURCE_PASSWORD`. Si `psql` no se encuentra, agregá el directorio `bin` de PostgreSQL a `PATH` o ejecutá su ruta completa.

## 2. Configurar la sesión de PowerShell

Desde la carpeta del repositorio, definí las variables de esta sesión:

```powershell
$env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:5432/laferreteria_demo'
$env:SPRING_DATASOURCE_USERNAME = 'laferreteria_demo'
$databasePassword = Read-Host 'Contraseña del usuario PostgreSQL de demo' -AsSecureString
$env:SPRING_DATASOURCE_PASSWORD = [Net.NetworkCredential]::new('', $databasePassword).Password
Remove-Variable databasePassword

$env:APP_USUARIO_INICIAL_NOMBRE = 'Administrador de demo'
$env:APP_USUARIO_INICIAL_USUARIO = 'demo_admin'
$adminPassword = Read-Host 'Elegí una contraseña de 8 a 100 caracteres para la cuenta de demo' -AsSecureString
$env:APP_USUARIO_INICIAL_CONTRASENA = [Net.NetworkCredential]::new('', $adminPassword).Password
Remove-Variable adminPassword
```

Elegí contraseñas propias. Los nombres del ejemplo identifican únicamente la nueva demo. Las contraseñas quedan en el entorno de esta sesión y de los procesos que inicies desde ella; no se escriben al repositorio. Cerrá la terminal al terminar.

Las cinco variables obligatorias son:

| Variable | Uso |
| --- | --- |
| `SPRING_DATASOURCE_USERNAME` | Usuario de la nueva base PostgreSQL. |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de ese usuario. |
| `APP_USUARIO_INICIAL_NOMBRE` | Nombre visible del administrador inicial. |
| `APP_USUARIO_INICIAL_USUARIO` | Nombre de acceso del administrador inicial. |
| `APP_USUARIO_INICIAL_CONTRASENA` | Contraseña inicial, de 8 a 100 caracteres. |

Definilas antes de cada ejecución del backend. La aplicación crea el administrador únicamente cuando no hay uno activo; cambiar estas variables no cambia la contraseña de una cuenta existente. Los archivos `.env` están excluidos de Git, pero Spring Boot no los carga automáticamente: para el backend usá variables del entorno o tu mecanismo local de configuración.

## 3. Ejecutar la aplicación completa

En la misma terminal que tiene las variables configuradas:

```powershell
Set-Location backend
.\gradlew.bat bootRun
```

Gradle instala el frontend con el archivo de dependencias fijadas, lo compila y lo sirve dentro del backend. Abrí [http://localhost:9092/app/](http://localhost:9092/app/) e ingresá con la cuenta que elegiste. Detené la aplicación con `Ctrl+C`.

Para construir un archivo ejecutable:

```powershell
.\gradlew.bat bootJar
java -jar build\libs\LaFerreteriaReactBackend-0.0.1-SNAPSHOT.jar
```

El archivo también necesita las variables de entorno y PostgreSQL disponibles al ejecutarse.

## Desarrollo con recarga automática

Dejá el backend funcionando y abrí otra terminal en la carpeta del repositorio:

```powershell
Set-Location frontend
npx --yes pnpm@11.9.0 install --frozen-lockfile
npx --yes pnpm@11.9.0 run dev
```

Abrí [http://localhost:5173](http://localhost:5173). Vite reenvía `/api` al backend. La terminal del frontend no necesita las contraseñas de PostgreSQL ni del administrador.

## Puertos y copias de seguridad

| Variable opcional | Valor predeterminado | Efecto |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/laferreteria_demo` | Conexión a la base de demo. |
| `SERVER_PORT` | `9092` | Puerto del backend. |
| `APP_FRONTEND_URL` | `http://localhost:5173` | Origen permitido del frontend durante desarrollo. |
| `FRONTEND_PORT` | `5173` | Puerto de Vite. Si está ocupado, Vite informa el error. |
| `BACKEND_URL` | `http://localhost:9092` | Destino del proxy de Vite durante desarrollo. |
| `APP_COPIAS_SEGURIDAD_DIRECTORIO` | `backups-demo` | Carpeta local de backups, relativa al directorio de ejecución del backend. |
| `APP_COPIAS_SEGURIDAD_PG_DUMP` | `pg_dump` | Ejecutable de PostgreSQL buscado en `PATH`, o su ruta absoluta. |
| `APP_COPIAS_SEGURIDAD_AUTOMATICAS` | `false` | Permite habilitar las copias programadas de la demo. |

Si cambiás el backend a otro puerto, actualizá `SERVER_PORT` en su terminal y `BACKEND_URL` en la del frontend. Si cambiás el puerto del frontend, actualizá `FRONTEND_PORT` y `APP_FRONTEND_URL` en sus terminales correspondientes. Reiniciá los procesos después de cambiar las variables. También podés definir `FRONTEND_PORT` y `BACKEND_URL` en un `.env.local` dentro de `frontend`; sus valores se leen solo en la configuración del servidor de Vite.

Para usar backups, instalá una versión de `pg_dump` compatible con tu servidor PostgreSQL. Si no está en `PATH`, por ejemplo:

```powershell
$env:APP_COPIAS_SEGURIDAD_PG_DUMP = 'C:/Program Files/PostgreSQL/18/bin/pg_dump.exe'
```

## Verificaciones

Desde `backend`:

```powershell
.\gradlew.bat test
```

Las pruebas normales usan una base H2 en memoria y credenciales ficticias definidas solo para pruebas. La prueba opcional `UnidadMedidaPostgresTests` solo se activa cuando existe `TEST_POSTGRES_URL`; requiere también `TEST_POSTGRES_USER` y `TEST_POSTGRES_PASSWORD`. Usá una base de pruebas aislada: el test crea y elimina su propio esquema. La construcción del frontend forma parte del procesamiento de recursos de Gradle.

Desde `frontend`:

```powershell
npx --yes pnpm@11.9.0 run lint
npx --yes pnpm@11.9.0 run build
node --test src/utils/quantities.test.js
```

## Archivos locales excluidos

El `.gitignore` omite credenciales locales, dependencias, compilados, logs, bases de datos y backups. Esta copia tampoco distribuye los scripts de carga de datos de la instalación, el manual interno ni los accesos directos locales. Para mostrar pantallas con información, cargá únicamente datos ficticios en `laferreteria_demo`.

Los encabezados de los comprobantes de esta copia usan textos de demostración en lugar de la dirección y el contacto de la instalación original.
