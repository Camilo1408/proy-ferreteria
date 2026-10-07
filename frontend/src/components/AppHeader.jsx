/**
 * nombre: AppHeader.jsx
 * descripcion: Cabecera: marca, navegación por permisos, campana de alertas, idioma, tema y sesión.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
import { useEffect, useState } from 'react';
import { Badge, Box, Button, IconButton, ToggleButton, ToggleButtonGroup, Tooltip, Typography } from '@mui/material';
import DarkModeOutlined from '@mui/icons-material/DarkModeOutlined';
import LightModeOutlined from '@mui/icons-material/LightModeOutlined';
import NotificationsNoneOutlined from '@mui/icons-material/NotificationsNoneOutlined';
import { useTranslation } from 'react-i18next';
import { NavLink, useNavigate } from 'react-router-dom';
import { resumenAlertas } from '../api/inventario';
import { useAuth } from '../auth/AuthContext';
import { P } from '../constants';
import { useThemeMode } from '../theme/ThemeModeProvider';

/** Secciones de la navegación y el permiso (cualquiera de la lista) que las habilita. */
export const SECCIONES = [
  { ruta: '/', clave: 'panel', permisos: [P.PRODUCTOS_VER], fin: true },
  { ruta: '/productos', clave: 'productos', permisos: [P.PRODUCTOS_VER] },
  { ruta: '/movimientos', clave: 'movimientos', permisos: [P.MOVIMIENTOS_VER] },
  { ruta: '/alertas', clave: 'alertas', permisos: [P.ALERTAS_VER] },
  { ruta: '/usuarios', clave: 'usuarios', permisos: [P.USUARIOS_GESTIONAR] },
  { ruta: '/perfiles', clave: 'perfiles', permisos: [P.PERFILES_GESTIONAR] },
  { ruta: '/cuenta', clave: 'cuenta', permisos: [] },
];

/** Evento que cualquier pantalla emite tras cambiar el stock para refrescar la campana al instante. */
export const EVENTO_ALERTAS = 'alertas:cambio';

/** Campana con el número de alertas pendientes; se actualiza cada minuto y al cambiar el stock. */
function CampanaAlertas() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [abiertas, setAbiertas] = useState(0);

  useEffect(() => {
    let vivo = true;
    const cargar = () => resumenAlertas().then((r) => vivo && setAbiertas(r.abiertas)).catch(() => {});
    cargar();
    const id = setInterval(cargar, 60000);
    window.addEventListener(EVENTO_ALERTAS, cargar);
    return () => {
      vivo = false;
      clearInterval(id);
      window.removeEventListener(EVENTO_ALERTAS, cargar);
    };
  }, []);

  const etiqueta = abiertas > 0 ? t('nav.alertasPendientes', { n: abiertas }) : t('nav.sinAlertas');
  return (
    <Tooltip title={etiqueta}>
      <IconButton onClick={() => navigate('/alertas')} aria-label={etiqueta}>
        <Badge badgeContent={abiertas} color="error" max={99}>
          <NotificationsNoneOutlined />
        </Badge>
      </IconButton>
    </Tooltip>
  );
}

/** Cabecera de la aplicación. Sin sesión solo muestra marca, idioma y tema. */
export default function AppHeader() {
  const { t, i18n } = useTranslation();
  const { usuario, tiene, salir } = useAuth();
  const { mode, toggle } = useThemeMode();
  const visibles = SECCIONES.filter((s) => s.permisos.length === 0 || s.permisos.some(tiene));
  return (
    <Box component="header" sx={{ borderBottom: 1, borderColor: 'divider', bgcolor: 'background.paper' }}>
      <Box sx={{ px: { xs: 2, md: 4 }, py: 1.5, display: 'flex', alignItems: 'center', gap: 2, flexWrap: 'wrap' }}>
        <Typography component="p" sx={{ fontWeight: 700, letterSpacing: '0.04em', textTransform: 'uppercase', flexGrow: 1 }}>
          <Box component="span" sx={{ color: 'secondary.main' }} aria-hidden="true">■ </Box>
          {t('app.nombre')}
        </Typography>
        {usuario && (
          <Typography variant="body2" color="text.secondary" sx={{ fontFamily: '"IBM Plex Mono", monospace' }}>
            {usuario.username} · {usuario.perfil}
          </Typography>
        )}
        {usuario && tiene(P.ALERTAS_VER) && <CampanaAlertas />}
        <ToggleButtonGroup
          size="small" exclusive value={i18n.language}
          onChange={(_, v) => v && i18n.changeLanguage(v)} aria-label={t('nav.idioma')}
        >
          <ToggleButton value="es" aria-label="Español">ES</ToggleButton>
          <ToggleButton value="en" aria-label="English">EN</ToggleButton>
        </ToggleButtonGroup>
        <Tooltip title={t('nav.tema')}>
          <IconButton onClick={toggle} aria-label={t('nav.tema')}>
            {mode === 'light' ? <DarkModeOutlined /> : <LightModeOutlined />}
          </IconButton>
        </Tooltip>
        {usuario && <Button variant="outlined" size="small" onClick={salir}>{t('nav.salir')}</Button>}
      </Box>
      {usuario && (
        <Box component="nav" aria-label={t('nav.principal')} sx={{ px: { xs: 1, md: 3 }, display: 'flex', overflowX: 'auto' }}>
          {visibles.map((s) => (
            <Box
              key={s.ruta} component={NavLink} to={s.ruta} end={s.fin}
              sx={{
                px: 1.5, py: 1.25, whiteSpace: 'nowrap', textDecoration: 'none', color: 'text.secondary', fontWeight: 600,
                fontSize: '0.9rem', borderBottom: '3px solid transparent',
                '&.active': { color: 'text.primary', borderBottomColor: 'secondary.main' },
                '&:hover': { color: 'text.primary' },
              }}
            >
              {t(`nav.${s.clave}`)}
            </Box>
          ))}
        </Box>
      )}
    </Box>
  );
}
