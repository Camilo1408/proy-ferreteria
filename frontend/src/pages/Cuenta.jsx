/**
 * nombre: Cuenta.jsx
 * descripcion: Autogestión de la cuenta: datos personales, contraseña y permisos propios.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
import { useState } from 'react';
import { Alert, Box, Button, Chip, Paper, TextField, Typography } from '@mui/material';
import { useFormik } from 'formik';
import { useTranslation } from 'react-i18next';
import { actualizarCuenta, cambiarClave } from '../api/inventario';
import { useAuth } from '../auth/AuthContext';
import { useAviso } from '../context/AvisoContext';
import { cambioClaveSchema, cuentaSchema } from '../validation/schemas';

function campos(formik, t) {
  return (n, ayuda) => ({
    id: n, name: n, value: formik.values[n], onChange: formik.handleChange, onBlur: formik.handleBlur, fullWidth: true, margin: 'dense',
    error: formik.touched[n] && Boolean(formik.errors[n]),
    helperText: formik.touched[n] && formik.errors[n] ? t(formik.errors[n]) : ayuda ?? ' ',
  });
}

/** Datos propios: nombre y correo editables; usuario y perfil de solo lectura. */
function DatosPropios() {
  const { t } = useTranslation();
  const { usuario, refrescar } = useAuth();
  const { avisar } = useAviso();
  const [error, setError] = useState('');
  const formik = useFormik({
    enableReinitialize: true,
    initialValues: { nombreCompleto: usuario.nombreCompleto, email: usuario.email ?? '' },
    validationSchema: cuentaSchema,
    onSubmit: async (v, { setSubmitting }) => {
      setError('');
      try {
        await actualizarCuenta({ nombreCompleto: v.nombreCompleto.trim(), email: v.email.trim() || null });
        await refrescar();
        avisar(t('cuenta.guardado'));
      } catch (e) {
        setError(e.message);
      } finally {
        setSubmitting(false);
      }
    },
  });
  const campo = campos(formik, t);
  return (
    <Paper component="form" onSubmit={formik.handleSubmit} noValidate sx={{ p: 3 }} aria-labelledby="datos-titulo">
      <Typography id="datos-titulo" component="h2" variant="h2" sx={{ mb: 1 }}>{t('cuenta.datos')}</Typography>
      <div>{error && <Alert severity="error" sx={{ mb: 1 }}>{error}</Alert>}</div>
      <TextField label={t('cuenta.usuario')} value={usuario.username} fullWidth margin="dense" disabled helperText={' '} />
      <TextField label={t('cuenta.perfil')} value={usuario.perfil} fullWidth margin="dense" disabled helperText={' '} />
      <TextField {...campo('nombreCompleto')} label={t('cuenta.nombreCompleto')} required inputProps={{ maxLength: 100 }} />
      <TextField {...campo('email')} label={t('cuenta.email')} type="email" inputProps={{ maxLength: 120 }} />
      <Button type="submit" variant="contained" disabled={formik.isSubmitting}>{t('cuenta.guardar')}</Button>
    </Paper>
  );
}

/** Cambio de contraseña con confirmación. */
function CambioClave() {
  const { t } = useTranslation();
  const { avisar } = useAviso();
  const [error, setError] = useState('');
  const formik = useFormik({
    initialValues: { actual: '', nueva: '', confirmar: '' },
    validationSchema: cambioClaveSchema,
    onSubmit: async (v, { setSubmitting, resetForm }) => {
      setError('');
      try {
        await cambiarClave(v.actual, v.nueva);
        resetForm();
        avisar(t('cuenta.claveCambiada'));
      } catch (e) {
        setError(e.message);
      } finally {
        setSubmitting(false);
      }
    },
  });
  const campo = campos(formik, t);
  return (
    <Paper component="form" onSubmit={formik.handleSubmit} noValidate sx={{ p: 3 }} aria-labelledby="clave-titulo">
      <Typography id="clave-titulo" component="h2" variant="h2" sx={{ mb: 1 }}>{t('cuenta.seguridad')}</Typography>
      <div>{error && <Alert severity="error" sx={{ mb: 1 }}>{error}</Alert>}</div>
      <TextField {...campo('actual')} type="password" label={t('cuenta.actual')} required inputProps={{ autoComplete: 'current-password' }} />
      <TextField {...campo('nueva', t('usuarios.ayudaClave'))} type="password" label={t('cuenta.nueva')} required inputProps={{ autoComplete: 'new-password' }} />
      <TextField {...campo('confirmar')} type="password" label={t('cuenta.confirmar')} required inputProps={{ autoComplete: 'new-password' }} />
      <Button type="submit" variant="contained" disabled={formik.isSubmitting}>{t('cuenta.cambiar')}</Button>
    </Paper>
  );
}

/** Pantalla «Mi cuenta». Disponible para cualquier usuario autenticado. */
export default function Cuenta() {
  const { t } = useTranslation();
  const { usuario } = useAuth();
  return (
    <Box component="main" id="contenido" sx={{ px: { xs: 2, md: 4 }, py: 3, maxWidth: 1000, mx: 'auto' }}>
      <Typography component="h1" variant="h1" sx={{ mb: 2 }}>{t('cuenta.titulo')}</Typography>
      <Box sx={{ display: 'grid', gap: 3, gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' }, alignItems: 'start' }}>
        <DatosPropios />
        <CambioClave />
      </Box>
      <Typography component="h2" variant="h2" sx={{ mt: 4, mb: 1 }}>{t('cuenta.misPermisos')}</Typography>
      <Box component="ul" sx={{ listStyle: 'none', m: 0, p: 0, display: 'flex', gap: 1, flexWrap: 'wrap' }}>
        {usuario.permisos.map((p) => (
          <li key={p}><Chip size="small" variant="outlined" label={t(`permisos.${p}`)} /></li>
        ))}
      </Box>
    </Box>
  );
}
