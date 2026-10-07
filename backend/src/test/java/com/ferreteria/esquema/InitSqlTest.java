/*
 * nombre: InitSqlTest.java
 * descripcion: Valida db/init.sql: se ejecuta sobre una base vacía y Hibernate (ddl-auto=validate) confirma que coincide con las entidades.
 * fecha_creacion: 2026-10-08
 * actualizacion: 2026-10-08
 * autor: Camilo1408
 * version: 1.1.0
 */
package com.ferreteria.esquema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ferreteria.alerta.AlertaRepository;
import com.ferreteria.alerta.EstadoAlerta;
import com.ferreteria.inventario.MovimientoRepository;
import com.ferreteria.producto.ProductoRepository;
import com.ferreteria.security.PerfilRepository;
import com.ferreteria.security.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:initsql;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=file:../db/init.sql"})
@ActiveProfiles("test")
class InitSqlTest {

    @Autowired ProductoRepository productos;
    @Autowired MovimientoRepository movimientos;
    @Autowired AlertaRepository alertas;
    @Autowired UsuarioRepository usuarios;
    @Autowired PerfilRepository perfiles;

    @Test @DisplayName("init.sql crea un esquema que Hibernate valida y carga datos de ejemplo coherentes")
    void esquemaValido() {
        assertEquals(7, productos.count());
        assertEquals(5, movimientos.count(), "un movimiento de stock inicial por cada producto con existencias");
        assertEquals(2, alertas.count());
        assertEquals(2, alertas.countByEstado(EstadoAlerta.ABIERTA));
        assertTrue(productos.findAll().stream().allMatch(p -> p.getStockActual().signum() >= 0));
        assertEquals(2, usuarios.count(), "la aplicación siembra admin y user sobre el esquema del script");
        assertEquals(2, perfiles.count());
    }
}
