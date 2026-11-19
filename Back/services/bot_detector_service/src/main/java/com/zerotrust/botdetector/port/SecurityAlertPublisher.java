package com.zerotrust.botdetector.port;

import com.zerotrust.botdetector.domain.SecurityAlert;

public interface SecurityAlertPublisher {
    void publish(SecurityAlert alert);
}
