package com.lakesidemutual.policymanagement.interfaces.configuration;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.kafka.core.KafkaAdmin;

/**
 * The MessagingConfiguration class configures the Kafka message broker. This broker is used
 * to send events to the Risk Management Server when a policy changes.
 * */
@Configuration
public class KafkaMessagingConfiguration {

	@Value(value = "${spring.kafka.bootstrap-servers}")
	private String bootstrapServers;

	@Value(value = "${insuranceQuoteExpiredEvent.topicName}")
	private String insuranceQuoteExpiredTopic;

	@Value(value = "${policyCreatedEvent.topicName}")
	private String policyCreatedTopic;

	@Bean
	public KafkaAdmin kafkaAdmin() {
		Map<String, Object> configs = new HashMap<>();
		configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		return new KafkaAdmin(configs);
	}

	@Bean
	public NewTopic insuranceQuoteExpirationTopic() {
		return new NewTopic(insuranceQuoteExpiredTopic, 1, (short) 1);
	}

	@Bean
	public NewTopic policyCreationTopic() {
		return new NewTopic(policyCreatedTopic, 1, (short) 1);
	}

}