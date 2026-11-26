package com.acme.sica.repository;

import com.acme.sica.model.VwPersonasActualmenteDentro;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

public class VwPersonasActualmenteDentroRepository extends JdbcRepositorySupport {
    public List<VwPersonasActualmenteDentro> findAll() {
        return query("""
                SELECT visita_id, documento_identidad, persona_nombre, tipo_persona, empresa_destino,
                       punto_entrada, fecha_entrada, vehiculo_placa
                FROM vw_personas_actualmente_dentro ORDER BY fecha_entrada DESC
                """, this::map);
    }

    public long countByTipoPersona(String tipoPersona) {
        return queryOne("SELECT COUNT(*) AS total FROM vw_personas_actualmente_dentro WHERE tipo_persona = ?",
                rs -> rs.getLong("total"), tipoPersona).orElse(0L);
    }

    private VwPersonasActualmenteDentro map(ResultSet rs) throws SQLException {
        Timestamp entered = rs.getTimestamp("fecha_entrada");
        return new VwPersonasActualmenteDentro(rs.getInt("visita_id"), rs.getString("documento_identidad"),
                rs.getString("persona_nombre"), rs.getString("tipo_persona"), rs.getString("empresa_destino"),
                rs.getString("punto_entrada"), entered == null ? null : entered.toLocalDateTime(),
                rs.getString("vehiculo_placa"));
    }
}
