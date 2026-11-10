package com.algaworks.algashop.notifications.application.template.query;

import com.algaworks.algashop.notifications.domain.model.template.EmailTemplate;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EmailTemplateOutput(
		UUID id,
		String name,
		String subject,
		String body,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt) {

	public static EmailTemplateOutput from(EmailTemplate template) {
		return new EmailTemplateOutput(template.getId(), template.getName(), template.getSubject(),
				template.getBody(), template.getCreatedAt(), template.getUpdatedAt());
	}

}
