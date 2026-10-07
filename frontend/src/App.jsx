/**
 * nombre: App.jsx
 * descripcion: Raíz de la aplicación: cabecera, enlace de salto y vista según sesión.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
 */
import { Box } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useAuth } from './auth/AuthContext';
import AppHeader from './components/AppHeader';
import Login from './pages/Login';
import Productos from './pages/Productos';

/** Muestra el login o el listado de productos. */
export default function App() {
  const { t } = useTranslation();
  const { sesion } = useAuth();
  return (
    <>
      <Box
        component="a"
        href="#contenido"
        sx={{
          position: 'absolute', left: -9999, top: 0, bgcolor: 'secondary.main', color: 'secondary.contrastText', p: 1,
          '&:focus': { left: 0, zIndex: 2000 },
        }}
      >
        {t('app.saltar')}
      </Box>
      <AppHeader />
      {sesion ? <Productos /> : <Box component="main" id="contenido"><Login /></Box>}
    </>
  );
}
