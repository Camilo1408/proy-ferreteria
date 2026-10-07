/**
 * nombre: main.jsx
 * descripcion: Punto de entrada: monta proveedores de tema, i18n y sesión.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
 */
import React from 'react';
import { createRoot } from 'react-dom/client';
import '@fontsource/ibm-plex-sans/400.css';
import '@fontsource/ibm-plex-sans/600.css';
import '@fontsource/ibm-plex-sans/700.css';
import '@fontsource/ibm-plex-mono/400.css';
import './i18n';
import App from './App';
import { AuthProvider } from './auth/AuthContext';
import { ThemeModeProvider } from './theme/ThemeModeProvider';

createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <ThemeModeProvider>
      <AuthProvider>
        <App />
      </AuthProvider>
    </ThemeModeProvider>
  </React.StrictMode>,
);
