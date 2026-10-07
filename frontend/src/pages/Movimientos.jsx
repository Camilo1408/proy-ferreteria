/**
 * nombre: Movimientos.jsx
 * descripcion: Historial de movimientos (kárdex) con filtros y registro de entradas, salidas y ajustes.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
import { useCallback, useEffect, useState } from 'react';
import {
  Alert, Box, Button, Chip, LinearProgress, MenuItem, Paper, Table, TableBody, TableCell, TableContainer, TableHead,
  TablePagination, TableRow, TextField, Typography, useMediaQuery,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import { useTranslation } from 'react-i18next';
import { useSearchParams } from 'react-router-dom';
import { listarMovimientos } from '../api/inventario';
import { obtener } from '../api/productos';
import { useAuth } from '../auth/AuthContext';
import { EVENTO_ALERTAS } from '../components/AppHeader';
import { TipoChip } from '../components/Chips';
import MovimientoDialog from '../components/MovimientoDialog';
import { P } from '../constants';
import { useAviso } from '../context/AvisoContext';
import { formatoCantidad, formatoFecha, formatoVariacion } from '../utils/format';

const MONO = { fontFamily: '"IBM Plex Mono", monospace', fontSize: '0.85rem' };

/** Historial paginado; puede filtrarse por producto (`?productoId=`), tipo y fechas. */
export default function Movimientos() {
  const { t, i18n } = useTranslation();
  const { tiene } = useAuth();
  const { avisar } = useAviso();
  const [params, setParams] = useSearchParams();
  const movil = useMediaQuery((th) => th.breakpoints.down('md'));
  const productoId = params.get('productoId') ?? '';
  const [producto, setProducto] = useState(null);
  const [tipo, setTipo] = useState('');
  const [desde, setDesde] = useState('');
  const [hasta, setHasta] = useState('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [pagina, setPagina] = useState({ content: [], totalElements: 0 });
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState('');
  const [abierto, setAbierto] = useState(false);
  const puedeRegistrar = tiene(P.MOVIMIENTOS_REGISTRAR) || tiene(P.AJUSTES_REGISTRAR);

  useEffect(() => {
    if (!productoId) { setProducto(null); return; }
    obtener(productoId).then(setProducto).catch(() => setProducto(null));
  }, [productoId]);

  const cargar = useCallback(async () => {
    setCargando(true);
    setError('');
    try {
      setPagina(await listarMovimientos({ page, size, productoId, tipo, desde, hasta }));
    } catch (e) {
      setError(e.message);
    } finally {
      setCargando(false);
    }
  }, [page, size, productoId, tipo, desde, hasta]);
  useEffect(() => { cargar(); }, [cargar]);

  const limpiar = () => {
    setTipo(''); setDesde(''); setHasta(''); setPage(0);
    setParams({}, { replace: true });
  };
  const registrado = () => {
    setAbierto(false);
    avisar(t('movimientos.registrado'));
    window.dispatchEvent(new Event(EVENTO_ALERTAS));
    setPage(0);
    cargar();
  };

  const variacion = (m) => (
    <Typography component="span" sx={{ ...MONO, fontWeight: 600, color: Number(m.variacion) < 0 ? 'error.main' : 'success.main' }}>
      {formatoVariacion(m.variacion, i18n.language)} {m.unidad}
    </Typography>
  );
  const resultado = (m) => t('movimientos.anteriorResultante', {
    anterior: formatoCantidad(m.stockAnterior, i18n.language), resultante: formatoCantidad(m.stockResultante, i18n.language),
  });
  const detalle = (m) => [m.referencia, m.nota].filter(Boolean).join(' · ');

  return (
    <Box component="main" id="contenido" sx={{ px: { xs: 2, md: 4 }, py: 3, maxWidth: 1200, mx: 'auto' }}>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, flexWrap: 'wrap', mb: 2 }}>
        <Typography component="h1" variant="h1" sx={{ flexGrow: 1 }}>{t('movimientos.titulo')}</Typography>
        {puedeRegistrar && (
          <Button variant="contained" color="secondary" startIcon={<AddIcon />} onClick={() => setAbierto(true)}>{t('movimientos.registrar')}</Button>
        )}
      </Box>

      <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', mb: 2, alignItems: 'center' }}>
        {productoId && (
          <Chip label={producto ? `${producto.codigo} · ${producto.nombre}` : `#${productoId}`} onDelete={limpiar} />
        )}
        <TextField size="small" select label={t('movimientos.tipo')} value={tipo} onChange={(e) => { setTipo(e.target.value); setPage(0); }} sx={{ minWidth: 150 }}>
          <MenuItem value="">{t('movimientos.todos')}</MenuItem>
          {['ENTRADA', 'SALIDA', 'AJUSTE'].map((x) => <MenuItem key={x} value={x}>{t(`movimientos.tipos.${x}`)}</MenuItem>)}
        </TextField>
        <TextField size="small" type="date" label={t('movimientos.desde')} value={desde} onChange={(e) => { setDesde(e.target.value); setPage(0); }} InputLabelProps={{ shrink: true }} />
        <TextField size="small" type="date" label={t('movimientos.hasta')} value={hasta} onChange={(e) => { setHasta(e.target.value); setPage(0); }} InputLabelProps={{ shrink: true }} />
        <Button onClick={limpiar}>{t('movimientos.limpiar')}</Button>
      </Box>

      <div>{error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}</div>

      <Paper>
        {cargando && <LinearProgress aria-label={t('app.cargando')} />}
        {movil ? (
          <Box component="ul" sx={{ listStyle: 'none', m: 0, p: 0 }}>
            {pagina.content.map((m) => (
              <Box component="li" key={m.id} sx={{ p: 2, borderBottom: 1, borderColor: 'divider' }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', gap: 1 }}>
                  <Typography sx={{ fontWeight: 600 }}>{m.nombre}</Typography>
                  <TipoChip tipo={m.tipo} />
                </Box>
                <Typography variant="body2" color="text.secondary">{formatoFecha(m.fecha, i18n.language)} · {m.usuario} · {t(`motivos.${m.motivo}`)}</Typography>
                <Box sx={{ display: 'flex', gap: 1.5, mt: 0.5 }}>{variacion(m)}<Typography component="span" sx={MONO}>{resultado(m)}</Typography></Box>
                {detalle(m) && <Typography variant="body2" sx={{ mt: 0.5 }}>{detalle(m)}</Typography>}
              </Box>
            ))}
          </Box>
        ) : (
          <TableContainer>
            <Table aria-label={t('movimientos.titulo')}>
              <TableHead>
                <TableRow>
                  <TableCell>{t('movimientos.cols.fecha')}</TableCell>
                  <TableCell>{t('movimientos.cols.producto')}</TableCell>
                  <TableCell>{t('movimientos.cols.tipo')}</TableCell>
                  <TableCell>{t('movimientos.cols.motivo')}</TableCell>
                  <TableCell align="right">{t('movimientos.cols.variacion')}</TableCell>
                  <TableCell align="right">{t('movimientos.cols.stock')}</TableCell>
                  <TableCell>{t('movimientos.cols.usuario')}</TableCell>
                  <TableCell>{t('movimientos.cols.detalle')}</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {pagina.content.map((m) => (
                  <TableRow key={m.id} hover>
                    <TableCell sx={{ whiteSpace: 'nowrap' }}>{formatoFecha(m.fecha, i18n.language)}</TableCell>
                    <TableCell><Typography sx={{ fontWeight: 600 }} component="span">{m.nombre}</Typography><Typography component="span" sx={{ ...MONO, color: 'text.secondary' }}> {m.codigo}</Typography></TableCell>
                    <TableCell><TipoChip tipo={m.tipo} /></TableCell>
                    <TableCell>{t(`motivos.${m.motivo}`)}</TableCell>
                    <TableCell align="right" sx={{ whiteSpace: 'nowrap' }}>{variacion(m)}</TableCell>
                    <TableCell align="right" sx={{ ...MONO, whiteSpace: 'nowrap' }}>{resultado(m)}</TableCell>
                    <TableCell sx={MONO}>{m.usuario}</TableCell>
                    <TableCell sx={{ color: 'text.secondary', maxWidth: 260 }}>{detalle(m)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
        {!cargando && pagina.content.length === 0 && !error && <Typography sx={{ p: 3 }} color="text.secondary">{t('movimientos.vacio')}</Typography>}
        <TablePagination
          component="div" count={pagina.totalElements} page={page} rowsPerPage={size} rowsPerPageOptions={[5, 10, 25]}
          labelRowsPerPage={t('productos.filasPorPagina')} onPageChange={(_, n) => setPage(n)}
          onRowsPerPageChange={(e) => { setSize(Number(e.target.value)); setPage(0); }}
        />
      </Paper>
      <MovimientoDialog abierto={abierto} producto={producto?.estado === 'ACTIVO' ? producto : null} onCerrar={() => setAbierto(false)} onRegistrado={registrado} />
    </Box>
  );
}
