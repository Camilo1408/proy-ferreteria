/*
 * nombre: PerfilService.java
 * descripcion: Reglas de negocio de perfiles y permisos modulares.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.security;

import com.ferreteria.error.NegocioException;
import com.ferreteria.security.PerfilDtos.PerfilRequest;
import com.ferreteria.security.PerfilDtos.PerfilResponse;
import com.ferreteria.security.PerfilDtos.PermisoInfo;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Servicio de perfiles. El perfil de sistema no se edita ni se elimina. */
@Service
@RequiredArgsConstructor
@Transactional
public class PerfilService {

    private final PerfilRepository perfiles;
    private final UsuarioRepository usuarios;
    private final AdminGuard guard;

    /** Catálogo de permisos asignables. */
    public List<PermisoInfo> catalogo() {
        return Arrays.stream(Permiso.values()).map(p -> new PermisoInfo(p.name(), p.modulo())).toList();
    }

    /** Lista todos los perfiles con su número de usuarios. */
    @Transactional(readOnly = true)
    public List<PerfilResponse> listar() {
        return perfiles.findAll().stream()
                .sorted(Comparator.comparing(Perfil::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(p -> PerfilResponse.de(p, usuarios.countByPerfilId(p.getId()))).toList();
    }

    /** Crea un perfil nuevo. */
    public PerfilResponse crear(PerfilRequest r) {
        String nombre = r.nombre().trim();
        if (perfiles.existsByNombreIgnoreCase(nombre)) {
            throw NegocioException.conflicto("PERFIL_DUPLICADO", "error.perfilDuplicado", nombre);
        }
        Perfil p = Perfil.builder().nombre(nombre).descripcion(limpiar(r.descripcion()))
                .permisos(new HashSet<>(r.permisos())).build();
        return PerfilResponse.de(perfiles.save(p), 0);
    }

    /** Edita nombre, descripción y permisos; rige de inmediato para sus usuarios. */
    public PerfilResponse actualizar(Long id, PerfilRequest r) {
        Perfil p = buscar(id);
        if (p.isSistema()) {
            throw NegocioException.conflicto("PERFIL_SISTEMA", "error.perfilSistema");
        }
        String nombre = r.nombre().trim();
        if (perfiles.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw NegocioException.conflicto("PERFIL_DUPLICADO", "error.perfilDuplicado", nombre);
        }
        p.setNombre(nombre);
        p.setDescripcion(limpiar(r.descripcion()));
        p.getPermisos().clear();
        p.getPermisos().addAll(r.permisos());
        PerfilResponse resp = PerfilResponse.de(perfiles.save(p), usuarios.countByPerfilId(id));
        guard.verificar();
        return resp;
    }

    /** Elimina un perfil que no sea de sistema y no tenga usuarios. */
    public void eliminar(Long id) {
        Perfil p = buscar(id);
        if (p.isSistema()) {
            throw NegocioException.conflicto("PERFIL_SISTEMA", "error.perfilSistema");
        }
        if (usuarios.existsByPerfilId(id)) {
            throw NegocioException.conflicto("PERFIL_EN_USO", "error.perfilEnUso");
        }
        perfiles.delete(p);
    }

    private Perfil buscar(Long id) {
        return perfiles.findById(id)
                .orElseThrow(() -> NegocioException.noEncontrado("PERFIL_NO_ENCONTRADO", "error.perfilNoEncontrado", id));
    }

    private static String limpiar(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
