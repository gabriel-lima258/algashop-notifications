package com.algaworks.algashop.notifications.infrastructure.kafka;

import com.algaworks.algashop.notifications.application.email.TemplateRenderException;
import com.algaworks.algashop.notifications.domain.model.template.TemplateNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailPreparationException;
import org.springframework.util.backoff.ExponentialBackOff;

import java.time.Duration;
import java.util.Map;

/**
 * Topicos e resiliencia do CONSUMO de comandos de e-mail.
 *
 * Sem este arquivo, o default do Spring Kafka seriam 10 tentativas sem intervalo e descarte silencioso.
 * Aqui a regra e: so vale retentar o que pode dar certo na proxima tentativa (SMTP fora do ar);
 * o resto (dado ruim, template ausente, render quebrado) vai direto para a DLT.
 */
@Configuration
public class KafkaConfig {

	private static final String DLT_PREFIX = "notifications.dlt.";

	// fixos por enquanto; serao externalizados num desafio posterior
	private static final int TOPIC_PARTITIONS = 3;
	private static final int TOPIC_REPLICAS = 3;
	private static final long RETENTION_30_DAYS = Duration.ofDays(30).toMillis();

	// quem CONSOME o topico de comandos e quem o declara (mesmo padrao do billing com billing.invoice.commands)
	@Bean
	public NewTopic emailCommandTopic(AlgaShopMessagingKafkaProperties properties) {
		return TopicBuilder.name(properties.getEmailCommandsTopicName())
				.partitions(TOPIC_PARTITIONS)
				.replicas(TOPIC_REPLICAS)
				.configs(Map.of("min.insync.replicas", "2"))
				.build();
	}

	@Bean
	public NewTopic emailCommandTopicDlt(AlgaShopMessagingKafkaProperties properties) {
		return createDeadLetterTopic(properties.getEmailCommandsTopicName());
	}

	// mesmas particoes do topico de origem: o registro vai para a DLT na MESMA particao em que estava
	private NewTopic createDeadLetterTopic(String originalTopicName) {
		return TopicBuilder.name(DLT_PREFIX + originalTopicName)
				.partitions(TOPIC_PARTITIONS)
				.replicas(TOPIC_REPLICAS)
				.configs(Map.of(
						"min.insync.replicas", "2",
						"retention.ms", String.valueOf(RETENTION_30_DAYS)
				))
				.build();
	}

	// chamado pelo DefaultErrorHandler quando o registro esgota as retentativas ou falha com excecao nao retentavel;
	// publica o conteudo original + headers kafka_dlt-* (excecao, topico, particao e offset de origem)
	@Bean
	public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(KafkaTemplate<String, Object> kafkaTemplate) {
		DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
				(consumerRecord, exception) ->
						new TopicPartition(DLT_PREFIX + consumerRecord.topic(), consumerRecord.partition()));
		recoverer.setLogRecoveryRecord(true);
		recoverer.setFailIfSendResultIsError(false);
		return recoverer;
	}

	@Bean
	public DefaultErrorHandler defaultErrorHandler(DeadLetterPublishingRecoverer recoverer) {
		// 3 RETENTATIVAS com 2 s, 4 s e 8 s: com a entrega original sao 4 execucoes
		ExponentialBackOff exponentialBackOff = new ExponentialBackOff(2_000L, 2);
		exponentialBackOff.setMaxAttempts(3L);
		exponentialBackOff.setMaxInterval(8_000L);

		DefaultErrorHandler defaultErrorHandler = new DefaultErrorHandler(recoverer, exponentialBackOff);

		// direto para a DLT: repetir nao muda o resultado. A falha de envio ao SMTP (MailSendException) fica de fora
		// e e retentada. Validacao do payload e desserializacao ja sao nao retentaveis por padrao no Spring Kafka.
		defaultErrorHandler.addNotRetryableExceptions(
				IllegalArgumentException.class, // tipo de comando desconhecido (handler default do listener)
				ConstraintViolationException.class,
				TemplateNotFoundException.class,
				TemplateRenderException.class,
				MailParseException.class,
				MailPreparationException.class
		);

		return defaultErrorHandler;
	}
}
