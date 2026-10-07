/**
 * nombre: Dashboard.jsx
 * descripcion: Panel de inicio: indicadores de inventario y productos en mínimos.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
import { useCallback, useEffect, useState } from 'react';
import { Alert, Box, Button, LinearProgress, Paper, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import * as api from '../api/productos';
import { useAuth } from '../auth/AuthContext';
import { NivelChip } from '../components/Chips';
import MovimientoDialog from '../components/MovimientoDialog';
import { EVENTO_ALERTAS } from '../components/AppHeader';
import { useAviso } from '../context/AvisoContext';
import { P } from '../constants';
import { formatoCantidad } from '../utils/format';

function Indicador({ etiqueta, valor, tono, a }) {
  return (
    <Paper
      component={a ? RouterLink : 'div'} to={a} sx={{ p: 2, textDecoration: 'none', color: 'inherit', display: 'block', '&:hover': a ? { borderColor: 'text.secondary' } : undefined }}
    >
      <Typography variant="body2" color="text.secondary">{etiqueta}</Typography>
      <Typography component="p" sx={{ fontSize: '2rem', fontWeight: 600, lineHeight: 1.2, color: tono ?? 'text.primary', fontFamily: '"IBM Plex Mono", monospace' }}>
        {valor}
      </Typography>
    </Paper>
  );
}

/** Pantalla de inicio con los números del inventario. */
export default function Dashboard() {
  const { t, i18n } = useTranslation();
  const { tiene } = useAuth();
  const navigate = useNavigate();
  const { avisar } = useAviso();
  const [datos, setDatos] = useState(null);
  const [minimos, setMinimos] = useState([]);
  const [error, setError] = useState('');
  const [cargando, setCargando] = useState(true);
  const [reponer, setReponer] = useState(null);

  const cargar = useCallback(async () => {
    setCargando(true);
    setError('');
    try {
      const [d, m] = await Promise.all([api.dashboard(), api.listar({ bajoMinimo: true, size: 5 })]);
      setDatos(d);
      setMinimos(m.content);
    } catch (e) {
      setError(e.message);
    } finally {
      setCargando(false);
    }
  }, []);
  useEffect(() => { cargar(); }, [cargar]);

  const registrado = () => {
    setReponer(null);
    avisar(t('movimientos.registrado'));
    window.dispatchEvent(new Event(EVENTO_ALERTAS));
    cargar();
  };

  return (
    <Box component="main" id="contenido" sx={{ px: { xs: 2, md: 4 }, py: 3, maxWidth: 1100, mx: 'auto' }}>
      <Typography component="h1" variant="h1" sx={{ mb: 2 }}>{t('panel.titulo')}</Typography>
      {cargando && <LinearProgress aria-label={t('app.cargando')} />}
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      {datos && (
        <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr 1fr', md: 'repeat(5, 1fr)' }, mb: 4 }}>
          <Indicador etiqueta={t('panel.productosActivos')} valor={datos.productosActivos} a="/productos" />
          <Indicador etiqueta={t('panel.bajoMinimo')} valor={datos.bajoMinimo} tono={datos.bajoMinimo ? 'warning.main' : undefined} a="/productos?minimos=1" />
          <Indicador etiqueta={t('panel.agotados')} valor={datos.agotados} tono={datos.agotados ? 'error.main' : undefined} />
          <Indicador etiqueta={t('panel.alertas')} valor={datos.alertasAbiertas} tono={datos.alertasAbiertas ? 'error.main' : undefined} a={tiene(P.ALERTAS_VER) ? '/alertas' : undefined} />
          <Indicador etiqueta={t('panel.movimientosHoy')} valor={datos.movimientosHoy} a={tiene(P.MOVIMIENTOS_VER) ? '/movimientos' : undefined} />
        </Box>
      )}
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
        <Typography component="h2" variant="h2" sx={{ flexGrow: 1 }}>{t('panel.enMinimos')}</Typography>
        <Button size="small" onClick={() => navigate('/productos?minimos=1')}>{t('panel.verTodos')}</Button>
      </Box>
      <Paper>
        {minimos.length === 0 && !cargando && <Typography sx={{ p: 3 }} color="text.secondary">{t('panel.todoBien')}</Typography>}
        <Box component="ul" sx={{ listStyle: 'none', m: 0, p: 0 }}>
          {minimos.map((p) => (
            <Box component="li" key={p.id} sx={{ p: 2, display: 'flex', gap: 2, alignItems: 'center', flexWrap: 'wrap', borderBottom: 1, borderColor: 'divider' }}>
              <Box sx={{ flexGrow: 1, minWidth: 180 }}>
                <Typography sx={{ fontWeight: 600 }}>{p.nombre}</Typography>
                <Typography variant="body2" color="text.secondary" sx={{ fontFamily: '"IBM Plex Mono", monospace' }}>{p.codigo}</Typography>
              </Box>
              <Typography sx={{ fontFamily: '"IBM Plex Mono", monospace' }}>
                {formatoCantidad(p.stockActual, i18n.language)} / {formatoCantidad(p.stockMinimo, i18n.language)} {p.unidad}
              </Typography>
              <NivelChip nivel={p.nivel} />
              {tiene(P.MOVIMIENTOS_REGISTRAR) && <Button size="small" variant="outlined" onClick={() => setReponer(p)}>{t('panel.reponer')}</Button>}
            </Box>
          ))}
        </Box>
      </Paper>
      <MovimientoDialog abierto={Boolean(reponer)} producto={reponer} tipoInicial="ENTRADA" onCerrar={() => setReponer(null)} onRegistrado={registrado} />
    </Box>
  );
}
