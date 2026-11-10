package com.algaworks.algashop.notifications.infrastructure.kafka;

import com.algaworks.algashop.notifications.application.email.SendEmailApplicationService;
import com.algaworks.algashop.notifications.application.email.command.SendEmailIntegrationCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Segunda porta de entrada do envio: o mesmo SendEmailApplicationService da API REST, agora por comando no Kafka.
 * O listener so converte (toInput) e delega; nenhuma regra de template, variaveis ou envio mora aqui.
 *
 * id = grupo de consumo (notifications.email-commands); concurrency 3 = uma thread por particao.
 * A chave do registro e do produtor: so aparece no log, o consumo nao depende dela.
 */
@Component
@Slf4j
@RequiredArgsConstructor
@KafkaListener(
		id = "#{algaShopMessagingKafkaProperties.emailConsumerGroup}",
		concurrency = "3",
		topics = "#{algaShopMessagingKafkaProperties.emailCommandsTopicName}")
public class KafkaSendEmailIntegrationCommandListener {

	private final SendEmailApplicationService sendEmailApplicationService;

	@KafkaHandler
	public void handle(
			@Payload @Valid SendEmailIntegrationCommand command,
			@Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String messageKey,
			@Header(value = KafkaHeaders.RECEIVED_PARTITION, required = false) Integer partition,
			@Header(value = KafkaHeaders.OFFSET, required = false) Long offset) {
		// sem e-mail e sem variaveis no log: sao dados pessoais
		log.info("Received SendEmailCommand | templateId={} | key={} | partition={} | offset={}",
				command.getTemplateId(), messageKey, partition, offset);

		sendEmailApplicationService.send(command.toInput());
	}

	// o topico e exclusivo de comandos de e-mail: tipo desconhecido (ou sem __TypeId__) e erro de contrato,
	// nao pode ser ignorado em silencio. IllegalArgumentException esta nos nao retentaveis -> DLT direto
	@KafkaHandler(isDefault = true)
	public void handle(
			@Payload Object command,
			@Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String messageKey,
			@Header(value = KafkaHeaders.OFFSET, required = false) Long offset) {
		log.warn("Unsupported email command: type={} key={} offset={}",
				command.getClass().getSimpleName(), messageKey, offset);
		throw new IllegalArgumentException(
				"Unsupported email command type " + command.getClass().getSimpleName());
	}
}
