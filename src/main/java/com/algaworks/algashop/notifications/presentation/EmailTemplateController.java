package com.algaworks.algashop.notifications.presentation;

import com.algaworks.algashop.notifications.application.template.management.EmailTemplateInput;
import com.algaworks.algashop.notifications.application.template.management.EmailTemplateManagementApplicationService;
import com.algaworks.algashop.notifications.application.template.query.EmailTemplateOutput;
import com.algaworks.algashop.notifications.application.template.query.EmailTemplateQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/email-templates")
@RequiredArgsConstructor
public class EmailTemplateController {

	private final EmailTemplateManagementApplicationService managementService;
	private final EmailTemplateQueryService queryService;

	// PagedModel gera o formato do contrato: { content: [...], page: { size, number, totalElements, totalPages } }
	@GetMapping
	public PagedModel<EmailTemplateOutput> listAll(Pageable pageable) {
		return new PagedModel<>(queryService.findAll(pageable));
	}

	@GetMapping("/{id}")
	public EmailTemplateOutput get(@PathVariable UUID id) {
		return queryService.findById(id);
	}

	@PostMapping
	public ResponseEntity<EmailTemplateOutput> create(@RequestBody @Valid EmailTemplateInput input) {
		UUID id = managementService.create(input);
		// Location = URL da requisicao atual + /{id}: o cliente sabe onde buscar o recurso criado
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}").buildAndExpand(id).toUri();
		return ResponseEntity.created(location).body(queryService.findById(id));
	}

	@PutMapping("/{id}")
	public EmailTemplateOutput update(@PathVariable UUID id, @RequestBody @Valid EmailTemplateInput input) {
		managementService.update(id, input);
		return queryService.findById(id);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		managementService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
