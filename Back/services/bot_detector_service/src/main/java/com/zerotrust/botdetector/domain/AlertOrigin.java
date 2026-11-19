package com.zerotrust.botdetector.domain;

public enum AlertOrigin {
    LIVE,
    // Prueba lanzada desde el panel: se audita sin confundirla con un ataque real.
    DRILL
}
