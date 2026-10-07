/*
 * nombre: AdminServicesTest.java
 * descripcion: Pruebas unitarias de las reglas de usuarios, perfiles y cuenta propia con Mockito.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ferreteria.error.NegocioException;
import com.ferreteria.security.PerfilDtos.PerfilRequest;
import com.ferreteria.security.PerfilDtos.PerfilResponse;
import com.ferreteria.security.UsuarioDtos.CambioClaveRequest;
import com.ferreteria.security.UsuarioDtos.CuentaRequest;
import com.ferreteria.security.UsuarioDtos.UsuarioActualizacion;
import com.ferreteria.security.UsuarioDtos.UsuarioRequest;
import com.ferreteria.security.UsuarioDtos.UsuarioResponse;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminServicesTest {

    @Mock UsuarioRepository usuarios;
    @Mock PerfilRepository perfiles;
    @Mock AdminGuard guard;
    PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    UsuarioService usuarioService;
    PerfilService perfilService;
    Perfil operario;

    @BeforeEach
    void preparar() {
        usuarioService = new UsuarioService(usuarios, perfiles, encoder, guard);
        perfilService = new PerfilService(perfiles, usuarios, guard);
        operario = Perfil.builder().id(2L).nombre("Operario").permisos(new HashSet<>(Set.of(Permiso.PRODUCTOS_VER))).build();
    }

    private Usuario usuario(String username, boolean activo, Perfil perfil) {
        return Usuario.builder().id(5L).username(username).password(encoder.encode("Clave1234")).nombreCompleto("Nombre")
                .activo(activo).perfil(perfil).build();
    }

    @Test @DisplayName("crear usuario: normaliza a minúscula, cifra la contraseña y limpia el correo vacío")
    void creaUsuario() {
        when(usuarios.existsByUsernameIgnoreCase("ana.perez")).thenReturn(false);
        when(perfiles.findById(2L)).thenReturn(Optional.of(operario));
        when(usuarios.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        UsuarioResponse r = usuarioService.crear(new UsuarioRequest(" Ana.Perez ", "Clave1234", " Ana Pérez ", "  ", 2L));
        ArgumentCaptor<Usuario> cap = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(cap.capture());
        assertEquals("ana.perez", cap.getValue().getUsername());
        assertNotEquals("Clave1234", cap.getValue().getPassword());
        assertTrue(encoder.matches("Clave1234", cap.getValue().getPassword()));
        assertNull(cap.getValue().getEmail());
        assertTrue(cap.getValue().isActivo());
        assertEquals("Ana Pérez", r.nombreCompleto());
        assertEquals("Operario", r.perfil().nombre());
    }

    @Test @DisplayName("crear usuario: duplicado o perfil inexistente se rechazan sin guardar")
    void creaUsuarioInvalido() {
        when(usuarios.existsByUsernameIgnoreCase("ana")).thenReturn(true);
        assertEquals("USUARIO_DUPLICADO", assertThrows(NegocioException.class,
                () -> usuarioService.crear(new UsuarioRequest("ana", "Clave1234", "Ana", null, 2L))).getCodigo());
        when(usuarios.existsByUsernameIgnoreCase("luis")).thenReturn(false);
        when(perfiles.findById(99L)).thenReturn(Optional.empty());
        assertEquals("PERFIL_NO_ENCONTRADO", assertThrows(NegocioException.class,
                () -> usuarioService.crear(new UsuarioRequest("luis", "Clave1234", "Luis", null, 99L))).getCodigo());
        verify(usuarios, never()).save(any());
    }

    @Test @DisplayName("actualizar usuario: aplica cambios, valida el último administrador y trata al otro usuario")
    void actualizaUsuario() {
        Usuario u = usuario("luis", true, operario);
        when(usuarios.findById(5L)).thenReturn(Optional.of(u));
        when(perfiles.findById(2L)).thenReturn(Optional.of(operario));
        when(usuarios.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        UsuarioResponse r = usuarioService.actualizar(5L, new UsuarioActualizacion(" Luis R ", "l@x.co", 2L, false), "admin");
        assertFalse(r.activo());
        assertEquals("Luis R", r.nombreCompleto());
        assertEquals("l@x.co", r.email());
        verify(guard).verificar();
    }

    @Test @DisplayName("actualizar usuario: el propio actor no puede desactivarse ni cambiar de perfil, pero sí renombrarse")
    void noAutoModificacion() {
        Usuario u = usuario("admin", true, operario);
        when(usuarios.findById(5L)).thenReturn(Optional.of(u));
        assertEquals("AUTO_MODIFICACION", assertThrows(NegocioException.class,
                () -> usuarioService.actualizar(5L, new UsuarioActualizacion("A", null, 2L, false), "admin")).getCodigo());
        assertEquals("AUTO_MODIFICACION", assertThrows(NegocioException.class,
                () -> usuarioService.actualizar(5L, new UsuarioActualizacion("A", null, 3L, true), "admin")).getCodigo());
        when(perfiles.findById(2L)).thenReturn(Optional.of(operario));
        when(usuarios.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        assertEquals("Nuevo", usuarioService.actualizar(5L, new UsuarioActualizacion("Nuevo", null, 2L, true), "admin").nombreCompleto());
    }

    @Test @DisplayName("actualizar usuario: si el guardián rechaza, la excepción se propaga")
    void guardianRechaza() {
        Usuario u = usuario("luis", true, operario);
        when(usuarios.findById(5L)).thenReturn(Optional.of(u));
        when(perfiles.findById(2L)).thenReturn(Optional.of(operario));
        when(usuarios.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        doThrow(NegocioException.conflicto("ULTIMO_ADMIN", "error.ultimoAdmin")).when(guard).verificar();
        assertEquals("ULTIMO_ADMIN", assertThrows(NegocioException.class,
                () -> usuarioService.actualizar(5L, new UsuarioActualizacion("L", null, 2L, false), "admin")).getCodigo());
    }

    @Test @DisplayName("restablecer contraseña: cifra la nueva; usuario inexistente lanza 404")
    void restablece() {
        Usuario u = usuario("luis", true, operario);
        when(usuarios.findById(5L)).thenReturn(Optional.of(u));
        usuarioService.restablecerClave(5L, "Nueva12345");
        assertTrue(encoder.matches("Nueva12345", u.getPassword()));
        verify(usuarios).save(u);
        when(usuarios.findById(6L)).thenReturn(Optional.empty());
        assertEquals("USUARIO_NO_ENCONTRADO", assertThrows(NegocioException.class, () -> usuarioService.restablecerClave(6L, "x")).getCodigo());
    }

    @Test @DisplayName("cuenta: cambiar clave exige la actual, rechaza la igual y guarda la nueva cifrada")
    void cambiaClave() {
        Usuario u = usuario("luis", true, operario);
        when(usuarios.findByUsername("luis")).thenReturn(Optional.of(u));
        assertEquals("CLAVE_ACTUAL_INCORRECTA", assertThrows(NegocioException.class,
                () -> usuarioService.cambiarClave("luis", new CambioClaveRequest("otra", "Nueva12345"))).getCodigo());
        assertEquals("CLAVE_IGUAL", assertThrows(NegocioException.class,
                () -> usuarioService.cambiarClave("luis", new CambioClaveRequest("Clave1234", "Clave1234"))).getCodigo());
        assertTrue(encoder.matches("Clave1234", u.getPassword()));
        usuarioService.cambiarClave("luis", new CambioClaveRequest("Clave1234", "Nueva12345"));
        assertTrue(encoder.matches("Nueva12345", u.getPassword()));
    }

    @Test @DisplayName("cuenta: editar nombre y correo recorta; ver sesión devuelve permisos ordenados")
    void cuenta() {
        operario.getPermisos().add(Permiso.ALERTAS_VER);
        Usuario u = usuario("luis", true, operario);
        when(usuarios.findByUsername("luis")).thenReturn(Optional.of(u));
        when(usuarios.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        var s = usuarioService.actualizarCuenta("luis", new CuentaRequest(" Luis Nuevo ", " l@x.co "));
        assertEquals("Luis Nuevo", s.nombreCompleto());
        assertEquals("l@x.co", s.email());
        assertEquals(java.util.List.of("ALERTAS_VER", "PRODUCTOS_VER"), usuarioService.sesion("luis").permisos());
        when(usuarios.findByUsername("nadie")).thenReturn(Optional.empty());
        assertEquals("USUARIO_NO_ENCONTRADO", assertThrows(NegocioException.class, () -> usuarioService.sesion("nadie")).getCodigo());
    }

    @Test @DisplayName("perfil: crear valida nombre duplicado y copia los permisos")
    void creaPerfil() {
        when(perfiles.existsByNombreIgnoreCase("Ventas")).thenReturn(false);
        when(perfiles.save(any(Perfil.class))).thenAnswer(i -> i.getArgument(0));
        PerfilResponse r = perfilService.crear(new PerfilRequest(" Ventas ", " ", EnumSet.of(Permiso.PRODUCTOS_VER, Permiso.ALERTAS_VER)));
        assertEquals("Ventas", r.nombre());
        assertNull(r.descripcion());
        assertFalse(r.sistema());
        assertEquals(2, r.permisos().size());
        when(perfiles.existsByNombreIgnoreCase("Dup")).thenReturn(true);
        assertEquals("PERFIL_DUPLICADO", assertThrows(NegocioException.class,
                () -> perfilService.crear(new PerfilRequest("Dup", null, EnumSet.of(Permiso.PRODUCTOS_VER)))).getCodigo());
    }

    @Test @DisplayName("perfil: el de sistema no se edita ni se elimina; los demás sí, validando duplicado y guardián")
    void perfilSistemaYEdicion() {
        Perfil sistema = Perfil.builder().id(1L).nombre("ADMINISTRADOR").sistema(true).build();
        when(perfiles.findById(1L)).thenReturn(Optional.of(sistema));
        assertEquals("PERFIL_SISTEMA", assertThrows(NegocioException.class,
                () -> perfilService.actualizar(1L, new PerfilRequest("X", null, EnumSet.of(Permiso.PRODUCTOS_VER)))).getCodigo());
        assertEquals("PERFIL_SISTEMA", assertThrows(NegocioException.class, () -> perfilService.eliminar(1L)).getCodigo());

        when(perfiles.findById(2L)).thenReturn(Optional.of(operario));
        when(perfiles.existsByNombreIgnoreCaseAndIdNot("Existente", 2L)).thenReturn(true);
        assertEquals("PERFIL_DUPLICADO", assertThrows(NegocioException.class,
                () -> perfilService.actualizar(2L, new PerfilRequest("Existente", null, EnumSet.of(Permiso.PRODUCTOS_VER)))).getCodigo());

        when(perfiles.save(any(Perfil.class))).thenAnswer(i -> i.getArgument(0));
        when(usuarios.countByPerfilId(2L)).thenReturn(3L);
        PerfilResponse r = perfilService.actualizar(2L, new PerfilRequest("Operario 2", "desc", EnumSet.of(Permiso.ALERTAS_VER)));
        assertEquals("Operario 2", r.nombre());
        assertEquals(java.util.List.of("ALERTAS_VER"), r.permisos());
        assertEquals(3, r.usuarios());
        verify(guard).verificar();
    }

    @Test @DisplayName("perfil: eliminar con usuarios lanza PERFIL_EN_USO; sin usuarios borra; inexistente 404")
    void eliminaPerfil() {
        when(perfiles.findById(2L)).thenReturn(Optional.of(operario));
        when(usuarios.existsByPerfilId(2L)).thenReturn(true);
        assertEquals("PERFIL_EN_USO", assertThrows(NegocioException.class, () -> perfilService.eliminar(2L)).getCodigo());
        verify(perfiles, never()).delete(any());
        when(usuarios.existsByPerfilId(2L)).thenReturn(false);
        perfilService.eliminar(2L);
        verify(perfiles).delete(operario);
        when(perfiles.findById(9L)).thenReturn(Optional.empty());
        assertEquals("PERFIL_NO_ENCONTRADO", assertThrows(NegocioException.class, () -> perfilService.eliminar(9L)).getCodigo());
    }

    @Test @DisplayName("perfil: el catálogo lista todos los permisos con su módulo")
    void catalogo() {
        var c = perfilService.catalogo();
        assertEquals(Permiso.values().length, c.size());
        assertTrue(c.stream().anyMatch(p -> p.codigo().equals("AJUSTES_REGISTRAR") && p.modulo().equals("inventario")));
        assertEquals("PERM_PRODUCTOS_VER", Permiso.PRODUCTOS_VER.authority());
    }
}
