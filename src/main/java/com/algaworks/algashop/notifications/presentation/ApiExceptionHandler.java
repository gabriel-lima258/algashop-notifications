package com.algaworks.algashop.notifications.presentation;

import com.algaworks.algashop.notifications.application.email.InvalidTemplateHtmlException;
import com.algaworks.algashop.notifications.application.email.TemplateRenderException;
import com.algaworks.algashop.notifications.domain.model.DomainEntityNotFoundException;
import com.algaworks.algashop.notifications.domain.model.DomainException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
@Slf4j
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(DomainEntityNotFoundException.class)
	public ProblemDetail handle(DomainEntityNotFoundException e) {
		return problem(HttpStatus.NOT_FOUND, "Not found", e.getMessage());
	}

	@ExceptionHandler(TemplateRenderException.class)
	public ProblemDetail handle(TemplateRenderException e) {
		return problem(HttpStatus.UNPROCESSABLE_CONTENT, "Invalid email template", e.getMessage());
	}

	@ExceptionHandler(InvalidTemplateHtmlException.class)
	public ProblemDetail handle(InvalidTemplateHtmlException e) {
		return problem(HttpStatus.UNPROCESSABLE_CONTENT, "Invalid email template", e.getMessage());
	}

	// ?sort=campoQueNaoExiste: o Spring Data falha ao montar o ORDER BY. E erro de quem chamou (400), nao do servidor (500).
	// O detail cita so o nome pedido, sem expor o nome da classe JPA.
	@ExceptionHandler(PropertyReferenceException.class)
	public ProblemDetail handle(PropertyReferenceException e) {
		return problem(HttpStatus.BAD_REQUEST, "Invalid sort", "Invalid sort property: " + e.getPropertyName());
	}

	@ExceptionHandler(MailException.class)
	public ProblemDetail handle(MailException e) {
		log.error("Email delivery failed", e);
		return problem(HttpStatus.BAD_GATEWAY, "Email delivery failed", "The email could not be delivered");
	}

	@ExceptionHandler(DomainException.class)
	public ProblemDetail handle(DomainException e) {
		return problem(HttpStatus.UNPROCESSABLE_CONTENT, "Unprocessable content", e.getMessage());
	}

	private ProblemDetail problem(HttpStatus status, String title, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		return problem;
	}
}
