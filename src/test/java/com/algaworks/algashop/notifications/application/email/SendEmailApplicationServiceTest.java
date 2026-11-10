package com.algaworks.algashop.notifications.application.email;

import com.algaworks.algashop.notifications.domain.model.template.EmailTemplate;
import com.algaworks.algashop.notifications.domain.model.template.EmailTemplateRepository;
import com.algaworks.algashop.notifications.domain.model.template.TemplateNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// teste de unidade do caso de uso de envio: sem Spring, sem banco e sem SMTP
@ExtendWith(MockitoExtension.class)
class SendEmailApplicationServiceTest {

	@Mock
	private EmailTemplateRenderer renderer;

	@Mock
	private EmailSender emailSender;

	@Mock
	private EmailTemplateRepository templateRepository;

	@InjectMocks
	private SendEmailApplicationService service;

	private final EmailTemplate template = new EmailTemplate(
			"Order Placed", "Order [[${orderId}]] received", "<p th:text=\"${recipient.name}\">customer</p>");

	private SendEmailInput input(Map<String, Object> variables) {
		return new SendEmailInput("john.doe@example.com", "John Doe", template.getId(), variables);
	}

	@Test
	void givenUnknownTemplate_whenSend_shouldThrowNotFoundWithoutRenderingOrSending() {
		UUID id = UUID.randomUUID();
		when(templateRepository.findById(id)).thenReturn(Optional.empty());

		assertThatExceptionOfType(TemplateNotFoundException.class)
				.isThrownBy(() -> service.send(new SendEmailInput("a@b.com", "A", id, Map.of())));
		verifyNoInteractions(renderer, emailSender);
	}

	@Test
	void givenValidInput_whenSend_shouldRenderStoredTemplateAndSendToRecipient() {
		when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
		when(renderer.renderSubject(eq(template.getSubject()), anyMap())).thenReturn("Order ORD-1 received");
		when(renderer.renderBody(eq(template.getBody()), anyMap())).thenReturn("<p>John Doe</p>");

		service.send(input(Map.of("orderId", "ORD-1")));

		verify(emailSender).send("john.doe@example.com", "John Doe", "Order ORD-1 received", "<p>John Doe</p>");
	}

	@Test
	@SuppressWarnings("unchecked")
	void givenVariablesWithFakeRecipient_whenSend_shouldKeepCommandRecipient() {
		when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
		when(renderer.renderSubject(anyString(), anyMap())).thenReturn("s");
		when(renderer.renderBody(anyString(), anyMap())).thenReturn("b");

		service.send(input(Map.of("orderId", "ORD-1",
				"recipient", Map.of("email", "evil@example.com", "name", "Mallory"))));

		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(renderer).renderBody(anyString(), captor.capture());
		assertThat(captor.getValue())
				.containsEntry("orderId", "ORD-1")
				.containsEntry("recipient", Map.of("email", "john.doe@example.com", "name", "John Doe"));
		verify(emailSender).send(eq("john.doe@example.com"), eq("John Doe"), any(), any());
	}

	@Test
	@SuppressWarnings("unchecked")
	void givenNullVariables_whenSend_shouldExposeOnlyRecipient() {
		when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
		when(renderer.renderSubject(anyString(), anyMap())).thenReturn("s");
		when(renderer.renderBody(anyString(), anyMap())).thenReturn("b");

		service.send(input(null));

		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(renderer).renderSubject(anyString(), captor.capture());
		assertThat(captor.getValue()).containsOnlyKeys("recipient");
	}

	@Test
	void givenRenderFailure_whenSend_shouldPropagateWithoutSending() {
		when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
		when(renderer.renderSubject(anyString(), anyMap())).thenReturn("s");
		when(renderer.renderBody(anyString(), anyMap())).thenThrow(new TemplateRenderException("boom", null));

		assertThatExceptionOfType(TemplateRenderException.class).isThrownBy(() -> service.send(input(Map.of())));
		verifyNoInteractions(emailSender);
	}

	@Test
	void givenSmtpFailure_whenSend_shouldPropagateMailException() {
		when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
		when(renderer.renderSubject(anyString(), anyMap())).thenReturn("s");
		when(renderer.renderBody(anyString(), anyMap())).thenReturn("b");
		doThrow(new MailSendException("smtp down")).when(emailSender).send(any(), any(), any(), any());

		assertThatExceptionOfType(MailSendException.class).isThrownBy(() -> service.send(input(Map.of())));
	}
}
