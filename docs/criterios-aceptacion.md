# Criterios de aceptación

Cada criterio (CA) se verifica con al menos una prueba automática; la matriz completa está en [casos-prueba.md](casos-prueba.md).

| ID | Criterio |
|---|---|
| CA-01 | Crear un producto válido devuelve 201 con id y estado ACTIVO; aparece en el listado. |
| CA-02 | Un nombre de menos de 3 o más de 100 caracteres devuelve 400 (límites: 2 falla, 3 y 100 pasan, 101 falla). |
| CA-03 | Un nombre repetido, sin distinguir mayúsculas, devuelve 409 y no crea el producto. |
| CA-04 | Eliminar desactiva el producto (borrado lógico); el producto sigue consultable como INACTIVO y la interfaz pide confirmación. |
| CA-05 | El rol USER puede leer, pero crear, editar y desactivar devuelven 403; la interfaz no muestra esos controles. |
| CA-06 | Sin token, con token inválido o expirado se recibe 401; la interfaz regresa al login. |
| CA-07 | Los mensajes de la API y de la interfaz salen en el idioma pedido (es por defecto, en). |
| CA-08 | El login correcto entrega un token; credenciales incorrectas devuelven 401 y campos vacíos 400. |
| CA-09 | La categoría es obligatoria (máx. 60) y la descripción admite hasta 500 caracteres (límites 500 pasa, 501 falla). |
| CA-10 | Actualizar cambia los datos; renombrar a un nombre ajeno devuelve 409; un id inexistente devuelve 404. |
| CA-11 | El listado es paginado (máx. 100 por página), filtra por estado y busca por nombre o categoría; una página fuera de rango devuelve lista vacía. |
| CA-12 | JSON malformado devuelve 400 y la búsqueda no es vulnerable a inyección SQL. |
| CA-13 | La interfaz es responsive (tarjetas en móvil sin desbordamiento) y accesible (enlace de salto, etiquetas, foco visible, tema claro/oscuro). |
| CA-14 | Rendimiento: con 50 usuarios virtuales, p95 < 800 ms y errores < 1 %. |
| CA-15 | Calidad de pruebas: puntaje de mutación ≥ 70 % sobre la lógica de negocio. |
