package com.algaworks.algashop.notifications.presentation;

import com.algaworks.algashop.notifications.application.email.SendEmailApplicationService;
import com.algaworks.algashop.notifications.application.email.command.SendEmailIntegrationCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/emails")
@RequiredArgsConstructor
public class EmailController {

	private final SendEmailApplicationService sendEmailApplicationService;

	// 204: o e-mail foi aceito pelo SMTP e nao ha nada para devolver; um corpo invalido para no @Valid (400)
	@PostMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void send(@RequestBody @Valid SendEmailIntegrationCommand command) {
		sendEmailApplicationService.send(command.toInput());
	}
}
