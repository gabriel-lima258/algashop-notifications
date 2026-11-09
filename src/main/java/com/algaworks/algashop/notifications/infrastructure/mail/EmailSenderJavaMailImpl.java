package com.algaworks.algashop.notifications.infrastructure.mail;

import com.algaworks.algashop.notifications.application.email.EmailSender;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * Adapter da porta EmailSender: entrega o e-mail ja renderizado por SMTP, com o JavaMailSender do Spring Boot
 * (host e porta em spring.mail.*; em desenvolvimento, o Mailpit).
 *
 * Montar e enviar ficam separados de proposito:
 * - falha ao montar (endereco invalido, encoding) e dado ruim: repetir nao resolve -> MailPreparationException;
 * - falha ao enviar (SMTP fora do ar, timeout) sai como MailException do proprio send: aqui uma retentativa pode resolver.
 */
@Service
@RequiredArgsConstructor
public class EmailSenderJavaMailImpl implements EmailSender {

	private final JavaMailSender mailSender;
	private final MailProperties properties;

	@Override
	public void send(String toEmail, String toName, String subject, String htmlBody) {
		MimeMessage message = mailSender.createMimeMessage();

		try {
			// multipart = true: deixa espaco para anexo e imagem inline (cid:); UTF-8 para acento no assunto e no nome
			MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
			helper.setFrom(properties.getFrom());
			// endereco com nome: o cliente ve "John Doe <john@...>" em vez do e-mail cru
			helper.setTo(new InternetAddress(toEmail, toName, StandardCharsets.UTF_8.name()));
			helper.setSubject(subject);
			helper.setText(htmlBody, true);
		} catch (Exception e) {
			throw new MailPreparationException("Failed to build email message", e);
		}

		mailSender.send(message);
	}
}
