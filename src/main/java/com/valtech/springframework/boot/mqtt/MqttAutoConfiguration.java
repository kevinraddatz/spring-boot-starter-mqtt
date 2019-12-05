package com.valtech.springframework.boot.mqtt;

import com.valtech.springframework.boot.mqtt.annotation.MqttController;
import com.valtech.springframework.boot.mqtt.annotation.MqttMapping;
import com.valtech.springframework.boot.mqtt.config.MqttProperties;
import com.valtech.springframework.boot.mqtt.error.MqttListenerTopicAlreadyExistsException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@ConditionalOnProperty(value = "spring.mqtt.enabled", havingValue = "true")
@EnableConfigurationProperties({MqttProperties.class})
public class MqttAutoConfiguration {

    private final ApplicationContext context;

    public MqttAutoConfiguration(ApplicationContext context) {
        this.context = context;
    }

    private void validateMqttProperties(MqttProperties.MqttClientProperties properties) {
        if ((properties.getConnectionUrl() == null) || (properties.getConnectionUrl().isEmpty())) {
            throw new InvalidPropertyException(MqttProperties.class, "connectionUrl", "ConnectionUrl must not be null or empty");
        }
        if ((properties.getClientId() == null) || (properties.getClientId().isEmpty())) {
            throw new InvalidPropertyException(MqttProperties.class, "clientId", "ClientId must not be null or empty");
        }
        if ((properties.getAuthentication() != null) && (properties.getAuthentication().getUsername() == null) || properties.getAuthentication().getUsername().isEmpty()) {
            throw new InvalidPropertyException(MqttProperties.class, "username", "If authentication is enabled, username must not be null or empty");
        }
        if ((properties.getAuthentication() != null) && (properties.getAuthentication().getPassword() == null) || properties.getAuthentication().getPassword().isEmpty()) {
            throw new InvalidPropertyException(MqttProperties.class, "password", "If authentication is enabled, password must not be null or empty");
        }
    }

    private void validateLastWill(MqttProperties.MqttClientProperties properties) {
        if ((properties.getLastWill().getMessage() == null) || (properties.getLastWill().getMessage().isEmpty())) {
            throw new InvalidPropertyException(MqttProperties.class, "message", "If last will is enabled, last will message must not be null or empty.");
        }
        if ((properties.getLastWill().getTopic() == null) || (properties.getLastWill().getTopic().isEmpty()) &&
                (properties.getDefaultTopic() == null) || (properties.getDefaultTopic().isEmpty())) {
            throw new InvalidPropertyException(MqttProperties.class, "topic", "If last will is enabled, specify either at least a default topic or a last will topic");
        }
        if ((properties.getLastWill().getRetained() == null) && (properties.getDefaultRetained() == null)) {
            throw new InvalidPropertyException(MqttProperties.class, "retained", "I last will is enabled, specify either at least the default retained or the last will retained");
        }
        if ((properties.getLastWill().getQos() == null) || (properties.getQos() == null)) {
            throw new InvalidPropertyException(MqttProperties.class, "qos", "If last will is enabled, specify either at least a default qos or the last will qos");
        }
    }

    private void setLastWill(MqttProperties.MqttClientProperties properties, MqttConnectOptions options) {
        validateLastWill(properties);
        Boolean retained = properties.getLastWill().getRetained() != null ? properties.getLastWill().getRetained() : properties.getDefaultRetained();
        String topic = properties.getLastWill().getTopic() != null ? properties.getLastWill().getTopic() : properties.getDefaultTopic();
        Integer qos = properties.getLastWill().getQos() != null ? properties.getLastWill().getQos() : properties.getQos();
        byte[] message = properties.getLastWill().getMessage().getBytes();

        options.setWill(topic, message, qos, retained);
    }

    @Getter
    @Setter
    @AllArgsConstructor
    private static class ListenerMethodInBean {
        private Method method;
        private Object bean;
        private String topic;
    }

    private void attachListenerToClient(IMqttClient client, MqttProperties.MqttClientProperties properties) throws MqttListenerTopicAlreadyExistsException, MqttException {
        Map<String, Object> beans = context.getBeansWithAnnotation(MqttController.class);

        Map<String, ListenerMethodInBean> topics = new HashMap<>();
        for (Object bean : beans.values()) {
            Method[] methods = bean.getClass().getMethods();
            for (Method method : methods) {
                MqttMapping mqttMapping = method.getDeclaredAnnotation(MqttMapping.class);
                if (mqttMapping != null) {
                    String topicName = mqttMapping.topic();
                    String clientId = mqttMapping.clientId();
                    if (clientId.equals(client.getClientId())) {
                        if (topics.values().stream().anyMatch((listenerMethodInBean -> listenerMethodInBean.getTopic().equals(topicName))) && topics.containsKey(clientId)) {
                            throw new MqttListenerTopicAlreadyExistsException(topicName, bean.getClass());
                        } else {
                            topics.put(clientId, new ListenerMethodInBean(method, bean, topicName));
                        }
                    }
                }
            }
        }

        for (Map.Entry<String, ListenerMethodInBean> entry : topics.entrySet()) {
            if (properties.getQos() != null) {
                client.subscribe(entry.getKey(), properties.getQos(), ((topic, message) -> entry.getValue().getMethod().invoke(entry.getValue().getBean(), message)));
            } else {
                client.subscribe(entry.getKey(), ((topic, message) -> entry.getValue().getMethod().invoke(entry.getValue().getBean(), message)));
            }
        }
    }

    private IMqttClient createMqttClient(MqttProperties.MqttClientProperties properties) throws MqttException, MqttListenerTopicAlreadyExistsException {
        validateMqttProperties(properties);
        IMqttClient client = new MqttClient(properties.getConnectionUrl(), properties.getClientId());
        MqttConnectOptions options = new MqttConnectOptions();
        setLastWill(properties, options);
        options.setAutomaticReconnect(properties.getAutomaticReconnect());
        client.connect(options);

        attachListenerToClient(client, properties);

        return client;
    }

    @Bean
    public List<MqttSender> mqttClients(MqttProperties mqttProperties) throws MqttException, MqttListenerTopicAlreadyExistsException {
        List<MqttSender> clients = new ArrayList<>();

        for (MqttProperties.MqttClientProperties properties : mqttProperties.getClients()) {
            clients.add(new MqttSender(createMqttClient(properties), properties));
        }

        return clients;
    }
}
