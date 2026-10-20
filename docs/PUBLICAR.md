# Publicar esta copia en GitHub

Usar solamente esta carpeta preparada, no la carpeta de la instalacion del
negocio. No arrastrar el contenido original completo a la web de GitHub.

## Que se conserva

- Codigo Java y React, recursos de la interfaz y pruebas.
- Gradle Wrapper, configuracion de compilacion y archivo de versiones de pnpm.
- Iniciadores de Windows, sin configuracion privada del equipo.
- Documentacion de configuracion sin contrasenas reales.

## Que se deja afuera

- Base de datos, respaldos y archivos de importacion de productos.
- Documentos del cliente, capturas de uso y manual Word.
- Registros, configuracion de impresora y archivos de trabajo.
- Dependencias instaladas, compilaciones y accesos directos personales.
- Contrasenas predeterminadas de la instalacion original.
- Domicilio y telefono reales del comercio.

No agregar estos archivos ni siquiera a un repositorio privado. La carpeta
`.aplicacion` SI forma parte del codigo; `.runtime` NO.

## Revision final

1. Comprobar que se esta abriendo esta carpeta en GitHub Desktop o en Git.
2. Revisar cada archivo que aparece como nuevo o modificado antes de confirmar.
3. Conservar `.gitignore`, `pnpm-lock.yaml` y el JAR del Gradle Wrapper.
4. Mantener fuera todas las credenciales y los archivos de clientes.
5. Elegir repositorio privado cuando no se quiera compartir el codigo con todos.
6. Solo despues de revisar, crear el primer commit y publicar.

Las credenciales ficticias de pruebas se conservan para ejecutar los tests.
No usarlas en instalaciones reales. No se eligio una licencia de distribucion:
esa decision corresponde al titular del proyecto.

El `.gitignore` no elimina archivos que ya fueron agregados a Git ni limpia el
historial. Si alguna contrasena real ya se compartio, cambiarla primero y revisar
tambien el historial y las copias existentes.

Referencias:

- https://docs.github.com/en/get-started/git-basics/ignoring-files
- https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/removing-sensitive-data-from-a-repository
