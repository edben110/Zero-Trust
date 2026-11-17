package com.zerotrust.authserver.port;

import com.zerotrust.authserver.domain.ClientId;

public interface RegistroEventos {

    void emisionCorrecta(ClientId clientId);

    void rechazoClienteRegistrado(String codigoError, ClientId clientId);

    void rechazoClienteDesconocido(String codigoError);
}
