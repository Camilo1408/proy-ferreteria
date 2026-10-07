/**
 * nombre: ProductoForm.jsx
 * descripcion: Diálogo para crear o editar un producto con Formik y Yup.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
import { useState } from 'react';
import {
  Alert, Box, Button, Dialog, DialogActions, DialogContent, DialogTitle, MenuItem, TextField,
} from '@mui/material';
import { useFormik } from 'formik';
import { useTranslation } from 'react-i18next';
import { UNIDADES } from '../constants';
import { aNumero } from '../utils/format';
import { productoSchema } from '../validation/schemas';

/** Convierte los valores del formulario en el cuerpo que espera la API. */
export function aCuerpoProducto(v, editando) {
  const cuerpo = {
    codigo: v.codigo.trim(),
    nombre: v.nombre.trim(),
    categoria: v.categoria.trim(),
    descripcion: v.descripcion,
    unidad: v.unidad,
    stockMinimo: v.stockMinimo.trim() === '' ? 0 : aNumero(v.stockMinimo),
  };
  if (editando) cuerpo.estado = v.estado;
  else if (v.stockInicial.trim() !== '') cuerpo.stockInicial = aNumero(v.stockInicial);
  return cuerpo;
}

/**
 * @param {{abierto: boolean, producto: object|null, onCerrar: () => void, onGuardar: (cuerpo: object) => Promise<void>}} props
 *  `producto` null = crear; con valor = editar (el stock actual no se edita aquí: cambia con movimientos).
 */
export default function ProductoForm({ abierto, producto, onCerrar, onGuardar }) {
  const { t } = useTranslation();
  const [errorApi, setErrorApi] = useState('');
  const editando = Boolean(producto);
  const formik = useFormik({
    enableReinitialize: true,
    initialValues: {
      codigo: producto?.codigo ?? '',
      nombre: producto?.nombre ?? '',
      categoria: producto?.categoria ?? '',
      descripcion: producto?.descripcion ?? '',
      unidad: producto?.unidad ?? 'UND',
      stockMinimo: producto ? String(Number(producto.stockMinimo)) : '',
      stockInicial: '',
      estado: producto?.estado ?? 'ACTIVO',
    },
    validationSchema: productoSchema,
    onSubmit: async (v, { setSubmitting }) => {
      setErrorApi('');
      try {
        await onGuardar(aCuerpoProducto(v, editando));
      } catch (e) {
        setErrorApi([e.message, ...(e.detalles ?? [])].join(' · '));
      } finally {
        setSubmitting(false);
      }
    },
  });
  const campo = (nombre, ayuda) => ({
    id: nombre,
    name: nombre,
    value: formik.values[nombre],
    onChange: formik.handleChange,
    onBlur: formik.handleBlur,
    error: formik.touched[nombre] && Boolean(formik.errors[nombre]),
    helperText: formik.touched[nombre] && formik.errors[nombre] ? t(formik.errors[nombre]) : ayuda ?? ' ',
    fullWidth: true,
    margin: 'dense',
  });
  return (
    <Dialog open={abierto} onClose={onCerrar} fullWidth maxWidth="sm" aria-labelledby="producto-titulo">
      <form onSubmit={formik.handleSubmit} noValidate>
        <DialogTitle id="producto-titulo">{editando ? t('form.editarTitulo') : t('form.crear')}</DialogTitle>
        <DialogContent>
          <div>{errorApi && <Alert severity="error" sx={{ mb: 1 }}>{errorApi}</Alert>}</div>
          <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', sm: '1fr 2fr' } }}>
            <TextField {...campo('codigo')} label={t('form.codigo')} autoFocus required inputProps={{ maxLength: 30 }} />
            <TextField {...campo('nombre')} label={t('form.nombre')} required inputProps={{ maxLength: 100 }} />
          </Box>
          <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' } }}>
            <TextField {...campo('categoria')} label={t('form.categoria')} required inputProps={{ maxLength: 60 }} />
            <TextField {...campo('unidad')} label={t('form.unidad')} select required>
              {UNIDADES.map((u) => <MenuItem key={u} value={u}>{u} · {t(`unidades.${u}`)}</MenuItem>)}
            </TextField>
          </Box>
          <TextField {...campo('descripcion')} label={t('form.descripcion')} multiline minRows={2} />
          <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' } }}>
            <TextField {...campo('stockMinimo', t('form.ayudaMinimo'))} label={t('form.stockMinimo')} inputProps={{ inputMode: 'decimal' }} />
            {editando ? (
              <TextField {...campo('estado')} label={t('form.estado')} select>
                <MenuItem value="ACTIVO">{t('productos.estados.ACTIVO')}</MenuItem>
                <MenuItem value="INACTIVO">{t('productos.estados.INACTIVO')}</MenuItem>
              </TextField>
            ) : (
              <TextField {...campo('stockInicial', t('form.ayudaInicial'))} label={t('form.stockInicial')} inputProps={{ inputMode: 'decimal' }} />
            )}
          </Box>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={onCerrar}>{t('form.cancelar')}</Button>
          <Button type="submit" variant="contained" disabled={formik.isSubmitting}>{t('form.guardar')}</Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
