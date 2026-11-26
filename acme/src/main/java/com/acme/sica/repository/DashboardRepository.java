package com.acme.sica.repository;

import com.acme.sica.dto.DashboardSnapshot;

import java.sql.Timestamp;
import java.util.List;

public final class DashboardRepository extends JdbcRepositorySupport {
    public long countPeopleInside() {
        return queryOne("SELECT COUNT(*) AS total FROM vw_personas_actualmente_dentro",
                result -> result.getLong("total")).orElse(0L);
    }

    public long countPendingRequests(String hostName, String companyName, boolean companyScoped) {
        String scope = companyScoped
                ? " AND (anfitrion = ? OR (? IS NOT NULL AND empresa_destino = ?))"
                : "";
        Object[] parameters = companyScoped
                ? new Object[]{hostName, companyName, companyName}
                : new Object[0];
        return queryOne("SELECT COUNT(*) AS total FROM vw_solicitudes_pendientes WHERE 1 = 1"
                        + scope,
                result -> result.getLong("total"), parameters).orElse(0L);
    }

    public long countOpenIncidents() {
        return queryOne("""
                SELECT COUNT(*) AS total
                FROM incidentes
                WHERE estado IN ('Abierto', 'En Investigacion')
                """, result -> result.getLong("total")).orElse(0L);
    }

    public long countVisitsToday(Integer userId, Integer companyId, boolean companyScoped) {
        String scope = companyScoped
                ? " AND (v.anfitrion_id = ? OR v.empresa_destino_id = ?)"
                : "";
        Object[] parameters = companyScoped
                ? new Object[]{userId, companyId}
                : new Object[0];
        return queryOne("""
                SELECT COUNT(*) AS total
                FROM visitas v
                WHERE v.created_at >= CURRENT_DATE
                  AND v.created_at < CURRENT_DATE + INTERVAL 1 DAY
                """ + scope, result -> result.getLong("total"), parameters)
                .orElse(0L);
    }

    public List<DashboardSnapshot.RecentVisit> findRecentVisits(
            Integer userId, Integer companyId, boolean companyScoped) {
        String scope = companyScoped
                ? " AND (v.anfitrion_id = ? OR v.empresa_destino_id = ?)"
                : "";
        String sql = """
                SELECT p.nombre AS persona,
                       COALESCE(e.nombre, 'Sin empresa') AS empresa,
                       ve.nombre_estado AS estado,
                       v.fecha_entrada
                FROM visitas v
                JOIN personas p ON p.id = v.persona_id
                JOIN visita_estados ve ON ve.id = v.estado_visita_id
                LEFT JOIN empresas e ON e.id = v.empresa_destino_id
                WHERE 1 = 1
                """ + scope + """
                ORDER BY COALESCE(v.fecha_entrada, v.created_at) DESC, v.id DESC
                LIMIT 10
                """;
        Object[] parameters = companyScoped ? new Object[]{userId, companyId} : new Object[0];
        return query(sql, result -> new DashboardSnapshot.RecentVisit(
                result.getString("persona"),
                result.getString("empresa"),
                result.getString("estado"),
                formatTimestamp(result.getTimestamp("fecha_entrada"))), parameters);
    }

    private static String formatTimestamp(Timestamp timestamp) {
        return timestamp == null ? "Sin check-in" : timestamp.toLocalDateTime()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
}
