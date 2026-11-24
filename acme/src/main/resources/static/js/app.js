/**
 * SICA - Sistema Integrado de Control de Acceso para Zona Acme
 * Frontend Client Application
 * Arquitectura SPA con WebSocket en tiempo real (STOMP + SockJS)
 */

// Application State
const state = {
    currentUser: null,
    stompClient: null,
    currentPerson: null,
    puntosAcceso: [],
    empresas: []
};

// Initialize on DOM Ready
document.addEventListener('DOMContentLoaded', () => {
    initAuth();
    initWebSocket();
    cargarPuntosAcceso();
    cargarEmpresas();
    cargarPersonasPrueba();
    cargarReporteEvacuacion();
});

// =====================================================================
// 1. AUTHENTICATION & ROLE MANAGEMENT
// =====================================================================

function initAuth() {
    const saved = localStorage.getItem('sica_user');
    if (saved) {
        state.currentUser = JSON.parse(saved);
        actualizarUIUsuario();
    } else {
        // Por defecto loguear automáticamente como Guarda para facilidad de uso
        loginRapido('guarda@acme.com', 'Guarda123*');
    }
}

async function enviarLogin(e) {
    e.preventDefault();
    const email = document.getElementById('login-email').value;
    const password = document.getElementById('login-password').value;
    await ejecutarLogin(email, password);
}

async function loginRapido(email, password) {
    await ejecutarLogin(email, password);
}

async function ejecutarLogin(email, password) {
    try {
        const res = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });
        const data = await res.json();
        if (res.ok && data.autenticado) {
            state.currentUser = data;
            localStorage.setItem('sica_user', JSON.stringify(data));
            cerrarModalLogin();
            actualizarUIUsuario();
            mostrarToast('success', 'Sesión Iniciada', `Bienvenido, ${data.nombre} (${data.rol})`);
            reconnectWebSocket();
            cambiarTabSegunRol();
        } else {
            mostrarToast('error', 'Error de Acceso', data.mensaje || 'Credenciales inválidas');
        }
    } catch (err) {
        mostrarToast('error', 'Error de Conexión', 'No se pudo conectar con el servidor SICA.');
    }
}

function cerrarSesion() {
    state.currentUser = null;
    localStorage.removeItem('sica_user');
    actualizarUIUsuario();
    mostrarToast('info', 'Sesión Finalizada', 'Ha salido de la plataforma.');
    loginRapido('guarda@acme.com', 'Guarda123*');
}

function actualizarUIUsuario() {
    const nameEl = document.getElementById('user-display-name');
    const roleEl = document.getElementById('user-display-role');
    const btnLogin = document.getElementById('btn-login-modal');
    const btnLogout = document.getElementById('btn-logout');

    if (state.currentUser) {
        nameEl.innerText = state.currentUser.nombre;
        roleEl.innerText = `${state.currentUser.rol} • ${state.currentUser.empresaNombre || 'SICA'}`;
        btnLogin.classList.add('hidden');
        btnLogout.classList.remove('hidden');
        filtrarTabsPorRol();
    } else {
        nameEl.innerText = 'No autenticado';
        roleEl.innerText = 'Invitado';
        btnLogin.classList.remove('hidden');
        btnLogout.classList.add('hidden');
    }
}

function cambiarTabSegunRol() {
    if (!state.currentUser) return;
    const rol = state.currentUser.rol;
    if (rol === 'Funcionario de Empresa') {
        cambiarTab('bandeja');
    } else {
        cambiarTab('porteria');
    }
}

function filtrarTabsPorRol() {
    if (!state.currentUser) return;
    const rol = state.currentUser.rol;

    const tabPorteria = document.getElementById('tab-porteria');
    const tabBandeja = document.getElementById('tab-bandeja');
    const tabIncidentes = document.getElementById('tab-incidentes');
    const tabEvac = document.getElementById('tab-evacuacion');
    const tabAudit = document.getElementById('tab-auditoria');

    // Superusuario y Supervisor ven todo
    if (rol === 'Superusuario' || rol === 'Supervisor de Seguridad') {
        tabPorteria.classList.remove('hidden');
        tabBandeja.classList.remove('hidden');
        tabIncidentes.classList.remove('hidden');
        tabEvac.classList.remove('hidden');
        tabAudit.classList.remove('hidden');
    } else if (rol === 'Guarda de Seguridad') {
        tabPorteria.classList.remove('hidden');
        tabBandeja.classList.add('hidden');
        tabIncidentes.classList.remove('hidden');
        tabEvac.classList.remove('hidden');
        tabAudit.classList.add('hidden');
    } else if (rol === 'Funcionario de Empresa') {
        tabPorteria.classList.add('hidden');
        tabBandeja.classList.remove('hidden');
        tabIncidentes.classList.add('hidden');
        tabEvac.classList.remove('hidden');
        tabAudit.classList.add('hidden');
    }
}

// =====================================================================
// 2. WEBSOCKET REAL-TIME CONNECTION (PATRÓN OBSERVER)
// =====================================================================

function initWebSocket() {
    try {
        const socket = new SockJS('/ws-sica');
        state.stompClient = Stomp.over(socket);
        state.stompClient.debug = null; // deshabilitar debug ruidoso

        state.stompClient.connect({}, (frame) => {
            const ind = document.getElementById('ws-indicator');
            const txt = document.getElementById('ws-status');
            ind.className = 'w-2.5 h-2.5 rounded-full bg-emerald-500 pulse-indicator';
            txt.innerText = 'WebSocket En Línea';

            // Canal 1: Eventos de Portería para Guardas y Supervisores
            state.stompClient.subscribe('/topic/porteria', (message) => {
                const evento = JSON.parse(message.body);
                handleEventoWebSocket(evento);
            });

            // Canal 2: Eventos específicos para el Funcionario autenticado
            if (state.currentUser && state.currentUser.usuarioId) {
                state.stompClient.subscribe(`/topic/funcionario/${state.currentUser.usuarioId}`, (message) => {
                    const evento = JSON.parse(message.body);
                    handleEventoWebSocket(evento);
                });
            }

            // Canal 3: Censo de Evacuación en tiempo real
            state.stompClient.subscribe('/topic/evacuacion', () => {
                cargarReporteEvacuacion();
            });

        }, (err) => {
            const ind = document.getElementById('ws-indicator');
            const txt = document.getElementById('ws-status');
            ind.className = 'w-2.5 h-2.5 rounded-full bg-amber-500';
            txt.innerText = 'Reconectando WS...';
            setTimeout(initWebSocket, 4000);
        });
    } catch (e) {
        console.warn('WebSocket init warning:', e);
    }
}

function reconnectWebSocket() {
    if (state.stompClient && state.stompClient.connected) {
        state.stompClient.disconnect();
    }
    initWebSocket();
}

function handleEventoWebSocket(evento) {
    console.log('WS Evento recibido:', evento);

    if (evento.tipo === 'SOLICITUD_INGRESO' || evento.tipo === 'PASE_TEMPORAL') {
        mostrarToast('warning', 'Nueva Solicitud de Ingreso', evento.mensaje);
        cargarSolicitudesPendientes();
    } else if (evento.tipo === 'RESPUESTA_APROBACION') {
        mostrarToast('info', 'Aprobación en Portería', evento.mensaje);
        cargarSolicitudesPendientes();
        if (state.currentPerson && state.currentPerson.documentoIdentidad === evento.persona?.documentoIdentidad) {
            buscarPersonaPorDoc(state.currentPerson.documentoIdentidad);
        }
    } else if (evento.tipo === 'ALERTA_SEGURIDAD') {
        mostrarToast('error', 'Alerta de Seguridad', evento.mensaje);
        cargarIncidentes();
    } else if (evento.tipo === 'CHECK_IN' || evento.tipo === 'CHECK_OUT' || evento.tipo === 'REGULARIZACION') {
        mostrarToast('success', 'Movimiento de Acceso', evento.mensaje);
        cargarReporteEvacuacion();
    }
}

// =====================================================================
// 3. TAB NAVIGATION
// =====================================================================

function cambiarTab(tabName) {
    const tabs = ['porteria', 'bandeja', 'incidentes', 'evacuacion', 'auditoria'];
    tabs.forEach(t => {
        const view = document.getElementById(`view-${t}`);
        const btn = document.getElementById(`tab-${t}`);
        if (view && btn) {
            if (t === tabName) {
                view.classList.remove('hidden');
                btn.className = 'py-3 border-b-2 border-slate-900 text-slate-900 font-semibold flex items-center space-x-2 whitespace-nowrap';
            } else {
                view.classList.add('hidden');
                btn.className = 'py-3 border-b-2 border-transparent hover:text-slate-900 flex items-center space-x-2 whitespace-nowrap';
            }
        }
    });

    if (tabName === 'bandeja') cargarSolicitudesPendientes();
    if (tabName === 'incidentes') cargarIncidentes();
    if (tabName === 'evacuacion') cargarReporteEvacuacion();
    if (tabName === 'auditoria') cargarAuditoria();
}

// =====================================================================
// 4. PANEL DE PORTERÍA & LOS 4 FLUJOS DE ACCESO
// =====================================================================

async function cargarPuntosAcceso() {
    try {
        const res = await fetch('/api/acceso/puntos');
        const data = await res.json();
        state.puntosAcceso = data;
        const select = document.getElementById('select-punto-acceso');
        select.innerHTML = data.map(p => `<option value="${p.id}">${p.nombre} (${p.tipo})</option>`).join('');
    } catch (e) {
        console.error('Error cargando puntos de acceso:', e);
    }
}

async function cargarEmpresas() {
    try {
        const res = await fetch('/api/personas/empresas');
        const data = await res.json();
        state.empresas = data;
        const select = document.getElementById('modal-na-empresa');
        if (select) {
            select.innerHTML = data.map(emp => `<option value="${emp.id}">${emp.nombre} - ${emp.ubicacion}</option>`).join('');
        }
    } catch (e) {
        console.error('Error cargando empresas:', e);
    }
}

async function buscarPersona() {
    const doc = document.getElementById('input-buscar-doc').value.trim();
    if (!doc) {
        mostrarToast('info', 'Búsqueda', 'Ingrese un número de documento.');
        return;
    }
    await buscarPersonaPorDoc(doc);
}

async function buscarPersonaPorDoc(doc) {
    try {
        const res = await fetch(`/api/personas/${doc}`);
        if (!res.ok) {
            mostrarToast('error', 'Persona No Encontrada', `No existe registro para el documento ${doc}`);
            return;
        }
        const persona = await res.json();
        state.currentPerson = persona;
        renderPersonaCard(persona);
        document.getElementById('input-buscar-doc').value = persona.documentoIdentidad;
    } catch (e) {
        mostrarToast('error', 'Error', 'Error al buscar datos de la persona.');
    }
}

function renderPersonaCard(p) {
    document.getElementById('persona-nombre').innerText = p.nombre;
    document.getElementById('persona-doc').innerText = p.documentoIdentidad;
    document.getElementById('persona-tipo').innerText = p.tipoPersona;
    document.getElementById('persona-empresa').innerText = p.empresaNombre || 'Sin empresa asignada';
    document.getElementById('persona-telefono').innerText = p.telefono || 'N/A';
    document.getElementById('persona-foto').src = p.urlFoto || 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150';

    const badge = document.getElementById('persona-badge-estado');
    const restrBox = document.getElementById('persona-restriccion-box');
    const restrTxt = document.getElementById('persona-motivo-restriccion');

    if (p.estadoAccesoNombre === 'Con Prohibicion de Ingreso') {
        badge.className = 'px-3 py-1 text-xs font-bold rounded-full badge-prohibido';
        badge.innerText = 'CON PROHIBICIÓN DE INGRESO';
        restrBox.classList.remove('hidden');
        restrTxt.innerText = p.motivoRestriccion || 'Acceso vetado por Dirección de Seguridad.';
    } else {
        badge.className = 'px-3 py-1 text-xs font-bold rounded-full badge-activo';
        badge.innerText = 'Activo';
        restrBox.classList.add('hidden');
    }
}

async function ejecutarFlujo(tipoFlujo) {
    if (!state.currentPerson) {
        mostrarToast('info', 'Seleccione Persona', 'Debe buscar o seleccionar una persona primero.');
        return;
    }

    const puntoId = document.getElementById('select-punto-acceso').value;
    const placa = document.getElementById('input-vehiculo-placa').value.trim();

    const requestBody = {
        tipoFlujo: tipoFlujo,
        documentoIdentidad: state.currentPerson.documentoIdentidad,
        puntoAccesoId: parseInt(puntoId),
        vehiculoPlaca: placa || null,
        empresaDestinoId: state.currentPerson.empresaId,
        anfitrionId: 4 // Funcionario Laura Restrepo por defecto
    };

    try {
        const res = await fetch('/api/acceso/procesar', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-Usuario-Email': state.currentUser ? state.currentUser.email : 'guarda@acme.com'
            },
            body: JSON.stringify(requestBody)
        });

        const data = await res.json();
        if (data.exito) {
            mostrarToast('success', 'Operación Exitosa', data.mensaje);
        } else {
            mostrarToast('error', 'Acceso Denegado', data.mensaje);
        }
    } catch (e) {
        mostrarToast('error', 'Error', 'Fallo al procesar el flujo de acceso.');
    }
}

async function ejecutarCheckOut() {
    if (!state.currentPerson) {
        mostrarToast('info', 'Seleccione Persona', 'Seleccione la persona para registrar salida.');
        return;
    }
    // Buscar la última visita activa o enviar salida directa
    try {
        const res = await fetch(`/api/acceso/procesar`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-Usuario-Email': state.currentUser ? state.currentUser.email : 'guarda@acme.com'
            },
            body: JSON.stringify({
                tipoFlujo: 'TRABAJADOR_REGULAR',
                documentoIdentidad: state.currentPerson.documentoIdentidad
            })
        });
        const data = await res.json();
        if (data.visitaId) {
            const outRes = await fetch(`/api/acceso/check-out?visitaId=${data.visitaId}`, {
                method: 'POST',
                headers: { 'X-Usuario-Email': state.currentUser ? state.currentUser.email : 'guarda@acme.com' }
            });
            const outData = await outRes.json();
            mostrarToast('info', 'Check-Out Completado', outData.mensaje);
        }
    } catch (e) {
        mostrarToast('error', 'Error', 'No se pudo procesar la salida.');
    }
}

function abrirModalNoAnunciado() {
    if (!state.currentPerson) {
        mostrarToast('info', 'Seleccione Persona', 'Busque al visitante primero.');
        return;
    }
    document.getElementById('modal-no-anunciado').classList.remove('hidden');
}

function cerrarModalNoAnunciado() {
    document.getElementById('modal-no-anunciado').classList.add('hidden');
}

async function enviarSolicitudNoAnunciado(e) {
    e.preventDefault();
    const empId = document.getElementById('modal-na-empresa').value;
    const motivo = document.getElementById('modal-na-motivo').value;
    const obs = document.getElementById('modal-na-observaciones').value;
    const puntoId = document.getElementById('select-punto-acceso').value;

    const requestBody = {
        tipoFlujo: 'INVITADO_NO_ANUNCIADO',
        documentoIdentidad: state.currentPerson.documentoIdentidad,
        puntoAccesoId: parseInt(puntoId),
        empresaDestinoId: parseInt(empId),
        motivoVisita: motivo,
        observaciones: obs,
        anfitrionId: 4 // Funcionario
    };

    try {
        const res = await fetch('/api/acceso/procesar', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-Usuario-Email': state.currentUser ? state.currentUser.email : 'guarda@acme.com'
            },
            body: JSON.stringify(requestBody)
        });
        const data = await res.json();
        cerrarModalNoAnunciado();
        if (data.exito) {
            mostrarToast('warning', 'Esperando Aprobación', data.mensaje);
        } else {
            mostrarToast('error', 'Acceso Denegado', data.mensaje);
        }
    } catch (err) {
        mostrarToast('error', 'Error', 'Error al notificar al funcionario.');
    }
}

async function cargarPersonasPrueba() {
    try {
        const res = await fetch('/api/personas/buscar');
        const data = await res.json();
        const tbody = document.getElementById('tabla-personas-prueba');
        tbody.innerHTML = data.map(p => `
            <tr onclick="buscarPersonaPorDoc('${p.documentoIdentidad}')" class="hover:bg-slate-50 cursor-pointer transition">
                <td class="p-3">
                    <img src="${p.urlFoto}" class="w-8 h-8 rounded-full object-cover">
                </td>
                <td class="p-3 font-semibold text-slate-900">${p.nombre}</td>
                <td class="p-3 font-mono">${p.documentoIdentidad}</td>
                <td class="p-3">${p.tipoPersona}</td>
                <td class="p-3 text-slate-500">${p.empresaNombre}</td>
                <td class="p-3">
                    <span class="px-2 py-0.5 rounded text-[11px] font-bold ${p.estadoAccesoNombre === 'Activo' ? 'badge-activo' : 'badge-prohibido'}">
                        ${p.estadoAccesoNombre}
                    </span>
                </td>
                <td class="p-3 text-right">
                    <button class="px-2 py-1 bg-slate-900 text-white rounded text-[10px] font-bold hover:bg-slate-800">
                        Cargar
                    </button>
                </td>
            </tr>
        `).join('');

        // Cargar por defecto la primera persona
        if (data.length > 0) {
            state.currentPerson = data[0];
            renderPersonaCard(data[0]);
        }
    } catch (e) {
        console.error('Error cargando personas:', e);
    }
}

// =====================================================================
// 5. BANDEJA DE SOLICITUDES PENDIENTES (FUNCIONARIO DE EMPRESA)
// =====================================================================

async function cargarSolicitudesPendientes() {
    try {
        const email = state.currentUser ? state.currentUser.email : '';
        const res = await fetch('/api/acceso/pendientes', {
            headers: { 'X-Usuario-Email': email }
        });
        const data = await res.json();

        const badge = document.getElementById('badge-pendientes-count');
        badge.innerText = data.length;
        badge.classList.toggle('hidden', data.length === 0);

        const container = document.getElementById('lista-solicitudes-pendientes');
        if (data.length === 0) {
            container.innerHTML = `
                <div class="col-span-2 p-8 text-center bg-white rounded-lg border border-slate-200 text-slate-400">
                    <i class="fa-solid fa-circle-check text-4xl text-emerald-400 mb-2"></i>
                    <p class="font-semibold text-sm">No hay solicitudes pendientes por revisar.</p>
                </div>
            `;
            return;
        }

        container.innerHTML = data.map(s => `
            <div class="bg-white p-5 rounded-lg border border-slate-200 shadow-sm space-y-3">
                <div class="flex items-center justify-between">
                    <span class="px-2.5 py-0.5 text-xs font-bold rounded-full ${s.esPaseTemporal ? 'bg-indigo-100 text-indigo-800 border border-indigo-200' : 'badge-pendiente'}">
                        ${s.esPaseTemporal ? 'PASE TEMPORAL (CARNET OLVIDADO)' : 'INVITADO NO ANUNCIADO'}
                    </span>
                    <span class="text-xs text-slate-400 font-mono">${s.fechaSolicitud || 'Reciente'}</span>
                </div>
                <div>
                    <h3 class="text-base font-bold text-slate-900">${s.visitante}</h3>
                    <p class="text-xs text-slate-500">Documento: <strong class="text-slate-700">${s.documentoIdentidad}</strong> • Tipo: ${s.tipoPersona}</p>
                    <p class="text-xs text-slate-500">Empresa: <strong>${s.empresaDestino || 'Acme Corporation'}</strong></p>
                </div>
                <div class="flex space-x-2 pt-2 border-t border-slate-100">
                    <button onclick="resolverSolicitud(${s.visitaId}, true)" class="flex-1 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded text-xs font-bold transition flex items-center justify-center">
                        <i class="fa-solid fa-check mr-1.5"></i> Aprobar Ingreso
                    </button>
                    <button onclick="resolverSolicitud(${s.visitaId}, false)" class="flex-1 py-1.5 bg-slate-100 hover:bg-red-50 hover:text-red-600 text-slate-700 border border-slate-300 rounded text-xs font-bold transition flex items-center justify-center">
                        <i class="fa-solid fa-xmark mr-1.5"></i> Rechazar
                    </button>
                </div>
            </div>
        `).join('');
    } catch (e) {
        console.error('Error cargando solicitudes:', e);
    }
}

async function resolverSolicitud(visitaId, aprobado) {
    try {
        const res = await fetch('/api/acceso/resolver', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-Usuario-Email': state.currentUser ? state.currentUser.email : 'funcionario@acme.com'
            },
            body: JSON.stringify({
                visitaId: visitaId,
                aprobado: aprobado,
                observaciones: aprobado ? 'Autorizado en tiempo real por Funcionario' : 'Rechazado por Funcionario'
            })
        });
        const data = await res.json();
        mostrarToast(aprobado ? 'success' : 'info', aprobado ? 'Visita Aprobada' : 'Visita Rechazada', data.mensaje);
        cargarSolicitudesPendientes();
    } catch (e) {
        mostrarToast('error', 'Error', 'No se pudo resolver la solicitud.');
    }
}

// =====================================================================
// 6. GESTIÓN DE INCIDENTES
// =====================================================================

async function cargarIncidentes() {
    try {
        const res = await fetch('/api/incidentes');
        const data = await res.json();
        const tbody = document.getElementById('tabla-incidentes');
        if (data.length === 0) {
            tbody.innerHTML = `<tr><td colspan="8" class="p-6 text-center text-slate-400">No se han registrado incidentes.</td></tr>`;
            return;
        }

        tbody.innerHTML = data.map(inc => `
            <tr class="hover:bg-slate-50">
                <td class="p-3 font-bold font-mono">#${inc.id}</td>
                <td class="p-3 text-slate-500">${inc.fecha}</td>
                <td class="p-3">
                    <span class="px-2 py-0.5 rounded text-[10px] font-bold ${
                        inc.severidad === 'Critica' ? 'bg-red-100 text-red-800 border border-red-300' :
                        inc.severidad === 'Alta' ? 'bg-orange-100 text-orange-800 border border-orange-300' :
                        inc.severidad === 'Media' ? 'bg-amber-100 text-amber-800 border border-amber-300' :
                        'bg-slate-100 text-slate-700'
                    }">${inc.severidad}</span>
                </td>
                <td class="p-3">
                    <span class="px-2 py-0.5 rounded text-[10px] font-bold ${
                        inc.estado === 'Abierto' ? 'bg-red-50 text-red-700' :
                        inc.estado === 'En Investigacion' ? 'bg-amber-50 text-amber-700' :
                        'bg-emerald-50 text-emerald-700'
                    }">${inc.estado}</span>
                </td>
                <td class="p-3 font-medium text-slate-800">${inc.descripcion}</td>
                <td class="p-3 text-slate-500">${inc.accionesTomadas || 'Sin acción'}</td>
                <td class="p-3 text-slate-600">${inc.reportadoPorNombre}</td>
                <td class="p-3 text-right">
                    ${inc.estado !== 'Cerrado' ? `
                        <button onclick="cambiarEstadoIncidente(${inc.id}, 'Cerrado')" class="px-2 py-1 bg-slate-900 text-white rounded text-[10px] font-semibold hover:bg-slate-800">
                            Cerrar
                        </button>
                    ` : '<span class="text-xs text-slate-400">Finalizado</span>'}
                </td>
            </tr>
        `).join('');
    } catch (e) {
        console.error('Error cargando incidentes:', e);
    }
}

function abrirModalNuevoIncidente() {
    document.getElementById('modal-incidente').classList.remove('hidden');
}

function cerrarModalIncidente() {
    document.getElementById('modal-incidente').classList.add('hidden');
}

async function enviarIncidente(e) {
    e.preventDefault();
    const visitaId = document.getElementById('inc-visita-id').value;
    const severidad = document.getElementById('inc-severidad').value;
    const desc = document.getElementById('inc-descripcion').value;
    const acciones = document.getElementById('inc-acciones').value;

    try {
        const res = await fetch('/api/incidentes', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-Usuario-Email': state.currentUser ? state.currentUser.email : 'guarda@acme.com'
            },
            body: JSON.stringify({
                visitaId: parseInt(visitaId),
                severidad: severidad,
                descripcion: desc,
                accionesTomadas: acciones
            })
        });
        cerrarModalIncidente();
        mostrarToast('warning', 'Incidente Registrado', 'Novedad registrada en bitácora de incidentes.');
        cargarIncidentes();
    } catch (e) {
        mostrarToast('error', 'Error', 'No se pudo guardar el incidente.');
    }
}

async function cambiarEstadoIncidente(id, nuevoEstado) {
    try {
        await fetch(`/api/incidentes/${id}/estado`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'X-Usuario-Email': state.currentUser ? state.currentUser.email : 'supervisor@acme.com'
            },
            body: JSON.stringify({ estado: nuevoEstado, accionesTomadas: 'Novedad solventada y validada en garita.' })
        });
        mostrarToast('success', 'Incidente Actualizado', `Estado cambiado a ${nuevoEstado}`);
        cargarIncidentes();
    } catch (e) {
        mostrarToast('error', 'Error', 'No se pudo actualizar el estado.');
    }
}

// =====================================================================
// 7. CENSO DE EVACUACIÓN (VISTA SQL: vw_personas_actualmente_dentro)
// =====================================================================

async function cargarReporteEvacuacion() {
    try {
        const [resLista, resMetricas] = await Promise.all([
            fetch('/api/acceso/evacuacion'),
            fetch('/api/acceso/metricas-evacuacion')
        ]);

        const personas = await resLista.json();
        const metricas = await resMetricas.json();

        // Actualizar Contadores de Emergencia
        document.getElementById('metric-total-dentro').innerText = metricas.totalDentro || 0;
        document.getElementById('metric-trabajadores-dentro').innerText = metricas.trabajadoresDentro || 0;
        document.getElementById('metric-invitados-dentro').innerText = metricas.invitadosDentro || 0;
        document.getElementById('badge-evac-total').innerText = metricas.totalDentro || 0;

        const tbody = document.getElementById('tabla-evacuacion-body');
        if (personas.length === 0) {
            tbody.innerHTML = `<tr><td colspan="7" class="p-6 text-center text-slate-400">Instalaciones despejadas. Ninguna persona registrada actualmente dentro.</td></tr>`;
            return;
        }

        tbody.innerHTML = personas.map(p => `
            <tr class="hover:bg-slate-50">
                <td class="p-3 font-mono font-bold text-slate-900">${p.documentoIdentidad}</td>
                <td class="p-3 font-semibold">${p.personaNombre}</td>
                <td class="p-3">
                    <span class="px-2 py-0.5 rounded text-[10px] font-bold ${p.tipoPersona === 'Trabajador' ? 'bg-blue-100 text-blue-800' : 'bg-emerald-100 text-emerald-800'}">
                        ${p.tipoPersona}
                    </span>
                </td>
                <td class="p-3 text-slate-600">${p.empresaDestino}</td>
                <td class="p-3 text-slate-500">${p.puntoEntrada}</td>
                <td class="p-3 font-mono text-slate-600">${p.fechaEntrada}</td>
                <td class="p-3 font-mono">${p.vehiculoPlaca || '—'}</td>
            </tr>
        `).join('');
    } catch (e) {
        console.error('Error cargando censo de evacuación:', e);
    }
}

function imprimirEvacuacion() {
    window.print();
}

// =====================================================================
// 8. VISOR DE BITÁCORA DE AUDITORÍA
// =====================================================================

async function cargarAuditoria() {
    try {
        const res = await fetch('/api/auditoria');
        const data = await res.json();
        const tbody = document.getElementById('tabla-auditoria');
        tbody.innerHTML = data.map(a => `
            <tr class="hover:bg-slate-50">
                <td class="p-3 font-bold">#${a.id}</td>
                <td class="p-3 text-slate-500">${a.fechaHora}</td>
                <td class="p-3">
                    <span class="px-2 py-0.5 bg-slate-100 text-slate-800 rounded font-semibold text-[10px]">
                        ${a.accionRealizada}
                    </span>
                </td>
                <td class="p-3 text-slate-600">${a.tablaAfectada || '—'}</td>
                <td class="p-3 text-slate-900 font-sans font-medium">${a.usuario ? a.usuario.email : 'SISTEMA / ANÓNIMO'}</td>
                <td class="p-3 text-slate-500">${a.ipOrigen || '127.0.0.1'}</td>
                <td class="p-3 text-slate-600 max-w-xs truncate" title="${escapeHtml(a.detalles || '')}">
                    ${escapeHtml(a.detalles || '{}')}
                </td>
            </tr>
        `).join('');
    } catch (e) {
        console.error('Error cargando bitácora:', e);
    }
}

function escapeHtml(str) {
    return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

// =====================================================================
// 9. MODAL & TOAST NOTIFICATION HELPERS
// =====================================================================

function abrirModalLogin() {
    document.getElementById('modal-login').classList.remove('hidden');
}

function cerrarModalLogin() {
    document.getElementById('modal-login').classList.add('hidden');
}

function mostrarToast(tipo, titulo, mensaje) {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');

    const colores = {
        success: 'border-l-4 border-emerald-500 bg-white text-slate-800 shadow-md',
        error: 'border-l-4 border-red-500 bg-white text-slate-800 shadow-md',
        warning: 'border-l-4 border-amber-500 bg-white text-slate-800 shadow-md',
        info: 'border-l-4 border-blue-500 bg-white text-slate-800 shadow-md'
    };

    const iconos = {
        success: 'fa-solid fa-circle-check text-emerald-500',
        error: 'fa-solid fa-triangle-exclamation text-red-500',
        warning: 'fa-solid fa-bell text-amber-500',
        info: 'fa-solid fa-circle-info text-blue-500'
    };

    toast.className = `p-4 rounded border border-slate-200 transition-all duration-300 transform translate-y-2 opacity-0 ${colores[tipo] || colores.info}`;
    toast.innerHTML = `
        <div class="flex items-start space-x-3">
            <i class="${iconos[tipo]} mt-0.5 text-base"></i>
            <div class="flex-1">
                <div class="text-xs font-bold text-slate-900">${titulo}</div>
                <div class="text-xs text-slate-600 mt-0.5 leading-snug">${mensaje}</div>
            </div>
            <button onclick="this.parentElement.parentElement.remove()" class="text-slate-400 hover:text-slate-600 text-xs">
                <i class="fa-solid fa-xmark"></i>
            </button>
        </div>
    `;

    container.appendChild(toast);
    setTimeout(() => {
        toast.classList.remove('translate-y-2', 'opacity-0');
    }, 10);

    setTimeout(() => {
        toast.classList.add('opacity-0', 'translate-y-2');
        setTimeout(() => toast.remove(), 300);
    }, 5000);
}
