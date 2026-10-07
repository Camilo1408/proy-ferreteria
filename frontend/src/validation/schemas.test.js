/**
 * nombre: schemas.test.js
 * descripcion: Pruebas unitarias de los esquemas Yup (reglas y casos límite de CA-02 y CA-09).
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
 */
import { loginSchema, productoSchema } from './schemas';

const valido = { nombre: 'Martillo', categoria: 'Herramientas', descripcion: '', estado: 'ACTIVO' };
const errores = async (v) => {
  try {
    await productoSchema.validate(v, { abortEarly: false });
    return [];
  } catch (e) {
    return e.errors;
  }
};

describe('productoSchema', () => {
  it('acepta un producto válido', async () => expect(await errores(valido)).toEqual([]));
  it.each([['ab', false], ['abc', true], ['x'.repeat(100), true], ['x'.repeat(101), false]])(
    'nombre de %s caracteres -> válido=%s', async (nombre, ok) => {
      expect((await errores({ ...valido, nombre })).length === 0).toBe(ok);
    });
  it('rechaza nombre vacío o solo espacios', async () => {
    expect(await errores({ ...valido, nombre: '   ' })).toContain('validacion.nombreRequerido');
  });
  it('exige categoría y limita a 60', async () => {
    expect(await errores({ ...valido, categoria: '' })).toContain('validacion.categoriaRequerida');
    expect(await errores({ ...valido, categoria: 'c'.repeat(61) })).toContain('validacion.categoriaTamano');
    expect(await errores({ ...valido, categoria: 'c'.repeat(60) })).toEqual([]);
  });
  it('limita la descripción a 500', async () => {
    expect(await errores({ ...valido, descripcion: 'd'.repeat(500) })).toEqual([]);
    expect(await errores({ ...valido, descripcion: 'd'.repeat(501) })).toContain('validacion.descripcionTamano');
  });
});

describe('loginSchema', () => {
  it('exige usuario y contraseña', async () => {
    await expect(loginSchema.validate({ username: '', password: '' }, { abortEarly: false })).rejects.toThrow();
    await expect(loginSchema.isValid({ username: 'admin', password: 'x' })).resolves.toBe(true);
  });
});
