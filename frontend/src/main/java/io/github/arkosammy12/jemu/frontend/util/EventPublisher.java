package io.github.arkosammy12.jemu.frontend.util;

import io.github.arkosammy12.jemu.frontend.events.BaseEvent;

public interface EventPublisher {

    void publishEvent(BaseEvent event);

}
