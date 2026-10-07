/**
 * nombre: schemas.test.js
 * descripcion: Pruebas unitarias de los esquemas Yup (reglas y casos límite de productos, movimientos, usuarios y perfiles).
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
import {
  cambioClaveSchema, cuentaSchema, loginSchema, movimientoSchema, perfilSchema, productoSchema, restablecerClaveSchema,
  usuarioCrearSchema, usuarioEditarSchema,
} from './schemas';

const errores = async (schema, v) => {
  try {
    await schema.validate(v, { abortEarly: false });
    return [];
  } catch (e) {
    return e.errors;
  }
};

const producto = { codigo: 'MAR-1', nombre: 'Martillo', categoria: 'Herramientas', descripcion: '', unidad: 'UND', stockMinimo: '', stockInicial: '', estado: 'ACTIVO' };

describe('productoSchema', () => {
  it('acepta un producto válido', async () => expect(await errores(productoSchema, producto)).toEqual([]));
  it.each([['ab', false], ['abc', true], ['x'.repeat(100), true], ['x'.repeat(101), false]])(
    'nombre de %s caracteres -> válido=%s', async (nombre, ok) => {
      expect((await errores(productoSchema, { ...producto, nombre })).length === 0).toBe(ok);
    });
  it('rechaza nombre vacío o solo espacios', async () => {
    expect(await errores(productoSchema, { ...producto, nombre: '   ' })).toContain('validacion.nombreRequerido');
  });
  it('exige categoría y limita a 60', async () => {
    expect(await errores(productoSchema, { ...producto, categoria: '' })).toContain('validacion.categoriaRequerida');
    expect(await errores(productoSchema, { ...producto, categoria: 'c'.repeat(61) })).toContain('validacion.categoriaTamano');
    expect(await errores(productoSchema, { ...producto, categoria: 'c'.repeat(60) })).toEqual([]);
  });
  it('limita la descripción a 500', async () => {
    expect(await errores(productoSchema, { ...producto, descripcion: 'd'.repeat(500) })).toEqual([]);
    expect(await errores(productoSchema, { ...producto, descripcion: 'd'.repeat(501) })).toContain('validacion.descripcionTamano');
  });
  it.each([['A', false], ['AB', true], ['a.b_c-9', true], ['x'.repeat(30), true], ['x'.repeat(31), false], ['a b', false], ['ñu', false], ['', false]])(
    'código %j -> válido=%s', async (codigo, ok) => {
      expect((await errores(productoSchema, { ...producto, codigo })).length === 0).toBe(ok);
    });
  it('exige una unidad válida', async () => {
    expect(await errores(productoSchema, { ...producto, unidad: '' })).toContain('validacion.unidadRequerida');
    expect(await errores(productoSchema, { ...producto, unidad: 'GALON' })).toContain('validacion.unidadRequerida');
  });
  it.each([['', true], ['0', true], ['5', true], ['1,5', true], ['1.234', true], ['1.2345', false], ['-1', false], ['abc', false], ['12345678901.5', true], ['123456789012', false]])(
    'stock mínimo %j -> válido=%s', async (stockMinimo, ok) => {
      expect((await errores(productoSchema, { ...producto, stockMinimo })).length === 0).toBe(ok);
    });
});

describe('movimientoSchema', () => {
  const entrada = { tipo: 'ENTRADA', productoId: 1, cantidad: '2,5', stockContado: '', motivo: 'COMPRA', referencia: '', nota: '' };
  it('acepta una entrada válida', async () => expect(await errores(movimientoSchema, entrada)).toEqual([]));
  it.each([['', false], ['0', false], ['0,001', true], ['-1', false], ['1.2345', false], ['abc', false], ['3', true]])(
    'cantidad %j -> válido=%s', async (cantidad, ok) => {
      expect((await errores(movimientoSchema, { ...entrada, cantidad })).length === 0).toBe(ok);
    });
  it('exige producto y motivo', async () => {
    expect(await errores(movimientoSchema, { ...entrada, productoId: '' })).toContain('validacion.productoRequerido');
    expect(await errores(movimientoSchema, { ...entrada, motivo: '' })).toContain('validacion.motivoRequerido');
  });
  it('un motivo de otro tipo no es válido', async () => {
    expect(await errores(movimientoSchema, { ...entrada, motivo: 'VENTA' })).toContain('validacion.motivoRequerido');
    expect(await errores(movimientoSchema, { ...entrada, tipo: 'SALIDA', motivo: 'VENTA' })).toEqual([]);
    expect(await errores(movimientoSchema, { ...entrada, tipo: 'SALIDA', motivo: 'COMPRA' })).toContain('validacion.motivoRequerido');
  });
  it('el ajuste pide stock contado (puede ser 0) y una nota de al menos 5 caracteres', async () => {
    const ajuste = { tipo: 'AJUSTE', productoId: 1, cantidad: '', stockContado: '0', motivo: 'CONTEO_FISICO', referencia: '', nota: 'Conteo' };
    expect(await errores(movimientoSchema, ajuste)).toEqual([]);
    expect(await errores(movimientoSchema, { ...ajuste, stockContado: '' })).toContain('validacion.contadoRequerido');
    expect(await errores(movimientoSchema, { ...ajuste, stockContado: '-2' })).toContain('validacion.cantidadFormato');
    expect(await errores(movimientoSchema, { ...ajuste, nota: '' })).toContain('validacion.notaAjuste');
    expect(await errores(movimientoSchema, { ...ajuste, nota: 'abcd' })).toContain('validacion.notaAjuste');
    expect(await errores(movimientoSchema, { ...ajuste, nota: 'abcde' })).toEqual([]);
  });
  it('limita referencia a 40 y nota a 300', async () => {
    expect(await errores(movimientoSchema, { ...entrada, referencia: 'r'.repeat(41) })).toContain('validacion.referenciaTamano');
    expect(await errores(movimientoSchema, { ...entrada, referencia: 'r'.repeat(40) })).toEqual([]);
    expect(await errores(movimientoSchema, { ...entrada, nota: 'n'.repeat(301) })).toContain('validacion.notaTamano');
  });
});

describe('usuarios y contraseñas', () => {
  const nuevo = { username: 'ana.perez', password: 'Clave1234', nombreCompleto: 'Ana Pérez', email: '', perfilId: 2 };
  it('acepta un usuario válido', async () => expect(await errores(usuarioCrearSchema, nuevo)).toEqual([]));
  it.each([['ab', false], ['abc', true], ['a b', false], ['x'.repeat(51), false], ['a@b', false]])(
    'username %j -> válido=%s', async (username, ok) => {
      expect((await errores(usuarioCrearSchema, { ...nuevo, username })).length === 0).toBe(ok);
    });
  it.each([['corta1A', false], ['sinnumeroaqui', false], ['123456789', false], ['con espacio 12', false], ['Abcdef12', true], ['x'.repeat(70) + '1a', true], ['x'.repeat(71) + '1a', false]])(
    'contraseña %j -> válida=%s', async (password, ok) => {
      expect((await errores(usuarioCrearSchema, { ...nuevo, password })).length === 0).toBe(ok);
      expect((await errores(restablecerClaveSchema, { nueva: password })).length === 0).toBe(ok);
    });
  it('valida correo, nombre y perfil', async () => {
    expect(await errores(usuarioCrearSchema, { ...nuevo, email: 'malo' })).toContain('validacion.emailFormato');
    expect(await errores(usuarioCrearSchema, { ...nuevo, email: 'a@b.co' })).toEqual([]);
    expect(await errores(usuarioCrearSchema, { ...nuevo, nombreCompleto: '' })).toContain('validacion.nombreCompletoRequerido');
    expect(await errores(usuarioCrearSchema, { ...nuevo, perfilId: '' })).toContain('validacion.perfilRequerido');
    expect(await errores(usuarioEditarSchema, { nombreCompleto: 'A', email: '', perfilId: 1, activo: true })).toContain('validacion.nombreCompletoTamano');
    expect(await errores(usuarioEditarSchema, { nombreCompleto: 'Ana', email: '', perfilId: 1, activo: false })).toEqual([]);
    expect(await errores(cuentaSchema, { nombreCompleto: 'Ana', email: 'x' })).toContain('validacion.emailFormato');
  });
  it('cambio de contraseña: distinta de la actual y confirmada', async () => {
    const ok = { actual: 'Vieja1234', nueva: 'Nueva1234', confirmar: 'Nueva1234' };
    expect(await errores(cambioClaveSchema, ok)).toEqual([]);
    expect(await errores(cambioClaveSchema, { ...ok, nueva: 'Vieja1234', confirmar: 'Vieja1234' })).toContain('validacion.claveIgual');
    expect(await errores(cambioClaveSchema, { ...ok, confirmar: 'Otra12345' })).toContain('validacion.confirmarDistinta');
    expect(await errores(cambioClaveSchema, { ...ok, actual: '' })).toContain('validacion.claveActualRequerida');
  });
});

describe('perfilSchema y loginSchema', () => {
  it('el perfil exige nombre de 3 a 60 y al menos un permiso', async () => {
    const ok = { nombre: 'Ventas', descripcion: '', permisos: ['PRODUCTOS_VER'] };
    expect(await errores(perfilSchema, ok)).toEqual([]);
    expect(await errores(perfilSchema, { ...ok, nombre: 'ab' })).toContain('validacion.perfilNombreTamano');
    expect(await errores(perfilSchema, { ...ok, nombre: '' })).toContain('validacion.perfilNombreRequerido');
    expect(await errores(perfilSchema, { ...ok, permisos: [] })).toContain('validacion.permisosRequeridos');
    expect(await errores(perfilSchema, { ...ok, descripcion: 'd'.repeat(201) })).toContain('validacion.perfilDescripcionTamano');
  });
  it('el login exige usuario y contraseña', async () => {
    expect((await errores(loginSchema, { username: '', password: '' })).length).toBe(2);
    expect(await errores(loginSchema, { username: 'admin', password: 'x' })).toEqual([]);
  });
});
