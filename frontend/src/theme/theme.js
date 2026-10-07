/**
 * nombre: theme.js
 * descripcion: Construye el tema de Material UI a partir de los tokens.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
 */
import { createTheme } from '@mui/material/styles';
import { baseTokens, colorTokens } from './tokens';

/**
 * Crea el tema para un modo.
 * @param {'light'|'dark'} mode modo de color
 * @returns {import('@mui/material/styles').Theme} tema MUI
 */
export function buildTheme(mode) {
  const c = colorTokens[mode];
  return createTheme({
    spacing: baseTokens.spacing,
    shape: { borderRadius: baseTokens.radius },
    palette: {
      mode,
      primary: { main: c.primary, contrastText: c.onPrimary },
      secondary: { main: c.accent, contrastText: c.onAccent },
      error: { main: c.danger },
      success: { main: c.success },
      warning: { main: c.warning },
      background: { default: c.bg, paper: c.surface },
      text: { primary: c.ink, secondary: c.muted },
      divider: c.line,
    },
    typography: {
      fontFamily: baseTokens.fontSans,
      h1: { fontSize: '1.75rem', fontWeight: 600, letterSpacing: '-0.01em' },
      h2: { fontSize: '1.25rem', fontWeight: 600 },
      button: { textTransform: 'none', fontWeight: 600 },
    },
    components: {
      MuiCssBaseline: {
        styleOverrides: {
          'a:focus-visible, button:focus-visible, [tabindex]:focus-visible, input:focus-visible': {
            outline: `3px solid ${c.accent}`,
            outlineOffset: 2,
          },
        },
      },
      MuiPaper: { defaultProps: { elevation: 0 }, styleOverrides: { root: { border: `1px solid ${c.line}` } } },
      MuiButton: { defaultProps: { disableElevation: true } },
      MuiTableCell: {
        styleOverrides: {
          head: { fontWeight: 600, color: c.muted, fontSize: '0.78rem', textTransform: 'uppercase', letterSpacing: '0.06em' },
        },
      },
    },
  });
}
