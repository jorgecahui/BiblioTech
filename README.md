# BiblioTech - Práctica de integración continua

Funcionalidad elegida: **Límite de préstamos**. Cada estudiante puede tener un máximo de **3 préstamos activos**. Con dos puede registrar el tercero; con tres se rechaza otro registro mediante HTTP 409 y un mensaje que explica cómo liberar cupo.

## Entregables

- [Informe en PDF](output/pdf/Informe_Practica_CI_Limite_Prestamos.pdf).
- [Informe de la práctica](docs/INFORME_PRACTICA_CI.md).
- [Guía de GitHub Desktop, pull request y demostración Failure/Success](docs/GUIA_GITHUB.md).
- [Workflow de GitHub Actions](.github/workflows/ci.yml).
- [Pruebas JUnit del servicio](bibliotech_v2/src/test/java/upeu/bibliotech_v2/service/PrestamoServiceTest.java).
- [Pruebas de integración y concurrencia](bibliotech_v2/src/test/java/upeu/bibliotech_v2/service/PrestamoIntegrationTest.java).
- [Pruebas HTTP](bibliotech_v2/src/test/java/upeu/bibliotech_v2/Controller/PrestamoControllerTest.java).

## Backend

Requiere JDK 21 y Maven. Desde la raíz:

```powershell
mvn -B verify --file bibliotech_v2/pom.xml
```

Las pruebas usan H2 en memoria con el perfil `test`; no necesitan PostgreSQL ni credenciales. La aplicación de producción sigue usando PostgreSQL. Para iniciarla con tu configuración local:

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/bibliotech_db'
$env:DB_USER = 'postgres'
$env:DB_PASSWORD = 'tu_clave_local'
mvn spring-boot:run --file bibliotech_v2/pom.xml
```

El registro se ejecuta en una transacción y bloquea al estudiante antes de contar sus préstamos. También bloquea el libro antes de descontar inventario. Así se evita que dos solicitudes simultáneas consuman el mismo cupo. Los estados `ACTIVO` y `activo` se cuentan por igual; los devueltos y los de otros estudiantes no consumen su cupo.

## Frontend

Desde `bibliotech-v2fe/bibliotech-v2fe`:

```powershell
npm ci
npm test -- --watch=false
npm run build
npm start
```

Abrir `http://localhost:4200`. La API se espera en `http://localhost:8080`. Se permite ese origen local mediante CORS. Los IDs deben ser enteros positivos y el formulario se bloquea mientras espera respuesta. Al rechazar el registro por límite, muestra el detalle enviado por la API.

## Estado de la entrega

Las ramas locales `main`, `develop` y `feature/nueva-funcionalidad` están preparadas. El remoto configurado es [jorgecahui/BiblioTech](https://github.com/jorgecahui/BiblioTech). La publicación, el pull request y las capturas reales de GitHub Actions requieren autorización para publicar. Las evidencias locales de `docs/evidencias/` no sustituyen las ejecuciones remotas solicitadas por la práctica.
