package com.zerotrust.authserver.service;

import com.zerotrust.authserver.domain.SecretoCliente;

// client_id y scope llegan crudos: su formato se valida después de comprobar el grant y autenticar (RF-7, RF-24).
public record PeticionToken(String grantType, String clientIdCrudo, SecretoCliente secreto, String scopeCrudo,
                            boolean porBasic) {
}
