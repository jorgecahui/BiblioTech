# Guía de integración continua: límite de préstamos

## Alcance

La funcionalidad elegida es **Límite de préstamos**: un estudiante con tres o más préstamos activos no puede obtener otro préstamo. Con cero, uno o dos préstamos activos puede solicitarlo si cumple las demás condiciones (estudiante activo y ejemplares disponibles).

El flujo de GitHub Actions está en `.github/workflows/ci.yml`. Usa Java 21 (Temurin), guarda la caché de Maven y ejecuta:

```powershell
mvn -B verify --file bibliotech_v2/pom.xml
```

La compilación y las pruebas se ejecutan para el backend. Los reportes de Surefire se publican como el artefacto `reportes-pruebas-backend`, incluso si las pruebas fallan, siempre que se hayan generado. Se conservan durante 14 días. La ausencia de reportes produce una advertencia: un error anterior a la ejecución de pruebas puede impedir su generación.

Los eventos configurados son:

- Push a `main`, `develop`, `feature/**` y `practica/**`.
- Pull request cuyo destino sea `develop`.
- Ejecución manual desde la pestaña Actions, cuando el flujo esté disponible en la rama predeterminada.

Las pruebas unitarias de la regla usan repositorios simulados. La prueba de carga del contexto usa el perfil `test`, con H2 en memoria; no requiere una base de datos PostgreSQL externa en GitHub Actions.

## Estado y evidencias pendientes

El remoto `origin` **ya está configurado** como `https://github.com/jorgecahui/BiblioTech.git`. Se creó localmente el commit inicial `3421075` (`chore: inicializar BiblioTech`), que contiene solo `.gitignore`, y las ramas `main`, `develop` y `feature/nueva-funcionalidad`. La rama de funcionalidad se creó desde `develop` y es la rama de trabajo actual; contiene la aplicación y los entregables de esta práctica.

La consulta de referencias remotas no devolvió ramas. No se realizó ningún push, pull request ni ejecución de Actions. Estos pasos quedan pendientes de autorización para publicar.

La verificación local del backend terminó en **BUILD SUCCESS** el 22/10/2026 a las 15:48:32 (Lima), con **27 pruebas, 0 fallos y 0 errores**. La interfaz aprobó 24 pruebas y compiló correctamente. El registro del backend está en [backend-success.log](evidencias/backend-success.log). El [informe](INFORME_PRACTICA_CI.md) detalla también la demostración local del error y su corrección.

Una ejecución local de Maven demuestra el resultado local. Las marcas **Success** y **Failure**, los enlaces a ejecuciones, las revisiones y la integración del pull request solo se pueden acreditar después de publicar el repositorio y ejecutar el flujo en GitHub. Registra esos resultados con sus enlaces y capturas reales; no sustituyas las evidencias remotas por capturas de esta guía.

## Continuar desde las ramas locales existentes

El PDF solicita trabajar con **GitHub Desktop**. La preparación de las ramas locales se hizo mediante la terminal; no se presenta como una acción realizada con Desktop. Usa GitHub Desktop para revisar este estado y continuar la práctica. Los comandos incluidos son una equivalencia y no sustituyen las capturas solicitadas.

1. Agrega a GitHub Desktop la carpeta local de BiblioTech. Comprueba que el remoto corresponde a [jorgecahui/BiblioTech](https://github.com/jorgecahui/BiblioTech); no agregues otro `origin`.
2. Comprueba que existen `main`, `develop` y **`feature/nueva-funcionalidad`**, y que esta última es la rama seleccionada. No vuelvas a crear el commit inicial ni esas ramas.
3. Revisa Changes y el historial para comprobar qué archivos faltan por confirmar. El primer pull request debe incorporar la aplicación junto con la regla, sus pruebas, el flujo y los documentos; las ramas base solo contienen `.gitignore`.
4. Una vez autorizada la publicación, publica las ramas locales `main` y `develop` en el remoto existente. Conserva los cambios de la práctica en `feature/nueva-funcionalidad` y publícala después de confirmar su contenido.
5. Después de revisar e integrar la aplicación, promueve una versión validada a `main`. Si el entorno de GitHub lo permite, protege `develop` y exige el check `Backend - Java 21 y pruebas` antes de integrar.

## Publicar el pull request hacia develop

El pull request debe contener cambios reales de la funcionalidad y de sus pruebas. Como las ramas base contienen únicamente `.gitignore`, el primer pull request incorporará la aplicación existente junto con la funcionalidad, sus pruebas y el flujo CI. Todos los pasos de publicación de este apartado están pendientes de autorización explícita.

1. Consulta la verificación local ya registrada. Si modificas el código o las pruebas después, repite:

   ```powershell
   mvn -B verify --file bibliotech_v2/pom.xml
   ```

2. En GitHub Desktop, selecciona los cambios de la práctica, revisa el diff y crea el commit `feat: validar límite de tres préstamos activos`, si todavía no se ha creado. Con autorización de publicación, publica las tres ramas locales. Equivalencia en terminal:

   ```powershell
   git diff
   git add .
   git commit -m "feat: validar límite de tres préstamos activos"
   git push -u origin main
   git push -u origin develop
   git push -u origin feature/nueva-funcionalidad
   ```

3. Desde GitHub Desktop inicia la creación del pull request y comprueba en GitHub **base: develop** y **compare: feature/nueva-funcionalidad**.
4. Usa el título `feat: límite de préstamos activos`. Describe la regla de tres préstamos y los casos probados, incluidos dos préstamos permitidos y tres rechazados.
5. Espera el resultado de `CI - BiblioTech`. Abre el trabajo `Backend - Java 21 y pruebas`, revisa la salida de Maven y descarga los reportes desde Artifacts.
6. Solicita una revisión según la práctica. Integra solo después de que el flujo esté en **Success** y se hayan atendido las observaciones.

## Demostrar un fallo controlado y su corrección

Haz esta demostración en una rama independiente **después de disponer de una versión correcta publicada con sus pruebas**. Mantén el error fuera de `feature/nueva-funcionalidad`, `develop` y `main`; la rama de demostración no debe integrarse mientras esté defectuosa.

1. En GitHub Desktop, actualiza `develop` después de integrar la funcionalidad correcta y crea la rama `practica/fallo-limite`. Equivalencia en terminal:

   ```powershell
   git switch develop
   git pull --ff-only origin develop
   git switch -c practica/fallo-limite
   ```

2. En `bibliotech_v2/src/main/java/upeu/bibliotech_v2/service/PrestamoService.java`, dentro del método **`puedePrestar`**, cambia únicamente el operador de la comparación del límite:

   ```java
   // Correcto:
   if (prestamosActivos >= MAX_PRESTAMOS_ACTIVOS) {
   // Error deliberado para la demostración:
   if (prestamosActivos > MAX_PRESTAMOS_ACTIVOS) {
   ```

   Son dos alternativas de la misma línea: no copies ambas sentencias juntas. Conserva `MAX_PRESTAMOS_ACTIVOS = 3` y la validación adicional de `registrarPrestamo`.

3. Ejecuta Maven. El caso con tres préstamos de `rechazaValidarDesdeElTercerPrestamoActivo` debe fallar porque la validación defectuosa permite solicitar un cuarto préstamo. La validación de registro permanece intacta. No cambies la expectativa de la prueba para hacerla pasar.
4. En GitHub Desktop, confirma y publica únicamente la modificación deliberada. Equivalencia en terminal:

   ```powershell
   git add bibliotech_v2/src/main/java/upeu/bibliotech_v2/service/PrestamoService.java
   git commit -m "test: demostrar detección de error en límite de préstamos"
   git push -u origin practica/fallo-limite
   ```

5. En Actions, abre la ejecución de ese push y captura el estado **Failure**, la prueba fallida y el detalle de su aserción. Descarga los reportes para sustentar el resultado.
6. Restaura la comparación correcta `>=` en esa misma rama y vuelve a ejecutar Maven. Después confirma y publica la corrección con GitHub Desktop. Equivalencia en terminal:

   ```powershell
   git add bibliotech_v2/src/main/java/upeu/bibliotech_v2/service/PrestamoService.java
   git commit -m "fix: restablecer límite máximo de tres préstamos activos"
   git push
   ```

7. Captura la nueva ejecución en **Success**. Conserva los enlaces de ambos commits y ambas ejecuciones. No hace falta integrar la rama de demostración: la implementación correcta ya debe estar en `develop`.

Si GitHub falla al descargar dependencias, configurar Java o compilar, ese fallo no demuestra que las pruebas detectaron el error del límite. Resuelve primero el problema de infraestructura y repite la demostración hasta que el fallo corresponda a la aserción esperada.

## Registro de evidencias de GitHub

Completa esta tabla con resultados reales después de publicar:

| Evidencia | Enlace o archivo | Estado al preparar la práctica |
| --- | --- | --- |
| Repositorio y ramas publicadas | Pendiente | Pendiente en GitHub |
| Pull request hacia develop | Pendiente | Pendiente en GitHub |
| CI correcto de la funcionalidad | Pendiente | Pendiente en GitHub |
| CI con error `>` | Pendiente | Debe mostrar Failure por el límite exacto |
| CI después de restaurar `>=` | Pendiente | Debe mostrar Success |
| Reportes de Surefire descargados | Pendiente | Pendiente en GitHub |
| Revisión e integración del pull request | Pendiente | Pendiente en GitHub |

## Anexo: preparación local realizada

El repositorio comenzó sin historial. Se preparó un commit que incluye únicamente `.gitignore` y se crearon las ramas base y la rama de funcionalidad desde `develop`, mediante la terminal. Esta es una descripción de la preparación ya realizada; **no se debe repetir** sobre el estado actual:

```powershell
git add .gitignore
git commit -m "chore: inicializar BiblioTech"
git branch -M main
git switch -c develop
git switch -c feature/nueva-funcionalidad
```

No se agregó otro remoto y no se publicaron ramas. El uso de terminal para esta preparación debe declararse al presentar la práctica; las evidencias de GitHub Desktop siguen pendientes.
