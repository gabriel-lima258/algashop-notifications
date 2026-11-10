package com.algaworks.algashop.notifications.presentation;

import com.algaworks.algashop.notifications.application.template.management.EmailTemplateManagementApplicationService;
import com.algaworks.algashop.notifications.application.template.query.EmailTemplateQueryService;
import com.algaworks.algashop.notifications.domain.model.template.EmailTemplate;
import com.algaworks.algashop.notifications.infrastructure.security.NotificationsSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.data.core.TypeInformation;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// so a camada web: o query service e mock e lanca o mesmo erro que o Spring Data lanca para ?sort=foo
@WebMvcTest(EmailTemplateController.class)
@Import(NotificationsSecurityConfig.class)
class EmailTemplateControllerSortTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private EmailTemplateQueryService queryService;

	@MockitoBean
	private EmailTemplateManagementApplicationService managementService;

	// o issuer nao e consultado no teste: o jwt() ja entrega o token decodificado
	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void givenUnknownSortProperty_whenList_shouldReturnBadRequest() throws Exception {
		when(queryService.findAll(any())).thenThrow(
				new PropertyReferenceException("foo", TypeInformation.of(EmailTemplate.class), List.of()));

		mockMvc.perform(get("/api/v1/email-templates").param("sort", "foo")
						.with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_email-templates:read"))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Invalid sort"))
				.andExpect(jsonPath("$.detail").value("Invalid sort property: foo"));
	}
}
