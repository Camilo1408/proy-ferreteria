/**
 * nombre: AuthContext.jsx
 * descripcion: Contexto de sesión (token, usuario y permisos) con persistencia en sessionStorage y revalidación.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { setOnUnauthorized, setToken } from '../api/client';
import * as inventario from '../api/inventario';
import * as api from '../api/productos';

const AuthContext = createContext(null);
const CLAVE = 'sesion';

function leerSesion() {
  try {
    return JSON.parse(sessionStorage.getItem(CLAVE));
  } catch {
    return null;
  }
}

/**
 * Indica si un usuario tiene un permiso.
 * @param {{permisos: string[]}|null|undefined} usuario usuario de la sesión
 * @param {string} permiso nombre del permiso
 * @returns {boolean} verdadero si lo tiene
 */
export const tienePermiso = (usuario, permiso) => Boolean(usuario?.permisos?.includes(permiso));

/** Proveedor de autenticación. Al cargar con sesión guardada vuelve a pedir los permisos vigentes. */
export function AuthProvider({ children }) {
  const [sesion, setSesion] = useState(() => {
    const s = leerSesion();
    if (s) setToken(s.token);
    return s;
  });

  const guardar = useCallback((s) => {
    sessionStorage.setItem(CLAVE, JSON.stringify(s));
    setSesion(s);
  }, []);

  const salir = useCallback(() => {
    setToken(null);
    sessionStorage.removeItem(CLAVE);
    setSesion(null);
  }, []);

  useEffect(() => setOnUnauthorized(salir), [salir]);

  const entrar = useCallback(async (username, password) => {
    const r = await api.login(username, password);
    setToken(r.token);
    guardar({ token: r.token, usuario: r.usuario });
  }, [guardar]);

  /** Vuelve a pedir los datos y permisos del usuario (tras editar la cuenta o cambiar un perfil). */
  const refrescar = useCallback(async () => {
    const usuario = await inventario.obtenerCuenta();
    setSesion((actual) => {
      if (!actual) return actual;
      const nueva = { ...actual, usuario };
      sessionStorage.setItem(CLAVE, JSON.stringify(nueva));
      return nueva;
    });
    return usuario;
  }, []);

  useEffect(() => {
    if (sesion) refrescar().catch(() => {});
    // Solo al montar: los permisos guardados pueden estar desactualizados.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const valor = useMemo(() => {
    const usuario = sesion?.usuario ?? null;
    return { sesion, usuario, tiene: (p) => tienePermiso(usuario, p), entrar, salir, refrescar };
  }, [sesion, entrar, salir, refrescar]);
  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>;
}

/** Acceso al contexto de autenticación. */
export const useAuth = () => useContext(AuthContext);
