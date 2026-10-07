/**
 * nombre: ProductoSelector.jsx
 * descripcion: Buscador de productos activos con autocompletado (por código, nombre o categoría).
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
import { useEffect, useState } from 'react';
import { Autocomplete, TextField } from '@mui/material';
import { useTranslation } from 'react-i18next';
import * as api from '../api/productos';

/**
 * @param {{value: object|null, onChange: (p: object|null) => void, error?: boolean, helperText?: string, soloActivos?: boolean}} props
 */
export default function ProductoSelector({ value, onChange, error, helperText, soloActivos = true }) {
  const { t } = useTranslation();
  const [texto, setTexto] = useState('');
  const [opciones, setOpciones] = useState([]);

  useEffect(() => {
    let cancelado = false;
    const id = setTimeout(async () => {
      try {
        const r = await api.listar({ q: texto, size: 20, estado: soloActivos ? 'ACTIVO' : '' });
        if (!cancelado) setOpciones(r.content);
      } catch {
        if (!cancelado) setOpciones([]);
      }
    }, 250);
    return () => {
      cancelado = true;
      clearTimeout(id);
    };
  }, [texto, soloActivos]);

  return (
    <Autocomplete
      value={value}
      options={opciones}
      filterOptions={(o) => o}
      getOptionLabel={(p) => `${p.codigo} · ${p.nombre}`}
      isOptionEqualToValue={(a, b) => a.id === b.id}
      onChange={(_, p) => onChange(p)}
      onInputChange={(_, v, motivo) => motivo === 'input' && setTexto(v)}
      noOptionsText={t('movimientos.sinProductos')}
      renderInput={(params) => (
        <TextField {...params} label={t('movimientos.buscarProducto')} margin="dense" required error={error} helperText={helperText ?? ' '} />
      )}
    />
  );
}
