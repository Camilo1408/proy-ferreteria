/*
 * nombre: I18nConfig.java
 * descripcion: Internacionalización: mensajes es/en por Accept-Language y validador.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
 */
package com.ferreteria.config;

import java.util.List;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

/** Configura idiomas soportados (es por defecto, en) y mensajes de validación. */
@Configuration
public class I18nConfig {

    /** Fuente de mensajes: {@code classpath:messages}. */
    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
        ms.setBasename("classpath:messages");
        ms.setDefaultEncoding("UTF-8");
        ms.setFallbackToSystemLocale(false);
        return ms;
    }

    /** Resuelve el idioma desde la cabecera Accept-Language. */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver r = new AcceptHeaderLocaleResolver();
        r.setSupportedLocales(List.of(Locale.forLanguageTag("es"), Locale.ENGLISH));
        r.setDefaultLocale(Locale.forLanguageTag("es"));
        return r;
    }

    /** Validador de Bean Validation que usa los mensajes traducidos. */
    @Bean
    public LocalValidatorFactoryBean validator(MessageSource ms) {
        LocalValidatorFactoryBean v = new LocalValidatorFactoryBean();
        v.setValidationMessageSource(ms);
        return v;
    }
}
