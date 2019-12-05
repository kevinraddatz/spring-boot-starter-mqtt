package com.valtech.springframework.boot.mqtt;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MqttSenderRepository {

    @Autowired private List<MqttSender> clients;

    public MqttSender findMqttSenderById(String clientId) {
        return clients.stream().filter((client) -> client.getMqttClient().getClientId().equals(clientId)).findFirst().orElse(null);
    }
}
