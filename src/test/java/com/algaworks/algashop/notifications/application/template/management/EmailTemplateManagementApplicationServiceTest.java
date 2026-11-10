package com.algaworks.algashop.notifications.application.template.management;

import com.algaworks.algashop.notifications.application.email.EmailTemplateHtmlValidator;
import com.algaworks.algashop.notifications.application.email.EmailTemplateRenderer;
import com.algaworks.algashop.notifications.application.email.InvalidTemplateHtmlException;
import com.algaworks.algashop.notifications.application.email.TemplateRenderException;
import com.algaworks.algashop.notifications.domain.model.template.EmailTemplate;
import com.algaworks.algashop.notifications.domain.model.template.EmailTemplateRepository;
import com.algaworks.algashop.notifications.domain.model.template.TemplateNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// teste de unidade do servico: sem Spring e sem banco, as dependencias sao mocks
@ExtendWith(MockitoExtension.class)
class EmailTemplateManagementApplicationServiceTest {

	@Mock
	private EmailTemplateHtmlValidator validator;

	@Mock
	private EmailTemplateRenderer renderer;

	@Mock
	private EmailTemplateRepository repository;

	@InjectMocks
	private EmailTemplateManagementApplicationService service;

	private final EmailTemplateInput input = new EmailTemplateInput(
			"Welcome", "Welcome, [[${recipient.name}]]!", "<p th:text=\"${recipient.name}\">customer</p>");

	@Test
	void givenValidInput_whenCreate_shouldValidateSaveAndReturnUuidV7() {
		when(repository.saveAndFlush(any(EmailTemplate.class))).thenAnswer(i -> i.getArgument(0));

		UUID id = service.create(input);

		ArgumentCaptor<EmailTemplate> captor = ArgumentCaptor.forClass(EmailTemplate.class);
		verify(renderer).assertRenderable(input.subject(), input.body());
		verify(validator).validateSyntax(input.body());
		verify(repository).saveAndFlush(captor.capture());
		assertThat(id).isEqualTo(captor.getValue().getId());
		assertThat(id.version()).isEqualTo(7);
		assertThat(captor.getValue().getVersion()).isNull();
	}

	@Test
	void givenForbiddenHtml_whenCreate_shouldNotSave() {
		doThrow(new InvalidTemplateHtmlException(List.of("Element <script> is not allowed")))
				.when(validator).validateSyntax(input.body());

		assertThatExceptionOfType(InvalidTemplateHtmlException.class).isThrownBy(() -> service.create(input));
		verify(repository, never()).saveAndFlush(any());
	}

	@Test
	void givenInvalidSyntax_whenCreate_shouldNotSave() {
		doThrow(new TemplateRenderException("invalid", null))
				.when(renderer).assertRenderable(input.subject(), input.body());

		assertThatExceptionOfType(TemplateRenderException.class).isThrownBy(() -> service.create(input));
		verify(repository, never()).saveAndFlush(any());
	}

	@Test
	void givenUnknownId_whenUpdate_shouldThrowNotFoundWithoutValidating() {
		UUID id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.empty());

		assertThatExceptionOfType(TemplateNotFoundException.class).isThrownBy(() -> service.update(id, input));
		verifyNoInteractions(validator, renderer);
		verify(repository, never()).saveAndFlush(any());
	}

	@Test
	void givenInvalidContent_whenUpdate_shouldKeepStoredTemplateUnchanged() {
		EmailTemplate stored = new EmailTemplate("Old", "Old subject", "<p>old</p>");
		when(repository.findById(stored.getId())).thenReturn(Optional.of(stored));
		doThrow(new InvalidTemplateHtmlException(List.of("Element <script> is not allowed")))
				.when(validator).validateSyntax(any());

		assertThatExceptionOfType(InvalidTemplateHtmlException.class)
				.isThrownBy(() -> service.update(stored.getId(), input));

		assertThat(stored.getName()).isEqualTo("Old");
		assertThat(stored.getSubject()).isEqualTo("Old subject");
		assertThat(stored.getBody()).isEqualTo("<p>old</p>");
		verify(repository, never()).saveAndFlush(any());
	}

	@Test
	void givenValidInput_whenUpdate_shouldReplaceAllFields() {
		EmailTemplate stored = new EmailTemplate("Old", "Old subject", "<p>old</p>");
		when(repository.findById(stored.getId())).thenReturn(Optional.of(stored));

		service.update(stored.getId(), input);

		assertThat(stored.getName()).isEqualTo(input.name());
		assertThat(stored.getSubject()).isEqualTo(input.subject());
		assertThat(stored.getBody()).isEqualTo(input.body());
		verify(repository).saveAndFlush(stored);
	}

	@Test
	void givenUnknownId_whenDelete_shouldThrowNotFound() {
		UUID id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.empty());

		assertThatExceptionOfType(TemplateNotFoundException.class).isThrownBy(() -> service.delete(id));
		verify(repository, never()).delete(any());
	}

	@Test
	void givenExistingId_whenDelete_shouldRemove() {
		EmailTemplate stored = new EmailTemplate("Old", "Old subject", "<p>old</p>");
		when(repository.findById(stored.getId())).thenReturn(Optional.of(stored));

		service.delete(stored.getId());

		verify(repository).delete(stored);
	}
}
