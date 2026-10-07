/**
 * nombre: App.jsx
 * descripcion: Raíz de la aplicación: cabecera, enlace de salto y rutas protegidas por permiso.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
import { Box } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth/AuthContext';
import AppHeader, { SECCIONES } from './components/AppHeader';
import Alertas from './pages/Alertas';
import Cuenta from './pages/Cuenta';
import Dashboard from './pages/Dashboard';
import Login from './pages/Login';
import Movimientos from './pages/Movimientos';
import Perfiles from './pages/Perfiles';
import Productos from './pages/Productos';
import SinAcceso from './pages/SinAcceso';
import Usuarios from './pages/Usuarios';

/** Muestra la pantalla solo si el usuario tiene alguno de los permisos; si no, «Sin acceso». */
function Protegida({ permisos, children }) {
  const { tiene } = useAuth();
  return permisos.some(tiene) ? children : <SinAcceso />;
}

/** Primera sección disponible para el usuario (página de inicio). */
function Inicio() {
  const { tiene } = useAuth();
  const primera = SECCIONES.find((s) => s.permisos.length > 0 && s.permisos.some(tiene));
  if (primera && primera.ruta !== '/') return <Navigate to={primera.ruta} replace />;
  return primera ? <Dashboard /> : <Navigate to="/cuenta" replace />;
}

/** Muestra el login o las pantallas según los permisos del usuario. */
export default function App() {
  const { t } = useTranslation();
  const { usuario } = useAuth();
  return (
    <>
      <Box
        component="a" href="#contenido"
        sx={{ position: 'absolute', left: -9999, top: 0, bgcolor: 'secondary.main', color: 'secondary.contrastText', p: 1, '&:focus': { left: 0, zIndex: 2000 } }}
      >
        {t('app.saltar')}
      </Box>
      <AppHeader />
      {usuario ? (
        <Routes>
          <Route path="/" element={<Inicio />} />
          <Route path="/productos" element={<Protegida permisos={['PRODUCTOS_VER']}><Productos /></Protegida>} />
          <Route path="/movimientos" element={<Protegida permisos={['MOVIMIENTOS_VER']}><Movimientos /></Protegida>} />
          <Route path="/alertas" element={<Protegida permisos={['ALERTAS_VER']}><Alertas /></Protegida>} />
          <Route path="/usuarios" element={<Protegida permisos={['USUARIOS_GESTIONAR']}><Usuarios /></Protegida>} />
          <Route path="/perfiles" element={<Protegida permisos={['PERFILES_GESTIONAR']}><Perfiles /></Protegida>} />
          <Route path="/cuenta" element={<Cuenta />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      ) : (
        <Box component="main" id="contenido"><Login /></Box>
      )}
    </>
  );
}
