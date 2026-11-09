package com.algaworks.algashop.notifications.infrastructure.template;

import com.algaworks.algashop.notifications.application.email.TemplateRenderException;
import org.junit.jupiter.api.Test;
import org.assertj.core.api.Assertions;

import java.util.Map;

class EmailTemplateRendererThymeleafImplTest {

    private final EmailTemplateRendererThymeleafImpl renderer = new EmailTemplateRendererThymeleafImpl();

    @Test
    void givenSubjectTemplateWithVariable_whenRender_shouldReplaceVariable() {
        String subject = renderer.renderSubject("Order [[${orderId}]] confirmed",
                Map.of("orderId", "EGT1234"));
        Assertions.assertThat(subject).isEqualTo("Order EGT1234 confirmed");
    }

    @Test
    void givenBodyTemplateWithNestedVariable_whenRender_shouldReplaceVariable() {
        String body = renderer.renderBody("<p>[[${name}]]</p>", Map.of("name", "John"));
        Assertions.assertThat(body).isEqualTo("<p>John</p>");
    }

    @Test
    void givenBodyTemplateWithNestedAttributeVariable_whenRender_shouldReplaceVariable() {
        String body = renderer.renderBody("<p th:text=\"${recipient.name}\">Alex</p>",
                Map.of("recipient", Map.of("name", "John")));
        Assertions.assertThat(body).isEqualTo("<p>John</p>");
    }

    @Test
    void givenMissingVariable_whenRender_shouldReturnEmpty() {
        String subject = renderer.renderSubject("Hi [[${name}]]!", Map.of());
        Assertions.assertThat(subject).isEqualTo("Hi !");
    }

    @Test
    void givenTemplateEvaluateWithVariable_whenAssertRenderable_shouldNotThrowErrors() {
        Assertions.assertThatCode(()-> renderer.assertRenderable(
                "Hi [[${recipient.name}]]!",
                "<p th:text=\"${order.id}\">xpto</p>"
        )).doesNotThrowAnyException();
    }

    @Test
    void givenTemplateWithInvalidSyntax_whenAssertRenderable_shouldThrowException() {
        Assertions.assertThatExceptionOfType(TemplateRenderException.class)
                .isThrownBy(()-> renderer.assertRenderable("Hi", "<p th:text=\"${\">x</p>"));
    }

    // o assunto falha so por falta de valor (tolerado); o corpo ainda precisa ser checado
    @Test
    void givenSubjectWithEvaluationFailureAndBodyWithInvalidSyntax_whenAssertRenderable_shouldThrowException() {
        Assertions.assertThatExceptionOfType(TemplateRenderException.class)
                .isThrownBy(()-> renderer.assertRenderable("Hi [[${recipient.name}]]!", "<p th:text=\"${\">x</p>"));
    }

}