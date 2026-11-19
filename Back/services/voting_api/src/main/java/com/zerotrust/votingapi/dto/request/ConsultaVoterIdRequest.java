package com.zerotrust.votingapi.dto.request;

// cedula es Object a propósito: solo se acepta si llega como texto JSON. Un número JSON perdería los ceros
// iniciales (01234567 pasaría a 1234567), así que se rechaza en vez de convertirlo.
public record ConsultaVoterIdRequest(Object cedula) {

    public String cedulaTexto() {
        return cedula instanceof String texto ? texto : null;
    }

    @Override
    public String toString() {
        return "ConsultaVoterIdRequest[***]";
    }
}
