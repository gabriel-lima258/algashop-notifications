package com.algaworks.algashop.notifications.infrastructure.template;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class WelcomeEmailTemplateTest {

    private static final String SUBJECT = "Welcome to AlgaShop, [[${recipient.name}]]!";

    private static final String BODY = """  
            <!DOCTYPE html>
            <html>
            <body style="font-family: Arial, Helvetica, sans-serif; color: #222;">
            <h2>Hello, <span th:text="${recipient.name}">customer</span>!</h2>              
            <p>Welcome to AlgaShop. Use the coupon <strong th:text="${couponCode}">COUPON</strong> on your first order.</p>            
            </body>            
            </html>            
            """;

    private final EmailTemplateRendererThymeleafImpl renderer = new EmailTemplateRendererThymeleafImpl();
    private final EmailTemplateHtmlValidatorJsoupImpl htmlValidator = new EmailTemplateHtmlValidatorJsoupImpl();

    private final Map<String, Object> variables = Map.of(
            "recipient", Map.of("name", "John Doe", "email", "john@email.com"),
            "couponCode", "WELCOME10");

    @Test
    void givenWelcomeSubject_whenRenderSubject_shouldReplaceRecipientName() {
        String subject = renderer.renderSubject(SUBJECT, variables);

        assertThat(subject).isEqualTo("Welcome to AlgaShop, John Doe!");
    }

    @Test
    void givenWelcomeBody_whenRenderBody_shouldReplaceRecipientNameAndCouponCode() {
        String body = renderer.renderBody(BODY, variables);

        assertThat(body)
                .contains("<h2>Hello, <span>John Doe</span>!</h2>")
                .contains("<strong>WELCOME10</strong>")
                .contains("<!DOCTYPE html>");
    }

    @Test
    void givenWelcomeBody_whenRenderBody_shouldNotKeepThymeleafAttributesNorPlaceholders() {
        String body = renderer.renderBody(BODY, variables);

        assertThat(body)
                .doesNotContain("th:text")
                .doesNotContain("customer")
                .doesNotContain("COUPON</strong>");
    }

    @Test
    void givenWelcomeBody_whenRenderBody_shouldKeepInlineStyle() {
        String body = renderer.renderBody(BODY, variables);

        assertThat(body).contains("style=\"font-family: Arial, Helvetica, sans-serif; color: #222;\"");
    }

    @Test
    void givenRecipientNameWithHtml_whenRenderBody_shouldEscapeHtml() {
        Map<String, Object> maliciousVariables = Map.of(
                "recipient", Map.of("name", "<script>alert(1)</script>"),
                "couponCode", "WELCOME10");

        String body = renderer.renderBody(BODY, maliciousVariables);

        assertThat(body)
                .contains("&lt;script&gt;alert(1)&lt;/script&gt;")
                .doesNotContain("<script>");
    }

    @Test
    void givenNoCouponCode_whenRenderBody_shouldRenderEmptyCoupon() {
        Map<String, Object> withoutCoupon = Map.of("recipient", Map.of("name", "John Doe"));

        String body = renderer.renderBody(BODY, withoutCoupon);

        assertThat(body).contains("<strong></strong>");
    }

    @Test
    void givenWelcomeTemplate_whenAssertRenderable_shouldNotThrow() {
        assertThatCode(() -> renderer.assertRenderable(SUBJECT, BODY))
                .doesNotThrowAnyException();
    }

    @Test
    void givenWelcomeBody_whenValidateSyntax_shouldNotThrow() {
        assertThatCode(() -> htmlValidator.validateSyntax(BODY))
                .doesNotThrowAnyException();
    }
}
