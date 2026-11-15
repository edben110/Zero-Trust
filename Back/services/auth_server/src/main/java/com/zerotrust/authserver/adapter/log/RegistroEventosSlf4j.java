package com.zerotrust.authserver.adapter.log;

import com.zerotrust.authserver.domain.ClientId;
import com.zerotrust.authserver.port.RegistroEventos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegistroEventosSlf4j implements RegistroEventos {

    private static final Logger LOG = LoggerFactory.getLogger(RegistroEventosSlf4j.class);

    @Override
    public void emisionCorrecta(ClientId clientId) {
        LOG.info("Token emitido para client_id={}", clientId.valor());
    }

    @Override
    public void rechazoClienteRegistrado(String codigoError, ClientId clientId) {
        LOG.warn("Petición de token rechazada error={} client_id={}", codigoError, clientId.valor());
    }

    @Override
    public void rechazoClienteDesconocido(String codigoError) {
        LOG.warn("Petición de token rechazada error={} cliente desconocido", codigoError);
    }
}
