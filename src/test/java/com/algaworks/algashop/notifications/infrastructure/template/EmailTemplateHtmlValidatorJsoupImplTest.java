package com.algaworks.algashop.notifications.infrastructure.template;

import com.algaworks.algashop.notifications.application.email.InvalidTemplateHtmlException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

class EmailTemplateHtmlValidatorJsoupImplTest {

    private final EmailTemplateHtmlValidatorJsoupImpl validator = new EmailTemplateHtmlValidatorJsoupImpl();

    @Test
    void givenWellFormedHTML_whenValidateSyntax_shouldNotThrowErrors() {
        Assertions.assertThatCode(() -> validator.validateSyntax("<p>Hello</p>")).doesNotThrowAnyException();
    }

    @Test
    void givenMalformedHTML_whenValidateSyntax_shouldThrowInvalidTemplateException() {
        Assertions.assertThatExceptionOfType(InvalidTemplateHtmlException.class)
                .isThrownBy(()-> validator.validateSyntax("</div><p>Hello</div>"));
    }

    @Test
    void givenHtmlWithScriptTag_whenValidateSyntax_shouldThrowInvalidTemplateException() {
        Assertions.assertThatExceptionOfType(InvalidTemplateHtmlException.class)
                .isThrownBy(()-> validator.validateSyntax("<p>Hello</p><script>console.log('hi')</script>"));
    }

    @Test
    void givenHtmlWithInlineEventHandler_whenValidateSyntax_shouldThrowInvalidTemplateException() {
        Assertions.assertThatExceptionOfType(InvalidTemplateHtmlException.class)
                .isThrownBy(()-> validator.validateSyntax("<p onclick=\"alert(1)\">Hello</p>"));
    }

    @Test
    void givenHtmlWithThymeleafAttribute_whenValidateSyntax_shouldNotThrowErrors() {
        Assertions.assertThatCode(()-> validator.validateSyntax("<p th:text=\"${recipient.name}\">Alex</p>"))
                .doesNotThrowAnyException();
    }

    @Test
    void givenHtmlWithJavascriptUrl_whenValidateSyntax_shouldThrowInvalidTemplateException() {
        Assertions.assertThatExceptionOfType(InvalidTemplateHtmlException.class)
                .isThrownBy(()-> validator.validateSyntax("<a href=\"javascript:alert(1)\">link</a>"));
    }

    @Test
    void givenHtmlWithValidHref_whenValidateSyntax_shouldNotThrowErrors() {
        Assertions.assertThatCode(()-> validator.validateSyntax("<a href=\"https://algashop.com\">Link</a>"))
                .doesNotThrowAnyException();
    }

    // ---------- entrada ----------

    @Test
    void givenNullHtml_whenValidateSyntax_shouldThrowNullPointerException() {
        Assertions.assertThatNullPointerException()
                .isThrownBy(() -> validator.validateSyntax(null));
    }

    @Test
    void givenEmptyHtml_whenValidateSyntax_shouldThrowIllegalArgumentException() {
        Assertions.assertThatIllegalArgumentException()
                .isThrownBy(() -> validator.validateSyntax(""))
                .withMessage("html must not be empty");
    }

    // ---------- tags proibidas ----------

    static Stream<Arguments> forbiddenTags() {
        return Stream.of(
                Arguments.of("script", "<p>Hello</p><script>alert(1)</script>"),
                Arguments.of("script", "<p>Hello</p><SCRIPT>alert(1)</SCRIPT>"),
                Arguments.of("noscript", "<p>Hello</p><noscript>fallback</noscript>"),
                Arguments.of("iframe", "<p>Hello</p><iframe src=\"https://evil.com\"></iframe>"),
                Arguments.of("frameset", "<html><head></head><frameset><frame src=\"https://evil.com\"></frameset></html>"),
                Arguments.of("frame", "<html><head></head><frameset><frame src=\"https://evil.com\"></frameset></html>"),
                Arguments.of("object", "<p>Hello</p><object data=\"movie.swf\"></object>"),
                Arguments.of("embed", "<p>Hello</p><embed src=\"movie.swf\">"),
                Arguments.of("applet", "<p>Hello</p><applet code=\"Evil.class\"></applet>"),
                Arguments.of("base", "<html><head><base href=\"https://evil.com/\"></head><body><p>Hello</p></body></html>"),
                Arguments.of("form", "<form><p>Hello</p></form>"),
                Arguments.of("input", "<p>Hello</p><input type=\"password\" name=\"pwd\">"),
                Arguments.of("button", "<p>Hello</p><button>Send</button>"),
                Arguments.of("textarea", "<p>Hello</p><textarea>text</textarea>"),
                Arguments.of("select", "<p>Hello</p><select><option>1</option></select>")
        );
    }

    @ParameterizedTest(name = "<{0}> em {1}")
    @MethodSource("forbiddenTags")
    void givenHtmlWithForbiddenTag_whenValidateSyntax_shouldReportTagViolation(String tag, String html) {
        Assertions.assertThatExceptionOfType(InvalidTemplateHtmlException.class)
                .isThrownBy(() -> validator.validateSyntax(html))
                .withMessageContaining("Element <" + tag + "> is not allowed");
    }

    // ---------- atributos de evento ----------

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"onclick", "onload", "onerror", "onmouseover", "ONMOUSEOVER", "th:onclick"})
    void givenHtmlWithEventAttribute_whenValidateSyntax_shouldReportEventViolation(String attribute) {
        String html = "<p " + attribute + "=\"alert(1)\">Hello</p>";

        Assertions.assertThatExceptionOfType(InvalidTemplateHtmlException.class)
                .isThrownBy(() -> validator.validateSyntax(html))
                .withMessageContaining("Event attribute '" + attribute.toLowerCase() + "' on tag <p> is not allowed");
    }

    // ---------- atributos de URL ----------

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "href", "src", "action", "formaction", "background",
            "poster", "srcset", "data", "codebase", "cite", "longdesc",
            "lowsrc", "dynsrc", "xlink:href",
            "th:href", "th:src", "th:action", "th:background"
    })
    void givenUrlAttributeWithJavascriptScheme_whenValidateSyntax_shouldReportUrlViolation(String attribute) {
        String html = "<div " + attribute + "=\"javascript:alert(1)\">Hello</div>";

        Assertions.assertThatExceptionOfType(InvalidTemplateHtmlException.class)
                .isThrownBy(() -> validator.validateSyntax(html))
                .withMessageContaining("Script URL attribute '" + attribute + "' on tag <div> is not allowed");
    }

    @Test
    void givenNonUrlAttributeWithJavascriptText_whenValidateSyntax_shouldNotThrowErrors() {
        Assertions.assertThatCode(() -> validator.validateSyntax("<p title=\"javascript:alert(1)\">Hello</p>"))
                .doesNotThrowAnyException();
    }

    // ---------- esquemas de URL ----------

    @ParameterizedTest(name = "[{0}]")
    @ValueSource(strings = {
            "javascript:alert(1)",
            "vbscript:msgbox(1)",
            "livescript:alert(1)",
            "mocha:alert(1)",
            "data:text/html;base64,PHNjcmlwdD5hbGVydCgxKTwvc2NyaXB0Pg==",
            "data:image/svg+xml;base64,PHN2Zz48L3N2Zz4=",
            "data:application/javascript,alert(1)",
            "data:application/x-javascript,alert(1)",
            "JAVASCRIPT:alert(1)",
            "JavaScript:alert(1)",
            "   javascript:alert(1)",
            "java\tscript:alert(1)",
            "java&#x0A;script:alert(1)",
            "java&#x09;script:alert(1)"
    })
    void givenHrefWithForbiddenScheme_whenValidateSyntax_shouldReportUrlViolation(String url) {
        String html = "<a href=\"" + url + "\">Link</a>";

        Assertions.assertThatExceptionOfType(InvalidTemplateHtmlException.class)
                .isThrownBy(() -> validator.validateSyntax(html))
                .withMessageContaining("Script URL attribute 'href' on tag <a> is not allowed");
    }

    // ---------- casos permitidos ----------

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "<a href=\"https://algashop.com\">Link</a>",
            "<a href=\"http://algashop.com\">Link</a>",
            "<a href=\"mailto:contato@algashop.com\">Mail</a>",
            "<a href=\"/orders/123\">Pedido</a>",
            "<a href=\"#top\">Topo</a>",
            "<img src=\"cid:logo\" alt=\"logo\">",
            "<img src=\"data:image/png;base64,iVBORw0KGgo=\" alt=\"logo\">",
            "<img src=\"data:image/jpeg;base64,/9j/4AAQ=\" alt=\"logo\">",
            "<div data-order-id=\"123\">Pedido</div>",
            "<a th:href=\"@{/orders/{id}(id=${order.id})}\">Pedido</a>",
            "<p th:text=\"${recipient.name}\" th:if=\"${recipient != null}\">Alex</p>",
            "<p style=\"color: red\">Hello</p>",
            "<table><tr><td>Item</td></tr></table>"
    })
    void givenSafeHtml_whenValidateSyntax_shouldNotThrowErrors(String html) {
        Assertions.assertThatCode(() -> validator.validateSyntax(html)).doesNotThrowAnyException();
    }

    @Test
    void givenCompleteEmailDocument_whenValidateSyntax_shouldNotThrowErrors() {
        String html = """
                <!DOCTYPE html>
                <html xmlns:th="http://www.thymeleaf.org">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Pedido confirmado</title>
                    <style>body { font-family: Arial, sans-serif; }</style>
                </head>
                <body>
                    <table width="100%" cellpadding="0" cellspacing="0">
                        <tr>
                            <td><img src="cid:logo" alt="AlgaShop"></td>
                        </tr>
                        <tr>
                            <td>
                                <p th:text="${recipient.name}">Alex</p>
                                <a th:href="@{https://algashop.com/orders/{id}(id=${order.id})}">Ver pedido</a>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """;

        Assertions.assertThatCode(() -> validator.validateSyntax(html)).doesNotThrowAnyException();
    }

    // ---------- agregação e limite ----------

    @Test
    void givenHtmlWithManyViolations_whenValidateSyntax_shouldReportAllOfThem() {
        String html = "<p onclick=\"alert(1)\">Hello</p>"
                + "<a href=\"javascript:alert(1)\">Link</a>"
                + "<script>alert(1)</script>";

        Assertions.assertThatExceptionOfType(InvalidTemplateHtmlException.class)
                .isThrownBy(() -> validator.validateSyntax(html))
                .withMessageStartingWith("Invalid email template HTML: ")
                .withMessageContaining("Event attribute 'onclick' on tag <p> is not allowed")
                .withMessageContaining("Script URL attribute 'href' on tag <a> is not allowed")
                .withMessageContaining("Element <script> is not allowed")
                .withMessageContaining("; ");
    }

    @Test
    void givenHtmlWithMoreParseErrorsThanLimit_whenValidateSyntax_shouldReportAtMostTenParseErrors() {
        String html = "<p>Hello</p>" + "</div>".repeat(20);

        Assertions.assertThatExceptionOfType(InvalidTemplateHtmlException.class)
                .isThrownBy(() -> validator.validateSyntax(html))
                .satisfies(e -> {
                    String message = e.getMessage();
                    int occurrences = message.split("Malformed HTML at position", -1).length - 1;
                    Assertions.assertThat(occurrences).isEqualTo(10);
                });
    }

}
