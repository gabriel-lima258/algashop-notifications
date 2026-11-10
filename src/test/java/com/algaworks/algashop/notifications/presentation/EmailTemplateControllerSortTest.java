package com.algaworks.algashop.notifications.presentation;

import com.algaworks.algashop.notifications.application.template.management.EmailTemplateManagementApplicationService;
import com.algaworks.algashop.notifications.application.template.query.EmailTemplateQueryService;
import com.algaworks.algashop.notifications.domain.model.template.EmailTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.data.core.TypeInformation;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// so a camada web: o query service e mock e lanca o mesmo erro que o Spring Data lanca para ?sort=foo
@WebMvcTest(EmailTemplateController.class)
class EmailTemplateControllerSortTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private EmailTemplateQueryService queryService;

	@MockitoBean
	private EmailTemplateManagementApplicationService managementService;

	@Test
	void givenUnknownSortProperty_whenList_shouldReturnBadRequest() throws Exception {
		when(queryService.findAll(any())).thenThrow(
				new PropertyReferenceException("foo", TypeInformation.of(EmailTemplate.class), List.of()));

		mockMvc.perform(get("/api/v1/email-templates").param("sort", "foo"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Invalid sort"))
				.andExpect(jsonPath("$.detail").value("Invalid sort property: foo"));
	}
}
