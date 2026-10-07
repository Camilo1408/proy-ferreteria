/**
 * nombre: Perfiles.jsx
 * descripcion: Administración de perfiles con permisos modulares agrupados por módulo.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
import { useCallback, useEffect, useState } from 'react';
import {
  Alert, Box, Button, Checkbox, Chip, Dialog, DialogActions, DialogContent, DialogTitle, FormControlLabel, FormGroup,
  FormHelperText, IconButton, LinearProgress, Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  TextField, Tooltip, Typography, useMediaQuery,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import DeleteOutline from '@mui/icons-material/DeleteOutline';
import EditOutlined from '@mui/icons-material/EditOutlined';
import { useFormik } from 'formik';
import { useTranslation } from 'react-i18next';
import { actualizarPerfil, catalogoPermisos, crearPerfil, eliminarPerfil, listarPerfiles } from '../api/inventario';
import { useAuth } from '../auth/AuthContext';
import ConfirmDialog from '../components/ConfirmDialog';
import { MODULOS } from '../constants';
import { useAviso } from '../context/AvisoContext';
import { perfilSchema } from '../validation/schemas';

/** Agrupa el catálogo plano `[{codigo, modulo}]` por módulo respetando el orden de {@link MODULOS}. */
export function agruparPorModulo(catalogo) {
  return MODULOS.map((m) => ({ modulo: m, permisos: catalogo.filter((p) => p.modulo === m).map((p) => p.codigo) }))
    .filter((g) => g.permisos.length > 0);
}

/** Diálogo de alta, edición o consulta (perfil de sistema) de un perfil. */
function PerfilDialog({ perfil, catalogo, onCerrar, onGuardado }) {
  const { t } = useTranslation();
  const editando = Boolean(perfil?.id);
  const soloLectura = Boolean(perfil?.sistema);
  const [errorApi, setErrorApi] = useState('');
  const formik = useFormik({
    enableReinitialize: true,
    initialValues: { nombre: perfil?.nombre ?? '', descripcion: perfil?.descripcion ?? '', permisos: perfil?.permisos ?? [] },
    validationSchema: perfilSchema,
    onSubmit: async (v, { setSubmitting }) => {
      setErrorApi('');
      try {
        const cuerpo = { nombre: v.nombre.trim(), descripcion: v.descripcion.trim() || null, permisos: v.permisos };
        await (editando ? actualizarPerfil(perfil.id, cuerpo) : crearPerfil(cuerpo));
        onGuardado(editando ? t('perfiles.actualizado') : t('perfiles.creado'));
      } catch (e) {
        setErrorApi([e.message, ...(e.detalles ?? [])].join(' · '));
      } finally {
        setSubmitting(false);
      }
    },
  });
  const alternar = (codigo) => {
    const s = new Set(formik.values.permisos);
    if (s.has(codigo)) s.delete(codigo); else s.add(codigo);
    formik.setFieldValue('permisos', [...s]);
  };
  const alternarModulo = (permisos, todos) => {
    const s = new Set(formik.values.permisos);
    permisos.forEach((p) => (todos ? s.delete(p) : s.add(p)));
    formik.setFieldValue('permisos', [...s]);
  };
  const errNombre = formik.touched.nombre && formik.errors.nombre;
  const errPermisos = formik.touched.permisos && formik.errors.permisos;
  return (
    <Dialog
      open={Boolean(perfil)} onClose={onCerrar} fullWidth maxWidth="sm" aria-labelledby="perfil-titulo"
      TransitionProps={{ onExited: () => { formik.resetForm(); setErrorApi(''); } }}
    >
      <form onSubmit={formik.handleSubmit} noValidate>
        <DialogTitle id="perfil-titulo">{editando ? t('perfiles.editarTitulo') : t('perfiles.crearTitulo')}</DialogTitle>
        <DialogContent>
          <div>{errorApi && <Alert severity="error" sx={{ mb: 1 }}>{errorApi}</Alert>}</div>
          {soloLectura && <Alert severity="info" sx={{ mb: 1 }}>{t('perfiles.soloLectura')}</Alert>}
          <TextField
            id="nombre" name="nombre" label={t('perfiles.nombre')} required fullWidth margin="dense" autoFocus disabled={soloLectura}
            value={formik.values.nombre} onChange={formik.handleChange} onBlur={formik.handleBlur}
            error={Boolean(errNombre)} helperText={errNombre ? t(errNombre) : ' '} inputProps={{ maxLength: 60 }}
          />
          <TextField
            id="descripcion" name="descripcion" label={t('perfiles.descripcion')} fullWidth margin="dense" disabled={soloLectura}
            value={formik.values.descripcion} onChange={formik.handleChange} onBlur={formik.handleBlur} inputProps={{ maxLength: 200 }}
          />
          <Typography component="h2" variant="h2" sx={{ mt: 1, mb: 0.5 }}>{t('perfiles.permisos')}</Typography>
          {agruparPorModulo(catalogo).map((g) => {
            const todos = g.permisos.every((p) => formik.values.permisos.includes(p));
            return (
              <Box component="fieldset" key={g.modulo} sx={{ border: 1, borderColor: 'divider', borderRadius: 1, mb: 1.5, p: 1.5, minInlineSize: 0 }}>
                <Box component="legend" sx={{ px: 0.5, fontWeight: 600, fontSize: '0.9rem' }}>{t(`modulos.${g.modulo}`)}</Box>
                <FormControlLabel
                  control={<Checkbox size="small" checked={todos} disabled={soloLectura} onChange={() => alternarModulo(g.permisos, todos)} />}
                  label={<Typography variant="body2" color="text.secondary">{t('perfiles.marcarModulo')}</Typography>}
                />
                <FormGroup>
                  {g.permisos.map((p) => (
                    <FormControlLabel
                      key={p} label={t(`permisos.${p}`)}
                      control={<Checkbox size="small" checked={formik.values.permisos.includes(p)} disabled={soloLectura} onChange={() => alternar(p)} inputProps={{ 'data-permiso': p }} />}
                    />
                  ))}
                </FormGroup>
              </Box>
            );
          })}
          {errPermisos && <FormHelperText error role="alert">{t(errPermisos)}</FormHelperText>}
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={onCerrar}>{soloLectura ? t('app.cerrar') : t('form.cancelar')}</Button>
          {!soloLectura && <Button type="submit" variant="contained" disabled={formik.isSubmitting}>{t('form.guardar')}</Button>}
        </DialogActions>
      </form>
    </Dialog>
  );
}

/** Listado y administración de perfiles. */
export default function Perfiles() {
  const { t } = useTranslation();
  const { refrescar } = useAuth();
  const { avisar } = useAviso();
  const movil = useMediaQuery((th) => th.breakpoints.down('md'));
  const [perfiles, setPerfiles] = useState([]);
  const [catalogo, setCatalogo] = useState([]);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState('');
  const [editando, setEditando] = useState(null);
  const [aEliminar, setAEliminar] = useState(null);

  const cargar = useCallback(async () => {
    setCargando(true);
    setError('');
    try {
      const [p, c] = await Promise.all([listarPerfiles(), catalogoPermisos()]);
      setPerfiles(p);
      setCatalogo(c);
    } catch (e) {
      setError(e.message);
    } finally {
      setCargando(false);
    }
  }, []);
  useEffect(() => { cargar(); }, [cargar]);

  const guardado = (mensaje) => {
    setEditando(null);
    avisar(mensaje);
    cargar();
    refrescar().catch(() => {});
  };
  const confirmarEliminar = async () => {
    try {
      await eliminarPerfil(aEliminar.id);
      avisar(t('perfiles.eliminado'));
      cargar();
    } catch (e) {
      setError(e.message);
    } finally {
      setAEliminar(null);
    }
  };

  const acciones = (p) => (
    <>
      <Tooltip title={p.sistema ? t('perfiles.soloLectura') : t('perfiles.editar')}>
        <IconButton aria-label={`${t('perfiles.editar')}: ${p.nombre}`} onClick={() => setEditando(p)}><EditOutlined /></IconButton>
      </Tooltip>
      {!p.sistema && (
        <Tooltip title={t('perfiles.eliminar')}>
          <IconButton aria-label={`${t('perfiles.eliminar')}: ${p.nombre}`} onClick={() => setAEliminar(p)}><DeleteOutline /></IconButton>
        </Tooltip>
      )}
    </>
  );
  const nombre = (p) => (
    <>
      <Typography component="span" sx={{ fontWeight: 600 }}>{p.nombre}</Typography>
      {p.sistema && <Chip size="small" label={t('perfiles.sistema')} sx={{ ml: 1 }} />}
    </>
  );

  return (
    <Box component="main" id="contenido" sx={{ px: { xs: 2, md: 4 }, py: 3, maxWidth: 1100, mx: 'auto' }}>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, flexWrap: 'wrap', mb: 2 }}>
        <Typography component="h1" variant="h1" sx={{ flexGrow: 1 }}>{t('perfiles.titulo')}</Typography>
        <Button variant="contained" color="secondary" startIcon={<AddIcon />} onClick={() => setEditando({})}>{t('perfiles.nuevo')}</Button>
      </Box>
      <div>{error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}</div>
      <Paper>
        {cargando && <LinearProgress aria-label={t('app.cargando')} />}
        {movil ? (
          <Box component="ul" sx={{ listStyle: 'none', m: 0, p: 0 }}>
            {perfiles.map((p) => (
              <Box component="li" key={p.id} sx={{ p: 2, borderBottom: 1, borderColor: 'divider' }}>
                <Box>{nombre(p)}</Box>
                {p.descripcion && <Typography variant="body2" color="text.secondary">{p.descripcion}</Typography>}
                <Typography variant="body2">{t('perfiles.cantidadPermisos', { n: p.permisos.length })} · {p.usuarios} {t('perfiles.cols.usuarios').toLowerCase()}</Typography>
                <Box sx={{ mt: 0.5 }}>{acciones(p)}</Box>
              </Box>
            ))}
          </Box>
        ) : (
          <TableContainer>
            <Table aria-label={t('perfiles.titulo')}>
              <TableHead>
                <TableRow>
                  <TableCell>{t('perfiles.cols.nombre')}</TableCell>
                  <TableCell>{t('perfiles.cols.descripcion')}</TableCell>
                  <TableCell align="right">{t('perfiles.cols.permisos')}</TableCell>
                  <TableCell align="right">{t('perfiles.cols.usuarios')}</TableCell>
                  <TableCell align="right">{t('perfiles.cols.acciones')}</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {perfiles.map((p) => (
                  <TableRow key={p.id} hover>
                    <TableCell>{nombre(p)}</TableCell>
                    <TableCell sx={{ color: 'text.secondary' }}>{p.descripcion}</TableCell>
                    <TableCell align="right">{p.permisos.length}</TableCell>
                    <TableCell align="right">{p.usuarios}</TableCell>
                    <TableCell align="right" sx={{ whiteSpace: 'nowrap' }}>{acciones(p)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
        {!cargando && perfiles.length === 0 && !error && <Typography sx={{ p: 3 }} color="text.secondary">{t('perfiles.vacio')}</Typography>}
      </Paper>
      <PerfilDialog perfil={editando} catalogo={catalogo} onCerrar={() => setEditando(null)} onGuardado={guardado} />
      <ConfirmDialog
        abierto={Boolean(aEliminar)} titulo={t('perfiles.confirmarTitulo')} texto={t('perfiles.confirmarTexto', { nombre: aEliminar?.nombre })}
        confirmar={t('perfiles.eliminarSi')} onConfirmar={confirmarEliminar} onCerrar={() => setAEliminar(null)}
      />
    </Box>
  );
}
