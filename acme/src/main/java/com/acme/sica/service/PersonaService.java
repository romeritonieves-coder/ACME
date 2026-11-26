package com.acme.sica.service;

import com.acme.sica.dto.PersonaDto;
import com.acme.sica.model.Empresa;
import com.acme.sica.model.Persona;
import com.acme.sica.model.PersonaEstadoAcceso;
import com.acme.sica.model.Usuario;
import com.acme.sica.repository.EmpresaRepository;
import com.acme.sica.repository.PersonaEstadoAccesoRepository;
import com.acme.sica.repository.PersonaRepository;
import com.acme.sica.security.SessionManager;

import java.util.List;
import java.util.Optional;

public class PersonaService {
    private final PersonaRepository personaRepository;
    private final PersonaEstadoAccesoRepository estadoAccesoRepository;
    private final EmpresaRepository empresaRepository;
    private final AuditedOperationProxy operations;

    public PersonaService(PersonaRepository personaRepository,
                          PersonaEstadoAccesoRepository estadoAccesoRepository,
                          EmpresaRepository empresaRepository, AuditedOperationProxy operations) {
        this.personaRepository = personaRepository;
        this.estadoAccesoRepository = estadoAccesoRepository;
        this.empresaRepository = empresaRepository;
        this.operations = operations;
    }

    public Optional<PersonaDto> buscarPorDocumento(String documento) {
        ensureCanSearchPeople();
        return personaRepository.findByDocumentoIdentidad(documento).map(this::mapToDto);
    }

    public List<PersonaDto> buscarPersonas(String query) {
        ensureCanSearchPeople();
        List<Persona> people = query == null || query.isBlank()
                ? personaRepository.findAll() : personaRepository.buscarPorNombreODocumento(query);
        return people.stream().map(this::mapToDto).toList();
    }

    public PersonaDto cambiarEstadoAcceso(Integer personaId, String nuevoEstado,
                                          String motivoRestriccion, Usuario usuarioOperador) {
        return operations.executeAs(usuarioOperador, "USUARIOS_EDITAR", "CAMBIO_ESTADO_ACCESO", "personas", personaId, () -> {
            Persona person = personaRepository.findById(personaId)
                    .orElseThrow(() -> new IllegalArgumentException("Persona no encontrada: " + personaId));
            PersonaEstadoAcceso state = estadoAccesoRepository.findByNombreEstado(nuevoEstado)
                    .orElseThrow(() -> new IllegalArgumentException("Estado de acceso no válido: " + nuevoEstado));
            person.setEstadoAcceso(state);
            person.setMotivoRestriccion(motivoRestriccion);
            return mapToDto(personaRepository.save(person));
        });
    }

    public PersonaDto registrarPersona(PersonaDto dto, Usuario usuarioOperador) {
        return operations.executeAs(usuarioOperador, "USUARIOS_CREAR", "CREAR_PERSONA", "personas", null, () -> {
            if (dto == null || dto.getNombre() == null || dto.getNombre().isBlank()
                    || dto.getDocumentoIdentidad() == null || dto.getDocumentoIdentidad().isBlank()) {
                throw new IllegalArgumentException("Nombre y documento de identidad son obligatorios.");
            }
            PersonaEstadoAcceso active = estadoAccesoRepository.findByNombreEstado("Activo")
                    .orElseThrow(() -> new IllegalStateException("Estado 'Activo' no existe en la BD."));
            Empresa company = dto.getEmpresaId() == null ? null
                    : empresaRepository.findById(dto.getEmpresaId())
                    .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada."));
            Persona person = Persona.builder().nombre(dto.getNombre())
                    .documentoIdentidad(dto.getDocumentoIdentidad()).empresa(company)
                    .tipoPersona(dto.getTipoPersona() == null ? "Invitado" : dto.getTipoPersona())
                    .estadoAcceso(active).telefono(dto.getTelefono())
                    .urlFoto(dto.getUrlFoto()).build();
            return mapToDto(personaRepository.save(person));
        });
    }

    public PersonaDto actualizarPersona(PersonaDto dto, Usuario usuarioOperador) {
        return operations.executeAs(usuarioOperador, "USUARIOS_EDITAR", "ACTUALIZAR_PERSONA", "personas",
                dto == null ? null : dto.getId(), () -> {
            if (dto == null || dto.getId() == null) {
                throw new IllegalArgumentException("El identificador de persona es obligatorio.");
            }
            Persona person = personaRepository.findById(dto.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Persona no encontrada: " + dto.getId()));
            Empresa company = dto.getEmpresaId() == null ? null
                    : empresaRepository.findById(dto.getEmpresaId())
                    .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada."));
            person.setNombre(dto.getNombre());
            person.setDocumentoIdentidad(dto.getDocumentoIdentidad());
            person.setEmpresa(company);
            person.setTipoPersona(dto.getTipoPersona());
            person.setTelefono(dto.getTelefono());
            person.setUrlFoto(dto.getUrlFoto());
            return mapToDto(personaRepository.save(person));
        });
    }

    public void eliminarPersona(Integer personaId, Usuario usuarioOperador) {
        operations.executeAs(usuarioOperador, "USUARIOS_ELIMINAR", "ELIMINAR_PERSONA", "personas", personaId,
                () -> personaRepository.deleteById(personaId));
    }

    public PersonaDto mapToDto(Persona person) {
        return PersonaDto.builder().id(person.getId()).nombre(person.getNombre())
                .documentoIdentidad(person.getDocumentoIdentidad()).tipoPersona(person.getTipoPersona())
                .empresaId(person.getEmpresa() == null ? null : person.getEmpresa().getId())
                .empresaNombre(person.getEmpresa() == null ? "Sin empresa asignada" : person.getEmpresa().getNombre())
                .estadoAccesoId(person.getEstadoAcceso() == null ? null : person.getEstadoAcceso().getId())
                .estadoAccesoNombre(person.getEstadoAcceso() == null ? "Desconocido"
                        : person.getEstadoAcceso().getNombreEstado())
                .motivoRestriccion(person.getMotivoRestriccion()).telefono(person.getTelefono())
                .urlFoto(person.getUrlFoto())
                .activo(person.getEstadoAcceso() != null
                        && "Activo".equalsIgnoreCase(person.getEstadoAcceso().getNombreEstado())).build();
    }

    private void ensureCanSearchPeople() {
        SessionManager sessions = SessionManager.getInstance();
        if (!sessions.hasPermission("USUARIOS_VER") && !sessions.hasPermission("ACCESO_CHECKIN")
                && !sessions.hasPermission("ACCESO_SOLICITAR")) {
            throw new SecurityException("No tiene permiso para buscar personas.");
        }
    }
}
