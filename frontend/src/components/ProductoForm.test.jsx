/**
 * nombre: ProductoForm.test.jsx
 * descripcion: Pruebas del formulario de producto: validación visible, cuerpo enviado y accesibilidad básica.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeModeProvider } from '../theme/ThemeModeProvider';
import ProductoForm, { aCuerpoProducto } from './ProductoForm';

const montar = (props) =>
  render(<ThemeModeProvider><ProductoForm abierto producto={null} onCerrar={() => {}} {...props} /></ThemeModeProvider>);

describe('ProductoForm', () => {
  it('muestra errores en español y no envía datos inválidos', async () => {
    const onGuardar = vi.fn();
    montar({ onGuardar });
    await userEvent.type(screen.getByLabelText(/^Nombre/), 'ab');
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));
    expect(await screen.findByText('El nombre debe tener entre 3 y 100 caracteres')).toBeInTheDocument();
    expect(await screen.findByText('La categoría es obligatoria')).toBeInTheDocument();
    expect(await screen.findByText('El código es obligatorio')).toBeInTheDocument();
    expect(onGuardar).not.toHaveBeenCalled();
  });

  it('envía código, unidad, mínimo e inicial con números y espacios recortados', async () => {
    const onGuardar = vi.fn().mockResolvedValue();
    montar({ onGuardar });
    await userEvent.type(screen.getByLabelText(/^Código/), ' MAR-1 ');
    await userEvent.type(screen.getByLabelText(/^Nombre/), '  Martillo  ');
    await userEvent.type(screen.getByLabelText(/^Categoría/), 'Herramientas');
    await userEvent.type(screen.getByLabelText(/Stock mínimo/), '2,5');
    await userEvent.type(screen.getByLabelText(/Stock inicial/), '10');
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));
    await waitFor(() => expect(onGuardar).toHaveBeenCalledTimes(1));
    expect(onGuardar.mock.calls[0][0]).toEqual({
      codigo: 'MAR-1', nombre: 'Martillo', categoria: 'Herramientas', descripcion: '', unidad: 'UND', stockMinimo: 2.5, stockInicial: 10,
    });
  });

  it('al editar no pide stock inicial, muestra el estado y no envía stock inicial', async () => {
    const onGuardar = vi.fn().mockResolvedValue();
    const producto = { id: 3, codigo: 'ALI-1', nombre: 'Alicate', categoria: 'Manuales', descripcion: 'd', unidad: 'M', stockMinimo: '4.000', estado: 'INACTIVO' };
    montar({ onGuardar, producto });
    expect(screen.queryByLabelText(/Stock inicial/)).not.toBeInTheDocument();
    expect(screen.getByRole('dialog', { name: 'Editar producto' })).toBeInTheDocument();
    expect(screen.getByLabelText(/Stock mínimo/)).toHaveValue('4');
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));
    await waitFor(() => expect(onGuardar).toHaveBeenCalledTimes(1));
    expect(onGuardar.mock.calls[0][0]).toEqual({
      codigo: 'ALI-1', nombre: 'Alicate', categoria: 'Manuales', descripcion: 'd', unidad: 'M', stockMinimo: 4, estado: 'INACTIVO',
    });
  });

  it('muestra el error de la API (nombre duplicado) junto al formulario', async () => {
    const err = Object.assign(new Error("Ya existe un producto con el nombre 'Martillo'"), { detalles: [] });
    montar({ onGuardar: vi.fn().mockRejectedValue(err) });
    await userEvent.type(screen.getByLabelText(/^Código/), 'M-1');
    await userEvent.type(screen.getByLabelText(/^Nombre/), 'Martillo');
    await userEvent.type(screen.getByLabelText(/^Categoría/), 'Herramientas');
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));
    expect(await screen.findByText(/Ya existe un producto/)).toBeInTheDocument();
  });

  it('el diálogo tiene nombre accesible', () => {
    montar({ onGuardar: vi.fn() });
    expect(screen.getByRole('dialog', { name: 'Crear producto' })).toBeInTheDocument();
  });
});

describe('aCuerpoProducto', () => {
  const v = { codigo: ' A-1 ', nombre: ' N ', categoria: ' C ', descripcion: '', unidad: 'KG', stockMinimo: '', stockInicial: '', estado: 'ACTIVO' };
  it('mínimo vacío es 0 y sin inicial no se envía', () => {
    expect(aCuerpoProducto(v, false)).toEqual({ codigo: 'A-1', nombre: 'N', categoria: 'C', descripcion: '', unidad: 'KG', stockMinimo: 0 });
  });
  it('al editar incluye el estado y nunca el stock inicial', () => {
    expect(aCuerpoProducto({ ...v, stockInicial: '5' }, true)).toMatchObject({ estado: 'ACTIVO' });
    expect(aCuerpoProducto({ ...v, stockInicial: '5' }, true)).not.toHaveProperty('stockInicial');
  });
});
