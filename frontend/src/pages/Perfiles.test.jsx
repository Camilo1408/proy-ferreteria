/**
 * nombre: Perfiles.test.jsx
 * descripcion: Pruebas de la gestión de perfiles: agrupación de permisos por módulo y formulario.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderConSesion } from '../test/render';
import Perfiles, { agruparPorModulo } from './Perfiles';

const catalogo = [
  ['PRODUCTOS_VER', 'productos'], ['PRODUCTOS_GESTIONAR', 'productos'], ['MOVIMIENTOS_VER', 'inventario'],
  ['MOVIMIENTOS_REGISTRAR', 'inventario'], ['AJUSTES_REGISTRAR', 'inventario'], ['ALERTAS_VER', 'alertas'],
  ['ALERTAS_GESTIONAR', 'alertas'], ['REPORTES_VER', 'reportes'], ['USUARIOS_GESTIONAR', 'administracion'],
  ['PERFILES_GESTIONAR', 'administracion'],
].map(([codigo, modulo]) => ({ codigo, modulo }));

const perfilesBase = [
  { id: 1, nombre: 'ADMINISTRADOR', descripcion: 'Acceso total', sistema: true, permisos: catalogo.map((c) => c.codigo), usuarios: 1 },
  { id: 2, nombre: 'Bodega', descripcion: null, sistema: false, permisos: ['PRODUCTOS_VER'], usuarios: 0 },
];

vi.mock('../api/inventario', () => ({
  listarPerfiles: vi.fn(), catalogoPermisos: vi.fn(), crearPerfil: vi.fn(), actualizarPerfil: vi.fn(), eliminarPerfil: vi.fn(),
  obtenerCuenta: vi.fn(() => Promise.resolve({ username: 'tester', nombreCompleto: 'T', email: null, perfil: 'P', permisos: ['PERFILES_GESTIONAR'] })),
}));
import { catalogoPermisos, crearPerfil, eliminarPerfil, listarPerfiles } from '../api/inventario';

beforeEach(() => {
  vi.clearAllMocks();
  listarPerfiles.mockResolvedValue(perfilesBase);
  catalogoPermisos.mockResolvedValue(catalogo);
});

describe('agruparPorModulo', () => {
  it('agrupa el catálogo en el orden de los módulos', () => {
    const g = agruparPorModulo(catalogo);
    expect(g.map((x) => x.modulo)).toEqual(['productos', 'inventario', 'alertas', 'reportes', 'administracion']);
    expect(g[1].permisos).toEqual(['MOVIMIENTOS_VER', 'MOVIMIENTOS_REGISTRAR', 'AJUSTES_REGISTRAR']);
  });
  it('omite módulos sin permisos y conserva solo los conocidos', () => {
    expect(agruparPorModulo([{ codigo: 'X', modulo: 'otro' }])).toEqual([]);
    expect(agruparPorModulo([])).toEqual([]);
  });
});

describe('Perfiles', () => {
  const montar = () => renderConSesion(<Perfiles />, { permisos: ['PERFILES_GESTIONAR'] });

  it('lista los perfiles, marca el de sistema y no permite eliminarlo', async () => {
    montar();
    expect(await screen.findByText('Bodega')).toBeInTheDocument();
    expect(screen.getByText('ADMINISTRADOR')).toBeInTheDocument();
    expect(screen.getByText('Sistema')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Eliminar perfil: Bodega' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Eliminar perfil: ADMINISTRADOR' })).not.toBeInTheDocument();
  });

  it('el perfil de sistema se abre en solo lectura', async () => {
    montar();
    await userEvent.click(await screen.findByRole('button', { name: 'Editar perfil: ADMINISTRADOR' }));
    const dialogo = within(await screen.findByRole('dialog'));
    expect(dialogo.getByText('Perfil del sistema: solo lectura')).toBeInTheDocument();
    expect(dialogo.getByLabelText(/Nombre del perfil/)).toBeDisabled();
    expect(dialogo.queryByRole('button', { name: 'Guardar' })).not.toBeInTheDocument();
  });

  it('exige elegir al menos un permiso y crea el perfil con los permisos marcados', async () => {
    crearPerfil.mockResolvedValue({ id: 3 });
    montar();
    await userEvent.click(await screen.findByRole('button', { name: 'Nuevo perfil' }));
    const dialogo = within(await screen.findByRole('dialog'));
    await userEvent.type(dialogo.getByLabelText(/Nombre del perfil/), 'Vendedor');
    await userEvent.click(dialogo.getByRole('button', { name: 'Guardar' }));
    expect(await dialogo.findByText('Elija al menos un permiso')).toBeInTheDocument();
    expect(crearPerfil).not.toHaveBeenCalled();
    await userEvent.click(dialogo.getByLabelText('Ver productos y panel'));
    await userEvent.click(dialogo.getByLabelText('Registrar entradas y salidas'));
    await userEvent.click(dialogo.getByRole('button', { name: 'Guardar' }));
    await waitFor(() => expect(crearPerfil).toHaveBeenCalledTimes(1));
    const cuerpo = crearPerfil.mock.calls[0][0];
    expect(cuerpo.nombre).toBe('Vendedor');
    expect(cuerpo.descripcion).toBeNull();
    expect([...cuerpo.permisos].sort()).toEqual(['MOVIMIENTOS_REGISTRAR', 'PRODUCTOS_VER']);
  });

  it('«Todo el módulo» marca y desmarca los permisos del módulo', async () => {
    montar();
    await userEvent.click(await screen.findByRole('button', { name: 'Nuevo perfil' }));
    const dialogo = within(await screen.findByRole('dialog'));
    const grupo = within(dialogo.getByRole('group', { name: 'Inventario' }));
    await userEvent.click(grupo.getByLabelText('Todo el módulo'));
    expect(grupo.getByLabelText('Ver historial de movimientos')).toBeChecked();
    expect(grupo.getByLabelText('Registrar entradas y salidas')).toBeChecked();
    expect(grupo.getByLabelText('Registrar ajustes por conteo')).toBeChecked();
    await userEvent.click(grupo.getByLabelText('Todo el módulo'));
    expect(grupo.getByLabelText('Registrar ajustes por conteo')).not.toBeChecked();
  });

  it('pide confirmación antes de eliminar y elimina el perfil elegido', async () => {
    eliminarPerfil.mockResolvedValue(null);
    montar();
    await userEvent.click(await screen.findByRole('button', { name: 'Eliminar perfil: Bodega' }));
    const dialogo = within(await screen.findByRole('dialog', { name: 'Eliminar perfil' }));
    expect(dialogo.getByText(/Bodega/)).toBeInTheDocument();
    await userEvent.click(dialogo.getByRole('button', { name: 'Eliminar' }));
    await waitFor(() => expect(eliminarPerfil).toHaveBeenCalledWith(2));
  });
});
