package com.algaworks.algashop.notifications.application.email.command;

import com.algaworks.algashop.notifications.application.email.SendEmailInput;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Comando "envie este e-mail", no formato do schema SendEmail do contrato.
 * O nome e de comando, nao de requisicao HTTP, porque o mesmo tipo vai chegar pelo Kafka
 * (notifications.email.commands); a API REST e so a primeira porta de entrada.
 *
 * O servico de aplicacao nao conhece esta classe: ele recebe o SendEmailInput, via toInput().
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendEmailIntegrationCommand {

	// @Valid faz a Bean Validation descer para dentro do Recipient; sem ele, email e name nao seriam checados
	@NotNull
	@Valid
	private Recipient recipient;

	@NotNull
	private UUID templateId;

	// opcional: o comando pode chegar sem variaveis
	@Builder.Default
	private Map<String, Object> variables = new HashMap<>();

	public SendEmailInput toInput() {
		return new SendEmailInput(recipient.email, recipient.name, templateId, variables);
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Recipient {

		@NotBlank
		@Email
		@Size(max = 254)
		private String email;

		@NotBlank
		private String name;
	}
}
