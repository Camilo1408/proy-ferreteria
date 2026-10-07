/**
 * nombre: Chips.jsx
 * descripcion: Etiquetas de estado reutilizables (nivel de stock, estado de producto, tipo de movimiento, estado de alerta).
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
import { Chip } from '@mui/material';
import { useTranslation } from 'react-i18next';

/** Nivel de stock: normal, bajo o agotado. El texto (no solo el color) transmite el estado. */
export function NivelChip({ nivel }) {
  const { t } = useTranslation();
  const color = { OK: 'success', BAJO: 'warning', AGOTADO: 'error' }[nivel] ?? 'default';
  return <Chip size="small" variant={nivel === 'OK' ? 'outlined' : 'filled'} color={color} label={t(`niveles.${nivel}`)} />;
}

/** Estado de un producto. */
export function EstadoChip({ estado }) {
  const { t } = useTranslation();
  return <Chip size="small" variant="outlined" color={estado === 'ACTIVO' ? 'success' : 'default'} label={t(`productos.estados.${estado}`)} />;
}

/** Tipo de movimiento. */
export function TipoChip({ tipo }) {
  const { t } = useTranslation();
  const color = { ENTRADA: 'success', SALIDA: 'primary', AJUSTE: 'secondary' }[tipo] ?? 'default';
  return <Chip size="small" variant="outlined" color={color} label={t(`movimientos.tipos.${tipo}`)} />;
}

/** Estado de una alerta. */
export function EstadoAlertaChip({ estado }) {
  const { t } = useTranslation();
  const color = { ABIERTA: 'error', RECONOCIDA: 'warning', RESUELTA: 'success' }[estado] ?? 'default';
  return <Chip size="small" variant="outlined" color={color} label={t(`alertas.estados.${estado}`)} />;
}
