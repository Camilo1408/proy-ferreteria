/**
 * nombre: administracion.spec.js
 * descripcion: Aceptación E2E de perfiles con permisos modulares, usuarios y cuenta propia (CA-42 a CA-56).
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
import { expect, test } from '@playwright/test';
import { CLAVE, crearPerfil, crearUsuario, elegir, entrar, irA, salir, sufijo, unico } from './helpers';

test.describe('Perfiles, usuarios y cuenta', () => {
  test('CA-42, CA-52 y CA-53 un perfil modular se asigna a un usuario y sus permisos rigen en la interfaz', async ({ page }) => {
    const perfil = unico('Bodega');
    const username = `bod${sufijo()}`.toLowerCase();
    await entrar(page);
    await crearPerfil(page, perfil, ['Ver productos y panel', 'Registrar entradas y salidas', 'Ver historial de movimientos']);
    await crearUsuario(page, { username, nombre: 'Operario de Bodega', perfil });
    await salir(page);

    await entrar(page, { u: username, p: CLAVE });
    const nav = page.getByRole('navigation', { name: 'Navegación principal' });
    await expect(nav.getByRole('link', { name: 'Productos', exact: true })).toBeVisible();
    await expect(nav.getByRole('link', { name: 'Movimientos', exact: true })).toBeVisible();
    await expect(nav.getByRole('link', { name: 'Alertas', exact: true })).toHaveCount(0);
    await expect(nav.getByRole('link', { name: 'Usuarios', exact: true })).toHaveCount(0);
    await irA(page, 'Productos');
    await expect(page.getByRole('button', { name: 'Nuevo producto' })).toHaveCount(0);
    await irA(page, 'Mi cuenta');
    await expect(page.getByText('Registrar entradas y salidas')).toBeVisible();
    await expect(page.getByText('Gestionar usuarios')).toHaveCount(0);
    await salir(page);

    // El administrador amplía el perfil: el usuario lo ve al volver a entrar.
    await entrar(page);
    await irA(page, 'Perfiles');
    await page.getByRole('button', { name: `Editar perfil: ${perfil}` }).click();
    const d = page.getByRole('dialog', { name: 'Editar perfil' });
    await d.getByLabel('Ver alertas de stock', { exact: true }).check();
    await d.getByLabel('Crear, editar y desactivar productos', { exact: true }).check();
    await d.getByRole('button', { name: 'Guardar' }).click();
    await expect(page.getByText('Perfil actualizado')).toBeVisible();
    await salir(page);
    await entrar(page, { u: username, p: CLAVE });
    await expect(page.getByRole('navigation', { name: 'Navegación principal' }).getByRole('link', { name: 'Alertas', exact: true })).toBeVisible();
    await irA(page, 'Productos');
    await expect(page.getByRole('button', { name: 'Nuevo producto' })).toBeVisible();
  });

  test('CA-43 y validaciones: nombre de perfil duplicado y permisos obligatorios', async ({ page }) => {
    await entrar(page);
    const nombre = unico('Compras');
    await crearPerfil(page, nombre, ['Ver productos y panel']);
    await page.getByRole('button', { name: 'Nuevo perfil' }).click();
    const d = page.getByRole('dialog', { name: 'Crear perfil' });
    await d.getByLabel(/Nombre del perfil/).fill(nombre.toLowerCase());
    await d.getByRole('button', { name: 'Guardar' }).click();
    await expect(d.getByText('Elija al menos un permiso')).toBeVisible();
    await d.getByLabel('Ver productos y panel', { exact: true }).check();
    await d.getByRole('button', { name: 'Guardar' }).click();
    await expect(d.getByRole('alert').filter({ hasText: 'Ya existe un perfil llamado' })).toBeVisible();
  });

  test('CA-44 y CA-45 el perfil del sistema es de solo lectura y un perfil en uso no se elimina', async ({ page }) => {
    await entrar(page);
    await irA(page, 'Perfiles');
    await expect(page.getByRole('button', { name: 'Eliminar perfil: ADMINISTRADOR' })).toHaveCount(0);
    await page.getByRole('button', { name: 'Editar perfil: ADMINISTRADOR' }).click();
    const d = page.getByRole('dialog');
    await expect(d.getByText('Perfil del sistema: solo lectura')).toBeVisible();
    await expect(d.getByRole('button', { name: 'Guardar' })).toHaveCount(0);
    await d.getByRole('button', { name: 'Cerrar' }).click();

    const perfil = unico('Temporal');
    await crearPerfil(page, perfil, ['Ver productos y panel']);
    await crearUsuario(page, { username: `tmp${sufijo()}`.toLowerCase(), nombre: 'Temporal', perfil });
    await irA(page, 'Perfiles');
    await page.getByRole('button', { name: `Eliminar perfil: ${perfil}` }).click();
    await page.getByRole('dialog', { name: 'Eliminar perfil' }).getByRole('button', { name: 'Eliminar' }).click();
    await expect(page.getByRole('alert').filter({ hasText: 'tiene usuarios asignados' })).toBeVisible();

    const libre = unico('Libre');
    await crearPerfil(page, libre, ['Ver productos y panel']);
    await page.getByRole('button', { name: `Eliminar perfil: ${libre}` }).click();
    await page.getByRole('dialog', { name: 'Eliminar perfil' }).getByRole('button', { name: 'Eliminar' }).click();
    await expect(page.getByText('Perfil eliminado')).toBeVisible();
    await expect(page.getByRole('row', { name: new RegExp(libre) })).toHaveCount(0);
  });

  test('CA-47, CA-48 y CA-50 alta de usuario con validación; un usuario desactivado no puede entrar', async ({ page }) => {
    await entrar(page);
    const perfil = unico('Lector');
    await crearPerfil(page, perfil, ['Ver productos y panel']);
    const username = `lec${sufijo()}`.toLowerCase();
    await irA(page, 'Usuarios');
    await page.getByRole('button', { name: 'Nuevo usuario' }).click();
    const d = page.getByRole('dialog', { name: 'Crear usuario' });
    await d.getByLabel(/Nombre de usuario/).fill('a b');
    await d.getByLabel(/^Contraseña/).fill('corta');
    await d.getByLabel(/Nombre completo/).fill('Lector');
    await d.getByLabel(/Correo/).fill('malo');
    await d.getByRole('button', { name: 'Guardar' }).click();
    await expect(d.getByText('De 3 a 50 caracteres: letras, números, punto, guion y guion bajo')).toBeVisible();
    await expect(d.getByText('De 8 a 72 caracteres, con al menos una letra y un número, sin espacios')).toBeVisible();
    await expect(d.getByText('El correo no es válido')).toBeVisible();
    await expect(d.getByText('Elija un perfil')).toBeVisible();
    await d.getByRole('button', { name: 'Cancelar' }).click();

    await crearUsuario(page, { username, nombre: 'Lector Uno', perfil });
    await page.getByLabel('Buscar por usuario o nombre').fill(username);
    const fila = page.getByRole('row', { name: new RegExp(username) });
    await expect(fila).toContainText('Activo');
    await fila.getByRole('button', { name: `Editar usuario: ${username}` }).click();
    const e = page.getByRole('dialog', { name: 'Editar usuario' });
    await e.getByRole('checkbox', { name: 'Activo' }).uncheck();
    await e.getByRole('button', { name: 'Guardar' }).click();
    await expect(page.getByText('Usuario actualizado')).toBeVisible();
    await expect(fila).toContainText('Inactivo');
    await salir(page);

    await page.getByLabel('Usuario').fill(username);
    await page.getByLabel('Contraseña').fill(CLAVE);
    await page.getByRole('button', { name: 'Entrar' }).click();
    await expect(page.getByRole('alert')).toContainText('Usuario o contraseña incorrectos');
  });

  test('CA-49 el administrador no puede desactivarse ni cambiarse el perfil a sí mismo', async ({ page }) => {
    await entrar(page);
    await irA(page, 'Usuarios');
    await page.getByLabel('Buscar por usuario o nombre').fill('admin');
    await page.getByRole('button', { name: 'Editar usuario: admin' }).click();
    const d = page.getByRole('dialog', { name: 'Editar usuario' });
    await expect(d.getByRole('checkbox', { name: 'Activo' })).toBeDisabled();
    await expect(d.getByRole('combobox', { name: /Perfil/ })).toHaveAttribute('aria-disabled', 'true');
    await expect(d.getByText('No puede desactivarse ni cambiarse el perfil a sí mismo')).toBeVisible();
  });

  test('restablecer la contraseña de un usuario permite entrar con la nueva', async ({ page }) => {
    await entrar(page);
    const perfil = unico('Clave');
    await crearPerfil(page, perfil, ['Ver productos y panel']);
    const username = `clv${sufijo()}`.toLowerCase();
    await crearUsuario(page, { username, nombre: 'Usuario Clave', perfil });
    await page.getByLabel('Buscar por usuario o nombre').fill(username);
    await page.getByRole('button', { name: `Restablecer contraseña: ${username}` }).click();
    const d = page.getByRole('dialog');
    await d.getByLabel(/Nueva contraseña/).fill('debil');
    await d.getByRole('button', { name: 'Guardar' }).click();
    await expect(d.getByText('De 8 a 72 caracteres, con al menos una letra y un número, sin espacios')).toBeVisible();
    await d.getByLabel(/Nueva contraseña/).fill('Nueva98765');
    await d.getByRole('button', { name: 'Guardar' }).click();
    await expect(page.getByText('Contraseña restablecida')).toBeVisible();
    await salir(page);
    await entrar(page, { u: username, p: 'Nueva98765' });
  });

  test('CA-54 a CA-56 cuenta propia: editar datos y cambiar la contraseña con sus reglas', async ({ page }) => {
    await entrar(page);
    const perfil = unico('Cuenta');
    await crearPerfil(page, perfil, ['Ver productos y panel']);
    const username = `cta${sufijo()}`.toLowerCase();
    await crearUsuario(page, { username, nombre: 'Nombre Original', perfil });
    await salir(page);

    await entrar(page, { u: username, p: CLAVE });
    await irA(page, 'Mi cuenta');
    await page.getByLabel(/Nombre completo/).fill('Nombre Nuevo');
    await page.getByLabel(/Correo/).fill('nuevo@ferreteria.test');
    await page.getByRole('button', { name: 'Guardar cambios' }).click();
    await expect(page.getByText('Datos actualizados')).toBeVisible();
    await page.reload();
    await expect(page.getByLabel(/Nombre completo/)).toHaveValue('Nombre Nuevo');
    await expect(page.getByLabel(/Correo/)).toHaveValue('nuevo@ferreteria.test');

    await page.getByLabel(/^Contraseña actual/).fill('incorrecta1');
    await page.getByLabel(/^Nueva contraseña/).fill('Otra123456');
    await page.getByLabel(/Confirmar nueva contraseña/).fill('Otra123456');
    await page.getByRole('button', { name: 'Cambiar contraseña' }).click();
    await expect(page.getByRole('alert').filter({ hasText: 'La contraseña actual es incorrecta' })).toBeVisible();

    await page.getByLabel(/^Contraseña actual/).fill(CLAVE);
    await page.getByLabel(/^Nueva contraseña/).fill(CLAVE);
    await page.getByLabel(/Confirmar nueva contraseña/).fill(CLAVE);
    await page.getByRole('button', { name: 'Cambiar contraseña' }).click();
    await expect(page.getByText('La nueva contraseña debe ser distinta de la actual')).toBeVisible();

    await page.getByLabel(/^Nueva contraseña/).fill('Otra123456');
    await page.getByLabel(/Confirmar nueva contraseña/).fill('Distinta1234');
    await page.getByRole('button', { name: 'Cambiar contraseña' }).click();
    await expect(page.getByText('Las contraseñas no coinciden')).toBeVisible();

    await page.getByLabel(/Confirmar nueva contraseña/).fill('Otra123456');
    await page.getByRole('button', { name: 'Cambiar contraseña' }).click();
    await expect(page.getByText('Contraseña actualizada')).toBeVisible();
    await salir(page);

    await page.getByLabel('Usuario').fill(username);
    await page.getByLabel('Contraseña').fill(CLAVE);
    await page.getByRole('button', { name: 'Entrar' }).click();
    await expect(page.getByRole('alert')).toContainText('Usuario o contraseña incorrectos');
    await page.getByLabel('Contraseña').fill('Otra123456');
    await page.getByRole('button', { name: 'Entrar' }).click();
    await expect(page.getByRole('navigation', { name: 'Navegación principal' })).toBeVisible();
  });
});
