/**
 * nombre: render.jsx
 * descripcion: Ayuda de pruebas: renderiza con tema, avisos, enrutador y una sesión con los permisos indicados.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
import { render } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { AuthProvider } from '../auth/AuthContext';
import { AvisoProvider } from '../context/AvisoContext';
import { ThemeModeProvider } from '../theme/ThemeModeProvider';

/** Crea el usuario de sesión de prueba. */
export const usuarioDe = (permisos, extra = {}) => ({
  username: 'tester', nombreCompleto: 'Persona de Prueba', email: null, perfil: 'Prueba', permisos, ...extra,
});

/**
 * Renderiza `ui` dentro de los proveedores de la aplicación.
 * @param {import('react').ReactElement} ui componente
 * @param {{permisos?: string[], ruta?: string}} [opciones] permisos de la sesión simulada y ruta inicial
 */
export function renderConSesion(ui, { permisos = [], ruta = '/' } = {}) {
  sessionStorage.setItem('sesion', JSON.stringify({ token: 't', usuario: usuarioDe(permisos) }));
  return render(
    <ThemeModeProvider>
      <AvisoProvider>
        <MemoryRouter initialEntries={[ruta]}>
          <AuthProvider>{ui}</AuthProvider>
        </MemoryRouter>
      </AvisoProvider>
    </ThemeModeProvider>,
  );
}
