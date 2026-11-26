package com.acme.sica.controller;

import com.acme.sica.config.SicaApplicationContext;
import com.acme.sica.model.Empresa;

import java.util.List;

public final class EmpresasController {
    private final SicaApplicationContext context;

    public EmpresasController(SicaApplicationContext context) {
        this.context = context;
    }

    public List<Empresa> listar() {
        return context.empresaService().listar();
    }

    public Empresa guardar(Empresa empresa) {
        return empresa.getId() == null
                ? context.empresaService().crear(empresa)
                : context.empresaService().actualizar(empresa);
    }

    public void eliminar(Integer id) {
        context.empresaService().eliminar(id);
    }
}
