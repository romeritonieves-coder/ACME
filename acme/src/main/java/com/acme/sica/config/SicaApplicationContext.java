package com.acme.sica.config;

import com.acme.sica.pattern.factory.VisitaFactory;
import com.acme.sica.pattern.observer.AccesoSubject;
import com.acme.sica.pattern.observer.DatabaseNotificacionObserver;
import com.acme.sica.pattern.strategy.*;
import com.acme.sica.repository.*;
import com.acme.sica.security.RbacService;
import com.acme.sica.security.SessionManager;
import com.acme.sica.service.*;

import java.util.List;

public final class SicaApplicationContext implements AutoCloseable {
    private final DatabaseConnectionManager database;
    private final AccesoSubject accesoSubject;
    private final AuthService authService;
    private final AccesoService accesoService;
    private final PersonaService personaService;
    private final EmpresaService empresaService;
    private final IncidenteService incidenteService;
    private final NotificacionService notificacionService;
    private final AuditoriaService auditoriaService;
    private final UsuarioService usuarioService;

    public SicaApplicationContext() {
        database = DatabaseConnectionManager.getInstance();
        SessionManager sessions = SessionManager.getInstance();

        UsuarioRepository usuarios = new UsuarioRepository();
        RolRepository roles = new RolRepository();
        PermisoRepository permisos = new PermisoRepository();
        PersonaRepository personas = new PersonaRepository();
        EmpresaRepository empresas = new EmpresaRepository();
        PuntoAccesoRepository puntos = new PuntoAccesoRepository();
        VisitaEstadoRepository estados = new VisitaEstadoRepository();
        VisitaRepository visitas = new VisitaRepository();
        NotificacionRepository notificaciones = new NotificacionRepository();
        BitacoraAuditoriaRepository bitacora = new BitacoraAuditoriaRepository();
        VwPersonasActualmenteDentroRepository dentro = new VwPersonasActualmenteDentroRepository();
        VwSolicitudesPendientesRepository pendientes = new VwSolicitudesPendientesRepository();
        PersonaEstadoAccesoRepository estadosPersona = new PersonaEstadoAccesoRepository();
        IncidenteRepository incidentes = new IncidenteRepository();

        auditoriaService = new AuditoriaService(bitacora);
        RbacService rbacService = new RbacService(permisos, sessions);
        AuditedOperationProxy operations =
                new AuditedOperationProxy(database, auditoriaService, rbacService, sessions);
        accesoSubject = new AccesoSubject(database);
        accesoSubject.registrarObservador(new DatabaseNotificacionObserver(notificaciones, usuarios));

        VisitaFactory visitaFactory = new VisitaFactory(estados);
        AccesoStrategyContext strategyContext = new AccesoStrategyContext(List.of(
                new InvitadoPreRegistradoStrategy(visitas, estados, visitaFactory, accesoSubject),
                new InvitadoNoAnunciadoStrategy(visitas, visitaFactory, accesoSubject, usuarios, empresas),
                new CarnetOlvidadoStrategy(visitas, visitaFactory, accesoSubject, usuarios),
                new SalidaOlvidadaStrategy(visitas, estados, visitaFactory, accesoSubject),
                new TrabajadorRegularStrategy(visitas, visitaFactory, accesoSubject)
        ));

        authService = new AuthService(usuarios, permisos, auditoriaService, sessions, database);
        accesoService = new AccesoService(strategyContext, personas, visitas, puntos, estados,
                dentro, pendientes, accesoSubject, operations);
        personaService = new PersonaService(personas, estadosPersona, empresas, operations);
        empresaService = new EmpresaService(empresas, operations);
        incidenteService = new IncidenteService(incidentes, visitas, puntos, accesoSubject, operations);
        notificacionService = new NotificacionService(notificaciones, operations, sessions);
        usuarioService = new UsuarioService(usuarios, roles, empresas, operations);
    }

    public AuthService authService() {
        return authService;
    }

    public AccesoService accesoService() {
        return accesoService;
    }

    public PersonaService personaService() {
        return personaService;
    }

    public EmpresaService empresaService() {
        return empresaService;
    }

    public IncidenteService incidenteService() {
        return incidenteService;
    }

    public NotificacionService notificacionService() {
        return notificacionService;
    }

    public AuditoriaService auditoriaService() {
        return auditoriaService;
    }

    public UsuarioService usuarioService() {
        return usuarioService;
    }

    @Override
    public void close() {
        accesoSubject.close();
        SessionManager.getInstance().endSession();
        database.close();
    }
}
