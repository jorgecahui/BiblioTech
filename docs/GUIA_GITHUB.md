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

Las pruebas usan repositorios simulados o el perfil `test`, con H2 en memoria; no requieren una base de datos PostgreSQL externa en GitHub Actions.

## Actividad 1: preparación y publicación de ramas

El remoto `origin` quedó configurado como `https://github.com/jorgecahui/BiblioTech.git`. Se creó el commit inicial `3421075` (`chore: inicializar BiblioTech`), que contiene solo `.gitignore`, y las ramas `main`, `develop` y `feature/nueva-funcionalidad`. La rama funcional se creó desde `develop`. Las tres ramas están publicadas en [GitHub](https://github.com/jorgecahui/BiblioTech).

El PDF solicita trabajar con **GitHub Desktop**. La preparación y publicación reales se hicieron mediante terminal y API de GitHub. No se realizó la actividad con Desktop y sus capturas quedan pendientes; no se deben recrear las ramas ni el commit inicial. Estos comandos documentan la preparación ya realizada:

```powershell
git add .gitignore
git commit -m "chore: inicializar BiblioTech"
git branch -M main
git switch -c develop
git switch -c feature/nueva-funcionalidad
```

Para revisar el estado equivalente en GitHub Desktop, agrega la carpeta local de BiblioTech y comprueba el remoto, las ramas y el historial existentes. No agregues otro `origin`.

## Actividad 2: funcionalidad implementada

Se implementó el máximo de tres préstamos activos. El servicio comprueba la frontera `>= 3`, vuelve a validar el cupo durante el registro y utiliza una transacción con bloqueos para proteger solicitudes simultáneas. La API responde HTTP 409 al alcanzar el límite. La interfaz muestra el rechazo, valida los identificadores y evita envíos repetidos.

La aplicación, la funcionalidad, las pruebas, el flujo CI y los documentos se guardaron en el commit [**`c449dbd`**](https://github.com/jorgecahui/BiblioTech/commit/c449dbddf582c8b91b6e43dabe21161697bcb1c2). Como las ramas base contienen únicamente `.gitignore`, el primer pull request incorpora también la aplicación existente.

## Actividad 3: pruebas y compilación locales

La verificación del backend terminó en **BUILD SUCCESS** el 22/10/2026 a las 15:48:32 (Lima), con **27 pruebas, 0 fallos y 0 errores**. La interfaz aprobó **24 pruebas** y compiló correctamente. El registro del backend está en [backend-success.log](evidencias/backend-success.log).

También se comprobó el fallo controlado en una copia aislada: al sustituir `>=` por `>` dentro de `puedePrestar`, falló el caso con tres préstamos; al restaurar `>=`, las 18 pruebas unitarias volvieron a pasar. El [informe](INFORME_PRACTICA_CI.md) conserva los detalles y registros locales.

Si se modifica el código o las pruebas, repite la verificación correspondiente antes de publicar:

```powershell
mvn -B verify --file bibliotech_v2/pom.xml
```

## Actividad 4: pull request y CI correcto

Se publicaron `main`, `develop` y `feature/nueva-funcionalidad`, y se abrió el [**pull request #1**](https://github.com/jorgecahui/BiblioTech/pull/1) con **base `develop`** y **compare `feature/nueva-funcionalidad`**. Permanece abierto para revisión, sin merge.

Los comandos de publicación utilizados tienen esta equivalencia:

```powershell
git push -u origin main
git push -u origin develop
git push -u origin feature/nueva-funcionalidad
```

GitHub Actions comprobó el backend y terminó en **Success** en ambas ejecuciones:

- [CI del push de la funcionalidad](https://github.com/jorgecahui/BiblioTech/actions/runs/39853181208).
- [CI del pull request](https://github.com/jorgecahui/BiblioTech/actions/runs/39853394870).

Se conservan la [captura del resultado del PR](evidencias/github/ci-pr-success.jpg) y los [registros de la ejecución correcta](evidencias/github/actions-success-inicial.zip). La revisión independiente e integración del PR quedan pendientes. No se registra una aprobación ni un merge inexistentes.

## Actividad 5: demostración remota del error y su corrección

Se creó **`practica/fallo-limite`** desde la versión publicada y validada de **`feature/nueva-funcionalidad`**. Se empleó esa base porque el PR hacia `develop` permanece abierto para revisión. La demostración quedó separada de la rama funcional y de las ramas base.

La creación de la rama tiene esta equivalencia en terminal:

```powershell
git switch feature/nueva-funcionalidad
git switch -c practica/fallo-limite
```

Dentro de `puedePrestar` en `bibliotech_v2/src/main/java/upeu/bibliotech_v2/service/PrestamoService.java`, se cambió únicamente el operador de comparación del límite. Se mantuvieron `MAX_PRESTAMOS_ACTIVOS = 3` y la validación adicional de `registrarPrestamo`:

```java
// Regla correcta:
if (prestamosActivos >= MAX_PRESTAMOS_ACTIVOS) {
// Cambio deliberado, en sustitución de la línea anterior:
if (prestamosActivos > MAX_PRESTAMOS_ACTIVOS) {
```

Estas son dos alternativas de la misma línea, no dos sentencias que deban copiarse juntas. Se publicó el commit de error [**`34a1f9d`**](https://github.com/jorgecahui/BiblioTech/commit/34a1f9d9bfcd9024c87f92e36c48f3cfc9ac9897), que activó la [ejecución #3](https://github.com/jorgecahui/BiblioTech/actions/runs/39853501576). Comenzó el 22/10/2026 a las **15:59:15** y terminó a las **15:59:55** (America/Lima) en **Failure**.

El reporte oficial de Surefire registró **27 pruebas, 3 fallos, 0 errores y 0 omitidas**. Fallaron el caso unitario de frontera con tres préstamos y las pruebas de integración `devolverUnPrestamoLiberaCupo` y `permiteElTerceroYRechazaElCuartoSinDescontarOtroEjemplar`. Las tres aserciones indicaron `expected: <false> but was: <true>`. Así se comprobó que CI detectó el defecto de la regla de negocio. Se conservaron la [captura Failure](evidencias/github/ci-failure.jpg), los [registros](evidencias/github/actions-failure.zip) y el [artefacto oficial de reportes](evidencias/github/reportes-failure.zip).

Se restauró `>=` en la misma rama y se publicó el commit de corrección [**`ee44be3`**](https://github.com/jorgecahui/BiblioTech/commit/ee44be3a4abbc75fe4a686f84735baacd08e73c1). La [ejecución #4](https://github.com/jorgecahui/BiblioTech/actions/runs/39853717419) comenzó a las **16:00:44** y terminó a las **16:01:25** (America/Lima) en **Success**. El reporte oficial descargado verificó **27 pruebas, 0 fallos, 0 errores y 0 omitidas**. Se conservaron la [captura Success](evidencias/github/ci-success-corregido.jpg), los [registros](evidencias/github/actions-success-corregido.zip) y los [reportes oficiales](evidencias/github/reportes-success-corregido.zip). Se mantuvieron las expectativas originales de las pruebas.

La rama de demostración quedó corregida y no necesita integrarse. La implementación correcta continúa en la rama funcional validada y en el PR #1, abierto para revisión. El fallo remoto corresponde a aserciones de frontera, no a un problema de descarga, configuración de Java o compilación.

## Registro de evidencias de GitHub

| Evidencia | Enlace o archivo | Estado real |
| --- | --- | --- |
| Repositorio y tres ramas publicadas | [jorgecahui/BiblioTech](https://github.com/jorgecahui/BiblioTech) | Publicadas |
| Commit de la funcionalidad | [c449dbd](https://github.com/jorgecahui/BiblioTech/commit/c449dbddf582c8b91b6e43dabe21161697bcb1c2) | Publicado |
| Pull request hacia develop | [PR #1](https://github.com/jorgecahui/BiblioTech/pull/1) | Abierto para revisión, sin merge |
| CI correcto de la funcionalidad | [Push](https://github.com/jorgecahui/BiblioTech/actions/runs/39853181208), [PR](https://github.com/jorgecahui/BiblioTech/actions/runs/39853394870), [captura](evidencias/github/ci-pr-success.jpg) | Success |
| Registros del CI correcto | [actions-success-inicial.zip](evidencias/github/actions-success-inicial.zip) | Conservados |
| CI con error `>` | [Commit 34a1f9d](https://github.com/jorgecahui/BiblioTech/commit/34a1f9d9bfcd9024c87f92e36c48f3cfc9ac9897), [Actions](https://github.com/jorgecahui/BiblioTech/actions/runs/39853501576), [captura](evidencias/github/ci-failure.jpg) | Failure; 27 pruebas, 3 fallos, 0 errores |
| Reportes oficiales del fallo | [reportes-failure.zip](evidencias/github/reportes-failure.zip), [registros](evidencias/github/actions-failure.zip) | Descargados y conservados |
| CI después de restaurar `>=` | [Commit ee44be3](https://github.com/jorgecahui/BiblioTech/commit/ee44be3a4abbc75fe4a686f84735baacd08e73c1), [Actions](https://github.com/jorgecahui/BiblioTech/actions/runs/39853717419), [captura](evidencias/github/ci-success-corregido.jpg) | Success; 27 pruebas, 0 fallos, 0 errores |
| Reportes oficiales de la corrección | [reportes-success-corregido.zip](evidencias/github/reportes-success-corregido.zip), [registros](evidencias/github/actions-success-corregido.zip) | Descargados y verificados |
| Resumen de conteos JUnit remotos | [resumen-junit-remoto.json](evidencias/github/resumen-junit-remoto.json) | Conservado |
| Revisión independiente e integración del pull request | [PR #1](https://github.com/jorgecahui/BiblioTech/pull/1) | Pendientes |
| Uso y capturas de GitHub Desktop | Pendientes | Las operaciones realizadas usaron terminal y API de GitHub |
