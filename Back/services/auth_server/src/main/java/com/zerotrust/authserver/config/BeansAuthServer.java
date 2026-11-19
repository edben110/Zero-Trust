package com.zerotrust.authserver.config;

import com.zerotrust.authserver.security.FirmadorJws;
import com.zerotrust.authserver.security.PublicadorJwksNimbus;
import com.zerotrust.authserver.security.VerificadorBcrypt;
import com.zerotrust.authserver.controller.InterpretePeticionToken;
import com.zerotrust.authserver.domain.RegistroClientes;
import com.zerotrust.authserver.port.FirmadorTokens;
import com.zerotrust.authserver.port.PublicadorClaves;
import com.zerotrust.authserver.port.RegistroEventos;
import com.zerotrust.authserver.port.VerificadorSecretos;
import com.zerotrust.authserver.service.ServicioEmisionToken;
import com.zerotrust.authserver.service.ServicioJwks;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class BeansAuthServer {

    @Bean
    Clock reloj() {
        return Clock.systemUTC();
    }

    @Bean
    RegistroClientes registroClientes(ConfiguracionAuthServer configuracion) {
        return configuracion.registro();
    }

    @Bean
    RegistroEventos registroEventos() {
        return new RegistroEventosSlf4j();
    }

    @Bean
    VerificadorSecretos verificadorSecretos(RegistroClientes registro) {
        return new VerificadorBcrypt(registro.costeComun());
    }

    @Bean
    FirmadorTokens firmadorTokens(ConfiguracionAuthServer configuracion) {
        return new FirmadorJws(configuracion.clavePrivada(), configuracion.idClave());
    }

    @Bean
    PublicadorClaves publicadorClaves(ConfiguracionAuthServer configuracion) {
        return new PublicadorJwksNimbus(configuracion.clavePublica(), configuracion.idClave());
    }

    @Bean
    ServicioEmisionToken servicioEmisionToken(ConfiguracionAuthServer configuracion, RegistroClientes registro,
                                              VerificadorSecretos verificador, FirmadorTokens firmador, Clock reloj) {
        return new ServicioEmisionToken(registro, verificador, firmador, configuracion.emisor(), configuracion.ttl(),
                reloj);
    }

    @Bean
    ServicioJwks servicioJwks(PublicadorClaves publicador) {
        return new ServicioJwks(publicador);
    }

    @Bean
    InterpretePeticionToken interpretePeticionToken() {
        return new InterpretePeticionToken();
    }
}
