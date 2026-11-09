package com.algaworks.algashop.notifications.infrastructure.template;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class OrderPlacedEmailTemplateTest {

    private static final String SUBJECT = "Order [[${orderId}]] received";

    // As datas chegam como string ISO, porque as variáveis vêm de JSON: o substring monta dia/mês/ano.
    private static final String BODY = """
            <!DOCTYPE html>
            <html>
            <body style="font-family: Arial, Helvetica, sans-serif; color: #222;">
              <h2>Hello, <span th:text="${recipient.name}">customer</span>!</h2>
              <p>
                We have received your order <strong th:text="${orderId}">ORDER-ID</strong>
                on <span th:text="|${#strings.substring(placedAt, 8, 10)}/${#strings.substring(placedAt, 5, 7)}/${#strings.substring(placedAt, 0, 4)}|">01/01/2000</span>.
              </p>
              <table style="border-collapse: collapse; width: 100%;">
                <thead>
                  <tr>
                    <th style="text-align: left; border-bottom: 1px solid #ccc;">Product</th>
                    <th style="text-align: right; border-bottom: 1px solid #ccc;">Qty.</th>
                    <th style="text-align: right; border-bottom: 1px solid #ccc;">Price</th>
                    <th style="text-align: right; border-bottom: 1px solid #ccc;">Total</th>
                  </tr>
                </thead>
                <tbody>
                  <tr th:each="item : ${items}">
                    <td th:text="${item.productName}">Product</td>
                    <td style="text-align: right;" th:text="${item.quantity}">1</td>
                    <td style="text-align: right;" th:text="${#numbers.formatCurrency(item.price)}">$0.00</td>
                    <td style="text-align: right;" th:text="${#numbers.formatCurrency(item.totalAmount)}">$0.00</td>
                  </tr>
                </tbody>
              </table>
              <p>Shipping: <span th:text="${#numbers.formatCurrency(shippingCost)}">$0.00</span></p>
              <p><strong>Order total: <span th:text="${#numbers.formatCurrency(totalAmount)}">$0.00</span></strong></p>
              <p>Estimated delivery: <span th:text="|${#strings.substring(expectedDeliveryDate, 8, 10)}/${#strings.substring(expectedDeliveryDate, 5, 7)}/${#strings.substring(expectedDeliveryDate, 0, 4)}|">01/01/2000</span></p>
              <p>Thank you for shopping at AlgaShop!</p>
            </body>
            </html>
            """;

    private final EmailTemplateRendererThymeleafImpl renderer = new EmailTemplateRendererThymeleafImpl();
    private final EmailTemplateHtmlValidatorJsoupImpl htmlValidator = new EmailTemplateHtmlValidatorJsoupImpl();

    private Map<String, Object> orderVariables() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("recipient", Map.of("name", "John Doe", "email", "john@email.com"));
        variables.put("orderId", "ORD-1");
        variables.put("placedAt", "2026-10-09T14:30:00.123-03:00");
        variables.put("items", List.of(
                Map.of("productName", "Keyboard", "quantity", 1,
                        "price", new BigDecimal("150.00"), "totalAmount", new BigDecimal("150.00")),
                Map.of("productName", "Mouse", "quantity", 2,
                        "price", new BigDecimal("25.50"), "totalAmount", new BigDecimal("51.00"))));
        variables.put("shippingCost", new BigDecimal("10.00"));
        variables.put("totalAmount", new BigDecimal("211.00"));
        variables.put("expectedDeliveryDate", "2026-10-15");
        return variables;
    }

    @Test
    void givenOrderPlacedSubject_whenRenderSubject_shouldReplaceOrderId() {
        String subject = renderer.renderSubject(SUBJECT, orderVariables());

        assertThat(subject).isEqualTo("Order ORD-1 received");
    }

    @Test
    void givenOrderPlacedBody_whenRenderBody_shouldGreetRecipientAndShowOrderId() {
        String body = renderer.renderBody(BODY, orderVariables());

        assertThat(body)
                .contains("<h2>Hello, <span>John Doe</span>!</h2>")
                .contains("<strong>ORD-1</strong>");
    }

    @Test
    void givenIsoExpectedDeliveryDate_whenRenderBody_shouldFormatDateAsDayMonthYear() {
        String body = renderer.renderBody(BODY, orderVariables());

        assertThat(body).contains("Estimated delivery: <span>15/10/2026</span>");
    }

    @Test
    void givenNoItems_whenRenderBody_shouldRenderTableWithoutRows() {
        Map<String, Object> variables = orderVariables();
        variables.put("items", List.of());

        String body = renderer.renderBody(BODY, variables);

        assertThat(Jsoup.parse(body).select("tbody tr")).isEmpty();
        assertThat(body).contains("<th style=\"text-align: left; border-bottom: 1px solid #ccc;\">Product</th>");
    }

    @Test
    void givenAmountWithThousands_whenRenderBody_shouldUseGroupingSeparator() {
        Map<String, Object> variables = orderVariables();
        variables.put("totalAmount", new BigDecimal("1234.50"));

        String body = renderer.renderBody(BODY, variables);

        assertThat(body).contains("Order total: <span>$1,234.50</span>");
    }

    @Test
    void givenProductNameWithHtml_whenRenderBody_shouldEscapeHtml() {
        Map<String, Object> variables = orderVariables();
        variables.put("items", List.of(
                Map.of("productName", "<script>alert(1)</script>", "quantity", 1,
                        "price", new BigDecimal("1.00"), "totalAmount", new BigDecimal("1.00"))));

        String body = renderer.renderBody(BODY, variables);

        assertThat(body)
                .contains("&lt;script&gt;alert(1)&lt;/script&gt;")
                .doesNotContain("<script>");
    }

    @Test
    void givenOrderPlacedBody_whenRenderBody_shouldNotKeepThymeleafAttributes() {
        String body = renderer.renderBody(BODY, orderVariables());

        assertThat(body)
                .doesNotContain("th:text")
                .doesNotContain("th:each")
                .doesNotContain("ORDER-ID");
    }

    @Test
    void givenOrderPlacedTemplate_whenAssertRenderable_shouldNotThrow() {
        assertThatCode(() -> renderer.assertRenderable(SUBJECT, BODY))
                .doesNotThrowAnyException();
    }

    @Test
    void givenOrderPlacedBody_whenValidateSyntax_shouldNotThrow() {
        assertThatCode(() -> htmlValidator.validateSyntax(BODY))
                .doesNotThrowAnyException();
    }
}