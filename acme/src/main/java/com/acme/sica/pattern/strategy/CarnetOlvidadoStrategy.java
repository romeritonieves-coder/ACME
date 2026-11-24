package com.acme.sica.pattern.strategy;

import com.acme.sica.dto.AccesoRequestDto;
import com.acme.sica.dto.AccesoResponseDto;
import com.acme.sica.dto.TipoFlujoAcceso;
import com.acme.sica.model.*;
import com.acme.sica.pattern.factory.VisitaFactory;
import com.acme.sica.pattern.observer.AccesoEvento;
import com.acme.sica.pattern.observer.AccesoSubject;
import com.acme.sica.repository.UsuarioRepository;
import com.acme.sica.repository.VisitaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * FLUJO 3: Trabajador con carnet olvidado
 * Genera un pase temporal (es_pase_temporal = TRUE) en estado 'Pendiente de Aprobacion por Olvido'
 * y despacha notificación inmediata al funcionario/supervisor para validación y autorización.
 */
@Component
@RequiredArgsConstructor
public class CarnetOlvidadoStrategy implements AccesoStrategy {

    private final VisitaRepository visitaRepository;
    private final VisitaFactory visitaFactory;
    private final AccesoSubject accesoSubject;
    private final UsuarioRepository usuarioRepository;

    @Override
    public boolean aplicaPara(TipoFlujoAcceso tipoFlujo) {
        return TipoFlujoAcceso.CARNET_OLVIDADO.equals(tipoFlujo);
    }

    @Override
    @Transactional
    public AccesoResponseDto procesar(AccesoRequestDto request, Persona persona, Usuario usuarioActual, PuntoAcceso puntoAcceso) {
        // Validar prohibición de ingreso
        if (persona.getEstadoAcceso() != null && "Con Prohibicion de Ingreso".equalsIgnoreCase(persona.getEstadoAcceso().getNombreEstado())) {
            return AccesoResponseDto.builder()
                    .exito(false)
                    .mensaje("ACCESO DENEGADO: El trabajador tiene prohibición de ingreso: " + persona.getMotivoRestriccion())
                    .personaNombre(persona.getNombre())
                    .documentoIdentidad(persona.getDocumentoIdentidad())
                    .urlFoto(persona.getUrlFoto())
                    .build();
        }

        // Buscar anfitrión (funcionario de la misma empresa o supervisor)
        Usuario anfitrion = null;
        if (persona.getEmpresa() != null) {
            anfitrion = usuarioRepository.findByEmpresaId(persona.getEmpresa().getId()).stream()
                    .filter(u -> u.getRol() != null && "Funcionario de Empresa".equalsIgnoreCase(u.getRol().getNombreRol()))
                    .findFirst()
                    .orElse(null);
        }
        if (anfitrion == null) {
            // Asignar primer supervisor de seguridad
            anfitrion = usuarioRepository.findByRolId(2).stream().findFirst().orElse(null);
        }

        Visita visita = visitaFactory.crearVisita(
                TipoFlujoAcceso.CARNET_OLVIDADO,
                persona,
                anfitrion,
                usuarioActual,
                puntoAcceso,
                persona.getEmpresa(),
                null,
                request
        );

        visita = visitaRepository.save(visita);

        // Notificar emisión de pase temporal vía WebSocket (Observer)
        String mensaje = String.format("Pase Temporal Requerido: El trabajador %s (Doc: %s) olvidó su carnet. Requiere aprobación inmediata.",
                persona.getNombre(), persona.getDocumentoIdentidad());

        accesoSubject.notificarObservadores(AccesoEvento.builder()
                .tipo("PASE_TEMPORAL")
                .visitaId(visita.getId())
                .visita(visita)
                .persona(persona)
                .destinatarioUsuarioId(anfitrion != null ? anfitrion.getId() : null)
                .mensaje(mensaje)
                .estadoVisita("Pendiente de Aprobacion por Olvido")
                .build());

        return AccesoResponseDto.builder()
                .exito(true)
                .mensaje("Pase temporal generado. Notificación enviada al supervisor/funcionario para aprobación en tiempo real.")
                .visitaId(visita.getId())
                .estadoVisita("Pendiente de Aprobacion por Olvido")
                .codigoPase(visita.getCodigoPase())
                .requiereAprobacion(true)
                .esPaseTemporal(true)
                .personaNombre(persona.getNombre())
                .documentoIdentidad(persona.getDocumentoIdentidad())
                .tipoPersona(persona.getTipoPersona())
                .urlFoto(persona.getUrlFoto())
                .anfitrionNombre(anfitrion != null ? anfitrion.getNombre() : "Supervisor de Seguridad")
                .empresaNombre(persona.getEmpresa() != null ? persona.getEmpresa().getNombre() : "Complejo Acme")
                .build();
    }
}
