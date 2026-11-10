package com.algaworks.algashop.notifications.domain.model.template;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, UUID> {
}
