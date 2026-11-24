package com.acme.sica.pattern.strategy;

import com.acme.sica.dto.AccesoRequestDto;
import com.acme.sica.dto.AccesoResponseDto;
import com.acme.sica.dto.TipoFlujoAcceso;
import com.acme.sica.model.*;
import com.acme.sica.pattern.factory.VisitaFactory;
import com.acme.sica.pattern.observer.AccesoEvento;
import com.acme.sica.pattern.observer.AccesoSubject;
import com.acme.sica.repository.EmpresaRepository;
import com.acme.sica.repository.UsuarioRepository;
import com.acme.sica.repository.VisitaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * FLUJO 2: Invitado no anunciado
 * Registra la solicitud en estado 'Pendiente de Aprobacion' y notifica en tiempo real
 * al Funcionario de Empresa vía WebSocket. La pantalla del Guarda se actualiza de inmediato.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InvitadoNoAnunciadoStrategy implements AccesoStrategy {

    private final VisitaRepository visitaRepository;
    private final VisitaFactory visitaFactory;
    private final AccesoSubject accesoSubject;
    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;

    @Override
    public boolean aplicaPara(TipoFlujoAcceso tipoFlujo) {
        return TipoFlujoAcceso.INVITADO_NO_ANUNCIADO.equals(tipoFlujo);
    }

    @Override
    @Transactional
    public AccesoResponseDto procesar(AccesoRequestDto request, Persona persona, Usuario usuarioActual, PuntoAcceso puntoAcceso) {
        // Validación de restricción
        if (persona.getEstadoAcceso() != null && "Con Prohibicion de Ingreso".equalsIgnoreCase(persona.getEstadoAcceso().getNombreEstado())) {
            return AccesoResponseDto.builder()
                    .exito(false)
                    .mensaje("ACCESO DENEGADO: La persona tiene prohibición de ingreso: " + persona.getMotivoRestriccion())
                    .personaNombre(persona.getNombre())
                    .documentoIdentidad(persona.getDocumentoIdentidad())
                    .urlFoto(persona.getUrlFoto())
                    .build();
        }

        // Determinar anfitrión
        Usuario anfitrion = null;
        if (request.getAnfitrionId() != null) {
            anfitrion = usuarioRepository.findById(request.getAnfitrionId()).orElse(null);
        }
        if (anfitrion == null && request.getEmpresaDestinoId() != null) {
            // Asignar primer funcionario disponible de esa empresa
            anfitrion = usuarioRepository.findByEmpresaId(request.getEmpresaDestinoId()).stream()
                    .filter(u -> u.getRol() != null && "Funcionario de Empresa".equalsIgnoreCase(u.getRol().getNombreRol()))
                    .findFirst()
                    .orElse(null);
        }

        Empresa empresaDestino = null;
        if (request.getEmpresaDestinoId() != null) {
            empresaDestino = empresaRepository.findById(request.getEmpresaDestinoId()).orElse(null);
        }

        Visita visita = visitaFactory.crearVisita(
                TipoFlujoAcceso.INVITADO_NO_ANUNCIADO,
                persona,
                anfitrion,
                usuarioActual,
                puntoAcceso,
                empresaDestino,
                null,
                request
        );

        visita = visitaRepository.save(visita);

        // Notificar al Funcionario de Empresa vía WebSocket en tiempo real (Observer)
        String mensaje = String.format("Solicitud de ingreso no anunciada: %s (Doc: %s) desea ingresar para: %s",
                persona.getNombre(), persona.getDocumentoIdentidad(),
                request.getMotivoVisita() != null ? request.getMotivoVisita() : "Reunión");

        accesoSubject.notificarObservadores(AccesoEvento.builder()
                .tipo("SOLICITUD_INGRESO")
                .visitaId(visita.getId())
                .visita(visita)
                .persona(persona)
                .destinatarioUsuarioId(anfitrion != null ? anfitrion.getId() : null)
                .mensaje(mensaje)
                .estadoVisita("Pendiente de Aprobacion")
                .build());

        return AccesoResponseDto.builder()
                .exito(true)
                .mensaje("Solicitud enviada al funcionario. Esperando aprobación en tiempo real...")
                .visitaId(visita.getId())
                .estadoVisita("Pendiente de Aprobacion")
                .codigoPase(visita.getCodigoPase())
                .requiereAprobacion(true)
                .esPaseTemporal(false)
                .personaNombre(persona.getNombre())
                .documentoIdentidad(persona.getDocumentoIdentidad())
                .tipoPersona(persona.getTipoPersona())
                .urlFoto(persona.getUrlFoto())
                .anfitrionNombre(anfitrion != null ? anfitrion.getNombre() : "Pendiente de Asignación")
                .empresaNombre(empresaDestino != null ? empresaDestino.getNombre() : "Complejo Acme")
                .build();
    }
}
