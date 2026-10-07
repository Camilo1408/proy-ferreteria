/**
 * nombre: setup.js
 * descripcion: Configuración global de Vitest (jest-dom e i18n en español).
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
 */
import '@testing-library/jest-dom/vitest';
import i18n from '../i18n';

beforeEach(() => i18n.changeLanguage('es'));
