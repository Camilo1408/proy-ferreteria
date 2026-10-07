/**
 * nombre: schemas.js
 * descripcion: Esquemas Yup que replican las reglas de validación del back-end.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
 */
import * as Yup from 'yup';

/**
 * Esquema del formulario de producto. Los mensajes son claves de i18n traducidas al mostrar el error.
 * Reglas: nombre 3-100, categoría obligatoria hasta 60, descripción hasta 500.
 */
export const productoSchema = Yup.object({
  nombre: Yup.string().trim().required('validacion.nombreRequerido').min(3, 'validacion.nombreTamano').max(100, 'validacion.nombreTamano'),
  categoria: Yup.string().trim().required('validacion.categoriaRequerida').max(60, 'validacion.categoriaTamano'),
  descripcion: Yup.string().max(500, 'validacion.descripcionTamano'),
  estado: Yup.string().oneOf(['ACTIVO', 'INACTIVO']),
});

/** Esquema del formulario de inicio de sesión. */
export const loginSchema = Yup.object({
  username: Yup.string().trim().required('validacion.usuarioRequerido'),
  password: Yup.string().required('validacion.claveRequerida'),
});
