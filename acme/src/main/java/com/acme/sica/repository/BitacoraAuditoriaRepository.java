package com.acme.sica.repository;

import com.acme.sica.model.BitacoraAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BitacoraAuditoriaRepository extends JpaRepository<BitacoraAuditoria, Long> {
    List<BitacoraAuditoria> findAllByOrderByFechaHoraDesc();
    List<BitacoraAuditoria> findTop100ByOrderByFechaHoraDesc();
    List<BitacoraAuditoria> findByUsuarioIdOrderByFechaHoraDesc(Integer usuarioId);
}
