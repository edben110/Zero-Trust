package com.zerotrust.votingapi.mapper;

import com.zerotrust.votingapi.domain.ComprobanteVoto;
import com.zerotrust.votingapi.dto.response.ComprobanteResponse;

public final class MapperComprobante {

    private MapperComprobante() {
    }

    public static ComprobanteResponse aResponse(ComprobanteVoto comprobante) {
        return new ComprobanteResponse(comprobante.voteId().valor(), comprobante.recibidoEn().toString());
    }
}
