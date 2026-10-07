/**
 * nombre: Productos.jsx
 * descripcion: Listado de productos con búsqueda, filtro, paginación y acciones de administrador.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
 */
import { useCallback, useEffect, useState } from 'react';
import {
  Alert, Box, Button, Chip, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle,
  IconButton, LinearProgress, MenuItem, Paper, Snackbar, Table, TableBody, TableCell, TableContainer,
  TableHead, TablePagination, TableRow, TextField, Tooltip, Typography, useMediaQuery,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import BlockOutlined from '@mui/icons-material/BlockOutlined';
import EditOutlined from '@mui/icons-material/EditOutlined';
import { useTranslation } from 'react-i18next';
import * as api from '../api/productos';
import { useAuth } from '../auth/AuthContext';
import ProductoForm from '../components/ProductoForm';

const CATEGORIA_FONT = { fontFamily: '"IBM Plex Mono", monospace', fontSize: '0.85rem' };

/** Pantalla principal del CRUD. */
export default function Productos() {
  const { t } = useTranslation();
  const { esAdmin } = useAuth();
  const movil = useMediaQuery((th) => th.breakpoints.down('md'));
  const [pagina, setPagina] = useState({ content: [], totalElements: 0 });
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [estado, setEstado] = useState('');
  const [busqueda, setBusqueda] = useState('');
  const [q, setQ] = useState('');
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState('');
  const [aviso, setAviso] = useState('');
  const [form, setForm] = useState({ abierto: false, producto: null });
  const [aDesactivar, setADesactivar] = useState(null);

  useEffect(() => {
    const id = setTimeout(() => { setQ(busqueda); setPage(0); }, 300);
    return () => clearTimeout(id);
  }, [busqueda]);

  const cargar = useCallback(async () => {
    setCargando(true);
    setError('');
    try {
      setPagina(await api.listar({ page, size, estado, q }));
    } catch (e) {
      setError(e.message);
    } finally {
      setCargando(false);
    }
  }, [page, size, estado, q]);

  useEffect(() => { cargar(); }, [cargar]);

  const guardar = async (v) => {
    if (form.producto) {
      await api.actualizar(form.producto.id, v);
      setAviso(t('avisos.actualizado'));
    } else {
      await api.crear({ nombre: v.nombre, categoria: v.categoria, descripcion: v.descripcion });
      setAviso(t('avisos.creado'));
    }
    setForm({ abierto: false, producto: null });
    cargar();
  };

  const confirmarDesactivar = async () => {
    try {
      await api.desactivar(aDesactivar.id);
      setAviso(t('avisos.desactivado'));
      cargar();
    } catch (e) {
      setError(e.message);
    } finally {
      setADesactivar(null);
    }
  };

  const chip = (e) => (
    <Chip size="small" variant="outlined" color={e === 'ACTIVO' ? 'success' : 'default'} label={t(`productos.estados.${e}`)} />
  );
  const acciones = (p) => esAdmin && (
    <>
      <Tooltip title={t('productos.editar')}>
        <IconButton aria-label={`${t('productos.editar')}: ${p.nombre}`} onClick={() => setForm({ abierto: true, producto: p })}>
          <EditOutlined />
        </IconButton>
      </Tooltip>
      <Tooltip title={t('productos.desactivar')}>
        <span>
          <IconButton
            aria-label={`${t('productos.desactivar')}: ${p.nombre}`}
            disabled={p.estado === 'INACTIVO'}
            onClick={() => setADesactivar(p)}
          >
            <BlockOutlined />
          </IconButton>
        </span>
      </Tooltip>
    </>
  );

  return (
    <Box component="main" id="contenido" sx={{ px: { xs: 2, md: 4 }, py: 3, maxWidth: 1100, mx: 'auto' }}>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, flexWrap: 'wrap', mb: 2 }}>
        <Typography component="h1" variant="h1" sx={{ flexGrow: 1 }}>{t('productos.titulo')}</Typography>
        {esAdmin && (
          <Button variant="contained" color="secondary" startIcon={<AddIcon />} onClick={() => setForm({ abierto: true, producto: null })}>
            {t('productos.nuevo')}
          </Button>
        )}
      </Box>

      <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', mb: 2 }}>
        <TextField
          size="small" type="search" label={t('productos.buscar')} value={busqueda}
          onChange={(e) => setBusqueda(e.target.value)} sx={{ flex: '1 1 260px' }}
        />
        <TextField
          size="small" select label={t('productos.estado')} value={estado}
          onChange={(e) => { setEstado(e.target.value); setPage(0); }} sx={{ minWidth: 160 }}
        >
          <MenuItem value="">{t('productos.todos')}</MenuItem>
          <MenuItem value="ACTIVO">{t('productos.estados.ACTIVO')}</MenuItem>
          <MenuItem value="INACTIVO">{t('productos.estados.INACTIVO')}</MenuItem>
        </TextField>
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
                  {chip(p.estado)}
                </Box>
                <Typography sx={{ ...CATEGORIA_FONT, color: 'text.secondary' }}>{p.categoria}</Typography>
                {p.descripcion && <Typography variant="body2" sx={{ mt: 0.5 }}>{p.descripcion}</Typography>}
                <Box sx={{ mt: 0.5 }}>{acciones(p)}</Box>
              </Box>
            ))}
          </Box>
        ) : (
          <TableContainer>
            <Table aria-label={t('productos.titulo')}>
              <TableHead>
                <TableRow>
                  <TableCell>{t('productos.cols.nombre')}</TableCell>
                  <TableCell>{t('productos.cols.categoria')}</TableCell>
                  <TableCell>{t('productos.cols.descripcion')}</TableCell>
                  <TableCell>{t('productos.cols.estado')}</TableCell>
                  {esAdmin && <TableCell align="right">{t('productos.cols.acciones')}</TableCell>}
                </TableRow>
              </TableHead>
              <TableBody>
                {pagina.content.map((p) => (
                  <TableRow key={p.id} hover>
                    <TableCell sx={{ fontWeight: 600 }}>{p.nombre}</TableCell>
                    <TableCell sx={CATEGORIA_FONT}>{p.categoria}</TableCell>
                    <TableCell sx={{ color: 'text.secondary', maxWidth: 320 }}>{p.descripcion}</TableCell>
                    <TableCell>{chip(p.estado)}</TableCell>
                    {esAdmin && <TableCell align="right">{acciones(p)}</TableCell>}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
        {!cargando && pagina.content.length === 0 && !error && (
          <Typography sx={{ p: 3 }} color="text.secondary">{t('productos.vacio')}</Typography>
        )}
        <TablePagination
          component="div" count={pagina.totalElements} page={page} rowsPerPage={size}
          rowsPerPageOptions={[5, 10, 25]} labelRowsPerPage={t('productos.filasPorPagina')}
          onPageChange={(_, n) => setPage(n)}
          onRowsPerPageChange={(e) => { setSize(Number(e.target.value)); setPage(0); }}
        />
      </Paper>

      <ProductoForm
        abierto={form.abierto} producto={form.producto}
        onCerrar={() => setForm({ abierto: false, producto: null })} onGuardar={guardar}
      />

      <Dialog open={Boolean(aDesactivar)} onClose={() => setADesactivar(null)} aria-labelledby="conf-titulo">
        <DialogTitle id="conf-titulo">{t('confirmar.titulo')}</DialogTitle>
        <DialogContent>
          <DialogContentText>{t('confirmar.texto', { nombre: aDesactivar?.nombre })}</DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setADesactivar(null)}>{t('form.cancelar')}</Button>
          <Button color="error" variant="contained" onClick={confirmarDesactivar}>{t('confirmar.si')}</Button>
        </DialogActions>
      </Dialog>

      <Snackbar open={Boolean(aviso)} autoHideDuration={4000} onClose={() => setAviso('')} message={aviso} />
    </Box>
  );
}
