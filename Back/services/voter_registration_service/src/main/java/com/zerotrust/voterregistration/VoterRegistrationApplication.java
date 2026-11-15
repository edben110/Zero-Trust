package com.zerotrust.voterregistration;

import com.zerotrust.voterregistration.config.CodigoSalida;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class VoterRegistrationApplication {

    // La tarea corre al arrancar (TareaRegistroPadron); si falla, SpringApplication.run lanza y el código es 1.
    public static void main(String[] args) {
        System.exit(CodigoSalida.de(() ->
                SpringApplication.exit(SpringApplication.run(VoterRegistrationApplication.class, args))));
    }
}
