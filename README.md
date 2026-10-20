# LaFerreteria

Aplicacion de gestion para una ferreteria: productos y stock, ventas, clientes,
cuentas corrientes, compras a proveedores, caja, reportes y copias de seguridad.
Incluye una interfaz React, un backend Spring Boot y un iniciador para Windows.

## Acerca de esta copia

Esta es una copia del codigo preparada para compartir. No incluye la base de
datos del negocio, respaldos, archivos Excel, manuales con capturas, registros de
uso ni contrasenas de la instalacion original. Los datos de domicilio y telefono
del ticket son ejemplos. La instalacion original no se modifica.

No es un instalador listo para abrir: primero hay que configurar una base propia
y compilar. Publicar el codigo en GitHub no publica la aplicacion en Internet.

## Estructura

- `backend/src/main/java`: controladores, servicios, entidades y repositorios.
- `backend/src/main/resources`: configuracion, plantillas y recursos.
- `backend/src/test`: pruebas automaticas y datos ficticios.
- `frontend/src`: pantallas y componentes React.
- `frontend/public`: iconos y recursos publicos.
- `.aplicacion`: iniciador, cierre y compilacion para Windows.
- `docs`: configuracion y recomendaciones para publicar.

## Requisitos

- JDK 21.
- PostgreSQL y una base dedicada. pgAdmin es opcional para administrarla.
- Node.js 22.12 o posterior compatible con Vite 8 y npm.
- Conexion a Internet para descargar dependencias la primera vez.
- En Windows, Edge o Chrome para la ventana de la aplicacion.

Se incluye Gradle Wrapper. La compilacion utiliza pnpm 11.9.0 mediante `npx`;
no es necesario instalar Gradle ni pnpm globalmente.

## Configuracion y ejecucion

Seguir [la guia de configuracion](docs/CONFIGURACION.md). No usar la base real del
negocio para probar esta copia.

Una vez definidas las variables de entorno, desde la carpeta `backend`:

```powershell
.\gradlew.bat bootJar
java -jar .\build\libs\LaFerreteriaReactBackend-0.0.1-SNAPSHOT.jar
```

Abrir <http://localhost:9092/app/>. El archivo JAR incluye React; no hace falta
mantener otro servidor frontend para usar la aplicacion compilada.

Para desarrollo, ejecutar `.\gradlew.bat bootRun` desde `backend` y, en otra
terminal dentro de `frontend`:

```powershell
npx.cmd --yes pnpm@11.9.0 install --frozen-lockfile
npx.cmd --yes pnpm@11.9.0 dev
```

El frontend de desarrollo esta en <http://localhost:5173>. No iniciar dos copias
al mismo tiempo en los mismos puertos.

## Pruebas

Desde `backend`:

```powershell
.\gradlew.bat test
```

Desde `frontend`:

```powershell
node --test src/utils/quantities.test.js
npx.cmd --yes pnpm@11.9.0 lint
```

Las pruebas de Spring usan H2 en memoria y credenciales ficticias definidas en
`src/test/resources`; esas credenciales no configuran la aplicacion normal.
La prueba adicional de PostgreSQL se omite si `TEST_POSTGRES_URL` no esta
definida. Ver las precauciones en la guia de configuracion.

## Antes de publicar

Consultar [PUBLICAR.md](docs/PUBLICAR.md). El `.gitignore` excluye archivos
privados y generados, pero no sustituye la revision de lo que se va a subir.
