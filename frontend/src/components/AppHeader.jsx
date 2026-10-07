/**
 * nombre: AppHeader.jsx
 * descripcion: Barra superior: marca, idioma, tema y cierre de sesión.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
 */
import { Box, Button, IconButton, ToggleButton, ToggleButtonGroup, Tooltip, Typography } from '@mui/material';
import DarkModeOutlined from '@mui/icons-material/DarkModeOutlined';
import LightModeOutlined from '@mui/icons-material/LightModeOutlined';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../auth/AuthContext';
import { useThemeMode } from '../theme/ThemeModeProvider';

/** Cabecera de la aplicación; muestra usuario y rol cuando hay sesión. */
export default function AppHeader() {
  const { t, i18n } = useTranslation();
  const { sesion, salir } = useAuth();
  const { mode, toggle } = useThemeMode();
  return (
    <Box
      component="header"
      sx={{
        borderBottom: 1,
        borderColor: 'divider',
        bgcolor: 'background.paper',
        px: { xs: 2, md: 4 },
        py: 1.5,
        display: 'flex',
        alignItems: 'center',
        gap: 2,
        flexWrap: 'wrap',
      }}
    >
      <Typography component="p" sx={{ fontWeight: 700, letterSpacing: '0.04em', textTransform: 'uppercase', flexGrow: 1 }}>
        <Box component="span" sx={{ color: 'secondary.main' }} aria-hidden="true">■ </Box>
        {t('app.nombre')}
      </Typography>
      {sesion && (
        <Typography variant="body2" color="text.secondary" sx={{ fontFamily: 'var(--mono, monospace)' }}>
          {sesion.username} · {sesion.rol}
        </Typography>
      )}
      <ToggleButtonGroup
        size="small"
        exclusive
        value={i18n.language}
        onChange={(_, v) => v && i18n.changeLanguage(v)}
        aria-label={t('nav.idioma')}
      >
        <ToggleButton value="es" aria-label="Español">ES</ToggleButton>
        <ToggleButton value="en" aria-label="English">EN</ToggleButton>
      </ToggleButtonGroup>
      <Tooltip title={t('nav.tema')}>
        <IconButton onClick={toggle} aria-label={t('nav.tema')}>
          {mode === 'light' ? <DarkModeOutlined /> : <LightModeOutlined />}
        </IconButton>
      </Tooltip>
      {sesion && (
        <Button variant="outlined" size="small" onClick={salir}>
          {t('nav.salir')}
        </Button>
      )}
    </Box>
  );
}
