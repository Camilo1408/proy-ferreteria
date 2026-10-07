/*
 * nombre: Usuario.java
 * descripcion: Entidad de usuario para autenticación (roles ADMIN y USER).
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
 */
package com.ferreteria.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Usuario del sistema. La contraseña se guarda con BCrypt. */
@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    /** Identificador generado. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre de usuario único. */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /** Hash BCrypt de la contraseña. */
    @Column(nullable = false, length = 100)
    private String password;

    /** Rol del usuario. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Rol rol;

    /** Roles disponibles. */
    public enum Rol {
        /** Puede leer y escribir. */
        ADMIN,
        /** Solo lectura. */
        USER
    }
}
