package com.algaworks.algashop.notifications.infrastructure.security;

import com.algaworks.algashop.notifications.application.email.SendEmailApplicationService;
import com.algaworks.algashop.notifications.application.template.management.EmailTemplateManagementApplicationService;
import com.algaworks.algashop.notifications.application.template.query.EmailTemplateQueryService;
import com.algaworks.algashop.notifications.presentation.EmailController;
import com.algaworks.algashop.notifications.presentation.EmailTemplateController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/**
 * Matriz de autorizacao do notifications: para cada endpoint, sem token (401), com escopo errado (403)
 * e com o escopo certo (passa pela seguranca).
 *
 * Corpo valido e id em formato UUID em todas as chamadas: o @Valid e a conversao do {id} rodam ANTES
 * do @PreAuthorize, e um corpo invalido daria 400 e esconderia o 403.
 * Os servicos sao mocks: aqui so importa a decisao de seguranca, nao o que o servico faz.
 */
@WebMvcTest(controllers = {EmailController.class, EmailTemplateController.class})
@Import(NotificationsSecurityConfig.class)
class NotificationsAuthorizationMatrixTest {

	private static final String SEND = "SCOPE_emails:send";
	private static final String READ = "SCOPE_email-templates:read";
	private static final String WRITE = "SCOPE_email-templates:write";

	private static final String ID = "0198d0a1-2c44-7d0b-8e11-9a0b7c5d3f21";
	private static final String EMAIL_BODY = """
			{"recipient":{"email":"john.doe@example.com","name":"John Doe"},"templateId":"%s","variables":{}}""".formatted(ID);
	private static final String TEMPLATE_BODY = """
			{"name":"Welcome","subject":"Hi [[${recipient.name}]]","body":"<p>Hi</p>"}""";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@MockitoBean
	private SendEmailApplicationService sendEmailApplicationService;

	@MockitoBean
	private EmailTemplateManagementApplicationService managementService;

	@MockitoBean
	private EmailTemplateQueryService queryService;

	// endpoint, escopo exigido e o status de sucesso esperado quando o escopo esta presente
	static Stream<Arguments> endpoints() {
		return Stream.of(
				Arguments.of(HttpMethod.POST, "/api/v1/emails", EMAIL_BODY, SEND, 204),
				Arguments.of(HttpMethod.GET, "/api/v1/email-templates", null, READ, 200),
				Arguments.of(HttpMethod.GET, "/api/v1/email-templates/" + ID, null, READ, 200),
				Arguments.of(HttpMethod.POST, "/api/v1/email-templates", TEMPLATE_BODY, WRITE, 201),
				Arguments.of(HttpMethod.PUT, "/api/v1/email-templates/" + ID, TEMPLATE_BODY, WRITE, 200),
				Arguments.of(HttpMethod.DELETE, "/api/v1/email-templates/" + ID, null, WRITE, 204)
		);
	}

	private MockHttpServletRequestBuilder request(HttpMethod method, String uri, String body) {
		MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.request(method, uri);
		if (body != null) {
			builder.contentType(MediaType.APPLICATION_JSON).content(body);
		}
		return builder;
	}

	@ParameterizedTest(name = "{0} {1} sem token -> 401")
	@MethodSource("endpoints")
	void givenNoToken_whenCall_shouldReturn401WithWwwAuthenticateAndNoBody(
			HttpMethod method, String uri, String body, String scope, int success) throws Exception {
		MvcResult result = mockMvc.perform(request(method, uri, body)).andReturn();

		assertThat(result.getResponse().getStatus()).isEqualTo(401);
		assertThat(result.getResponse().getHeader(HttpHeaders.WWW_AUTHENTICATE)).startsWith("Bearer");
		assertThat(result.getResponse().getContentAsString()).isEmpty();
	}

	@ParameterizedTest(name = "{0} {1} com escopo errado -> 403")
	@MethodSource("endpoints")
	void givenTokenWithOtherScopes_whenCall_shouldReturn403WithNoBody(
			HttpMethod method, String uri, String body, String scope, int success) throws Exception {
		SimpleGrantedAuthority[] others = Stream.of(SEND, READ, WRITE)
				.filter(s -> !s.equals(scope))
				.map(SimpleGrantedAuthority::new)
				.toArray(SimpleGrantedAuthority[]::new);

		MvcResult result = mockMvc.perform(request(method, uri, body).with(jwt().authorities(others))).andReturn();

		assertThat(result.getResponse().getStatus()).isEqualTo(403);
		assertThat(result.getResponse().getContentAsString()).isEmpty();
	}

	@ParameterizedTest(name = "{0} {1} com {3} -> {4}")
	@MethodSource("endpoints")
	void givenTokenWithRequiredScope_whenCall_shouldPassSecurity(
			HttpMethod method, String uri, String body, String scope, int success) throws Exception {
		when(queryService.findAll(any())).thenReturn(Page.empty());
		when(managementService.create(any())).thenReturn(java.util.UUID.fromString(ID));

		MvcResult result = mockMvc.perform(request(method, uri, body)
				.with(jwt().authorities(new SimpleGrantedAuthority(scope)))).andReturn();

		assertThat(result.getResponse().getStatus()).isEqualTo(success);
	}

	@Test
	void givenActuatorHealth_whenCallWithoutToken_shouldNotBeBlockedBySecurity() throws Exception {
		// o Actuator ainda nao existe: sem token a resposta e 404 (nao ha handler), e nao 401
		int status = mockMvc.perform(MockMvcRequestBuilders.get("/actuator/health")).andReturn().getResponse().getStatus();
		assertThat(status).isNotEqualTo(401);
	}
}
