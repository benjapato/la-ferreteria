# Verificación local

Fecha: 10 de octubre de 2026.

La versión preparada para el portfolio se validó en una copia temporal separada. Los 225 archivos de código y recursos de `frontend/src` y `backend/src` coincidieron con la entrega después de las pruebas.

## Resultados

| Comprobación | Resultado |
| --- | --- |
| Lint del frontend (`oxlint`) | Correcto; sin diagnósticos y salida 0. |
| Cantidades y redondeo (`node --test src/utils/quantities.test.js`) | 10 pruebas aprobadas; 0 fallos, 0 canceladas y 0 omitidas. |
| Compilación de producción (`vite build --base=/app/`) | Correcta; 1.844 módulos transformados. |
| Backend (`Gradle test`) | 113 pruebas aprobadas en 18 suites; 0 fallos, 0 errores y 0 omitidas dentro de las suites ejecutadas. |

Total: **123 pruebas aprobadas** entre frontend y backend.

El backend terminó con `BUILD SUCCESSFUL`. Usó la configuración de pruebas existente con **H2 en memoria**, comprobada también en la salida de ejecución. No se inició `bootRun` ni se conectó la base de datos del negocio.

`UnidadMedidaPostgresTests` contiene una prueba que requiere PostgreSQL real y modifica un esquema temporal. Se **excluyó explícitamente** de esta ejecución y se retiraron las variables `TEST_POSTGRES_*` del entorno de pruebas. Esta exclusión queda fuera del conteo de 113 y no aparece como una prueba omitida en los informes de Gradle.

Para evitar repetir instalaciones y compilaciones, Gradle reutilizó el frontend ya compilado en la copia temporal: se ejecutó `test` con un script de exclusión de PostgreSQL y con `-x frontendInstall -x frontendBuild`. La sincronización de los recursos del frontend sí se ejecutó.

## Entorno utilizado

- Windows, Java 21.0.10 y Gradle 9.5.1.
- Node.js 26.5.1 y pnpm 11.9.0.
- Dependencias instaladas desde el archivo de bloqueo con `--frozen-lockfile`.

Las versiones anteriores de Node.js no se probaron en esta ejecución.

## Capturas de interfaz

Las imágenes de [tablero](images/dashboard.png) y [catálogo](images/catalogo.png) tienen 1600 × 1100 píxeles y fueron revisadas visualmente. Se muestran 5 ventas recientes y 8 productos de ejemplo, con el usuario «Usuario demo» y la leyenda «Datos de demostración».

Se renderizó una copia local de la interfaz compilada. Todas las solicitudes `/api` se interceptaron con respuestas ficticias; no hubo inicio de sesión real ni conexiones a un backend. No se detectaron errores de renderizado, alertas o desbordes horizontales. El navegador y el servidor temporal se cerraron al finalizar.

Estas imágenes comprueban la presentación visual; no constituyen una prueba de integración completa entre navegador y backend.

## Alcance pendiente

No se validaron la conexión a PostgreSQL real, el despliegue en producción, una impresora física ni la creación o restauración de copias de una base de datos real. Las pruebas automatizadas de impresión usan simulaciones y archivos temporales.

Las dependencias, los compilados, los informes de ejecución y los archivos temporales de las pruebas quedaron fuera de la entrega. La revisión final no encontró carpetas `node_modules`, `build`, `dist`, `.gradle` o `logs`, ni archivos `.log`, `.backup` o `.dump` en el proyecto preparado para compartir.
