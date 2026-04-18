package com.gomezsystems.minierp.controller;

import com.gomezsystems.minierp.model.Socia;
import com.gomezsystems.minierp.model.Venta;
import com.gomezsystems.minierp.model.PagoComision;
import com.gomezsystems.minierp.model.Cita;
import com.gomezsystems.minierp.repository.CitaRepository;
import com.gomezsystems.minierp.repository.SociaRepository;
import com.gomezsystems.minierp.repository.VentaRepository;
import com.gomezsystems.minierp.repository.PagoComisionRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/socia")
public class SociaViewController {

    @Autowired private SociaRepository sociaRepository;
    @Autowired private CitaRepository citaRepository;
    @Autowired private VentaRepository ventaRepository;
    @Autowired private PagoComisionRepository pagoComisionRepository;

    @GetMapping("/login")
    public String loginSocia() {
        return "socia_login"; // Crearemos esta vista simple
    }

    @PostMapping("/login")
    public String procesarLogin(@RequestParam String usuario, @RequestParam String password, HttpSession session, Model model) {
        Optional<Socia> sociaOpt = sociaRepository.findByUsuario(usuario);
        
        if (sociaOpt.isPresent() && sociaOpt.get().getPassword().equals(password)) {
            session.setAttribute("sociaLogueada", sociaOpt.get().getId());
            return "redirect:/socia/agenda";
        }
        
        model.addAttribute("error", "Credenciales incorrectas");
        return "socia_login";
    }

    @GetMapping("/agenda")
    @Transactional(readOnly = true)
    public String verAgenda(HttpSession session, Model model) {
        try {
            Long sociaId = (Long) session.getAttribute("sociaLogueada");
            if (sociaId == null) return "redirect:/socia/login";

            Optional<Socia> sociaOpt = sociaRepository.findById(sociaId);
            if (sociaOpt.isEmpty()) return "redirect:/socia/login";
            
            Socia socia = sociaOpt.get();
            model.addAttribute("socia", socia);

            // Filtrar citas desde hoy en adelante para esta socia
            LocalDateTime inicioHoy = LocalDate.now().atStartOfDay();
            LocalDateTime finHoy = LocalDate.now().atTime(23, 59, 59);
            List<Cita> citasFuturas = citaRepository.findBySociaIdAndFechaHoraAfterOrderByFechaHoraAsc(sociaId, inicioHoy.minusMinutes(1));
            model.addAttribute("citas", citasFuturas != null ? citasFuturas : new java.util.ArrayList<>());



            // Comisiones y Saldo (forzamos Long para evitar errores de renderizado en Thymeleaf)
            Double ganadoHoy = ventaRepository.sumMontoSociaBySociaIdAndFechaHoraBetween(sociaId, inicioHoy, finHoy);
            model.addAttribute("ganadoHoy", ganadoHoy != null ? ganadoHoy.longValue() : 0L);

            Double saldoAcumulado = ventaRepository.sumMontoSociaBySociaIdAndLiquidadoSociaFalse(sociaId);
            model.addAttribute("saldoAcumulado", saldoAcumulado != null ? saldoAcumulado.longValue() : 0L);

            List<Venta> ventasPendientes = ventaRepository.findBySociaIdAndLiquidadoSociaFalseOrderByFechaHoraDesc(sociaId);
            model.addAttribute("ventasPendientes", ventasPendientes != null ? ventasPendientes : new java.util.ArrayList<>());

            List<PagoComision> historialPagos = pagoComisionRepository.findBySociaIdOrderByFechaHoraDesc(sociaId).stream()
                    .limit(5)
                    .collect(Collectors.toList());
            model.addAttribute("historialPagos", historialPagos != null ? historialPagos : new java.util.ArrayList<>());

            return "socia_agenda";
        } catch (Exception e) {
            e.printStackTrace(); // Log in console
            model.addAttribute("errorMessage", "Ocurrió un error al cargar la agenda: " + e.getMessage());
            return "error_debug";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/socia/login";
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public String handleException(Exception e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        e.printStackTrace(new java.io.PrintWriter(sw));
        return "<h1>OCURRIÓ UN ERROR CRÍTICO</h1><pre>" + sw.toString() + "</pre>";
    }
}
