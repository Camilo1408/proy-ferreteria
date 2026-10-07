/**
 * nombre: Productos.jsx
 * descripcion: Catálogo con existencias: búsqueda, filtros, productos en mínimos, movimientos rápidos y exportación.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
import { useCallback, useEffect, useState } from 'react';
import {
  Alert, Box, Button, FormControlLabel, IconButton, LinearProgress, MenuItem, Paper, Switch, Table, TableBody, TableCell,
  TableContainer, TableHead, TablePagination, TableRow, TextField, Tooltip, Typography, useMediaQuery,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import BlockOutlined from '@mui/icons-material/BlockOutlined';
import DownloadOutlined from '@mui/icons-material/DownloadOutlined';
import EditOutlined from '@mui/icons-material/EditOutlined';
import HistoryOutlined from '@mui/icons-material/HistoryOutlined';
import SwapVertOutlined from '@mui/icons-material/SwapVertOutlined';
import { useTranslation } from 'react-i18next';
import { useNavigate, useSearchParams } from 'react-router-dom';
import * as api from '../api/productos';
import { useAuth } from '../auth/AuthContext';
import { EVENTO_ALERTAS } from '../components/AppHeader';
import { EstadoChip, NivelChip } from '../components/Chips';
import ConfirmDialog from '../components/ConfirmDialog';
import MovimientoDialog from '../components/MovimientoDialog';
import ProductoForm from '../components/ProductoForm';
import { P } from '../constants';
import { useAviso } from '../context/AvisoContext';
import { formatoCantidad } from '../utils/format';

const MONO = { fontFamily: '"IBM Plex Mono", monospace', fontSize: '0.85rem' };

/** Pantalla principal de productos y existencias. */
export default function Productos() {
  const { t, i18n } = useTranslation();
  const { tiene } = useAuth();
  const { avisar } = useAviso();
  const navigate = useNavigate();
  const [params, setParams] = useSearchParams();
  const movil = useMediaQuery((th) => th.breakpoints.down('md'));
  const [pagina, setPagina] = useState({ content: [], totalElements: 0 });
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [estado, setEstado] = useState('');
  const [busqueda, setBusqueda] = useState('');
  const [q, setQ] = useState('');
  const bajoMinimo = params.get('minimos') === '1';
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState('');
  const [form, setForm] = useState({ abierto: false, producto: null });
  const [movimiento, setMovimiento] = useState(null);
  const [aDesactivar, setADesactivar] = useState(null);
  const puedeGestionar = tiene(P.PRODUCTOS_GESTIONAR);
  const puedeMover = tiene(P.MOVIMIENTOS_REGISTRAR) || tiene(P.AJUSTES_REGISTRAR);
  const hayAcciones = puedeGestionar || puedeMover || tiene(P.MOVIMIENTOS_VER);

  useEffect(() => {
    const id = setTimeout(() => { setQ(busqueda); setPage(0); }, 300);
    return () => clearTimeout(id);
  }, [busqueda]);

  const cargar = useCallback(async () => {
    setCargando(true);
    setError('');
    try {
      setPagina(await api.listar({ page, size, estado, q, bajoMinimo }));
    } catch (e) {
      setError(e.message);
    } finally {
      setCargando(false);
    }
  }, [page, size, estado, q, bajoMinimo]);
  useEffect(() => { cargar(); }, [cargar]);

  const alternarMinimos = (e) => {
    const siguiente = new URLSearchParams(params);
    if (e.target.checked) siguiente.set('minimos', '1');
    else siguiente.delete('minimos');
    setParams(siguiente, { replace: true });
    setPage(0);
  };

  const guardar = async (cuerpo) => {
    if (form.producto) {
      await api.actualizar(form.producto.id, cuerpo);
      avisar(t('avisos.actualizado'));
    } else {
      await api.crear(cuerpo);
      avisar(t('avisos.creado'));
    }
    setForm({ abierto: false, producto: null });
    window.dispatchEvent(new Event(EVENTO_ALERTAS));
    cargar();
  };

  const confirmarDesactivar = async () => {
    try {
      await api.desactivar(aDesactivar.id);
      avisar(t('avisos.desactivado'));
      window.dispatchEvent(new Event(EVENTO_ALERTAS));
      cargar();
    } catch (e) {
      setError(e.message);
    } finally {
      setADesactivar(null);
    }
  };

  const registrado = () => {
    setMovimiento(null);
    avisar(t('movimientos.registrado'));
    window.dispatchEvent(new Event(EVENTO_ALERTAS));
    cargar();
  };

  const exportar = async () => {
    try {
      await api.descargarInventario(bajoMinimo);
      avisar(t('avisos.exportado'));
    } catch (e) {
      setError(e.message);
    }
  };

  const stock = (p) => `${formatoCantidad(p.stockActual, i18n.language)} ${p.unidad}`;
  const acciones = (p) => hayAcciones && (
    <>
      {puedeMover && p.estado === 'ACTIVO' && (
        <Tooltip title={t('productos.movimiento')}>
          <IconButton aria-label={`${t('productos.movimiento')}: ${p.nombre}`} onClick={() => setMovimiento(p)}><SwapVertOutlined /></IconButton>
        </Tooltip>
      )}
      {tiene(P.MOVIMIENTOS_VER) && (
        <Tooltip title={t('productos.historial')}>
          <IconButton aria-label={`${t('productos.historial')}: ${p.nombre}`} onClick={() => navigate(`/movimientos?productoId=${p.id}`)}><HistoryOutlined /></IconButton>
        </Tooltip>
      )}
      {puedeGestionar && (
        <>
          <Tooltip title={t('productos.editar')}>
            <IconButton aria-label={`${t('productos.editar')}: ${p.nombre}`} onClick={() => setForm({ abierto: true, producto: p })}><EditOutlined /></IconButton>
          </Tooltip>
          <Tooltip title={t('productos.desactivar')}>
            <span>
              <IconButton aria-label={`${t('productos.desactivar')}: ${p.nombre}`} disabled={p.estado === 'INACTIVO'} onClick={() => setADesactivar(p)}><BlockOutlined /></IconButton>
            </span>
          </Tooltip>
        </>
      )}
    </>
  );

  return (
    <Box component="main" id="contenido" sx={{ px: { xs: 2, md: 4 }, py: 3, maxWidth: 1200, mx: 'auto' }}>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, flexWrap: 'wrap', mb: 2 }}>
        <Typography component="h1" variant="h1" sx={{ flexGrow: 1 }}>{t('productos.titulo')}</Typography>
        {tiene(P.REPORTES_VER) && (
          <Button variant="outlined" startIcon={<DownloadOutlined />} onClick={exportar}>{t('productos.exportar')}</Button>
        )}
        {puedeGestionar && (
          <Button variant="contained" color="secondary" startIcon={<AddIcon />} onClick={() => setForm({ abierto: true, producto: null })}>
            {t('productos.nuevo')}
          </Button>
        )}
      </Box>

      <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', mb: 2, alignItems: 'center' }}>
        <TextField size="small" type="search" label={t('productos.buscar')} value={busqueda} onChange={(e) => setBusqueda(e.target.value)} sx={{ flex: '1 1 280px' }} />
        <TextField size="small" select label={t('productos.estado')} value={estado} onChange={(e) => { setEstado(e.target.value); setPage(0); }} sx={{ minWidth: 150 }}>
          <MenuItem value="">{t('productos.todos')}</MenuItem>
          <MenuItem value="ACTIVO">{t('productos.estados.ACTIVO')}</MenuItem>
          <MenuItem value="INACTIVO">{t('productos.estados.INACTIVO')}</MenuItem>
        </TextField>
        <FormControlLabel control={<Switch checked={bajoMinimo} onChange={alternarMinimos} />} label={t('productos.soloMinimos')} />
      </Box>

      <div>{error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}</div>

      <Paper>
        {cargando && <LinearProgress aria-label={t('productos.cargando')} />}
        {movil ? (
          <Box component="ul" sx={{ listStyle: 'none', m: 0, p: 0 }}>
            {pagina.content.map((p) => (
              <Box component="li" key={p.id} sx={{ p: 2, borderBottom: 1, borderColor: 'divider' }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start', gap: 1 }}>
                  <Typography sx={{ fontWeight: 600 }}>{p.nombre}</Typography>
                  <EstadoChip estado={p.estado} />
                </Box>
                <Typography sx={{ ...MONO, color: 'text.secondary' }}>{p.codigo} · {p.categoria}</Typography>
                <Box sx={{ display: 'flex', gap: 1, alignItems: 'center', mt: 0.5 }}>
                  <Typography sx={MONO}>{stock(p)}</Typography>
                  <NivelChip nivel={p.nivel} />
                </Box>
                <Box sx={{ mt: 0.5 }}>{acciones(p)}</Box>
              </Box>
            ))}
          </Box>
        ) : (
          <TableContainer>
            <Table aria-label={t('productos.titulo')}>
              <TableHead>
                <TableRow>
                  <TableCell>{t('productos.cols.codigo')}</TableCell>
                  <TableCell>{t('productos.cols.nombre')}</TableCell>
                  <TableCell>{t('productos.cols.categoria')}</TableCell>
                  <TableCell align="right">{t('productos.cols.stock')}</TableCell>
                  <TableCell align="right">{t('productos.cols.minimo')}</TableCell>
                  <TableCell>{t('productos.cols.estado')}</TableCell>
                  {hayAcciones && <TableCell align="right">{t('productos.cols.acciones')}</TableCell>}
                </TableRow>
              </TableHead>
              <TableBody>
                {pagina.content.map((p) => (
                  <TableRow key={p.id} hover>
                    <TableCell sx={MONO}>{p.codigo}</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>{p.nombre}</TableCell>
                    <TableCell sx={{ color: 'text.secondary' }}>{p.categoria}</TableCell>
                    <TableCell align="right">
                      <Box sx={{ display: 'inline-flex', gap: 1, alignItems: 'center' }}>
                        <Typography component="span" sx={MONO}>{stock(p)}</Typography>
                        {p.nivel !== 'OK' && <NivelChip nivel={p.nivel} />}
                      </Box>
                    </TableCell>
                    <TableCell align="right" sx={{ ...MONO, color: 'text.secondary' }}>{formatoCantidad(p.stockMinimo, i18n.language)}</TableCell>
                    <TableCell><EstadoChip estado={p.estado} /></TableCell>
                    {hayAcciones && <TableCell align="right" sx={{ whiteSpace: 'nowrap' }}>{acciones(p)}</TableCell>}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
        {!cargando && pagina.content.length === 0 && !error && <Typography sx={{ p: 3 }} color="text.secondary">{t('productos.vacio')}</Typography>}
        <TablePagination
          component="div" count={pagina.totalElements} page={page} rowsPerPage={size} rowsPerPageOptions={[5, 10, 25]}
          labelRowsPerPage={t('productos.filasPorPagina')} onPageChange={(_, n) => setPage(n)}
          onRowsPerPageChange={(e) => { setSize(Number(e.target.value)); setPage(0); }}
        />
      </Paper>

      <ProductoForm abierto={form.abierto} producto={form.producto} onCerrar={() => setForm({ abierto: false, producto: null })} onGuardar={guardar} />
      <MovimientoDialog abierto={Boolean(movimiento)} producto={movimiento} onCerrar={() => setMovimiento(null)} onRegistrado={registrado} />
      <ConfirmDialog
        abierto={Boolean(aDesactivar)} titulo={t('confirmar.titulo')} texto={t('confirmar.texto', { nombre: aDesactivar?.nombre })}
        confirmar={t('confirmar.si')} onConfirmar={confirmarDesactivar} onCerrar={() => setADesactivar(null)}
      />
    </Box>
  );
}
