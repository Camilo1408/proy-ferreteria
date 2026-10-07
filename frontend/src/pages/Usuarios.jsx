/**
 * nombre: Usuarios.jsx
 * descripcion: Administración de usuarios: alta, edición, activación y restablecimiento de contraseña.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
import { useCallback, useEffect, useState } from 'react';
import {
  Alert, Box, Button, Chip, Dialog, DialogActions, DialogContent, DialogTitle, FormControlLabel, IconButton,
  LinearProgress, MenuItem, Paper, Switch, Table, TableBody, TableCell, TableContainer, TableHead, TablePagination,
  TableRow, TextField, Tooltip, Typography, useMediaQuery,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import EditOutlined from '@mui/icons-material/EditOutlined';
import LockResetOutlined from '@mui/icons-material/LockResetOutlined';
import { useFormik } from 'formik';
import { useTranslation } from 'react-i18next';
import {
  actualizarUsuario, crearUsuario, listarPerfiles, listarUsuarios, restablecerClave,
} from '../api/inventario';
import { useAuth } from '../auth/AuthContext';
import { useAviso } from '../context/AvisoContext';
import { restablecerClaveSchema, usuarioCrearSchema, usuarioEditarSchema } from '../validation/schemas';

const MONO = { fontFamily: '"IBM Plex Mono", monospace', fontSize: '0.85rem' };

function useCampos(formik, t) {
  return (n, extra = {}) => ({
    id: n, name: n, value: formik.values[n], onChange: formik.handleChange, onBlur: formik.handleBlur, fullWidth: true, margin: 'dense',
    error: formik.touched[n] && Boolean(formik.errors[n]),
    helperText: formik.touched[n] && formik.errors[n] ? t(formik.errors[n]) : extra.ayuda ?? ' ',
  });
}

/** Diálogo de alta o edición de usuario. En edición no se cambia el nombre de usuario ni la contraseña. */
function UsuarioDialog({ usuario, perfiles, esUnoMismo, onCerrar, onGuardado }) {
  const { t } = useTranslation();
  const editando = Boolean(usuario?.id);
  const [errorApi, setErrorApi] = useState('');
  const formik = useFormik({
    enableReinitialize: true,
    initialValues: editando
      ? { nombreCompleto: usuario.nombreCompleto, email: usuario.email ?? '', perfilId: usuario.perfil.id, activo: usuario.activo }
      : { username: '', password: '', nombreCompleto: '', email: '', perfilId: '' },
    validationSchema: editando ? usuarioEditarSchema : usuarioCrearSchema,
    onSubmit: async (v, { setSubmitting }) => {
      setErrorApi('');
      try {
        const cuerpo = { ...v, email: v.email.trim() || null, nombreCompleto: v.nombreCompleto.trim(), perfilId: Number(v.perfilId) };
        if (!editando) cuerpo.username = v.username.trim();
        await (editando ? actualizarUsuario(usuario.id, cuerpo) : crearUsuario(cuerpo));
        onGuardado(editando ? t('usuarios.actualizado') : t('usuarios.creado'));
      } catch (e) {
        setErrorApi([e.message, ...(e.detalles ?? [])].join(' · '));
      } finally {
        setSubmitting(false);
      }
    },
  });
  const campo = useCampos(formik, t);
  return (
    <Dialog open={Boolean(usuario)} onClose={onCerrar} fullWidth maxWidth="sm" aria-labelledby="usuario-titulo">
      <form onSubmit={formik.handleSubmit} noValidate>
        <DialogTitle id="usuario-titulo">{editando ? t('usuarios.editarTitulo') : t('usuarios.crearTitulo')}</DialogTitle>
        <DialogContent>
          <div>{errorApi && <Alert severity="error" sx={{ mb: 1 }}>{errorApi}</Alert>}</div>
          {!editando && (
            <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' } }}>
              <TextField {...campo('username')} label={t('usuarios.username')} required autoFocus inputProps={{ maxLength: 50, autoComplete: 'off' }} />
              <TextField {...campo('password', { ayuda: t('usuarios.ayudaClave') })} type="password" label={t('usuarios.password')} required inputProps={{ autoComplete: 'new-password' }} />
            </Box>
          )}
          <TextField {...campo('nombreCompleto')} label={t('usuarios.nombreCompleto')} required inputProps={{ maxLength: 100 }} />
          <TextField {...campo('email')} label={t('usuarios.email')} type="email" inputProps={{ maxLength: 120 }} />
          <TextField
            {...campo('perfilId', { ayuda: esUnoMismo ? t('usuarios.esUnoMismo') : undefined })} label={t('usuarios.perfil')} select required
            disabled={esUnoMismo}
          >
            {perfiles.map((p) => <MenuItem key={p.id} value={p.id}>{p.nombre}</MenuItem>)}
          </TextField>
          {editando && (
            <FormControlLabel
              control={<Switch name="activo" checked={formik.values.activo} onChange={formik.handleChange} disabled={esUnoMismo} />}
              label={t('usuarios.activo')}
            />
          )}
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={onCerrar}>{t('form.cancelar')}</Button>
          <Button type="submit" variant="contained" disabled={formik.isSubmitting}>{t('form.guardar')}</Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}

/** Diálogo para restablecer la contraseña de otro usuario. */
function ClaveDialog({ usuario, onCerrar, onGuardado }) {
  const { t } = useTranslation();
  const [errorApi, setErrorApi] = useState('');
  const formik = useFormik({
    initialValues: { nueva: '' },
    validationSchema: restablecerClaveSchema,
    onSubmit: async (v, { setSubmitting }) => {
      setErrorApi('');
      try {
        await restablecerClave(usuario.id, v.nueva);
        onGuardado(t('usuarios.claveCambiada'));
      } catch (e) {
        setErrorApi(e.message);
      } finally {
        setSubmitting(false);
      }
    },
  });
  const campo = useCampos(formik, t);
  return (
    <Dialog open={Boolean(usuario)} onClose={onCerrar} fullWidth maxWidth="xs" aria-labelledby="clave-titulo">
      <form onSubmit={formik.handleSubmit} noValidate>
        <DialogTitle id="clave-titulo">{t('usuarios.claveTitulo', { usuario: usuario?.username })}</DialogTitle>
        <DialogContent>
          <div>{errorApi && <Alert severity="error" sx={{ mb: 1 }}>{errorApi}</Alert>}</div>
          <TextField {...campo('nueva', { ayuda: t('usuarios.ayudaClave') })} type="password" label={t('usuarios.nuevaClave')} required autoFocus inputProps={{ autoComplete: 'new-password' }} />
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={onCerrar}>{t('form.cancelar')}</Button>
          <Button type="submit" variant="contained" disabled={formik.isSubmitting}>{t('form.guardar')}</Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}

/** Listado y administración de usuarios. */
export default function Usuarios() {
  const { t } = useTranslation();
  const { usuario: yo } = useAuth();
  const { avisar } = useAviso();
  const movil = useMediaQuery((th) => th.breakpoints.down('md'));
  const [pagina, setPagina] = useState({ content: [], totalElements: 0 });
  const [perfiles, setPerfiles] = useState([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [busqueda, setBusqueda] = useState('');
  const [q, setQ] = useState('');
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState('');
  const [editando, setEditando] = useState(null);
  const [claveDe, setClaveDe] = useState(null);

  useEffect(() => {
    const id = setTimeout(() => { setQ(busqueda); setPage(0); }, 300);
    return () => clearTimeout(id);
  }, [busqueda]);

  const cargar = useCallback(async () => {
    setCargando(true);
    setError('');
    try {
      setPagina(await listarUsuarios({ page, size, q }));
    } catch (e) {
      setError(e.message);
    } finally {
      setCargando(false);
    }
  }, [page, size, q]);
  useEffect(() => { cargar(); }, [cargar]);
  useEffect(() => { listarPerfiles().then(setPerfiles).catch(() => {}); }, []);

  const guardado = (mensaje) => { setEditando(null); setClaveDe(null); avisar(mensaje); cargar(); };

  const estado = (u) => <Chip size="small" variant="outlined" color={u.activo ? 'success' : 'default'} label={t(`usuarios.estados.${u.activo}`)} />;
  const acciones = (u) => (
    <>
      <Tooltip title={t('usuarios.editar')}>
        <IconButton aria-label={`${t('usuarios.editar')}: ${u.username}`} onClick={() => setEditando(u)}><EditOutlined /></IconButton>
      </Tooltip>
      <Tooltip title={t('usuarios.clave')}>
        <IconButton aria-label={`${t('usuarios.clave')}: ${u.username}`} onClick={() => setClaveDe(u)}><LockResetOutlined /></IconButton>
      </Tooltip>
    </>
  );

  return (
    <Box component="main" id="contenido" sx={{ px: { xs: 2, md: 4 }, py: 3, maxWidth: 1100, mx: 'auto' }}>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, flexWrap: 'wrap', mb: 2 }}>
        <Typography component="h1" variant="h1" sx={{ flexGrow: 1 }}>{t('usuarios.titulo')}</Typography>
        <Button variant="contained" color="secondary" startIcon={<AddIcon />} onClick={() => setEditando({})}>{t('usuarios.nuevo')}</Button>
      </Box>
      <TextField size="small" type="search" label={t('usuarios.buscar')} value={busqueda} onChange={(e) => setBusqueda(e.target.value)} sx={{ mb: 2, width: { xs: '100%', sm: 360 } }} />
      <div>{error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}</div>
      <Paper>
        {cargando && <LinearProgress aria-label={t('app.cargando')} />}
        {movil ? (
          <Box component="ul" sx={{ listStyle: 'none', m: 0, p: 0 }}>
            {pagina.content.map((u) => (
              <Box component="li" key={u.id} sx={{ p: 2, borderBottom: 1, borderColor: 'divider' }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', gap: 1 }}>
                  <Typography sx={{ fontWeight: 600 }}>{u.nombreCompleto}</Typography>{estado(u)}
                </Box>
                <Typography sx={{ ...MONO, color: 'text.secondary' }}>{u.username} · {u.perfil.nombre}</Typography>
                <Box sx={{ mt: 0.5 }}>{acciones(u)}</Box>
              </Box>
            ))}
          </Box>
        ) : (
          <TableContainer>
            <Table aria-label={t('usuarios.titulo')}>
              <TableHead>
                <TableRow>
                  <TableCell>{t('usuarios.cols.usuario')}</TableCell>
                  <TableCell>{t('usuarios.cols.nombre')}</TableCell>
                  <TableCell>{t('usuarios.cols.correo')}</TableCell>
                  <TableCell>{t('usuarios.cols.perfil')}</TableCell>
                  <TableCell>{t('usuarios.cols.estado')}</TableCell>
                  <TableCell align="right">{t('usuarios.cols.acciones')}</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {pagina.content.map((u) => (
                  <TableRow key={u.id} hover>
                    <TableCell sx={MONO}>{u.username}</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>{u.nombreCompleto}</TableCell>
                    <TableCell sx={{ color: 'text.secondary' }}>{u.email}</TableCell>
                    <TableCell>{u.perfil.nombre}</TableCell>
                    <TableCell>{estado(u)}</TableCell>
                    <TableCell align="right" sx={{ whiteSpace: 'nowrap' }}>{acciones(u)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
        {!cargando && pagina.content.length === 0 && !error && <Typography sx={{ p: 3 }} color="text.secondary">{t('usuarios.vacio')}</Typography>}
        <TablePagination
          component="div" count={pagina.totalElements} page={page} rowsPerPage={size} rowsPerPageOptions={[5, 10, 25]}
          labelRowsPerPage={t('productos.filasPorPagina')} onPageChange={(_, n) => setPage(n)}
          onRowsPerPageChange={(e) => { setSize(Number(e.target.value)); setPage(0); }}
        />
      </Paper>
      <UsuarioDialog usuario={editando} perfiles={perfiles} esUnoMismo={Boolean(editando?.id) && editando.username === yo?.username} onCerrar={() => setEditando(null)} onGuardado={guardado} />
      <ClaveDialog usuario={claveDe} onCerrar={() => setClaveDe(null)} onGuardado={guardado} />
    </Box>
  );
}
