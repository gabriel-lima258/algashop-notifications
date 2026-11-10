package com.algaworks.algashop.notifications.infrastructure.kafka;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Nomes do topico de comandos e do grupo de consumo, lidos de algashop.messaging.kafka.
 * O que liga o notifications a quem publica (o ordering, no proximo passo) nao e codigo compartilhado:
 * e a mesma string do nome do topico nos dois YAML. Com @NotBlank, sem nome de topico o servico nem sobe.
 */
@Component
@Validated
@Data
@ConfigurationProperties("algashop.messaging.kafka")
public class AlgaShopMessagingKafkaProperties {

	@NotBlank
	private String emailCommandsTopicName;

	@NotBlank
	private String emailConsumerGroup;

}
