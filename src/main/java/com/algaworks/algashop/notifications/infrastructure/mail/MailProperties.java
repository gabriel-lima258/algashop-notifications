package com.algaworks.algashop.notifications.infrastructure.mail;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Configuracao propria do envio (o host e a porta do SMTP ficam no spring.mail.* do Boot).
 * O remetente muda por ambiente, por isso e config e nao constante no codigo.
 * Com @Validated, a aplicacao nem sobe se o "from" estiver vazio: o erro aparece no boot, nao no primeiro e-mail.
 */
@Component
@Validated
@Data
@ConfigurationProperties("algashop.notifications.mail")
public class MailProperties {

	@NotBlank
	private String from;
}
