package com.acme.sica;

import com.acme.sica.dto.AccesoRequestDto;
import com.acme.sica.dto.TipoFlujoAcceso;
import com.acme.sica.model.*;
import com.acme.sica.pattern.factory.VisitaFactory;
import com.acme.sica.repository.VisitaEstadoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SicaFactoryAndStrategyTests {

    @Mock
    private VisitaEstadoRepository visitaEstadoRepository;

    @InjectMocks
    private VisitaFactory visitaFactory;

    private Persona personaTrabajador;
    private Persona personaInvitado;
    private VisitaEstado estadoAprobado;
    private VisitaEstado estadoPendiente;
    private VisitaEstado estadoOlvido;
    private VisitaEstado estadoDentro;

    @BeforeEach
    void setUp() {
        personaTrabajador = Persona.builder()
                .id(1)
                .nombre("Pedro Martínez")
                .documentoIdentidad("10102020")
                .tipoPersona("Trabajador")
                .build();

        personaInvitado = Persona.builder()
                .id(2)
                .nombre("Mariana López")
                .documentoIdentidad("20203030")
                .tipoPersona("Invitado")
                .build();

        estadoAprobado = VisitaEstado.builder().id(3).nombreEstado("Aprobado").build();
        estadoPendiente = VisitaEstado.builder().id(6).nombreEstado("Pendiente de Aprobacion").build();
        estadoOlvido = VisitaEstado.builder().id(7).nombreEstado("Pendiente de Aprobacion por Olvido").build();
        estadoDentro = VisitaEstado.builder().id(1).nombreEstado("Dentro").build();
    }

    @Test
    @DisplayName("Factory: Flujo 1 - Invitado Pre-registrado genera código y estado Aprobado")
    void testFactoryInvitadoPreRegistrado() {
        when(visitaEstadoRepository.findByNombreEstado("Aprobado")).thenReturn(Optional.of(estadoAprobado));

        Visita visita = visitaFactory.crearVisita(
                TipoFlujoAcceso.INVITADO_PRE_REGISTRADO,
                personaInvitado,
                null,
                null,
                null,
                null,
                null,
                new AccesoRequestDto()
        );

        assertNotNull(visita);
        assertEquals("Aprobado", visita.getEstadoVisita().getNombreEstado());
        assertFalse(visita.getEsPaseTemporal());
        assertTrue(visita.getCodigoPase().startsWith("PRE-"));
    }

    @Test
    @DisplayName("Factory: Flujo 2 - Invitado No Anunciado genera estado Pendiente de Aprobacion")
    void testFactoryInvitadoNoAnunciado() {
        when(visitaEstadoRepository.findByNombreEstado("Pendiente de Aprobacion")).thenReturn(Optional.of(estadoPendiente));

        Visita visita = visitaFactory.crearVisita(
                TipoFlujoAcceso.INVITADO_NO_ANUNCIADO,
                personaInvitado,
                null,
                null,
                null,
                null,
                null,
                new AccesoRequestDto()
        );

        assertNotNull(visita);
        assertEquals("Pendiente de Aprobacion", visita.getEstadoVisita().getNombreEstado());
        assertFalse(visita.getEsPaseTemporal());
        assertTrue(visita.getCodigoPase().startsWith("NOA-"));
    }

    @Test
    @DisplayName("Factory: Flujo 3 - Carnet Olvidado marca es_pase_temporal = TRUE y código TMP")
    void testFactoryCarnetOlvidado() {
        when(visitaEstadoRepository.findByNombreEstado("Pendiente de Aprobacion por Olvido")).thenReturn(Optional.of(estadoOlvido));

        Visita visita = visitaFactory.crearVisita(
                TipoFlujoAcceso.CARNET_OLVIDADO,
                personaTrabajador,
                null,
                null,
                null,
                null,
                null,
                new AccesoRequestDto()
        );

        assertNotNull(visita);
        assertEquals("Pendiente de Aprobacion por Olvido", visita.getEstadoVisita().getNombreEstado());
        assertTrue(visita.getEsPaseTemporal(), "Debe marcar bandera de pase temporal");
        assertTrue(visita.getCodigoPase().startsWith("TMP-"));
    }

    @Test
    @DisplayName("Factory: Flujo 4 - Salida Olvidada genera visita en estado Dentro y código REG")
    void testFactorySalidaOlvidada() {
        when(visitaEstadoRepository.findByNombreEstado("Dentro")).thenReturn(Optional.of(estadoDentro));

        Visita visita = visitaFactory.crearVisita(
                TipoFlujoAcceso.SALIDA_OLVIDADA,
                personaTrabajador,
                null,
                null,
                null,
                null,
                null,
                new AccesoRequestDto()
        );

        assertNotNull(visita);
        assertEquals("Dentro", visita.getEstadoVisita().getNombreEstado());
        assertNotNull(visita.getFechaEntrada());
        assertTrue(visita.getCodigoPase().startsWith("REG-"));
    }
}
