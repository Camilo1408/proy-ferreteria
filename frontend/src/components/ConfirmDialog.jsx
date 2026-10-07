/**
 * nombre: ConfirmDialog.jsx
 * descripcion: Diálogo de confirmación accesible para acciones destructivas.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
import { Button, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle } from '@mui/material';
import { useTranslation } from 'react-i18next';

/**
 * @param {{abierto: boolean, titulo: string, texto: string, confirmar: string, onConfirmar: () => void, onCerrar: () => void}} props
 */
export default function ConfirmDialog({ abierto, titulo, texto, confirmar, onConfirmar, onCerrar }) {
  const { t } = useTranslation();
  return (
    <Dialog open={abierto} onClose={onCerrar} aria-labelledby="confirmar-titulo">
      <DialogTitle id="confirmar-titulo">{titulo}</DialogTitle>
      <DialogContent>
        <DialogContentText>{texto}</DialogContentText>
      </DialogContent>
      <DialogActions>
        <Button onClick={onCerrar}>{t('form.cancelar')}</Button>
        <Button color="error" variant="contained" onClick={onConfirmar}>{confirmar}</Button>
      </DialogActions>
    </Dialog>
  );
}
