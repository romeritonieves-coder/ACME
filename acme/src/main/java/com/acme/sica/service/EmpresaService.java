package com.acme.sica.service;

import com.acme.sica.model.Empresa;
import com.acme.sica.repository.EmpresaRepository;

import java.util.List;

public class EmpresaService {
    private final EmpresaRepository empresaRepository;
    private final AuditedOperationProxy operations;

    public EmpresaService(EmpresaRepository empresaRepository, AuditedOperationProxy operations) {
        this.empresaRepository = empresaRepository;
        this.operations = operations;
    }

    public List<Empresa> listar() {
        return operations.execute("USUARIOS_VER", "CONSULTAR_EMPRESAS", "empresas", null,
                empresaRepository::findAll);
    }

    public Empresa crear(Empresa empresa) {
        return operations.execute("USUARIOS_CREAR", "CREAR_EMPRESA", "empresas", null,
                () -> empresaRepository.save(empresa));
    }

    public Empresa actualizar(Empresa empresa) {
        return operations.execute("USUARIOS_EDITAR", "ACTUALIZAR_EMPRESA", "empresas", empresa.getId(),
                () -> empresaRepository.save(empresa));
    }

    public void eliminar(Integer empresaId) {
        operations.execute("USUARIOS_ELIMINAR", "ELIMINAR_EMPRESA", "empresas", empresaId,
                () -> empresaRepository.deleteById(empresaId));
    }
}
