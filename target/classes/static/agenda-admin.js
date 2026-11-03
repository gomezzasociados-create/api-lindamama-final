/**
 * Linda Mamá Spa - Módulo de Agenda General Integrada en Centro de Mando
 * FullCalendar v6 + Ficha / Tarjeta de Cliente Agendado Cyber Neon
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
                selectable: true,
                selectMirror: true,
                unselectAuto: true,
                dateClick: function(info) {
                    if (typeof reproducirClick === 'function') reproducirClick();
                    if (typeof window.abrirModalAgendarDesdeAgenda === 'function') {
                        window.abrirModalAgendarDesdeAgenda(info.dateStr, selectedAgendaSociaId);
                    }
                },
                select: function(info) {
                    if (typeof reproducirClick === 'function') reproducirClick();
                    if (typeof window.abrirModalAgendarDesdeAgenda === 'function') {
                        window.abrirModalAgendarDesdeAgenda(info.startStr, selectedAgendaSociaId);
                    }
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
                    const cita = info.event.extendedProps.cita;
                    abrirModalDetalleCitaAgenda(cita);
                }
            });

            adminFullCalendar.render();
        } else {
            adminFullCalendar.refetchEvents();
            adminFullCalendar.updateSize();
        }
    }, 150);
};

window.filtrarCalendarioAgenda = function() {
    if (typeof reproducirClick === 'function') reproducirClick();
    selectedAgendaSociaId = document.getElementById('filtro-socia-agenda').value;
    if (adminFullCalendar) adminFullCalendar.refetchEvents();
};

window.abrirModalDetalleCitaAgenda = function(cita) {
    if (!cita) return;
    currentAgendaCita = cita;

    const idInput = document.getElementById('agendaDetalleCitaId');
    if (idInput) idInput.value = cita.id;

    // Nombre y teléfono
    const elNombre = document.getElementById('agendaCardClienteNombre');
    if (elNombre) elNombre.innerText = cita.cliente ? cita.cliente.nombre : 'Cliente Web';

    const elTel = document.getElementById('agendaCardClienteTelefono');
    if (elTel) elTel.innerText = (cita.cliente && cita.cliente.telefono) ? cita.cliente.telefono : '---';

    // Servicio y Profesional
    const elServicio = document.getElementById('agendaCardServicio');
    if (elServicio) elServicio.innerText = cita.nombreTratamiento || 'Servicio General';

    const elSocia = document.getElementById('agendaCardSocia');
    if (elSocia) elSocia.innerText = cita.socia ? cita.socia.nombre : 'Sin profesional asignada';

    // Fecha formateada
    const fechaObj = new Date(cita.fechaHora);
    const fechaFormateada = isNaN(fechaObj.getTime()) ? 'Por definir' : fechaObj.toLocaleString('es-CL', {
        weekday: 'short',
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
    const elFecha = document.getElementById('agendaCardFecha');
    if (elFecha) elFecha.innerText = fechaFormateada;

    // Badge HOY
    const badgeHoy = document.getElementById('agendaCardBadgeHoy');
    if (badgeHoy) {
        let esHoy = false;
        if (!isNaN(fechaObj.getTime())) {
            const hoy = new Date();
            esHoy = (fechaObj.getFullYear() === hoy.getFullYear() &&
                     fechaObj.getMonth() === hoy.getMonth() &&
                     fechaObj.getDate() === hoy.getDate());
        }
        badgeHoy.classList.toggle('d-none', !esHoy);
    }

    // Badge Estado y botón Aprobar
    const badge = document.getElementById('agendaCardEstadoBadge');
    const state = (cita.estado || 'PENDIENTE').toUpperCase();
    if (badge) badge.innerText = state;

    const btnAprobar = document.getElementById('agendaCardBtnAprobar');

    if (state === 'CONFIRMADA' || state === 'CONFIRMADO' || state === 'PAGADO') {
        if (badge) badge.className = 'badge badge-confirmado';
        if (btnAprobar) btnAprobar.style.display = 'none';
    } else if (state === 'PENDIENTE' || state === 'RESERVADO' || state === 'RESERVADA') {
        if (badge) badge.className = 'badge badge-reservado';
        if (btnAprobar) btnAprobar.style.display = 'inline-block';
    } else {
        if (badge) badge.className = 'badge bg-info text-dark fw-bold';
        if (btnAprobar) btnAprobar.style.display = 'inline-block';
    }

    // Total / Abono
    const mPagado = (cita.totalPagado != null && cita.totalPagado > 0) ? cita.totalPagado : (cita.montoPagado || 0);
    const montoEl = document.getElementById('agendaCardMonto');
    if (montoEl) {
        if (mPagado > 0) {
            montoEl.innerText = '$' + Number(mPagado).toLocaleString('es-CL');
            montoEl.className = 'fs-5 text-cyan-neon fw-bold';
        } else {
            montoEl.innerText = 'Sin pago ($0)';
            montoEl.className = 'badge bg-secondary';
        }
    }

    if (!modalDetalleAgendaInstance) {
        const modalEl = document.getElementById('modalDetalleCitaAgenda');
        if (modalEl) modalDetalleAgendaInstance = new bootstrap.Modal(modalEl);
    }
    if (modalDetalleAgendaInstance) modalDetalleAgendaInstance.show();
};

window.confirmarCitaDesdeCalendarAgenda = function() {
    const id = document.getElementById('agendaDetalleCitaId').value;
    if (modalDetalleAgendaInstance) modalDetalleAgendaInstance.hide();
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
    if (modalDetalleAgendaInstance) modalDetalleAgendaInstance.hide();
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

window.enviarWhatsAppCitaDesdeModalAgenda = function() {
    if (typeof reproducirClick === 'function') reproducirClick();
    const cita = currentAgendaCita;
    if (!cita) return;
    const fakeBtn = {
        getAttribute: function(attr) {
            if (attr === 'data-telefono') return (cita.cliente && cita.cliente.telefono) ? cita.cliente.telefono : '';
            if (attr === 'data-nombre') return (cita.cliente && cita.cliente.nombre) ? cita.cliente.nombre : '';
            if (attr === 'data-servicio') return cita.nombreTratamiento || 'Servicio';
            if (attr === 'data-fecha') return cita.fechaHora || '';
            return '';
        }
    };
    if (typeof enviarWhatsAppCita === 'function') {
        enviarWhatsAppCita(fakeBtn);
    }
};

window.enviarWhatsAppRecordatorioModalAgenda = function() {
    if (typeof reproducirClick === 'function') reproducirClick();
    const cita = currentAgendaCita;
    if (!cita) return;
    const fakeBtn = {
        getAttribute: function(attr) {
            if (attr === 'data-telefono') return (cita.cliente && cita.cliente.telefono) ? cita.cliente.telefono : '';
            if (attr === 'data-nombre') return (cita.cliente && cita.cliente.nombre) ? cita.cliente.nombre : '';
            if (attr === 'data-servicio') return cita.nombreTratamiento || 'Servicio';
            if (attr === 'data-fecha') {
                try {
                    let d = new Date(cita.fechaHora);
                    return d.toLocaleDateString('es-CL') + " a las " + d.toLocaleTimeString('es-CL').substring(0,5);
                } catch(e) { return 'Por definir'; }
            }
            if (attr === 'data-monto') return (cita.totalPagado != null && cita.totalPagado > 0) ? cita.totalPagado : (cita.montoPagado || 0);
            return '';
        }
    };
    if (typeof enviarWhatsAppConfirmacion === 'function') {
        enviarWhatsAppConfirmacion(fakeBtn);
    } else {
        const telefono = fakeBtn.getAttribute('data-telefono');
        const nombre = fakeBtn.getAttribute('data-nombre');
        const fecha = fakeBtn.getAttribute('data-fecha');
        const servicio = fakeBtn.getAttribute('data-servicio');
        if (!telefono || telefono === '---' || telefono.trim() === '') {
            return alert("⚠️ Este cliente no tiene número de teléfono registrado.");
        }
        let numeroLimpio = telefono.replace(/\D/g, '');
        if (numeroLimpio.length === 9) { numeroLimpio = '56' + numeroLimpio; }
        const mensaje = `🌸 ¡Hola ${nombre}! Te escribimos con mucho cariño de *Linda Mamá Spa* ✨.\n\nQueremos recordarte tu cita para tu sesión de *${servicio}* agendada para el *${fecha}*.\n\n¡Te esperamos para consentirte! Por favor confírmanos tu asistencia con un "Sí". 💕`;
        window.open(`https://wa.me/${numeroLimpio}?text=${encodeURIComponent(mensaje)}`, '_blank');
    }
};

window.imprimirTicketDesdeModalAgenda = function() {
    if (typeof reproducirClick === 'function') reproducirClick();
    const cita = currentAgendaCita;
    if (!cita) return;
    const cliente = cita.cliente ? cita.cliente.nombre : 'Cliente';
    const servicio = cita.nombreTratamiento || 'Servicio';
    const total = (cita.totalPagado != null && cita.totalPagado > 0) ? cita.totalPagado : (cita.montoPagado || 0);
    let fecha = '---';
    if (cita.fechaHora) {
        try {
            let d = new Date(cita.fechaHora);
            fecha = d.toLocaleDateString('es-CL') + " " + d.toLocaleTimeString('es-CL').substring(0,5);
        } catch(e) {}
    }
    const folio = cita.id;
    if (typeof imprimirTicketCliente === 'function') {
        imprimirTicketCliente(cliente, servicio, total, fecha, folio);
    }
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
