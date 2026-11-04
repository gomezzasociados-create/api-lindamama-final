package com.gomezsystems.minierp;

import com.gomezsystems.minierp.model.*;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MinierpApplicationTests {

    @Test
    void testThymeleafRendersAdminAndPosCompletely() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);

        Cliente cli = new Cliente();
        cli.setId(1L);
        cli.setNombre("Cliente O'Connor \"Vip\"");
        cli.setTelefono("+56911112222");
        cli.setKardexJson("{\"tratamiento\":{\"s_lipolaser\":\"5\"},\"visitas\":[{\"id\":1,\"fecha\":\"01/11/2026\",\"proc\":\"Lipolaser\"}]}");

        Socia socia = new Socia();
        socia.setId(1L);
        socia.setNombre("Crisley Paredes");
        socia.setActiva(true);

        Tratamiento trat = new Tratamiento();
        trat.setId(1L);
        trat.setNombre("Masaje Relajante");
        trat.setPrecio(35000);
        trat.setEnPromocion(true);
        trat.setPrecioPromo(25000);
        trat.setCantidad("1");

        Cita cita = new Cita();
        cita.setId(1L);
        cita.setCliente(cli);
        cita.setSocia(socia);
        cita.setNombreTratamiento("Masaje Relajante");
        cita.setEstado("RESERVADO");
        cita.setFechaHora(LocalDateTime.now());
        cita.setTotalPagado(25000);

        Venta venta = new Venta();
        venta.setId(10L);
        venta.setCliente(cli);
        venta.setDetalle("Masaje Relajante");
        venta.setMontoTotal(25000.0);
        venta.setTipoPago("EFECTIVO");
        venta.setEstado("PAGADO");
        venta.setFechaHora(LocalDateTime.now());

        Egreso egreso = new Egreso();
        egreso.setId(20L);
        egreso.setDescripcion("Compra Insumos 'Especiales'");
        egreso.setMonto(12000.0);
        egreso.setMetodoSalida("EFECTIVO");
        egreso.setFechaHora(LocalDateTime.now());

        CorteZ corte = new CorteZ();
        corte.setId(5L);
        corte.setFechaGeneracion(LocalDateTime.now());
        corte.setTotalEfectivo(25000.0);
        corte.setTotalEgresos(12000.0);
        corte.setEfectivoEsperadoCajon(13000.0);

        Context ctx = new Context();
        ctx.setVariable("fechaHoy", LocalDate.now().toString());
        ctx.setVariable("ingresosTotales", 150000.0);
        ctx.setVariable("citas", List.of(cita));
        ctx.setVariable("citasAgendadas", List.of(cita));
        ctx.setVariable("socias", List.of(socia));
        ctx.setVariable("tratamientos", List.of(trat));
        ctx.setVariable("clientes", List.of(cli));
        ctx.setVariable("ventasTurno", List.of(venta));
        ctx.setVariable("ventasHistorial", List.of(venta));
        ctx.setVariable("egresos", List.of(egreso));
        ctx.setVariable("cortes", List.of(corte));

        String adminHtml = engine.process("admin", ctx);
        assertTrue(adminHtml.contains("id=\"socias\""), "Debe renderizar la sección #socias");
        assertTrue(adminHtml.contains("id=\"ebitda\""), "Debe renderizar la sección #ebitda");
        assertTrue(adminHtml.contains("id=\"contrasenas\""), "Debe renderizar la sección #contrasenas");
        assertTrue(adminHtml.contains("window.actualizarMedidoresClientes"), "Debe renderizar el script completo al final");
        assertTrue(adminHtml.trim().endsWith("</html>"), "Debe terminar con </html>");

        Context posCtx = new Context();
        posCtx.setVariable("servicios", List.of(trat));
        posCtx.setVariable("clientes", List.of(cli));
        String posHtml = engine.process("pos", posCtx);
        assertTrue(posHtml.trim().endsWith("</html>"), "POS debe terminar con </html>");
    }
}
