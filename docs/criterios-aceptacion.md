# Criterios de aceptación

CA-01 a CA-15 corresponden al CRUD de productos (fase 1); CA-16 a CA-57 al módulo de inventario (fase 2, detalle en [requisitos-inventario.md](requisitos-inventario.md)).

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
| CA-16 | (RF-01) Crear con código duplicado (sin distinguir mayúsculas) devuelve 409. |
| CA-17 | (RF-01) Código con formato inválido, unidad ausente o mínimo negativo devuelven 400. |
| CA-18 | (RF-01) El stock actual no se puede fijar por la edición del producto. |
| CA-19 | (RF-02) Crear con stock inicial 25 deja stock 25 y un movimiento ENTRADA con motivo STOCK_INICIAL. |
| CA-20 | (RF-03) Una entrada de 10 sobre stock 5 deja stock 15 y un movimiento con stock anterior 5 y resultante 15. |
| CA-21 | (RF-03) Cantidad 0, negativa o con más de 3 decimales devuelve 400. |
| CA-22 | (RF-03) Un motivo que no corresponde al tipo devuelve 400. |
| CA-23 | (RF-04) Salida mayor que el stock devuelve 409 `STOCK_INSUFICIENTE` y no cambia nada. |
| CA-24 | (RF-04) Salida igual al stock deja 0. |
| CA-25 | (RF-04) Dos salidas simultáneas de la última unidad: solo una se registra. |
| CA-26 | (RF-05) Contado 8 con stock 10 registra AJUSTE con variación −2 y stock 8. |
| CA-27 | (RF-05) Contado igual al stock devuelve 409; sin nota (mínimo 5 caracteres) devuelve 400. |
| CA-28 | (RF-06) No existe operación para editar ni borrar movimientos. |
| CA-29 | (RF-06) El historial se filtra por producto, tipo y rango de fechas y muestra usuario, fecha, stock anterior y resultante. |
| CA-30 | (RF-07) Entrada, salida o ajuste sobre un producto inactivo devuelve 409. |
| CA-31 | (RF-08) Una salida que lleva el stock al mínimo crea una alerta ABIERTA. |
| CA-32 | (RF-08) Si el stock llega a 0 el nivel pasa a AGOTADO. |
| CA-33 | (RF-08) No se duplican alertas por producto. |
| CA-34 | (RF-08) Al reponer el stock por encima del mínimo, la alerta pasa a RESUELTA sola. |
| CA-35 | (RF-08) Desactivar el producto resuelve su alerta. |
| CA-36 | (RF-09) Reconocer pasa a RECONOCIDA y deja de contar como pendiente. |
| CA-37 | (RF-09) Si luego el stock llega a 0, la alerta vuelve a ABIERTA. |
| CA-38 | (RF-10) `bajoMinimo=true` lista solo activos con stock ≤ mínimo. |
| CA-39 | (RF-10) El resumen de alertas devuelve pendientes, bajos y agotados. |
| CA-40 | (RF-11) Los números coinciden con los datos. |
| CA-41 | (RF-12) El CSV tiene encabezado traducido, escapa comillas y comas y neutraliza fórmulas (`=`, `+`, `-`, `@`). |
| CA-42 | (RF-13) Un perfil nuevo con permisos elegidos se puede asignar a un usuario. |
| CA-43 | (RF-13) Nombre de perfil duplicado devuelve 409. |
| CA-44 | (RF-13) El perfil ADMINISTRADOR no se edita ni se elimina. |
| CA-45 | (RF-13) Un perfil con usuarios no se elimina (409). |
| CA-46 | (RF-13) Un cambio que deje al sistema sin ningún usuario activo capaz de gestionar usuarios y perfiles se rechaza. |
| CA-47 | (RF-14) Crear usuario con perfil; usuario duplicado devuelve 409. |
| CA-48 | (RF-14) Un usuario desactivado no puede iniciar sesión ni usar un token anterior. |
| CA-49 | (RF-14) Nadie puede desactivarse ni cambiarse el perfil a sí mismo. |
| CA-50 | (RF-14) Contraseña de menos de 8 caracteres o sin letra y número devuelve 400. |
| CA-51 | (RF-15) Cada endpoint exige su permiso: sin él devuelve 403 y sin sesión 401. |
| CA-52 | (RF-15) Los cambios de permisos de un perfil rigen de inmediato en el siguiente request. |
| CA-53 | (RF-15) La interfaz solo muestra las secciones y acciones permitidas. |
| CA-54 | (RF-16) Cambiar contraseña con la actual incorrecta devuelve 400 y no cambia nada. |
| CA-55 | (RF-16) Tras cambiarla, la anterior ya no sirve y la nueva sí. |
| CA-56 | (RF-16) La nueva contraseña no puede ser igual a la actual. |
| CA-57 | (RF-17) Una base creada con la primera versión arranca con el sistema nuevo: conserva sus productos (código `LEG-n`, unidad UND, stock 0 y mínimo 0), elimina los usuarios antiguos (con rol) y los rehace con perfiles, y permite registrar movimientos sobre los productos heredados. |
