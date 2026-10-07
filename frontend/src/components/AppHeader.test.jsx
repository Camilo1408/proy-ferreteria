/**
 * nombre: AppHeader.test.jsx
 * descripcion: Pruebas de la navegación por permisos y de la campana de alertas.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
import { screen, waitFor } from '@testing-library/react';
import { renderConSesion } from '../test/render';
import AppHeader from './AppHeader';

const mockCuenta = { current: null };
vi.mock('../api/inventario', () => ({
  resumenAlertas: vi.fn(() => Promise.resolve({ abiertas: 3, bajos: 2, agotados: 1 })),
  obtenerCuenta: vi.fn(() => Promise.resolve(mockCuenta.current)),
}));
import { resumenAlertas } from '../api/inventario';

const montar = (permisos) => {
  mockCuenta.current = { username: 'tester', nombreCompleto: 'T', email: null, perfil: 'Prueba', permisos };
  return renderConSesion(<AppHeader />, { permisos });
};
const enlaces = () => screen.getAllByRole('link').map((a) => a.textContent);

beforeEach(() => vi.clearAllMocks());

describe('AppHeader', () => {
  it('un perfil de solo productos ve Panel, Productos y Mi cuenta', () => {
    montar(['PRODUCTOS_VER']);
    expect(enlaces()).toEqual(['Panel', 'Productos', 'Mi cuenta']);
    expect(screen.queryByRole('button', { name: /Alertas pendientes/ })).not.toBeInTheDocument();
  });

  it('el administrador ve todas las secciones', () => {
    montar(['PRODUCTOS_VER', 'MOVIMIENTOS_VER', 'ALERTAS_VER', 'USUARIOS_GESTIONAR', 'PERFILES_GESTIONAR']);
    expect(enlaces()).toEqual(['Panel', 'Productos', 'Movimientos', 'Alertas', 'Usuarios', 'Perfiles', 'Mi cuenta']);
  });

  it('gestionar usuarios no da acceso a perfiles ni al revés', () => {
    montar(['USUARIOS_GESTIONAR']);
    expect(enlaces()).toEqual(['Usuarios', 'Mi cuenta']);
  });

  it('con permiso de alertas muestra la campana con el contador accesible', async () => {
    montar(['ALERTAS_VER']);
    expect(await screen.findByRole('button', { name: 'Alertas pendientes: 3' })).toBeInTheDocument();
    expect(resumenAlertas).toHaveBeenCalled();
  });

  it('el evento de cambio de alertas vuelve a consultar el contador', async () => {
    montar(['ALERTAS_VER']);
    await screen.findByRole('button', { name: 'Alertas pendientes: 3' });
    const antes = resumenAlertas.mock.calls.length;
    window.dispatchEvent(new Event('alertas:cambio'));
    await waitFor(() => expect(resumenAlertas.mock.calls.length).toBeGreaterThan(antes));
  });

  it('la sección activa se marca con aria-current', () => {
    sessionStorage.setItem('sesion', JSON.stringify({ token: 't', usuario: { username: 'u', nombreCompleto: 'U', perfil: 'P', permisos: ['PRODUCTOS_VER'] } }));
    mockCuenta.current = { username: 'u', nombreCompleto: 'U', email: null, perfil: 'P', permisos: ['PRODUCTOS_VER'] };
    renderConSesion(<AppHeader />, { permisos: ['PRODUCTOS_VER'], ruta: '/productos' });
    expect(screen.getByRole('link', { name: 'Productos' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('link', { name: 'Panel' })).not.toHaveAttribute('aria-current');
  });

  it('con el idioma en inglés traduce la navegación', async () => {
    const i18n = (await import('../i18n')).default;
    montar(['PRODUCTOS_VER']);
    await i18n.changeLanguage('en');
    await waitFor(() => expect(enlaces()).toEqual(['Dashboard', 'Products', 'My account']));
  });
});
