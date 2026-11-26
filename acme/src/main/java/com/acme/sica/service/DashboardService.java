package com.acme.sica.service;

import com.acme.sica.dto.DashboardSnapshot;
import com.acme.sica.model.BitacoraAuditoria;
import com.acme.sica.model.Notificacion;
import com.acme.sica.model.Usuario;
import com.acme.sica.repository.BitacoraAuditoriaRepository;
import com.acme.sica.repository.DashboardRepository;
import com.acme.sica.repository.NotificacionRepository;
import com.acme.sica.security.SessionManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class DashboardService {
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final DashboardRepository dashboardRepository;
    private final NotificacionRepository notificacionRepository;
    private final BitacoraAuditoriaRepository auditoriaRepository;
    private final com.acme.sica.config.DatabaseConnectionManager database;
    private final SessionManager sessions;

    public DashboardService(DashboardRepository dashboardRepository,
                            NotificacionRepository notificacionRepository,
                            BitacoraAuditoriaRepository auditoriaRepository,
                            com.acme.sica.config.DatabaseConnectionManager database,
                            SessionManager sessions) {
        this.dashboardRepository = dashboardRepository;
        this.notificacionRepository = notificacionRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.database = database;
        this.sessions = sessions;
    }

    public DashboardSnapshot load() {
        Usuario user = requireCurrentUser();
        boolean companyScoped = isCompanyUser(user);
        Integer companyId = user.getEmpresa() == null ? null : user.getEmpresa().getId();
        Long peopleInside = sessions.hasPermission("EVACUACION_VER")
                ? dashboardRepository.countPeopleInside() : null;
        Long pendingRequests = sessions.hasPermission("ACCESO_APROBAR")
                ? dashboardRepository.countPendingRequests(user.getNombre(),
                        user.getEmpresa() == null ? null : user.getEmpresa().getNombre(), companyScoped)
                : null;
        Long openIncidents = sessions.hasPermission("INCIDENTES_VER")
                ? dashboardRepository.countOpenIncidents() : null;
        boolean canSeeVisits = sessions.hasPermission("ACCESO_CHECKIN")
                || sessions.hasPermission("ACCESO_CHECKOUT")
                || sessions.hasPermission("ACCESO_SOLICITAR")
                || sessions.hasPermission("ACCESO_APROBAR");
        Long visitsToday = canSeeVisits
                ? dashboardRepository.countVisitsToday(user.getId(), companyId, companyScoped) : null;
        List<DashboardSnapshot.RecentVisit> recentVisits = canSeeVisits
                ? dashboardRepository.findRecentVisits(user.getId(), companyId, companyScoped) : List.of();

        List<DashboardSnapshot.ActivityItem> activity = sessions.hasPermission("AUDITORIA_VER")
                ? loadAuditActivity()
                : loadNotifications(user.getId());
        verifyDatabase();
        return new DashboardSnapshot(peopleInside, pendingRequests, openIncidents, visitsToday,
                recentVisits, activity);
    }

    private List<DashboardSnapshot.ActivityItem> loadAuditActivity() {
        return auditoriaRepository.findTop10ByOrderByFechaHoraDesc().stream()
                .map(this::toActivity).toList();
    }

    private List<DashboardSnapshot.ActivityItem> loadNotifications(Integer userId) {
        return notificacionRepository.findTop10ByDestinatarioUsuarioIdOrderByFechaCreacionDesc(userId).stream()
                .map(notification -> new DashboardSnapshot.ActivityItem(
                        notification.getFechaCreacion() == null ? "" :
                                notification.getFechaCreacion().format(TIMESTAMP_FORMAT),
                        notification.getTipo(), notification.getMensaje(), notification.getTipo()))
                .toList();
    }

    private DashboardSnapshot.ActivityItem toActivity(BitacoraAuditoria audit) {
        return new DashboardSnapshot.ActivityItem(
                audit.getFechaHora() == null ? "" : audit.getFechaHora().format(TIMESTAMP_FORMAT),
                audit.getAccionRealizada(),
                (audit.getTablaAfectada() == null ? "" : audit.getTablaAfectada())
                        + (audit.getUsuario() == null ? " · Sistema" : " · " + audit.getUsuario().getNombre()),
                audit.getAccionRealizada());
    }

    private void verifyDatabase() {
        try (Connection connection = database.getConnection()) {
            if (!connection.isValid(3)) {
                throw new SQLException("La conexión con la base de datos no está disponible.");
            }
        } catch (SQLException failure) {
            throw new IllegalStateException("No se pudo verificar la conexión a MySQL.", failure);
        }
    }

    private Usuario requireCurrentUser() {
        Usuario user = sessions.getCurrentUser();
        if (user == null) {
            throw new SecurityException("Se requiere una sesión activa.");
        }
        return user;
    }

    private boolean isCompanyUser(Usuario user) {
        return user.getRol() != null
                && "Funcionario de Empresa".equalsIgnoreCase(user.getRol().getNombreRol());
    }
}
