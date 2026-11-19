package com.zerotrust.authserver;

import com.zerotrust.authserver.adapter.config.FuenteVariablesEntorno;
import com.zerotrust.authserver.adapter.config.LectorArchivosSistema;
import com.zerotrust.authserver.config.CargadorConfiguracion;
import com.zerotrust.authserver.config.ConfiguracionAuthServer;
import com.zerotrust.authserver.exception.ConfiguracionInvalidaException;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.Clock;

@SpringBootApplication
public class AuthServerApplication {

    public static void main(String[] args) {
        ConfiguracionAuthServer configuracion;
        try {
            configuracion = new CargadorConfiguracion(new FuenteVariablesEntorno(), new LectorArchivosSistema(),
                    Clock.systemUTC()).cargar();
        } catch (ConfiguracionInvalidaException e) {
            // Fail-fast antes de levantar Spring: ninguna configuración inválida llega a abrir el puerto.
            LoggerFactory.getLogger(AuthServerApplication.class).error("No se puede arrancar: {}", e.getMessage());
            System.exit(1);
            return;
        }
        SpringApplication aplicacion = new SpringApplication(AuthServerApplication.class);
        aplicacion.addInitializers(contexto ->
                contexto.getBeanFactory().registerSingleton("configuracionAuthServer", configuracion));
        aplicacion.run(args);
    }
}
