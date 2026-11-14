package com.zerotrust.authserver.port;

import java.util.Map;

public interface PublicadorClaves {

    Map<String, Object> jwkSetPublico();
}
