/*
 * nombre: Usuario.java
 * descripcion: Entidad de usuario con perfil de permisos modulares.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Usuario del sistema. La contraseña se guarda con BCrypt. Los usuarios no se eliminan: se desactivan. */
@Entity
@Table(name = "app_usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre de usuario único, en minúscula. */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /** Hash BCrypt de la contraseña. */
    @Column(nullable = false, length = 100)
    private String password;

    @Column(name = "nombre_completo", nullable = false, length = 100)
    private String nombreCompleto;

    @Column(length = 120)
    private String email;

    /** Un usuario inactivo no puede iniciar sesión ni usar tokens emitidos antes. */
    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "perfil_id", nullable = false)
    private Perfil perfil;

    @Column(name = "creado_en", nullable = false)
    @Builder.Default
    private Instant creadoEn = Instant.now();
}
