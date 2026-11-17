package com.zerotrust.votingapi.domain;

import java.time.Instant;

public record ComprobanteVoto(VoteId voteId, Instant recibidoEn) {
}
