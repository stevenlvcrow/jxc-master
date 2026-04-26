package com.boboboom.jxc.common.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 领域事件发布器，统一收口工程内应用事件发布入口。
 */
@Component
public class DomainEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    /** 创建领域事件发布器。 */
    public DomainEventPublisher(ApplicationEventPublisher applicationEventPublisherValue) {
        this.applicationEventPublisher = applicationEventPublisherValue;
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
