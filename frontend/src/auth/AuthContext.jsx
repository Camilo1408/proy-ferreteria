/**
 * nombre: AuthContext.jsx
 * descripcion: Contexto de sesión (token JWT, usuario y rol) con persistencia en sessionStorage.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
 */
import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { setOnUnauthorized, setToken } from '../api/client';
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

/** Proveedor de autenticación. */
export function AuthProvider({ children }) {
  const [sesion, setSesion] = useState(() => {
    const s = leerSesion();
    if (s) setToken(s.token);
    return s;
  });

  const salir = useCallback(() => {
    setToken(null);
    sessionStorage.removeItem(CLAVE);
    setSesion(null);
  }, []);

  useEffect(() => setOnUnauthorized(salir), [salir]);

  const entrar = useCallback(async (username, password) => {
    const r = await api.login(username, password);
    setToken(r.token);
    const s = { token: r.token, username: r.username, rol: r.rol };
    sessionStorage.setItem(CLAVE, JSON.stringify(s));
    setSesion(s);
  }, []);

  const valor = useMemo(
    () => ({ sesion, esAdmin: sesion?.rol === 'ADMIN', entrar, salir }),
    [sesion, entrar, salir],
  );
  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>;
}

/** Acceso al contexto de autenticación. */
export const useAuth = () => useContext(AuthContext);
