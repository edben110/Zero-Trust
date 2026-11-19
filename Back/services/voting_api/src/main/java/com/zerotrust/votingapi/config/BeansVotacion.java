package com.zerotrust.votingapi.config;

import com.zerotrust.votingapi.domain.ElectionId;
import com.zerotrust.votingapi.port.ControlRegionVotante;
import com.zerotrust.votingapi.port.DerivadorVoterId;
import com.zerotrust.votingapi.port.EjecutorTransaccional;
import com.zerotrust.votingapi.port.PublicadorElegibles;
import com.zerotrust.votingapi.port.PublicadorVotos;
import com.zerotrust.votingapi.port.RegistroIncidentesFraude;
import com.zerotrust.votingapi.port.RegistroParticipacion;
import com.zerotrust.votingapi.port.RegistroVotoAceptado;
import com.zerotrust.votingapi.port.RepositorioPadron;
import com.zerotrust.votingapi.port.ValidadorVotante;
import com.zerotrust.votingapi.service.ServicioIdentificacion;
import com.zerotrust.votingapi.service.ServicioPadron;
import com.zerotrust.votingapi.service.ServicioVotacion;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.UUID;

@Configuration
public class BeansVotacion {

    @Bean
    ServicioIdentificacion servicioIdentificacion(DerivadorVoterId derivador, ValidadorVotante validador,
                                                  ElectionId eleccion) {
        return new ServicioIdentificacion(derivador, validador, eleccion);
    }

    // Las regiones válidas son las del catálogo vigente (el que edita el panel de administración).
    @Bean
    ServicioPadron servicioPadron(DerivadorVoterId derivador, RepositorioPadron repositorio,
                                  PublicadorElegibles publicador, EjecutorTransaccional transaccion,
                                  RepositorioCatalogo catalogo, ElectionId eleccion) {
        return new ServicioPadron(derivador, repositorio, publicador, transaccion, catalogo::actual, eleccion,
                Clock.systemUTC());
    }

    @Bean
    ServicioVotacion servicioVotacion(ValidadorVotante validador, PublicadorVotos publicador,
                                      RegistroParticipacion registro, RegistroVotoAceptado aceptados,
                                      ControlRegionVotante controlRegion, RegistroIncidentesFraude incidentes,
                                      EjecutorTransaccional transaccion, ElectionId eleccion) {
        return new ServicioVotacion(validador, publicador, registro, aceptados, controlRegion, incidentes, transaccion,
                eleccion, Clock.systemUTC(), UUID::randomUUID);
    }
}
