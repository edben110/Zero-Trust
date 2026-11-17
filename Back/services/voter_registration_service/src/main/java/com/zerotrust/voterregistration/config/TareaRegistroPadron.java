package com.zerotrust.voterregistration.config;

import com.zerotrust.voterregistration.service.ResultadoRegistro;
import com.zerotrust.voterregistration.service.ServicioRegistroPadron;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

public class TareaRegistroPadron implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(TareaRegistroPadron.class);

    private final ServicioRegistroPadron servicio;

    public TareaRegistroPadron(ServicioRegistroPadron servicio) {
        this.servicio = servicio;
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        ResultadoRegistro resultado = servicio.registrar();
        LOG.info("Padrón registrado: {} votantes publicados en eligible_voters, {} nuevos en registered_voters",
                resultado.total(), resultado.nuevos());
    }
}
