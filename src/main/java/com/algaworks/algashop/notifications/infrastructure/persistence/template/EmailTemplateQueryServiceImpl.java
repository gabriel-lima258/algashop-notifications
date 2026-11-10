package com.algaworks.algashop.notifications.infrastructure.persistence.template;

import com.algaworks.algashop.notifications.application.template.query.EmailTemplateOutput;
import com.algaworks.algashop.notifications.application.template.query.EmailTemplateQueryService;
import com.algaworks.algashop.notifications.domain.model.template.EmailTemplateRepository;
import com.algaworks.algashop.notifications.domain.model.template.TemplateNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// lado da leitura: nunca devolve a entidade, sempre o EmailTemplateOutput (a API nao fica presa ao modelo JPA)
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmailTemplateQueryServiceImpl implements EmailTemplateQueryService {

	private final EmailTemplateRepository repository;

	@Override
	public EmailTemplateOutput findById(UUID id) {
		return repository.findById(id)
				.map(EmailTemplateOutput::from)
				.orElseThrow(() -> new TemplateNotFoundException(id));
	}

	@Override
	public Page<EmailTemplateOutput> findAll(Pageable pageable) {
		return repository.findAll(pageable).map(EmailTemplateOutput::from);
	}
}
