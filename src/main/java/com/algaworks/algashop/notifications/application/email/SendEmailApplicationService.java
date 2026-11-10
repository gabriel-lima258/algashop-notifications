package com.algaworks.algashop.notifications.application.email;

import com.algaworks.algashop.notifications.domain.model.template.EmailTemplate;
import com.algaworks.algashop.notifications.domain.model.template.EmailTemplateRepository;
import com.algaworks.algashop.notifications.domain.model.template.TemplateNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Caso de uso do envio: template salvo + variaveis do comando -> e-mail entregue ao SMTP.
 *
 * Ordem: carrega o template (404 se nao existe), renderiza assunto e corpo (422 se nao der) e so entao envia
 * (502 se o SMTP recusar). As excecoes nao sao capturadas aqui: quem traduz em status e o ApiExceptionHandler,
 * e no consumidor Kafka quem decide retry ou DLT sera o error handler.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SendEmailApplicationService {

	private final EmailTemplateRenderer renderer;
	private final EmailSender emailSender;
	private final EmailTemplateRepository templateRepository;

	public void send(SendEmailInput input) {
		EmailTemplate template = templateRepository.findById(input.templateId())
				.orElseThrow(() -> new TemplateNotFoundException(input.templateId()));
		Map<String, Object> variables = buildVariables(input);

		// renderiza tudo antes de enviar: um corpo que falha no meio nao pode virar e-mail pela metade
		String subject = renderer.renderSubject(template.getSubject(), variables);
		String body = renderer.renderBody(template.getBody(), variables);

		emailSender.send(input.recipientEmail(), input.recipientName(), subject, body);

		// so o id do template: e-mail e variaveis sao dados pessoais e nao vao para o log
		log.info("Email sent: templateId={}", template.getId());
	}

	// as variaveis do comando + o destinatario em "recipient". O recipient entra por ULTIMO, de proposito:
	// se variables trouxer uma chave "recipient", ela e sobrescrita, e o template sempre ve quem realmente recebe.
	private Map<String, Object> buildVariables(SendEmailInput input) {
		Map<String, Object> variables = new HashMap<>();
		if (input.variables() != null) {
			variables.putAll(input.variables());
		}
		Map<String, Object> recipient = new HashMap<>();
		recipient.put("email", input.recipientEmail());
		recipient.put("name", input.recipientName());
		variables.put("recipient", recipient);
		return variables;
	}

}
