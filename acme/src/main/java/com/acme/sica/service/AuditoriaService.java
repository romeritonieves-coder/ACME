package com.acme.sica.service;

import com.acme.sica.model.BitacoraAuditoria;
import com.acme.sica.model.Usuario;
import com.acme.sica.repository.BitacoraAuditoriaRepository;
import com.acme.sica.security.SessionManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class AuditoriaService {
    private final BitacoraAuditoriaRepository bitacoraRepository;

    public AuditoriaService() {
        this(new BitacoraAuditoriaRepository());
    }

    public AuditoriaService(BitacoraAuditoriaRepository bitacoraRepository) {
        this.bitacoraRepository = bitacoraRepository;
    }

    public void registrarEvento(Usuario usuario, String accion, String tabla, Integer registroId,
                                String ipOrigen, Map<String, Object> detalles) {
        BitacoraAuditoria audit = BitacoraAuditoria.builder()
                .usuario(usuario)
                .fechaHora(LocalDateTime.now())
                .accionRealizada(accion)
                .tablaAfectada(tabla)
                .registroIdAfectado(registroId)
                .ipOrigen(ipOrigen == null ? "127.0.0.1" : ipOrigen)
                .detalles(toJson(detalles))
                .build();
        bitacoraRepository.save(audit);
    }

    public List<BitacoraAuditoria> obtenerUltimosEventos() {
        SessionManager.getInstance().requirePermission("AUDITORIA_VER");
        return bitacoraRepository.findTop100ByOrderByFechaHoraDesc();
    }

    private static String toJson(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return "{}";
        }
        return values.entrySet().stream()
                .map(entry -> quote(entry.getKey()) + ":" + quote(String.valueOf(entry.getValue())))
                .collect(java.util.stream.Collectors.joining(",", "{", "}"));
    }

    private static String quote(String value) {
        StringBuilder json = new StringBuilder("\"");
        for (char character : value.toCharArray()) {
            switch (character) {
                case '"' -> json.append("\\\"");
                case '\\' -> json.append("\\\\");
                case '\b' -> json.append("\\b");
                case '\f' -> json.append("\\f");
                case '\n' -> json.append("\\n");
                case '\r' -> json.append("\\r");
                case '\t' -> json.append("\\t");
                default -> {
                    if (character < 0x20) {
                        json.append(String.format("\\u%04x", (int) character));
                    } else {
                        json.append(character);
                    }
                }
            }
        }
        return json.append('"').toString();
    }
}
