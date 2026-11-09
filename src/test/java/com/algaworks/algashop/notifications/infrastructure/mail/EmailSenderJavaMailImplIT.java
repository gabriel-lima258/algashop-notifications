package com.algaworks.algashop.notifications.infrastructure.mail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

// envia de verdade por SMTP: precisa do Mailpit do docker-compose.tools.yml no ar (SMTP 1025, caixa em http://localhost:8025)
class EmailSenderJavaMailImplIT {

	private static final int SMTP_PORT = 1025;
	private static final String MAILPIT_HOST = "localhost";

	private MailProperties properties;
	private EmailSenderJavaMailImpl emailSender;

	@BeforeEach
	void setUp() {
		properties = new MailProperties();
		properties.setFrom("no-reply@algashop.local");

		JavaMailSenderImpl javaMailSender = new JavaMailSenderImpl();
		javaMailSender.setPort(SMTP_PORT);
		javaMailSender.setHost(MAILPIT_HOST);

		emailSender = new EmailSenderJavaMailImpl(javaMailSender, properties);
	}

	@Test
	void givenValidData_whenSend_shouldNotGenerateErrors() {
		// destinatario unico: facilita achar esta mensagem na caixa do Mailpit
		String toEmail = "john-" + UUID.randomUUID() + "@email.com";

		assertDoesNotThrow(() -> emailSender.send(toEmail, "John Doe", "Order confirmed", "<p>Hello John</p>"));
	}

}
