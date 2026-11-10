package com.algaworks.algashop.notifications.domain.model.template;

import com.algaworks.algashop.notifications.domain.model.AbstractAuditableAggregateRoot;
import com.fasterxml.uuid.Generators;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Agregado do template de e-mail: nome, assunto (template em modo texto) e corpo (template HTML).
 *
 * O id e um UUIDv7 gerado pela aplicacao no construtor, nao pelo banco: o objeto ja nasce com identidade,
 * e o v7 e ordenado pelo tempo (indice da chave primaria cresce em ordem, como um serial).
 * Como o id ja vem preenchido, quem diz ao Spring Data que a entidade e nova e o version (Long) nulo:
 * version nulo -> persist; preenchido -> merge. Por isso ele nao pode ser long primitivo.
 */
@Entity
@Table(name = "email_template")
@Getter
@Setter
@NoArgsConstructor // exigido pelo JPA
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class EmailTemplate extends AbstractAuditableAggregateRoot<EmailTemplate> {

	@Id
	@EqualsAndHashCode.Include
	private UUID id;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false)
	private String subject;

	@Column(nullable = false, columnDefinition = "text")
	private String body;

	public EmailTemplate(String name, String subject, String body) {
		this.id = Generators.timeBasedEpochRandomGenerator().generate();
		this.name = name;
		this.subject = subject;
		this.body = body;
	}
}
