package com.algaworks.algashop.notifications.infrastructure.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListenerConfigurer;
import org.springframework.kafka.config.KafkaListenerEndpointRegistrar;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * Liga a Bean Validation nos listeners Kafka. Por padrao o @Valid de um @KafkaHandler NAO valida nada:
 * e preciso registrar um validator no registrar, e e isso que esta classe faz.
 *
 * Com ele, um SendEmailIntegrationCommand que viole as regras da API (e-mail invalido, templateId ausente)
 * vira MethodArgumentNotValidException ANTES do handler rodar. O Spring Kafka ja trata essa excecao como
 * nao retentavel: o registro vai direto para a DLT, sem chegar ao SendEmailApplicationService.
 */
@Configuration
@RequiredArgsConstructor
public class KafkaBeanValidationConfigurer implements KafkaListenerConfigurer {

	private final LocalValidatorFactoryBean validator;

	@Override
	public void configureKafkaListeners(KafkaListenerEndpointRegistrar registrar) {
		registrar.setValidator(validator);
	}
}
