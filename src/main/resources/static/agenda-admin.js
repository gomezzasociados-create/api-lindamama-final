/**
 * Linda Mamá Spa - Módulo de Agenda General Integrada en Centro de Mando
 * FullCalendar v6 + Fichas Interactivas Cyber Neon Theme
 */

let adminFullCalendar = null;
let modalDetalleAgendaInstance = null;
let currentAgendaCita = null;
let selectedAgendaSociaId = '';

window.initAgendaCalendar = function() {
    setTimeout(() => {
        if (!modalDetalleAgendaInstance) {
            const modalEl = document.getElementById('modalDetalleCitaAgenda');
            if (modalEl) modalDetalleAgendaInstance = new bootstrap.Modal(modalEl);
        }

        // Cargar profesionales en el filtro desplegable si no estaban cargadas
        const filtroSocia = document.getElementById('filtro-socia-agenda');
        if (filtroSocia && filtroSocia.options.length <= 1) {
            fetch('/api/socias')
                .then(r => r.json())
                .then(socias => {
                    socias.forEach(s => {
                        if (s.activa) {
                            const opt = document.createElement('option');
                            opt.value = s.id;
                            opt.textContent = s.nombre;
                            filtroSocia.appendChild(opt);
                        }
                    });
                })
                .catch(err => console.error("Error cargando socias en agenda:", err));
        }

        if (!adminFullCalendar) {
            const calendarEl = document.getElementById('calendar-agenda');
            if (!calendarEl) return;

            adminFullCalendar = new FullCalendar.Calendar(calendarEl, {
                initialView: window.innerWidth < 768 ? 'timeGridDay' : 'dayGridMonth',
                headerToolbar: {
                    left: 'prev,next today',
                    center: 'title',
                    right: window.innerWidth < 768 ? 'timeGridDay,timeGridWeek' : 'dayGridMonth,timeGridWeek,timeGridDay,listMonth'
                },
                locale: 'es',
                firstDay: 1,
                slotMinTime: '08:00:00',
                slotMaxTime: '21:00:00',
                allDaySlot: false,
                slotDuration: '00:30:00',
                slotLabelFormat: {
                    hour: 'numeric',
                    minute: '2-digit',
                    omitZeroMinute: false,
                    meridiem: 'short'
                },
                events: function(info, successCallback, failureCallback) {
                    fetch('/api/citas')
                        .then(r => r.json())
                        .then(citas => {
                            if (selectedAgendaSociaId) {
                                citas = citas.filter(c => c.socia && c.socia.id == selectedAgendaSociaId);
                            }
                            const events = [];
                            citas.forEach(c => {
                                if (!c.fechaHora) return;
                                const startDate = new Date(c.fechaHora);
                                if (isNaN(startDate.getTime())) return;
                                const endDate = new Date(startDate.getTime() + 60 * 60 * 1000);

                                const cliNombre = c.cliente ? c.cliente.nombre : 'Cliente General';
                                const title = `${cliNombre} (${c.nombreTratamiento || 'Servicio'})`;

                                let color = '#ff00ff';
                                const state = (c.estado || '').toUpperCase();
                                if (state === 'CONFIRMADA' || state === 'CONFIRMADO' || state === 'PAGADO') {
                                    color = '#00e5ff'; // cian
                                } else if (state === 'PENDIENTE' || state === 'RESERVADO' || state === 'RESERVADA') {
                                    color = '#ff9800'; // naranja
                                } else if (state === 'AGENDADA' || state === 'AGENDADO') {
                                    color = '#af00ff'; // violeta
                                }

                                events.push({
                                    id: c.id,
                                    title: title,
                                    start: c.fechaHora,
                                    end: endDate.toISOString(),
                                    backgroundColor: color,
                                    borderColor: color,
                                    extendedProps: { cita: c }
                                });
                            });
                            successCallback(events);
                        })
                        .catch(err => {
                            console.error(err);
                            failureCallback(err);
                        });
                },
                eventClick: function(info) {
                    if (typeof reproducirClick === 'function') reproducirClick();
                    abrirModalDetalleCitaAgenda(info.event.extendedProps.cita);
                }
            });

            adminFullCalendar.render();
            window.fullCalendarAgenda = adminFullCalendar;
        } else {
            adminFullCalendar.updateSize();
            adminFullCalendar.refetchEvents();
        }
    }, 150);
};

// Cambio de vista en Agenda: Fichas vs Calendario
window.cambiarVistaAgenda = function(tipo, btn) {
    if (typeof reproducirClick === 'function') reproducirClick();
    const btnTarjetas = document.getElementById('btnVistaTarjetasAgenda');
    const btnCal = document.getElementById('btnVistaCalendarioAgenda');
    const contTarjetas = document.getElementById('vistaTarjetasAgendaContainer');
    const contCal = document.getElementById('vistaCalendarioAgendaContainer');
    
    if (btnTarjetas) btnTarjetas.classList.remove('active');
    if (btnCal) btnCal.classList.remove('active');
    
    if (tipo === 'tarjetas') {
        if (btnTarjetas) btnTarjetas.classList.add('active');
        if (contTarjetas) contTarjetas.classList.remove('d-none');
        if (contCal) contCal.classList.add('d-none');
    } else {
        if (btnCal) btnCal.classList.add('active');
        if (contTarjetas) contTarjetas.classList.add('d-none');
        if (contCal) contCal.classList.remove('d-none');
        initAgendaCalendar();
    }
};

// Filtro de fichas agendadas por tiempo (Todas, Hoy, Próximas)
window.filtrarCitasAgendadasPorTiempo = function(filtro, btn) {
    if (typeof reproducirClick === 'function') reproducirClick();
    if (btn) {
        btn.parentElement.querySelectorAll('button').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
    }
    const fechaHoyStr = document.querySelector('[data-fecha-hoy]') ? document.querySelector('[data-fecha-hoy]').getAttribute('data-fecha-hoy') : new Date().toISOString().split('T')[0];
    const cards = document.querySelectorAll('.item-cita-agendada-card');
    cards.forEach(card => {
        const esHoy = card.getAttribute('data-es-hoy') === 'true';
        const fecha = card.getAttribute('data-fecha');
        if (filtro === 'todas') {
            card.style.display = 'block';
        } else if (filtro === 'hoy') {
            card.style.display = esHoy ? 'block' : 'none';
        } else if (filtro === 'proximas') {
            card.style.display = (fecha && fecha >= fechaHoyStr) ? 'block' : 'none';
        }
    });
};

// Filtro general por socia para fichas y calendario
window.filtrarCitasAgendadasVista = function() {
    if (typeof reproducirClick === 'function') reproducirClick();
    const sociaId = document.getElementById('filtro-socia-agenda').value;
    selectedAgendaSociaId = sociaId;
    
    const cards = document.querySelectorAll('.item-cita-agendada-card');
    cards.forEach(card => {
        const cardSociaId = card.getAttribute('data-socia-id');
        if (!sociaId || cardSociaId === sociaId) {
            card.style.display = 'block';
        } else {
            card.style.display = 'none';
        }
    });
    
    if (adminFullCalendar) {
        adminFullCalendar.refetchEvents();
    }
};

window.filtrarCalendarioAgenda = window.filtrarCitasAgendadasVista;

// Envío de recordatorio personalizado por WhatsApp
window.enviarWhatsAppRecordatorio = function(btn) {
    if (typeof reproducirClick === 'function') reproducirClick();
    const telefono = btn.getAttribute('data-telefono');
    const nombre = btn.getAttribute('data-nombre');
    const fecha = btn.getAttribute('data-fecha');
    const servicio = btn.getAttribute('data-servicio');

    if (!telefono || telefono === '---' || telefono.trim() === '') {
        alert("⚠️ Este cliente no tiene un número de teléfono registrado.");
        return;
    }

    let numeroLimpio = telefono.replace(/\D/g, '');
    if (numeroLimpio.length === 9) { numeroLimpio = '56' + numeroLimpio; }

    const mensaje = `🌸 *RECORDATORIO DE CITA - LINDA MAMÁ SPA* 🌸\n\nHola *${nombre}* ✨, te saludamos con mucho cariño de *Linda Mamá Spa*.\n\nQueremos recordarte tu cita agendada:\n💆‍♀️ *Servicio:* ${servicio}\n📅 *Fecha y Hora:* ${fecha}\n\nPor favor, confírmanos tu asistencia respondiendo a este mensaje con un *"Confirmo"*. ¡Te esperamos para consentirte! 💕`;
    const url = `https://wa.me/${numeroLimpio}?text=${encodeURIComponent(mensaje)}`;
    window.open(url, '_blank');
};

window.abrirModalDetalleCitaAgenda = function(cita) {
    currentAgendaCita = cita;
    document.getElementById('agendaDetalleCitaId').value = cita.id;
    document.getElementById('agendaDetalleServicio').innerText = cita.nombreTratamiento || 'Servicio';
    document.getElementById('agendaDetalleCliente').innerText = cita.cliente ? cita.cliente.nombre : 'Cliente General';
    document.getElementById('agendaDetalleSocia').innerText = cita.socia ? cita.socia.nombre : 'Sin profesional asignada';

    const fechaObj = new Date(cita.fechaHora);
    const fechaFormateada = isNaN(fechaObj.getTime()) ? 'Por definir' : fechaObj.toLocaleString('es-CL', {
        weekday: 'short',
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
    document.getElementById('agendaDetalleFecha').innerText = fechaFormateada;

    const badge = document.getElementById('agendaDetalleEstadoBadge');
    const state = (cita.estado || 'PENDIENTE').toUpperCase();
    badge.innerText = state;
    const btnAprobar = document.getElementById('btnConfirmarPagoAgendaCal');

    if (state === 'CONFIRMADA' || state === 'CONFIRMADO' || state === 'PAGADO') {
        badge.className = 'badge bg-success text-dark fw-bold px-3 py-1';
        if (btnAprobar) btnAprobar.style.display = 'none';
    } else if (state === 'PENDIENTE' || state === 'RESERVADO' || state === 'RESERVADA') {
        badge.className = 'badge bg-warning text-dark fw-bold px-3 py-1';
        if (btnAprobar) btnAprobar.style.display = 'inline-block';
    } else {
        badge.className = 'badge bg-info text-dark fw-bold px-3 py-1';
        if (btnAprobar) btnAprobar.style.display = 'inline-block';
    }

    const mTotal = cita.montoTotal || (cita.totalPagado || 0);
    const mPagado = (cita.totalPagado != null && cita.totalPagado > 0) ? cita.totalPagado : (cita.montoPagado || 0);
    document.getElementById('agendaDetalleMontoTotal').innerText = '$' + Number(mTotal).toLocaleString('es-CL');
    document.getElementById('agendaDetalleMontoPagado').innerText = '$' + Number(mPagado).toLocaleString('es-CL');
    document.getElementById('agendaDetalleTelefono').innerText = (cita.cliente && cita.cliente.telefono) ? cita.cliente.telefono : '---';

    if (!modalDetalleAgendaInstance) {
        modalDetalleAgendaInstance = new bootstrap.Modal(document.getElementById('modalDetalleCitaAgenda'));
    }
    modalDetalleAgendaInstance.show();
};

window.confirmarCitaDesdeCalendarAgenda = function() {
    const id = document.getElementById('agendaDetalleCitaId').value;
    if (typeof confirmarCita === 'function') {
        confirmarCita(id);
    } else {
        fetch('/api/admin/citas/confirmar/' + id, {method: 'POST'})
            .then(res => {
                if (res.ok) window.location.reload();
                else alert("Error al confirmar cita");
            });
    }
};

window.cancelarCitaDesdeCalendarAgenda = function() {
    const id = document.getElementById('agendaDetalleCitaId').value;
    if (typeof cancelarCita === 'function') {
        cancelarCita(id);
    } else {
        fetch('/api/admin/citas/cancelar/' + id, {method: 'POST'})
            .then(res => {
                if (res.ok) window.location.reload();
                else alert("Error al cancelar cita");
            });
    }
};

window.abrirReagendarDesdeCalendarAgenda = function() {
    const id = document.getElementById('agendaDetalleCitaId').value;
    if (modalDetalleAgendaInstance) modalDetalleAgendaInstance.hide();
    if (typeof abrirModalReagendar === 'function') {
        abrirModalReagendar(id);
    }
};

window.enviarWhatsAppDesdeCalendarAgenda = function() {
    if (typeof reproducirClick === 'function') reproducirClick();
    const cita = currentAgendaCita;
    if (!cita || !cita.cliente || !cita.cliente.telefono || cita.cliente.telefono === '---' || cita.cliente.telefono.trim() === '') {
        return alert("⚠️ Este cliente no tiene número de teléfono registrado.");
    }
    
    // Usar la función centralizada de recordatorio
    const fakeBtn = {
        getAttribute: function(attr) {
            if (attr === 'data-telefono') return cita.cliente.telefono;
            if (attr === 'data-nombre') return cita.cliente.nombre;
            if (attr === 'data-servicio') return cita.nombreTratamiento || 'Spa';
            if (attr === 'data-fecha') {
                try {
                    let d = new Date(cita.fechaHora);
                    return d.toLocaleDateString('es-CL') + " a las " + d.toLocaleTimeString('es-CL').substring(0,5);
                } catch(e) { return 'Por definir'; }
            }
            return '';
        }
    };
    enviarWhatsAppRecordatorio(fakeBtn);
};

// Autoactivar tab si viene con #agenda en la URL
document.addEventListener('DOMContentLoaded', function() {
    if (window.location.hash === '#agenda') {
        const t = document.getElementById('agenda-tab');
        if (t) {
            new bootstrap.Tab(t).show();
            initAgendaCalendar();
        }
    }
    
    // Al cambiar a la pestaña de agenda, redibujar y ajustar tamaño del calendario
    const tabBtn = document.getElementById('agenda-tab');
    if (tabBtn) {
        tabBtn.addEventListener('shown.bs.tab', function() {
            initAgendaCalendar();
        });
    }
});
