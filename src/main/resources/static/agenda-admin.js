/**
 * Linda Mamá Spa - Módulo de Agenda General Integrada en Centro de Mando
 * FullCalendar v6 Cyber Neon Theme
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

        // Cargar profesionales en el filtro desplegable
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

window.filtrarCalendarioAgenda = function() {
    if (typeof reproducirClick === 'function') reproducirClick();
    selectedAgendaSociaId = document.getElementById('filtro-socia-agenda').value;
    if (adminFullCalendar) adminFullCalendar.refetchEvents();
};

window.abrirModalDetalleCitaAgenda = function(cita) {
    currentAgendaCita = cita;
    document.getElementById('agendaDetalleCitaId').value = cita.id;
    document.getElementById('agendaDetalleServicio').innerText = cita.nombreTratamiento || 'Servicio';
    document.getElementById('agendaDetalleCliente').innerText = cita.cliente ? cita.cliente.nombre : 'Cliente General';
    document.getElementById('agendaDetalleSocia').innerText = cita.socia ? cita.socia.nombre : 'Sin profesional asignada';

    const fechaObj = new Date(cita.fechaHora);
    document.getElementById('agendaDetalleFecha').innerText = isNaN(fechaObj.getTime()) ? 'Por definir' : fechaObj.toLocaleString('es-CL', {
        weekday: 'short',
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });

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
    if (confirm("¿Aprobar y confirmar cita?\n\n• Si registra abono o pago, ingresará a Tesorería, Caja y EBITDA.\n• Si no registra pago ($0), quedará confirmada sin ingresos ficticios.")) {
        fetch('/api/admin/citas/confirmar/' + id, {method: 'POST'})
            .then(res => {
                if (res.ok) {
                    if (modalDetalleAgendaInstance) modalDetalleAgendaInstance.hide();
                    if (adminFullCalendar) adminFullCalendar.refetchEvents();
                    window.location.reload();
                } else {
                    alert("Error confirmando la cita");
                }
            });
    }
};

window.cancelarCitaDesdeCalendarAgenda = function() {
    const id = document.getElementById('agendaDetalleCitaId').value;
    if (confirm("¿Eliminar cita definitivamente de la agenda?")) {
        fetch('/api/admin/citas/cancelar/' + id, {method: 'POST'})
            .then(res => {
                if (res.ok) {
                    if (modalDetalleAgendaInstance) modalDetalleAgendaInstance.hide();
                    if (adminFullCalendar) adminFullCalendar.refetchEvents();
                    window.location.reload();
                } else {
                    alert("Error eliminando la cita");
                }
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
    let numeroLimpio = cita.cliente.telefono.replace(/\D/g, '');
    if (numeroLimpio.length === 9) { numeroLimpio = '56' + numeroLimpio; }

    let fechaLimpia = "Por definir";
    if (cita.fechaHora) {
        try {
            let d = new Date(cita.fechaHora);
            fechaLimpia = d.toLocaleDateString('es-CL') + " a las " + d.toLocaleTimeString('es-CL').substring(0,5);
        } catch(e) {}
    }

    const mensaje = `🌸 *LINDA MAMÁ SPA* 🌸\n\nHola *${cita.cliente.nombre}* ✨,\nNos comunicamos para confirmar tu cita agendada para el servicio de *${cita.nombreTratamiento || 'Spa'}*.\n\n📅 *Fecha y Hora:* ${fechaLimpia}\n\nPor favor, respóndenos con un *"Confirmo"* para asegurar tu cupo en la agenda.\n¡Te esperamos! 💆‍♀️`;
    const url = `https://wa.me/${numeroLimpio}?text=${encodeURIComponent(mensaje)}`;
    window.open(url, '_blank');
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
