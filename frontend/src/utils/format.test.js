/**
 * nombre: format.test.js
 * descripcion: Pruebas de formato de cantidades, variaciones y números escritos por el usuario.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
import { aNumero, formatoCantidad, formatoFecha, formatoVariacion } from './format';

describe('formato', () => {
  it('cantidades con hasta 3 decimales según el idioma', () => {
    expect(formatoCantidad('2.500', 'es')).toBe('2,5');
    expect(formatoCantidad(2.5, 'en')).toBe('2.5');
    expect(formatoCantidad('0.001', 'en')).toBe('0.001');
    expect(formatoCantidad('1234.5', 'en')).toBe('1,234.5');
    expect(formatoCantidad(0, 'es')).toBe('0');
    expect(formatoCantidad(null)).toBe('');
    expect(formatoCantidad('')).toBe('');
  });
  it('variaciones con signo explícito', () => {
    expect(formatoVariacion(5, 'es')).toBe('+5');
    expect(formatoVariacion('-2.5', 'es')).toBe('−2,5');
    expect(formatoVariacion(0, 'es')).toBe('0');
  });
  it('fechas vacías y válidas', () => {
    expect(formatoFecha(null)).toBe('');
    expect(formatoFecha('2026-10-08T15:30:00Z', 'en')).toMatch(/2026|26/);
  });
  it('convierte texto con coma o punto en número', () => {
    expect(aNumero('1,5')).toBe(1.5);
    expect(aNumero(' 2.25 ')).toBe(2.25);
    expect(aNumero('abc')).toBeNaN();
  });
});
