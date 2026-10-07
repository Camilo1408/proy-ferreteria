/**
 * nombre: ProductoForm.jsx
 * descripcion: Diálogo para crear o editar un producto con Formik y Yup.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
 */
import { useState } from 'react';
import {
  Alert, Button, Dialog, DialogActions, DialogContent, DialogTitle, MenuItem, TextField,
} from '@mui/material';
import { useFormik } from 'formik';
import { useTranslation } from 'react-i18next';
import { productoSchema } from '../validation/schemas';

/**
 * @param {{abierto: boolean, producto: object|null, onCerrar: () => void, onGuardar: (v: object) => Promise<void>}} props
 *  `producto` null = crear; con valor = editar.
 */
export default function ProductoForm({ abierto, producto, onCerrar, onGuardar }) {
  const { t } = useTranslation();
  const [errorApi, setErrorApi] = useState('');
  const editando = Boolean(producto);
  const formik = useFormik({
    enableReinitialize: true,
    initialValues: {
      nombre: producto?.nombre ?? '',
      categoria: producto?.categoria ?? '',
      descripcion: producto?.descripcion ?? '',
      estado: producto?.estado ?? 'ACTIVO',
    },
    validationSchema: productoSchema,
    onSubmit: async (v, { setSubmitting }) => {
      setErrorApi('');
      try {
        await onGuardar({ ...v, nombre: v.nombre.trim(), categoria: v.categoria.trim() });
      } catch (e) {
        setErrorApi([e.message, ...(e.detalles ?? [])].join(' · '));
      } finally {
        setSubmitting(false);
      }
    },
  });
  const campo = (nombre) => ({
    id: nombre,
    name: nombre,
    value: formik.values[nombre],
    onChange: formik.handleChange,
    onBlur: formik.handleBlur,
    error: formik.touched[nombre] && Boolean(formik.errors[nombre]),
    helperText: formik.touched[nombre] && formik.errors[nombre] ? t(formik.errors[nombre]) : ' ',
    fullWidth: true,
    margin: 'dense',
  });
  return (
    <Dialog open={abierto} onClose={onCerrar} fullWidth maxWidth="sm" aria-labelledby="producto-titulo">
      <form onSubmit={formik.handleSubmit} noValidate>
        <DialogTitle id="producto-titulo">{editando ? t('form.editarTitulo') : t('form.crear')}</DialogTitle>
        <DialogContent>
          <div role="alert" aria-live="assertive">
            {errorApi && <Alert severity="error" sx={{ mb: 1 }}>{errorApi}</Alert>}
          </div>
          <TextField {...campo('nombre')} label={t('form.nombre')} autoFocus required inputProps={{ maxLength: 100 }} />
          <TextField {...campo('categoria')} label={t('form.categoria')} required inputProps={{ maxLength: 60 }} />
          <TextField {...campo('descripcion')} label={t('form.descripcion')} multiline minRows={3} />
          {editando && (
            <TextField {...campo('estado')} label={t('form.estado')} select>
              <MenuItem value="ACTIVO">{t('productos.estados.ACTIVO')}</MenuItem>
              <MenuItem value="INACTIVO">{t('productos.estados.INACTIVO')}</MenuItem>
            </TextField>
          )}
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={onCerrar}>{t('form.cancelar')}</Button>
          <Button type="submit" variant="contained" disabled={formik.isSubmitting}>{t('form.guardar')}</Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
