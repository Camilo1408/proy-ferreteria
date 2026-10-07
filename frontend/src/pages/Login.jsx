/**
 * nombre: Login.jsx
 * descripcion: Pantalla de inicio de sesión con Formik y Yup.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
 */
import { useState } from 'react';
import { Alert, Box, Button, Paper, TextField, Typography } from '@mui/material';
import { useFormik } from 'formik';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../auth/AuthContext';
import { loginSchema } from '../validation/schemas';

/** Formulario de acceso. Muestra el error de credenciales en una región anunciada a lectores de pantalla. */
export default function Login() {
  const { t } = useTranslation();
  const { entrar } = useAuth();
  const [error, setError] = useState('');
  const formik = useFormik({
    initialValues: { username: '', password: '' },
    validationSchema: loginSchema,
    onSubmit: async (v, { setSubmitting }) => {
      setError('');
      try {
        await entrar(v.username.trim(), v.password);
      } catch (e) {
        setError(e.status === 401 ? t('login.error') : e.message);
      } finally {
        setSubmitting(false);
      }
    },
  });
  const msg = (campo) => (formik.touched[campo] && formik.errors[campo] ? t(formik.errors[campo]) : ' ');
  return (
    <Box sx={{ display: 'grid', placeItems: 'center', px: 2, py: { xs: 4, md: 8 } }}>
      <Paper component="form" onSubmit={formik.handleSubmit} noValidate sx={{ p: { xs: 3, sm: 4 }, width: '100%', maxWidth: 400 }}>
        <Typography component="h1" variant="h1" sx={{ mb: 3 }}>
          {t('login.titulo')}
        </Typography>
        <div role="alert" aria-live="assertive">
          {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
        </div>
        <TextField
          fullWidth
          id="username"
          name="username"
          label={t('login.usuario')}
          autoComplete="username"
          autoFocus
          value={formik.values.username}
          onChange={formik.handleChange}
          onBlur={formik.handleBlur}
          error={formik.touched.username && Boolean(formik.errors.username)}
          helperText={msg('username')}
        />
        <TextField
          fullWidth
          id="password"
          name="password"
          type="password"
          label={t('login.clave')}
          autoComplete="current-password"
          sx={{ mt: 1 }}
          value={formik.values.password}
          onChange={formik.handleChange}
          onBlur={formik.handleBlur}
          error={formik.touched.password && Boolean(formik.errors.password)}
          helperText={msg('password')}
        />
        <Button type="submit" variant="contained" fullWidth size="large" disabled={formik.isSubmitting} sx={{ mt: 2 }}>
          {t('login.entrar')}
        </Button>
      </Paper>
    </Box>
  );
}
