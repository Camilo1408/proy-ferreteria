/**
 * nombre: setup.js
 * descripcion: Configuración global de Vitest (jest-dom, i18n en español y sesión limpia entre pruebas).
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
import '@testing-library/jest-dom/vitest';
import i18n from '../i18n';

beforeEach(() => i18n.changeLanguage('es'));
afterEach(() => sessionStorage.clear());
