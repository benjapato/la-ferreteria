# Configuracion de una instalacion propia

## 1. Crear una base separada

En PostgreSQL, crear un usuario con permiso de inicio de sesion, sin privilegios
de superusuario, y asignarle una contrasena propia. Por ejemplo, llamar al
usuario `laferreteria_app`.

Crear una base vacia, por ejemplo `laferreteria_demo`, con ese usuario como
propietario. El usuario necesita permisos para crear y actualizar las tablas de
esa base. No apuntar esta copia a la base de la ferreteria en funcionamiento.

## 2. Configurar el backend

La aplicacion exige estas variables de entorno, sin valores predeterminados:

| Variable | Contenido |
| --- | --- |
| `SPRING_DATASOURCE_URL` | URL de la base propia, por ejemplo `jdbc:postgresql://localhost:5432/laferreteria_demo` |
| `SPRING_DATASOURCE_USERNAME` | Usuario de PostgreSQL de esa base |
| `SPRING_DATASOURCE_PASSWORD` | Contrasena privada de ese usuario |

Para una base nueva, configurar tambien:

| Variable | Contenido |
| --- | --- |
| `APP_USUARIO_INICIAL_USUARIO` | Nombre de usuario del administrador inicial |
| `APP_USUARIO_INICIAL_CONTRASENA` | Contrasena privada de entre 8 y 100 caracteres; preferir una larga y unica |
| `APP_USUARIO_INICIAL_NOMBRE` | Nombre visible del administrador; opcional |

No hay una contrasena universal de acceso. Sin una contrasena inicial valida,
una base sin administrador no puede completar el inicio. Cuando ya existe un
administrador activo, esta configuracion no cambia su contrasena.

En PowerShell, se pueden definir variables solo para la terminal actual. Este
ejemplo pide las contrasenas sin escribirlas en el comando ni mostrarlas:

```powershell
$env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:5432/laferreteria_demo'
$env:SPRING_DATASOURCE_USERNAME = 'laferreteria_app'
$claveBase = Read-Host 'Contrasena de PostgreSQL' -AsSecureString
$env:SPRING_DATASOURCE_PASSWORD = [System.Net.NetworkCredential]::new('', $claveBase).Password
$env:APP_USUARIO_INICIAL_USUARIO = 'administrador'
$claveAdmin = Read-Host 'Contrasena del administrador inicial' -AsSecureString
$env:APP_USUARIO_INICIAL_CONTRASENA = [System.Net.NetworkCredential]::new('', $claveAdmin).Password
```

Despues ejecutar la compilacion y el inicio desde esa misma terminal, como
indica el README. Las variables son locales a ese proceso y sus hijos; no
configuran los accesos directos del escritorio. Cerrar la terminal al terminar.

Para utilizar los accesos directos, configurar las variables en
"Variables de entorno de la cuenta" en Windows antes de abrir la aplicacion.
No guardarlas en archivos que se suban a GitHub. Una variable de entorno no es
un almacen cifrado: proteger tambien la cuenta y el equipo.

Un archivo `.env` no se carga automaticamente en este proyecto. No poner
contrasenas en variables `VITE_*`: forman parte del frontend que ve el navegador.

## 3. Compilar y abrir en Windows

Desde `backend`, ejecutar `.\gradlew.bat bootJar`. Desde la carpeta principal,
`Crear accesos directos.bat` genera los accesos para abrir y cerrar.

Los accesos utilizan Java y buscan el JAR compilado en `backend/build/libs`.
Los puertos habituales son 9092 para la aplicacion y 5173 para desarrollo React.
No ejecutar estos iniciadores mientras otra instalacion este usando el 9092:
podrian abrir la instalacion que ya esta en marcha.

## 4. Respaldos e impresora

Configurar `APP_COPIAS_SEGURIDAD_PG_DUMP` con la ruta local de `pg_dump.exe` si
PostgreSQL no esta en la ubicacion predeterminada. Los respaldos se generan en
`backend/backups-react` al iniciar desde `backend` y estan excluidos de Git.
Eso no los copia a otro disco: conservar respaldos externos por separado.

La impresora se elige dentro de la aplicacion, una vez instalado su controlador
en Windows. No se distribuye la configuracion de la impresora de otro equipo.

El ticket de esta copia muestra domicilio y telefono de ejemplo. Para una
instalacion personalizada, revisar `TicketLayout.java` en el paquete `printing`
y `frontend/src/components/PrinterSettingsModal.jsx`; mantener coherente la
prueba de encabezado en `TicketPrintingTests.java`. No publicar datos privados
del cliente al personalizarlos.

## 5. Pruebas sin datos reales

Las pruebas normales utilizan H2 en memoria. Ejecutarlas sin variables
`SPRING_DATASOURCE_*` del negocio, para evitar que el entorno sobrescriba su
configuracion. Tampoco configurar `TEST_POSTGRES_*` para la ejecucion normal.

La prueba opcional `UnidadMedidaPostgresTests` solo se activa con
`TEST_POSTGRES_URL`; tambien requiere `TEST_POSTGRES_USER` y
`TEST_POSTGRES_PASSWORD`. Crea y elimina un esquema temporal. Usar exclusivamente
una base de pruebas dedicada y un usuario con permiso de crear esquemas.
