package com.valtech.springframework.boot.mqtt.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface MqttMapping {
    String topic();
    String clientId();
}
