package com.zerotrust.regionalrollup.port;

import com.zerotrust.regionalrollup.domain.RegionalResultSnapshot;

public interface RegionalResultPublisher {
    void publish(RegionalResultSnapshot snapshot);
}
