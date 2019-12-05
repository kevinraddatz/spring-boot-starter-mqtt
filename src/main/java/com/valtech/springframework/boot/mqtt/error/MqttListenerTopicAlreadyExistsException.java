package com.valtech.springframework.boot.mqtt.error;

public class MqttListenerTopicAlreadyExistsException extends Exception {

    public MqttListenerTopicAlreadyExistsException(String topic, Class clazz) {
        super("Listener for topic " + topic + " already defined in " + clazz.getCanonicalName());
    }
}
