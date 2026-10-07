/*
 * nombre: Perfil.java
 * descripcion: Entidad Perfil: conjunto nombrado de permisos asignable a usuarios.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.security;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Perfil de acceso. Un perfil de sistema (ADMINISTRADOR) no puede modificarse ni eliminarse. */
@Entity
@Table(name = "perfil")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Perfil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre único (sin distinguir mayúsculas), 3 a 60 caracteres. */
    @Column(nullable = false, unique = true, length = 60)
    private String nombre;

    @Column(length = 200)
    private String descripcion;

    /** Verdadero para perfiles protegidos creados por el sistema. */
    @Column(nullable = false)
    private boolean sistema;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "perfil_permiso", joinColumns = @JoinColumn(name = "perfil_id"))
    @Column(name = "permiso", nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<Permiso> permisos = new HashSet<>();
}
