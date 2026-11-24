package com.acme.sica.service;

import com.acme.sica.dto.PersonaDto;
import com.acme.sica.model.Empresa;
import com.acme.sica.model.Persona;
import com.acme.sica.model.PersonaEstadoAcceso;
import com.acme.sica.model.Usuario;
import com.acme.sica.pattern.proxy.Auditable;
import com.acme.sica.repository.EmpresaRepository;
import com.acme.sica.repository.PersonaEstadoAccesoRepository;
import com.acme.sica.repository.PersonaRepository;
import com.acme.sica.security.RequirePermiso;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PersonaService {

    private final PersonaRepository personaRepository;
    private final PersonaEstadoAccesoRepository estadoAccesoRepository;
    private final EmpresaRepository empresaRepository;

    @Transactional(readOnly = true)
    public Optional<PersonaDto> buscarPorDocumento(String documento) {
        return personaRepository.findByDocumentoIdentidad(documento)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public List<PersonaDto> buscarPersonas(String query) {
        List<Persona> personas = (query == null || query.isBlank())
                ? personaRepository.findAll()
                : personaRepository.buscarPorNombreODocumento(query);

        // Uso intensivo de Java Streams API para transformar y mapear entidades
        return personas.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    @RequirePermiso("USUARIOS_EDITAR")
    @Auditable(accion = "CAMBIO_ESTADO_ACCESO", tabla = "personas")
    public PersonaDto cambiarEstadoAcceso(Integer personaId, String nuevoEstado, String motivoRestriccion, Usuario usuarioOperador) {
        Persona persona = personaRepository.findById(personaId)
                .orElseThrow(() -> new IllegalArgumentException("Persona no encontrada con ID: " + personaId));

        PersonaEstadoAcceso estado = estadoAccesoRepository.findByNombreEstado(nuevoEstado)
                .orElseThrow(() -> new IllegalArgumentException("Estado no válido: " + nuevoEstado));

        persona.setEstadoAcceso(estado);
        persona.setMotivoRestriccion(motivoRestriccion);
        persona = personaRepository.save(persona);

        return mapToDto(persona);
    }

    @Transactional
    @RequirePermiso("USUARIOS_CREAR")
    @Auditable(accion = "CREAR_PERSONA", tabla = "personas")
    public PersonaDto registrarPersona(PersonaDto dto, Usuario usuarioOperador) {
        PersonaEstadoAcceso estadoActivo = estadoAccesoRepository.findByNombreEstado("Activo")
                .orElseThrow(() -> new IllegalStateException("Estado 'Activo' no encontrado"));

        Empresa empresa = null;
        if (dto.getEmpresaId() != null) {
            empresa = empresaRepository.findById(dto.getEmpresaId()).orElse(null);
        }

        Persona persona = Persona.builder()
                .nombre(dto.getNombre())
                .documentoIdentidad(dto.getDocumentoIdentidad())
                .empresa(empresa)
                .tipoPersona(dto.getTipoPersona() != null ? dto.getTipoPersona() : "Invitado")
                .estadoAcceso(estadoActivo)
                .telefono(dto.getTelefono())
                .urlFoto(dto.getUrlFoto() != null ? dto.getUrlFoto() : "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150")
                .build();

        persona = personaRepository.save(persona);
        return mapToDto(persona);
    }

    public PersonaDto mapToDto(Persona p) {
        return PersonaDto.builder()
                .id(p.getId())
                .nombre(p.getNombre())
                .documentoIdentidad(p.getDocumentoIdentidad())
                .tipoPersona(p.getTipoPersona())
                .empresaId(p.getEmpresa() != null ? p.getEmpresa().getId() : null)
                .empresaNombre(p.getEmpresa() != null ? p.getEmpresa().getNombre() : "Sin empresa asignada")
                .estadoAccesoId(p.getEstadoAcceso() != null ? p.getEstadoAcceso().getId() : null)
                .estadoAccesoNombre(p.getEstadoAcceso() != null ? p.getEstadoAcceso().getNombreEstado() : "Desconocido")
                .motivoRestriccion(p.getMotivoRestriccion())
                .telefono(p.getTelefono())
                .urlFoto(p.getUrlFoto())
                .activo(p.getEstadoAcceso() != null && "Activo".equalsIgnoreCase(p.getEstadoAcceso().getNombreEstado()))
                .build();
    }
}
