package com.zamaz.telemetry.domain.event;

public interface EventPublisher {
    void publish(Object event);
}