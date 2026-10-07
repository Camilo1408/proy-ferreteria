/**
 * nombre: AvisoContext.jsx
 * descripcion: Avisos breves (toast) accesibles y compartidos por toda la aplicación.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
import { createContext, useCallback, useContext, useMemo, useState } from 'react';
import { Snackbar } from '@mui/material';

const AvisoContext = createContext({ avisar: () => {} });

/** Proveedor del aviso; el mensaje se anuncia a lectores de pantalla (role=status). */
export function AvisoProvider({ children }) {
  const [mensaje, setMensaje] = useState('');
  const avisar = useCallback((m) => setMensaje(m), []);
  const valor = useMemo(() => ({ avisar }), [avisar]);
  return (
    <AvisoContext.Provider value={valor}>
      {children}
      <Snackbar
        open={Boolean(mensaje)}
        autoHideDuration={4000}
        onClose={() => setMensaje('')}
        message={mensaje}
        ContentProps={{ role: 'status' }}
      />
    </AvisoContext.Provider>
  );
}

/** Devuelve `{avisar(mensaje)}`. */
export const useAviso = () => useContext(AvisoContext);
