package com.algaworks.algashop.notifications.application.template.management;

import com.algaworks.algashop.notifications.application.email.EmailTemplateHtmlValidator;
import com.algaworks.algashop.notifications.application.email.EmailTemplateRenderer;
import com.algaworks.algashop.notifications.domain.model.template.EmailTemplate;
import com.algaworks.algashop.notifications.domain.model.template.EmailTemplateRepository;
import com.algaworks.algashop.notifications.domain.model.template.TemplateNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Casos de uso de escrita dos templates. Regra central: validar ANTES de gravar.
 * - create: valida e salva; devolve o id gerado na entidade.
 * - update: primeiro procura (404 vem antes de 422), depois valida, e so entao mexe no agregado:
 *   se a validacao falhar, o template salvo nao e tocado.
 * - delete: procura (404) e remove.
 * Cada metodo e uma transacao: ou tudo acontece, ou nada.
 */
@Service
@RequiredArgsConstructor
public class EmailTemplateManagementApplicationService {

	private final EmailTemplateHtmlValidator validator;
	private final EmailTemplateRenderer renderer;
	private final EmailTemplateRepository repository;

	@Transactional
	public UUID create(EmailTemplateInput input) {
		validateContent(input);
		return repository.saveAndFlush(new EmailTemplate(
				input.name(), input.subject(), input.body()
		)).getId();
	}

	@Transactional
	public void update(UUID id, EmailTemplateInput input) {
		EmailTemplate template = repository.findById(id).orElseThrow(() -> new TemplateNotFoundException(id));
		validateContent(input);
		template.setName(input.name());
		template.setSubject(input.subject());
		template.setBody(input.body());
		repository.saveAndFlush(template);
	}

	@Transactional
	public void delete(UUID id) {
		EmailTemplate template = repository.findById(id).orElseThrow(() -> new TemplateNotFoundException(id));
		repository.delete(template);
	}

	// renderer: sintaxe do assunto e do corpo (Thymeleaf); validator: HTML malformado ou com conteudo ativo
	private void validateContent(EmailTemplateInput input) {
		renderer.assertRenderable(input.subject(), input.body());
		validator.validateSyntax(input.body());
	}

}
