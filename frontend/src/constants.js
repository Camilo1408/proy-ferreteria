/**
 * nombre: constants.js
 * descripcion: Constantes compartidas: permisos, unidades, motivos de movimiento y módulos de permisos.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */

/** Permisos modulares (deben coincidir con el enum Permiso del back-end). */
export const P = {
  PRODUCTOS_VER: 'PRODUCTOS_VER',
  PRODUCTOS_GESTIONAR: 'PRODUCTOS_GESTIONAR',
  MOVIMIENTOS_VER: 'MOVIMIENTOS_VER',
  MOVIMIENTOS_REGISTRAR: 'MOVIMIENTOS_REGISTRAR',
  AJUSTES_REGISTRAR: 'AJUSTES_REGISTRAR',
  ALERTAS_VER: 'ALERTAS_VER',
  ALERTAS_GESTIONAR: 'ALERTAS_GESTIONAR',
  REPORTES_VER: 'REPORTES_VER',
  USUARIOS_GESTIONAR: 'USUARIOS_GESTIONAR',
  PERFILES_GESTIONAR: 'PERFILES_GESTIONAR',
};

/** Unidades de medida admitidas. */
export const UNIDADES = ['UND', 'KG', 'G', 'M', 'CM', 'L', 'ML', 'CAJA', 'PAQUETE', 'ROLLO'];

/** Motivos permitidos por tipo de movimiento. */
export const MOTIVOS = {
  ENTRADA: ['COMPRA', 'DEVOLUCION_CLIENTE', 'OTRA_ENTRADA'],
  SALIDA: ['VENTA', 'CONSUMO_INTERNO', 'MERMA', 'DEVOLUCION_PROVEEDOR', 'OTRA_SALIDA'],
  AJUSTE: ['CONTEO_FISICO', 'DANO', 'CORRECCION'],
};

/** Todos los motivos que puede mostrar el historial (incluye STOCK_INICIAL, generado por el sistema). */
export const MOTIVOS_HISTORIAL = [...MOTIVOS.ENTRADA, 'STOCK_INICIAL', ...MOTIVOS.SALIDA, ...MOTIVOS.AJUSTE];

/** Orden y nombre de los módulos en el formulario de perfiles. */
export const MODULOS = ['productos', 'inventario', 'alertas', 'reportes', 'administracion'];

/** Tipos de movimiento con el permiso que exige registrarlos. */
export const TIPOS = [
  { tipo: 'ENTRADA', permiso: P.MOVIMIENTOS_REGISTRAR },
  { tipo: 'SALIDA', permiso: P.MOVIMIENTOS_REGISTRAR },
  { tipo: 'AJUSTE', permiso: P.AJUSTES_REGISTRAR },
];
