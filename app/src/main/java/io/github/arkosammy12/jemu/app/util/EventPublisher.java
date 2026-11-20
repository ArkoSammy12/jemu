package io.github.arkosammy12.jemu.app.util;

import io.github.arkosammy12.jemu.app.events.BaseEvent;

public interface EventPublisher {

    void publishEvent(BaseEvent event);

}
