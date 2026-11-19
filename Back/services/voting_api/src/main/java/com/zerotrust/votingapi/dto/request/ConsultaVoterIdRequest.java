package com.zerotrust.votingapi.dto.request;

// cedula es Object a propósito: un número JSON perdería los ceros iniciales, así que solo se acepta texto.
public record ConsultaVoterIdRequest(Object cedula) {

    public String cedulaTexto() {
        return cedula instanceof String texto ? texto : null;
    }

    @Override
    public String toString() {
        return "ConsultaVoterIdRequest[***]";
    }
}
