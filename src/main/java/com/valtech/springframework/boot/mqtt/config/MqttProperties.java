/*
 *
 *  * Copyright 2019 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      https://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.valtech.springframework.boot.mqtt.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "spring.mqtt")
public class MqttProperties {

    private Boolean enabled;

    private List<MqttClientProperties> clients;

    @Getter
    @Setter
    public static class MqttClientProperties {
        private String connectionUrl;
        private String clientId;
        private Boolean automaticReconnect = false;
        private Boolean defaultRetained = false;
        private String defaultTopic;
        private Integer qos;
        private Authentication authentication;
        private LastWill lastWill;

        @Getter
        @Setter
        public static class Authentication {
            private String username;
            private String password;
        }

        @Getter
        @Setter
        public static class LastWill {
            private String message;
            private Integer qos;
            private String topic;
            private Boolean retained;
        }
    }
}
