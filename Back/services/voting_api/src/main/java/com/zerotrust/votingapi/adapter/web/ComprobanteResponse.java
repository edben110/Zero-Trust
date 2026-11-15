package com.zerotrust.votingapi.adapter.web;

import com.zerotrust.votingapi.domain.ComprobanteVoto;

public record ComprobanteResponse(String voteId, String receivedAt) {

    static ComprobanteResponse de(ComprobanteVoto comprobante) {
        return new ComprobanteResponse(comprobante.voteId().valor(), comprobante.recibidoEn().toString());
    }
}
