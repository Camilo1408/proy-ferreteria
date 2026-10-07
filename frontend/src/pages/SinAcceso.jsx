/**
 * nombre: SinAcceso.jsx
 * descripcion: Pantalla mostrada cuando el perfil no tiene permiso para una sección.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
import { Box, Button, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';

/** Aviso de falta de permiso con salida a la cuenta propia. */
export default function SinAcceso() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  return (
    <Box component="main" id="contenido" sx={{ px: { xs: 2, md: 4 }, py: 6, maxWidth: 600, mx: 'auto' }}>
      <Typography component="h1" variant="h1" sx={{ mb: 1 }}>{t('sinAcceso.titulo')}</Typography>
      <Typography sx={{ mb: 3 }} color="text.secondary">{t('sinAcceso.texto')}</Typography>
      <Button variant="outlined" onClick={() => navigate('/cuenta')}>{t('sinAcceso.ir')}</Button>
    </Box>
  );
}
