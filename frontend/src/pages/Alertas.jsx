/**
 * nombre: Alertas.jsx
 * descripcion: Alertas de stock mínimo: vigentes y resueltas, con reconocimiento y reposición rápida.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
import { useCallback, useEffect, useState } from 'react';
import {
  Alert, Box, Button, LinearProgress, Paper, Tab, Table, TableBody, TableCell, TableContainer, TableHead,
  TablePagination, TableRow, Tabs, Typography, useMediaQuery,
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import { listarAlertas, reconocerAlerta } from '../api/inventario';
import { useAuth } from '../auth/AuthContext';
import { EVENTO_ALERTAS } from '../components/AppHeader';
import { EstadoAlertaChip, NivelChip } from '../components/Chips';
import MovimientoDialog from '../components/MovimientoDialog';
import { P } from '../constants';
import { useAviso } from '../context/AvisoContext';
import { formatoCantidad, formatoFecha } from '../utils/format';

const MONO = { fontFamily: '"IBM Plex Mono", monospace', fontSize: '0.85rem' };

/** Pantalla de alertas. «Vigentes» agrupa las pendientes y las reconocidas. */
export default function Alertas() {
  const { t, i18n } = useTranslation();
  const { tiene } = useAuth();
  const { avisar } = useAviso();
  const movil = useMediaQuery((th) => th.breakpoints.down('md'));
  const [vista, setVista] = useState('vigentes');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [pagina, setPagina] = useState({ content: [], totalElements: 0 });
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState('');
  const [reponer, setReponer] = useState(null);
  const puedeReconocer = tiene(P.ALERTAS_GESTIONAR);
  const puedeReponer = tiene(P.MOVIMIENTOS_REGISTRAR);

  const cargar = useCallback(async () => {
    setCargando(true);
    setError('');
    try {
      setPagina(await listarAlertas({ page, size, estado: vista === 'resueltas' ? 'RESUELTA' : '' }));
    } catch (e) {
      setError(e.message);
    } finally {
      setCargando(false);
    }
  }, [page, size, vista]);
  useEffect(() => { cargar(); }, [cargar]);

  const reconocer = async (a) => {
    try {
      await reconocerAlerta(a.id);
      avisar(t('alertas.reconocida'));
      window.dispatchEvent(new Event(EVENTO_ALERTAS));
      cargar();
    } catch (e) {
      setError(e.message);
    }
  };
  const registrado = () => {
    setReponer(null);
    avisar(t('movimientos.registrado'));
    window.dispatchEvent(new Event(EVENTO_ALERTAS));
    cargar();
  };

  const fmt = (v) => formatoCantidad(v, i18n.language);
  const estado = (a) => (
    <Box sx={{ display: 'inline-flex', gap: 1, alignItems: 'center', flexWrap: 'wrap' }}>
      <EstadoAlertaChip estado={a.estado} />
      {a.reconocidaPor && <Typography variant="caption" color="text.secondary">{t('alertas.por', { usuario: a.reconocidaPor })}</Typography>}
    </Box>
  );
  const acciones = (a) => a.estado !== 'RESUELTA' && (
    <>
      {puedeReconocer && a.estado === 'ABIERTA' && (
        <Button size="small" onClick={() => reconocer(a)} aria-label={`${t('alertas.reconocer')}: ${a.nombre}`}>{t('alertas.reconocer')}</Button>
      )}
      {puedeReponer && (
        <Button
          size="small" variant="outlined" aria-label={`${t('alertas.reponer')}: ${a.nombre}`}
          onClick={() => setReponer({ id: a.productoId, codigo: a.codigo, nombre: a.nombre, unidad: a.unidad, stockActual: a.stockActual })}
        >
          {t('alertas.reponer')}
        </Button>
      )}
    </>
  );

  return (
    <Box component="main" id="contenido" sx={{ px: { xs: 2, md: 4 }, py: 3, maxWidth: 1200, mx: 'auto' }}>
      <Typography component="h1" variant="h1" sx={{ mb: 2 }}>{t('alertas.titulo')}</Typography>
      <Tabs value={vista} onChange={(_, v) => { setVista(v); setPage(0); }} sx={{ mb: 2 }}>
        <Tab value="vigentes" label={t('alertas.vigentes')} />
        <Tab value="resueltas" label={t('alertas.resueltas')} />
      </Tabs>
      <div>{error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}</div>
      <Paper>
        {cargando && <LinearProgress aria-label={t('app.cargando')} />}
        {movil ? (
          <Box component="ul" sx={{ listStyle: 'none', m: 0, p: 0 }}>
            {pagina.content.map((a) => (
              <Box component="li" key={a.id} sx={{ p: 2, borderBottom: 1, borderColor: 'divider' }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', gap: 1 }}>
                  <Typography sx={{ fontWeight: 600 }}>{a.nombre}</Typography>
                  <NivelChip nivel={a.nivel === 'AGOTADO' ? 'AGOTADO' : 'BAJO'} />
                </Box>
                <Typography sx={{ ...MONO, color: 'text.secondary' }}>{a.codigo}</Typography>
                <Typography sx={MONO}>{fmt(a.stockActual)} / {fmt(a.stockMinimo)} {a.unidad}</Typography>
                <Box sx={{ mt: 0.5 }}>{estado(a)}</Box>
                <Box sx={{ mt: 0.5 }}>{acciones(a)}</Box>
              </Box>
            ))}
          </Box>
        ) : (
          <TableContainer>
            <Table aria-label={t('alertas.titulo')}>
              <TableHead>
                <TableRow>
                  <TableCell>{t('alertas.cols.producto')}</TableCell>
                  <TableCell>{t('alertas.cols.nivel')}</TableCell>
                  <TableCell align="right">{t('alertas.cols.stock')}</TableCell>
                  <TableCell align="right">{t('alertas.cols.minimo')}</TableCell>
                  <TableCell align="right">{t('alertas.cols.faltante')}</TableCell>
                  <TableCell>{t('alertas.cols.estado')}</TableCell>
                  <TableCell>{t('alertas.cols.detectada')}</TableCell>
                  <TableCell align="right">{t('alertas.cols.acciones')}</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {pagina.content.map((a) => (
                  <TableRow key={a.id} hover>
                    <TableCell><Typography component="span" sx={{ fontWeight: 600 }}>{a.nombre}</Typography><Typography component="span" sx={{ ...MONO, color: 'text.secondary' }}> {a.codigo}</Typography></TableCell>
                    <TableCell><NivelChip nivel={a.nivel === 'AGOTADO' ? 'AGOTADO' : 'BAJO'} /></TableCell>
                    <TableCell align="right" sx={MONO}>{fmt(a.stockActual)} {a.unidad}</TableCell>
                    <TableCell align="right" sx={{ ...MONO, color: 'text.secondary' }}>{fmt(a.stockMinimo)}</TableCell>
                    <TableCell align="right" sx={MONO}>{fmt(a.faltante)}</TableCell>
                    <TableCell>{estado(a)}</TableCell>
                    <TableCell sx={{ whiteSpace: 'nowrap' }}>{formatoFecha(a.creadaEn, i18n.language)}</TableCell>
                    <TableCell align="right" sx={{ whiteSpace: 'nowrap' }}>{acciones(a)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
        {!cargando && pagina.content.length === 0 && !error && <Typography sx={{ p: 3 }} color="text.secondary">{t('alertas.vacio')}</Typography>}
        <TablePagination
          component="div" count={pagina.totalElements} page={page} rowsPerPage={size} rowsPerPageOptions={[5, 10, 25]}
          labelRowsPerPage={t('productos.filasPorPagina')} onPageChange={(_, n) => setPage(n)}
          onRowsPerPageChange={(e) => { setSize(Number(e.target.value)); setPage(0); }}
        />
      </Paper>
      <MovimientoDialog abierto={Boolean(reponer)} producto={reponer} tipoInicial="ENTRADA" onCerrar={() => setReponer(null)} onRegistrado={registrado} />
    </Box>
  );
}
