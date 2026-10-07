/**
 * nombre: index.js (i18n)
 * descripcion: Configuración de internacionalización con i18next (es por defecto, en).
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
 */
import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import es from './es.json';
import en from './en.json';

const guardado = (() => {
  try {
    return localStorage.getItem('idioma');
  } catch {
    return null;
  }
})();

i18n.use(initReactI18next).init({
  resources: { es: { translation: es }, en: { translation: en } },
  lng: guardado === 'en' ? 'en' : 'es',
  fallbackLng: 'es',
  interpolation: { escapeValue: false },
});

/** Mantiene el atributo lang del documento sincronizado con el idioma. */
i18n.on('languageChanged', (lng) => {
  document.documentElement.lang = lng;
  try {
    localStorage.setItem('idioma', lng);
  } catch {
    /* almacenamiento no disponible */
  }
});

export default i18n;
