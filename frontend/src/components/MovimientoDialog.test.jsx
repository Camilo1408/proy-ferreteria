/**
 * nombre: MovimientoDialog.test.jsx
 * descripcion: Pruebas del diálogo de movimientos: tipos por permiso, motivos por tipo, validación y envío.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderConSesion } from '../test/render';
import MovimientoDialog, { aCuerpoMovimiento } from './MovimientoDialog';

const cuenta = { current: null };
vi.mock('../api/inventario', () => ({
  registrarMovimiento: vi.fn(),
  obtenerCuenta: vi.fn(() => Promise.resolve(cuenta.current)),
}));
import { registrarMovimiento } from '../api/inventario';

const producto = { id: 1, codigo: 'MAR-1', nombre: 'Martillo', unidad: 'UND', stockActual: '10.000' };
const montar = (permisos, props = {}) => {
  cuenta.current = { username: 'tester', nombreCompleto: 'Persona de Prueba', email: null, perfil: 'Prueba', permisos };
  return renderConSesion(<MovimientoDialog abierto producto={producto} onCerrar={() => {}} onRegistrado={() => {}} {...props} />, { permisos });
};

beforeEach(() => vi.clearAllMocks());

describe('MovimientoDialog', () => {
  it('solo ofrece los tipos que el perfil permite', () => {
    montar(['MOVIMIENTOS_REGISTRAR']);
    expect(screen.getByRole('button', { name: 'Entrada' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Salida' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Ajuste' })).not.toBeInTheDocument();
  });

  it('con solo el permiso de ajustes ofrece únicamente Ajuste', () => {
    montar(['AJUSTES_REGISTRAR']);
    expect(screen.queryByRole('button', { name: 'Entrada' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Ajuste' })).toBeInTheDocument();
    expect(screen.getByLabelText(/Stock contado/)).toBeInTheDocument();
  });

  it('muestra el stock actual del producto y su nombre', () => {
    montar(['MOVIMIENTOS_REGISTRAR']);
    expect(screen.getByText('MAR-1 · Martillo')).toBeInTheDocument();
    expect(screen.getByText('Stock actual: 10 UND')).toBeInTheDocument();
  });

  it('los motivos dependen del tipo elegido', async () => {
    montar(['MOVIMIENTOS_REGISTRAR']);
    await userEvent.click(screen.getByRole('combobox', { name: /Motivo/ }));
    expect(screen.getByRole('option', { name: 'Compra' })).toBeInTheDocument();
    expect(screen.queryByRole('option', { name: 'Venta' })).not.toBeInTheDocument();
    await userEvent.keyboard('{Escape}');
    await userEvent.click(screen.getByRole('button', { name: 'Salida' }));
    await userEvent.click(screen.getByRole('combobox', { name: /Motivo/ }));
    const lista = within(screen.getByRole('listbox'));
    expect(lista.getByRole('option', { name: 'Venta' })).toBeInTheDocument();
    expect(lista.getByRole('option', { name: 'Merma' })).toBeInTheDocument();
    expect(lista.queryByRole('option', { name: 'Compra' })).not.toBeInTheDocument();
  });

  it('muestra errores y no envía si faltan cantidad y motivo', async () => {
    montar(['MOVIMIENTOS_REGISTRAR']);
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));
    expect(await screen.findByText('La cantidad es obligatoria')).toBeInTheDocument();
    expect(await screen.findByText('Elija un motivo')).toBeInTheDocument();
    expect(registrarMovimiento).not.toHaveBeenCalled();
  });

  it('rechaza cantidad cero y con más de 3 decimales', async () => {
    montar(['MOVIMIENTOS_REGISTRAR']);
    const cantidad = screen.getByLabelText(/^Cantidad/);
    await userEvent.type(cantidad, '0');
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));
    expect(await screen.findByText('La cantidad debe ser mayor que cero')).toBeInTheDocument();
    await userEvent.clear(cantidad);
    await userEvent.type(cantidad, '1,2345');
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));
    expect(await screen.findByText('Use un número no negativo con hasta 3 decimales')).toBeInTheDocument();
    expect(registrarMovimiento).not.toHaveBeenCalled();
  });

  it('envía la entrada con números y notifica el movimiento registrado', async () => {
    registrarMovimiento.mockResolvedValue({ id: 9 });
    const onRegistrado = vi.fn();
    montar(['MOVIMIENTOS_REGISTRAR'], { onRegistrado });
    await userEvent.type(screen.getByLabelText(/^Cantidad/), '2,5');
    await userEvent.click(screen.getByRole('combobox', { name: /Motivo/ }));
    await userEvent.click(screen.getByRole('option', { name: 'Compra' }));
    await userEvent.type(screen.getByLabelText(/Referencia/), ' FAC-9 ');
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));
    await waitFor(() => expect(registrarMovimiento).toHaveBeenCalledTimes(1));
    expect(registrarMovimiento).toHaveBeenCalledWith('ENTRADA', { productoId: 1, motivo: 'COMPRA', nota: null, cantidad: 2.5, referencia: 'FAC-9' });
    await waitFor(() => expect(onRegistrado).toHaveBeenCalledWith({ id: 9 }));
  });

  it('el ajuste exige una nota de al menos 5 caracteres y envía el stock contado', async () => {
    registrarMovimiento.mockResolvedValue({ id: 1 });
    montar(['AJUSTES_REGISTRAR']);
    await userEvent.type(screen.getByLabelText(/Stock contado/), '8');
    await userEvent.click(screen.getByRole('combobox', { name: /Motivo/ }));
    await userEvent.click(screen.getByRole('option', { name: 'Conteo físico' }));
    await userEvent.type(screen.getByLabelText(/^Nota/), 'abc');
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));
    expect(await screen.findByText('La nota es obligatoria (mínimo 5 caracteres)')).toBeInTheDocument();
    expect(registrarMovimiento).not.toHaveBeenCalled();
    await userEvent.type(screen.getByLabelText(/^Nota/), 'de');
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));
    await waitFor(() => expect(registrarMovimiento).toHaveBeenCalledWith('AJUSTE', { productoId: 1, motivo: 'CONTEO_FISICO', nota: 'abcde', stockContado: 8 }));
  });

  it('muestra el error de la API (stock insuficiente) sin cerrar el diálogo', async () => {
    registrarMovimiento.mockRejectedValue(Object.assign(new Error('Stock insuficiente: hay 10 disponibles'), { detalles: [] }));
    const onRegistrado = vi.fn();
    montar(['MOVIMIENTOS_REGISTRAR'], { onRegistrado, tipoInicial: 'SALIDA' });
    await userEvent.type(screen.getByLabelText(/^Cantidad/), '11');
    await userEvent.click(screen.getByRole('combobox', { name: /Motivo/ }));
    await userEvent.click(screen.getByRole('option', { name: 'Venta' }));
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));
    expect(await screen.findByText('Stock insuficiente: hay 10 disponibles')).toBeInTheDocument();
    expect(onRegistrado).not.toHaveBeenCalled();
    expect(screen.getByRole('dialog', { name: 'Registrar movimiento' })).toBeInTheDocument();
  });

  it('sin permisos de registro no se puede guardar', () => {
    montar([]);
    expect(screen.getByRole('button', { name: 'Guardar' })).toBeDisabled();
  });
});

describe('aCuerpoMovimiento', () => {
  const base = { tipo: 'SALIDA', productoId: '4', cantidad: '3,5', stockContado: '', motivo: 'VENTA', referencia: ' ', nota: '' };
  it('salida: cantidad numérica y textos vacíos como null', () => {
    expect(aCuerpoMovimiento(base)).toEqual({ productoId: 4, motivo: 'VENTA', nota: null, cantidad: 3.5, referencia: null });
  });
  it('ajuste: usa stockContado y no envía cantidad ni referencia', () => {
    const c = aCuerpoMovimiento({ ...base, tipo: 'AJUSTE', stockContado: '0', motivo: 'DANO', nota: ' roto ' });
    expect(c).toEqual({ productoId: 4, motivo: 'DANO', nota: 'roto', stockContado: 0 });
  });
});
