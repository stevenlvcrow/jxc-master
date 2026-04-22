package com.boboboom.jxc.common.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 领域事件发布器，统一收口工程内应用事件发布入口。
 */
@Component
public class DomainEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public DomainEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * 发布应用内同步事件。
     *
     * @param event 事件对象
     */
    public void publish(Object event) {
        applicationEventPublisher.publishEvent(event);
    }
}
