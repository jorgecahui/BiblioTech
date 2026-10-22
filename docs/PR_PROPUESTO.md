# feat: límite de tres préstamos activos

Destino: `develop`. Origen: `feature/nueva-funcionalidad`.

## Cambio

Un estudiante con dos préstamos activos puede registrar el tercero. Con tres o más, la API rechaza el registro con HTTP 409, explica el límite y conserva el inventario. El registro transaccional bloquea al estudiante antes de contar sus préstamos y bloquea el libro antes de descontar ejemplares para proteger las solicitudes simultáneas.

La interfaz muestra el máximo permitido y el detalle del rechazo, valida IDs positivos y evita envíos repetidos. Se añaden pruebas JUnit, pruebas Angular, un flujo de GitHub Actions para el backend y el informe de la práctica.

Este es el primer PR de un repositorio inicialmente vacío: también incorpora la aplicación base, porque `develop` solo contiene el commit inicial con `.gitignore`.

## Verificación local

- Backend: `mvn -B verify --file bibliotech_v2/pom.xml`, 27 pruebas aprobadas y ejecutable generado.
- Interfaz: `npm test -- --watch=false`, 24 pruebas aprobadas; `npm run build`, compilación correcta.
- Demostración aislada: al cambiar `>=` por `>` falla una aserción con tres préstamos activos; al restaurar `>=`, las 18 pruebas unitarias vuelven a pasar.

El error deliberado no forma parte de esta rama. Las ejecuciones y capturas de GitHub Actions se añadirán después de publicar.
