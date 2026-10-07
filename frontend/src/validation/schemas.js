/**
 * nombre: schemas.js
 * descripcion: Esquemas Yup que replican las reglas de validación del back-end.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
import * as Yup from 'yup';
import { MOTIVOS, UNIDADES } from '../constants';
import { aNumero } from '../utils/format';

/** Cantidad con hasta 11 enteros y 3 decimales (coma o punto). */
export const PATRON_CANTIDAD = /^\d{1,11}([.,]\d{1,3})?$/;
/** 8 a 72 caracteres, sin espacios, con al menos una letra y un número. */
export const PATRON_CLAVE = /^(?=.*[A-Za-z])(?=.*\d)\S{8,72}$/;

/** Cantidad opcional (vacía o con formato válido y no negativa). */
const cantidadOpcional = Yup.string().trim().test('formato', 'validacion.cantidadFormato', (v) => !v || PATRON_CANTIDAD.test(v));

/**
 * Esquema del formulario de producto. Los mensajes son claves de i18n traducidas al mostrar el error.
 * Reglas: código 2-30, nombre 3-100, categoría obligatoria hasta 60, descripción hasta 500, unidad válida,
 * mínimo e inicial opcionales con hasta 3 decimales.
 */
export const productoSchema = Yup.object({
  codigo: Yup.string().trim().required('validacion.codigoRequerido').matches(/^[A-Za-z0-9._-]{2,30}$/, 'validacion.codigoFormato'),
  nombre: Yup.string().trim().required('validacion.nombreRequerido').min(3, 'validacion.nombreTamano').max(100, 'validacion.nombreTamano'),
  categoria: Yup.string().trim().required('validacion.categoriaRequerida').max(60, 'validacion.categoriaTamano'),
  descripcion: Yup.string().max(500, 'validacion.descripcionTamano'),
  unidad: Yup.string().required('validacion.unidadRequerida').oneOf(UNIDADES, 'validacion.unidadRequerida'),
  stockMinimo: cantidadOpcional,
  stockInicial: cantidadOpcional,
  estado: Yup.string().oneOf(['ACTIVO', 'INACTIVO']),
});

/** Esquema del formulario de inicio de sesión. */
export const loginSchema = Yup.object({
  username: Yup.string().trim().required('validacion.usuarioRequerido'),
  password: Yup.string().required('validacion.claveRequerida'),
});

/**
 * Esquema del formulario de movimiento. Entrada y salida piden `cantidad` (> 0); el ajuste pide `stockContado`
 * (>= 0) y una nota de al menos 5 caracteres. El motivo debe corresponder al tipo.
 */
export const movimientoSchema = Yup.object({
  tipo: Yup.string().oneOf(['ENTRADA', 'SALIDA', 'AJUSTE']).required(),
  productoId: Yup.number().typeError('validacion.productoRequerido').required('validacion.productoRequerido'),
  cantidad: Yup.string().when('tipo', {
    is: (t) => t !== 'AJUSTE',
    then: (s) => s.trim().required('validacion.cantidadRequerida')
      .matches(PATRON_CANTIDAD, 'validacion.cantidadFormato')
      .test('positiva', 'validacion.cantidadPositiva', (v) => !v || aNumero(v) > 0),
    otherwise: (s) => s.strip(),
  }),
  stockContado: Yup.string().when('tipo', {
    is: 'AJUSTE',
    then: (s) => s.trim().required('validacion.contadoRequerido').matches(PATRON_CANTIDAD, 'validacion.cantidadFormato'),
    otherwise: (s) => s.strip(),
  }),
  motivo: Yup.string().required('validacion.motivoRequerido').test('motivo-tipo', 'validacion.motivoRequerido', function (v) {
    return !v || (MOTIVOS[this.parent.tipo] ?? []).includes(v);
  }),
  referencia: Yup.string().max(40, 'validacion.referenciaTamano'),
  nota: Yup.string().max(300, 'validacion.notaTamano').when('tipo', {
    is: 'AJUSTE',
    then: (s) => s.trim().required('validacion.notaAjuste').min(5, 'validacion.notaAjuste'),
  }),
});

const nombreCompleto = Yup.string().trim().required('validacion.nombreCompletoRequerido').min(2, 'validacion.nombreCompletoTamano').max(100, 'validacion.nombreCompletoTamano');
const email = Yup.string().trim().email('validacion.emailFormato').max(120, 'validacion.emailTamano');
const claveNueva = Yup.string().required('validacion.claveRequerida').matches(PATRON_CLAVE, 'validacion.claveFormato');

/** Alta de usuario. */
export const usuarioCrearSchema = Yup.object({
  username: Yup.string().trim().required('validacion.usernameRequerido').matches(/^[A-Za-z0-9._-]{3,50}$/, 'validacion.usernameFormato'),
  password: claveNueva,
  nombreCompleto,
  email,
  perfilId: Yup.number().typeError('validacion.perfilRequerido').required('validacion.perfilRequerido'),
});

/** Edición de usuario por un administrador. */
export const usuarioEditarSchema = Yup.object({
  nombreCompleto,
  email,
  perfilId: Yup.number().typeError('validacion.perfilRequerido').required('validacion.perfilRequerido'),
  activo: Yup.boolean().required(),
});

/** Restablecimiento de contraseña por un administrador. */
export const restablecerClaveSchema = Yup.object({ nueva: claveNueva });

/** Alta o edición de perfil: al menos un permiso. */
export const perfilSchema = Yup.object({
  nombre: Yup.string().trim().required('validacion.perfilNombreRequerido').min(3, 'validacion.perfilNombreTamano').max(60, 'validacion.perfilNombreTamano'),
  descripcion: Yup.string().max(200, 'validacion.perfilDescripcionTamano'),
  permisos: Yup.array().of(Yup.string()).min(1, 'validacion.permisosRequeridos'),
});

/** Edición de nombre y correo propios. */
export const cuentaSchema = Yup.object({ nombreCompleto, email });

/** Cambio de contraseña propio: la nueva debe cumplir la política, ser distinta y estar confirmada. */
export const cambioClaveSchema = Yup.object({
  actual: Yup.string().required('validacion.claveActualRequerida'),
  nueva: claveNueva.notOneOf([Yup.ref('actual')], 'validacion.claveIgual'),
  confirmar: Yup.string().required('validacion.confirmarRequerida').oneOf([Yup.ref('nueva')], 'validacion.confirmarDistinta'),
});
