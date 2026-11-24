package com.acme.sica.service;

import com.acme.sica.model.BitacoraAuditoria;
import com.acme.sica.model.Usuario;
import com.acme.sica.repository.BitacoraAuditoriaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditoriaService {

    private final BitacoraAuditoriaRepository bitacoraRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void registrarEvento(Usuario usuario,
                                String accion,
                                String tabla,
                                Integer registroId,
                                String ipOrigen,
                                Map<String, Object> detalles) {
        String detallesJson = "{}";
        if (detalles != null) {
            try {
                detallesJson = objectMapper.writeValueAsString(detalles);
            } catch (Exception ignored) {}
        }

        BitacoraAuditoria audit = BitacoraAuditoria.builder()
                .usuario(usuario)
                .fechaHora(LocalDateTime.now())
                .accionRealizada(accion)
                .tablaAfectada(tabla)
                .registroIdAfectado(registroId)
                .ipOrigen(ipOrigen != null ? ipOrigen : "127.0.0.1")
                .detalles(detallesJson)
                .build();

        bitacoraRepository.save(audit);
    }

    @Transactional(readOnly = true)
    public List<BitacoraAuditoria> obtenerUltimosEventos() {
        return bitacoraRepository.findTop100ByOrderByFechaHoraDesc();
    }
}
