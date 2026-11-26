package com.acme.sica.controller;

import com.acme.sica.config.SicaApplicationContext;
import com.acme.sica.dto.PersonaDto;
import com.acme.sica.model.Usuario;
import com.acme.sica.security.SessionManager;

import java.util.List;

public final class PersonasController {
    private final SicaApplicationContext context;

    public PersonasController(SicaApplicationContext context) {
        this.context = context;
    }

    public List<PersonaDto> buscar(String filtro) {
        return context.personaService().buscarPersonas(filtro);
    }

    public PersonaDto guardar(PersonaDto persona) {
        Usuario user = currentUser();
        return persona.getId() == null
                ? context.personaService().registrarPersona(persona, user)
                : context.personaService().actualizarPersona(persona, user);
    }

    public void eliminar(Integer id) {
        context.personaService().eliminarPersona(id, currentUser());
    }

    private Usuario currentUser() {
        Usuario user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            throw new SecurityException("No hay una sesión activa.");
        }
        return user;
    }
}
