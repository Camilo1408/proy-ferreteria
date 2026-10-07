/**
 * nombre: tokens.js
 * descripcion: Tokens de diseño (colores, tipografía, espaciado, radios) para los temas claro y oscuro.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
 */

/** Paleta: papel cálido, azul acero y naranja de seguridad. Contraste AA verificado para texto. */
export const colorTokens = {
  light: {
    bg: '#F4F2EE',
    surface: '#FFFFFF',
    ink: '#1B1F23',
    muted: '#555D65',
    line: '#D9D5CC',
    primary: '#1F3A4D',
    onPrimary: '#FFFFFF',
    accent: '#B8440A',
    onAccent: '#FFFFFF',
    success: '#2B6A3C',
    danger: '#B3261E',
  },
  dark: {
    bg: '#14171A',
    surface: '#1C2024',
    ink: '#ECE9E3',
    muted: '#A4ABB2',
    line: '#2F353B',
    primary: '#8DBBD9',
    onPrimary: '#10202B',
    accent: '#F29254',
    onAccent: '#241104',
    success: '#7CC38F',
    danger: '#F2877F',
  },
};

/** Tokens independientes del tema. */
export const baseTokens = {
  radius: 4,
  spacing: 8,
  fontSans: '"IBM Plex Sans", system-ui, sans-serif',
  fontMono: '"IBM Plex Mono", ui-monospace, monospace',
};
