package com.algaworks.algashop.notifications.infrastructure.security.check;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meta-anotacoes de escopo: um nome de negocio (CanSendEmails) no lugar de repetir a SpEL em cada metodo.
 * Os escopos sao os do contrato docs/openapi/notifications.yml; no JWT eles chegam como SCOPE_<escopo>.
 */
public class SecurityAnnotations {

	@Target({ElementType.METHOD, ElementType.TYPE})
	@Retention(RetentionPolicy.RUNTIME)
	@PreAuthorize("hasAuthority('SCOPE_emails:send')")
	public @interface CanSendEmails {}

	@Target({ElementType.METHOD, ElementType.TYPE})
	@Retention(RetentionPolicy.RUNTIME)
	@PreAuthorize("hasAuthority('SCOPE_email-templates:read')")
	public @interface CanReadEmailTemplates {}

	@Target({ElementType.METHOD, ElementType.TYPE})
	@Retention(RetentionPolicy.RUNTIME)
	@PreAuthorize("hasAuthority('SCOPE_email-templates:write')")
	public @interface CanWriteEmailTemplates {}

}
