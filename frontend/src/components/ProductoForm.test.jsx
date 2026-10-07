/**
 * nombre: ProductoForm.test.jsx
 * descripcion: Pruebas del formulario: validación visible, envío y accesibilidad básica.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
 */
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeModeProvider } from '../theme/ThemeModeProvider';
import ProductoForm from './ProductoForm';

const montar = (props) =>
  render(<ThemeModeProvider><ProductoForm abierto producto={null} onCerrar={() => {}} {...props} /></ThemeModeProvider>);

describe('ProductoForm', () => {
  it('muestra errores en español y no envía datos inválidos', async () => {
    const onGuardar = vi.fn();
    montar({ onGuardar });
    await userEvent.type(screen.getByLabelText(/Nombre/), 'ab');
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));
    expect(await screen.findByText('El nombre debe tener entre 3 y 100 caracteres')).toBeInTheDocument();
    expect(await screen.findByText('La categoría es obligatoria')).toBeInTheDocument();
    expect(onGuardar).not.toHaveBeenCalled();
  });

  it('envía los datos con espacios recortados cuando es válido', async () => {
    const onGuardar = vi.fn().mockResolvedValue();
    montar({ onGuardar });
    await userEvent.type(screen.getByLabelText(/Nombre/), '  Martillo  ');
    await userEvent.type(screen.getByLabelText(/Categoría/), 'Herramientas');
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));
    await waitFor(() => expect(onGuardar).toHaveBeenCalledTimes(1));
    expect(onGuardar.mock.calls[0][0]).toMatchObject({ nombre: 'Martillo', categoria: 'Herramientas' });
  });

  it('muestra el error de la API (nombre duplicado) en una región role=alert', async () => {
    const err = Object.assign(new Error("Ya existe un producto con el nombre 'Martillo'"), { detalles: [] });
    montar({ onGuardar: vi.fn().mockRejectedValue(err) });
    await userEvent.type(screen.getByLabelText(/Nombre/), 'Martillo');
    await userEvent.type(screen.getByLabelText(/Categoría/), 'Herramientas');
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));
    expect(await screen.findByText(/Ya existe un producto/)).toBeInTheDocument();
  });

  it('el diálogo tiene nombre accesible', () => {
    montar({ onGuardar: vi.fn() });
    expect(screen.getByRole('dialog', { name: 'Crear producto' })).toBeInTheDocument();
  });
});
