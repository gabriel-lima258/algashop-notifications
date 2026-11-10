package com.algaworks.algashop.notifications.infrastructure.kafka;

import com.algaworks.algashop.notifications.application.email.SendEmailApplicationService;
import com.algaworks.algashop.notifications.application.email.SendEmailInput;
import com.algaworks.algashop.notifications.application.email.command.SendEmailIntegrationCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.node.JsonNodeFactory;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

// o listener so converte e delega: a regra de envio continua toda no SendEmailApplicationService
@ExtendWith(MockitoExtension.class)
class KafkaSendEmailIntegrationCommandListenerTest {

	@Mock
	private SendEmailApplicationService sendEmailApplicationService;

	@InjectMocks
	private KafkaSendEmailIntegrationCommandListener listener;

	@Test
	void givenSendEmailCommand_whenHandle_shouldDelegateConvertedInput() {
		UUID templateId = UUID.randomUUID();
		SendEmailIntegrationCommand command = SendEmailIntegrationCommand.builder()
				.recipient(new SendEmailIntegrationCommand.Recipient("john.doe@example.com", "John Doe"))
				.templateId(templateId)
				.variables(Map.of("orderId", "ORD-1001"))
				.build();

		listener.handle(command, null, 0, 10L);

		verify(sendEmailApplicationService).send(
				new SendEmailInput("john.doe@example.com", "John Doe", templateId, Map.of("orderId", "ORD-1001")));
	}

	@Test
	void givenUnknownPayload_whenHandleDefault_shouldThrowAndNotSend() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> listener.handle(JsonNodeFactory.instance.objectNode(), "key", 3L))
				.withMessageContaining("Unsupported email command type");
		verifyNoInteractions(sendEmailApplicationService);
	}
}
