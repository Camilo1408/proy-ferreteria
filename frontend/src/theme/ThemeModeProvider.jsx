/**
 * nombre: ThemeModeProvider.jsx
 * descripcion: Alterna entre tema claro y oscuro respetando la preferencia del sistema.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
 */
import { createContext, useContext, useMemo, useState } from 'react';
import { CssBaseline, ThemeProvider, useMediaQuery } from '@mui/material';
import { buildTheme } from './theme';

const ModeContext = createContext({ mode: 'light', toggle: () => {} });

/** Proveedor del tema. El modo elegido se guarda en localStorage. */
export function ThemeModeProvider({ children }) {
  const prefiereOscuro = useMediaQuery('(prefers-color-scheme: dark)');
  const [mode, setMode] = useState(() => {
    try {
      return localStorage.getItem('tema') || (prefiereOscuro ? 'dark' : 'light');
    } catch {
      return 'light';
    }
  });
  const theme = useMemo(() => buildTheme(mode), [mode]);
  const toggle = () =>
    setMode((m) => {
      const n = m === 'light' ? 'dark' : 'light';
      try {
        localStorage.setItem('tema', n);
      } catch {
        /* almacenamiento no disponible */
      }
      return n;
    });
  return (
    <ModeContext.Provider value={{ mode, toggle }}>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        {children}
      </ThemeProvider>
    </ModeContext.Provider>
  );
}

/** Acceso al modo de tema. */
export const useThemeMode = () => useContext(ModeContext);
