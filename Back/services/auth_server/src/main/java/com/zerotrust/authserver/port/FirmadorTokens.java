package com.zerotrust.authserver.port;

import com.zerotrust.authserver.domain.ClaimsToken;

public interface FirmadorTokens {

    String firmar(ClaimsToken claims);
}
