/**
 * nombre: MovimientoDialog.jsx
 * descripcion: Diálogo para registrar una entrada, salida o ajuste de stock, con permisos por tipo.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
import { useState } from 'react';
import {
  Alert, Box, Button, Dialog, DialogActions, DialogContent, DialogTitle, MenuItem, TextField, ToggleButton,
  ToggleButtonGroup, Typography,
} from '@mui/material';
import { useFormik } from 'formik';
import { useTranslation } from 'react-i18next';
import { registrarMovimiento } from '../api/inventario';
import { useAuth } from '../auth/AuthContext';
import { MOTIVOS, TIPOS } from '../constants';
import { aNumero, formatoCantidad } from '../utils/format';
import { movimientoSchema } from '../validation/schemas';
import ProductoSelector from './ProductoSelector';

/** Cuerpo de la petición según el tipo (el ajuste informa el stock contado). */
export function aCuerpoMovimiento(v) {
  const base = { productoId: Number(v.productoId), motivo: v.motivo, nota: v.nota.trim() || null };
  if (v.tipo === 'AJUSTE') return { ...base, stockContado: aNumero(v.stockContado) };
  return { ...base, cantidad: aNumero(v.cantidad), referencia: v.referencia.trim() || null };
}

/**
 * @param {{abierto: boolean, producto?: object|null, tipoInicial?: string, onCerrar: () => void, onRegistrado: (mov: object) => void}} props
 *  Con `producto` el producto queda fijo; sin él se muestra el buscador. Solo ofrece los tipos que el perfil permite.
 */
export default function MovimientoDialog({ abierto, producto = null, tipoInicial = 'ENTRADA', onCerrar, onRegistrado }) {
  const { t, i18n } = useTranslation();
  const { tiene } = useAuth();
  const permitidos = TIPOS.filter((x) => tiene(x.permiso)).map((x) => x.tipo);
  const tipoPorDefecto = permitidos.includes(tipoInicial) ? tipoInicial : permitidos[0];
  const [elegido, setElegido] = useState(null);
  const [errorApi, setErrorApi] = useState('');
  const seleccionado = producto ?? elegido;

  const formik = useFormik({
    enableReinitialize: true,
    initialValues: {
      tipo: tipoPorDefecto ?? 'ENTRADA', productoId: producto?.id ?? '', cantidad: '', stockContado: '', motivo: '', referencia: '', nota: '',
    },
    validationSchema: movimientoSchema,
    onSubmit: async (v, { setSubmitting, resetForm }) => {
      setErrorApi('');
      try {
        const mov = await registrarMovimiento(v.tipo, aCuerpoMovimiento(v));
        resetForm();
        setElegido(null);
        onRegistrado(mov);
      } catch (e) {
        setErrorApi([e.message, ...(e.detalles ?? [])].join(' · '));
      } finally {
        setSubmitting(false);
      }
    },
  });

  const cambiarTipo = (_, nuevo) => {
    if (!nuevo) return;
    formik.setValues({ ...formik.values, tipo: nuevo, motivo: '', cantidad: '', stockContado: '' });
    formik.setTouched({});
    setErrorApi('');
  };
  const elegirProducto = (p) => {
    setElegido(p);
    formik.setFieldValue('productoId', p?.id ?? '');
  };
  const error = (n) => formik.touched[n] && Boolean(formik.errors[n]);
  const ayuda = (n) => (formik.touched[n] && formik.errors[n] ? t(formik.errors[n]) : ' ');
  const esAjuste = formik.values.tipo === 'AJUSTE';

  return (
    <Dialog
      open={abierto} onClose={onCerrar} fullWidth maxWidth="sm" aria-labelledby="mov-titulo"
      TransitionProps={{ onExited: () => { formik.resetForm(); setElegido(null); setErrorApi(''); } }}
    >
      <form onSubmit={formik.handleSubmit} noValidate>
        <DialogTitle id="mov-titulo">{t('movimientos.registrar')}</DialogTitle>
        <DialogContent>
          <div>{errorApi && <Alert severity="error" sx={{ mb: 1 }}>{errorApi}</Alert>}</div>
          <ToggleButtonGroup
            exclusive fullWidth size="small" value={formik.values.tipo} onChange={cambiarTipo}
            aria-label={t('movimientos.tipo')} sx={{ mb: 1 }}
          >
            {permitidos.map((tipo) => (
              <ToggleButton key={tipo} value={tipo}>{t(`movimientos.tipos.${tipo}`)}</ToggleButton>
            ))}
          </ToggleButtonGroup>

          {producto ? (
            <Typography sx={{ fontWeight: 600, mt: 1 }}>{producto.codigo} · {producto.nombre}</Typography>
          ) : (
            <ProductoSelector value={elegido} onChange={elegirProducto} error={error('productoId')} helperText={ayuda('productoId')} />
          )}
          {seleccionado && (
            <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
              {t('movimientos.stockActual', { stock: formatoCantidad(seleccionado.stockActual, i18n.language), unidad: seleccionado.unidad })}
            </Typography>
          )}

          <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, mt: 1 }}>
            {esAjuste ? (
              <TextField
                id="stockContado" name="stockContado" label={t('movimientos.stockContado')} required fullWidth margin="dense"
                value={formik.values.stockContado} onChange={formik.handleChange} onBlur={formik.handleBlur}
                error={error('stockContado')} helperText={ayuda('stockContado')} inputProps={{ inputMode: 'decimal' }}
              />
            ) : (
              <TextField
                id="cantidad" name="cantidad" label={t('movimientos.cantidad')} required fullWidth margin="dense"
                value={formik.values.cantidad} onChange={formik.handleChange} onBlur={formik.handleBlur}
                error={error('cantidad')} helperText={ayuda('cantidad')} inputProps={{ inputMode: 'decimal' }}
              />
            )}
            <TextField
              id="motivo" name="motivo" label={t('movimientos.motivo')} select required fullWidth margin="dense"
              value={formik.values.motivo} onChange={formik.handleChange} onBlur={formik.handleBlur}
              error={error('motivo')} helperText={ayuda('motivo')}
            >
              {(MOTIVOS[formik.values.tipo] ?? []).map((m) => <MenuItem key={m} value={m}>{t(`motivos.${m}`)}</MenuItem>)}
            </TextField>
          </Box>
          {!esAjuste && (
            <TextField
              id="referencia" name="referencia" label={t('movimientos.referencia')} fullWidth margin="dense"
              value={formik.values.referencia} onChange={formik.handleChange} onBlur={formik.handleBlur}
              error={error('referencia')} helperText={ayuda('referencia')} inputProps={{ maxLength: 40 }}
            />
          )}
          <TextField
            id="nota" name="nota" label={esAjuste ? t('movimientos.notaAjuste') : t('movimientos.nota')} fullWidth margin="dense"
            multiline minRows={2} required={esAjuste}
            value={formik.values.nota} onChange={formik.handleChange} onBlur={formik.handleBlur}
            error={error('nota')} helperText={ayuda('nota')} inputProps={{ maxLength: 300 }}
          />
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={onCerrar}>{t('form.cancelar')}</Button>
          <Button type="submit" variant="contained" disabled={formik.isSubmitting || permitidos.length === 0}>{t('form.guardar')}</Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
