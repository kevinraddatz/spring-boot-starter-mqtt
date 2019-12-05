package com.valtech.springframework.boot.mqtt;

import com.valtech.springframework.boot.mqtt.config.MqttProperties;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;

public class MqttSender {

    private IMqttClient mqttClient;
    private MqttProperties.MqttClientProperties properties;

    public MqttSender(IMqttClient client, MqttProperties.MqttClientProperties properties) {
        this.mqttClient = client;
        this.properties = properties;
    }

    public IMqttClient getMqttClient() {
        return this.mqttClient;
    }

    public void publish(String topic, byte[] message, Integer qos, Boolean retained) throws MqttException {
        mqttClient.publish(topic, message, qos, retained);
    }

    public void publish(String topic, String message, Integer qos, Boolean retained) throws MqttException {
        if (topic == null || topic.isEmpty()) {
            throw new RuntimeException("Topic must not be null or empty");
        }
        if (qos == null) {
            throw new RuntimeException("Qos must not be null");
        }
        publish(topic, message.getBytes(), qos, retained);
    }

    public void publish(byte[] message) throws MqttException {
        publish(properties.getDefaultTopic(), message, properties.getQos(), properties.getDefaultRetained());
    }

    public void publish(String message) throws MqttException {
        publish(properties.getDefaultTopic(), message.getBytes(), properties.getQos(), properties.getDefaultRetained());
    }
}
