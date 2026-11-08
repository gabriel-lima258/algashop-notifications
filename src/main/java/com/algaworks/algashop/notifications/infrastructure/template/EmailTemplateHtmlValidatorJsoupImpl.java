package com.algaworks.algashop.notifications.infrastructure.template;

import com.algaworks.algashop.notifications.application.email.EmailTemplateHtmlValidator;
import com.algaworks.algashop.notifications.application.email.InvalidTemplateHtmlException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.ParseError;
import org.jsoup.parser.Parser;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Adapter da porta EmailTemplateHtmlValidator: valida o HTML de um template de e-mail na entrada,
 * antes de ele ser salvo, para que HTML quebrado ou perigoso nunca chegue a caixa do cliente.
 *
 * Faz duas checagens com o parser do jsoup:
 * 1. sintaxe: erros de parse (tag fechada sem abrir, etc.);
 * 2. seguranca: lista de bloqueio de tags, atributos de evento e URLs com esquema de script.
 *
 * Nao para no primeiro problema: junta todas as violacoes numa unica InvalidTemplateHtmlException,
 * para quem cadastra o template corrigir tudo de uma vez.
 */
@Component
public class EmailTemplateHtmlValidatorJsoupImpl implements EmailTemplateHtmlValidator {

	// quantos erros de parse o jsoup guarda para a mensagem; um erro ja basta para reprovar
	private static final int MAX_PARSE_ERRORS = 10;

	// tags que executam script, carregam conteudo externo ou coletam dados do destinatario
	private static final Set<String> FORBIDDEN_TAGS = Set.of(
			"script", "noscript", "iframe", "frame", "frameset",
			"object", "embed", "applet", "base",
			"form", "input", "button", "textarea", "select"
	);

	// atributos que carregam URL; os th:* sao os equivalentes Thymeleaf que viram os mesmos atributos no render
	private static final Set<String> URL_ATTRIBUTES = Set.of(
			"href", "src", "action", "formaction", "background",
			"poster", "srcset", "data", "codebase", "cite", "longdesc",
			"lowsrc", "dynsrc", "xlink:href",
			"th:href", "th:src", "th:action", "th:background"
	);

	// data:image/png e data:image/jpeg continuam liberados (imagem inline em e-mail)
	private static final Set<String> FORBIDDEN_URL_SCHEMES = Set.of(
			"javascript:", "vbscript:", "livescript:", "mocha:",
			"data:text/html", "data:image/svg+xml",
			"data:application/javascript", "data:application/x-javascript"
	);

	// onclick, onload... e o equivalente Thymeleaf th:onclick
	private static final Set<String> EVENT_ATTRIBUTE_PREFIXES = Set.of("on", "th:on");

	// espacos e caracteres de controle que o navegador ignora dentro do esquema (ex.: "java\tscript:")
	private static final Pattern IGNORED_URL_CHARS = Pattern.compile("[\\s\\p{Cntrl}]");

	@Override
	public void validateSyntax(String html) {
		Objects.requireNonNull(html);

		if (html.isEmpty()) {
			throw new IllegalArgumentException("html must not be empty");
		}

		// parser com rastreio de erros ligado: sem isso o jsoup corrige o HTML em silencio
		Parser parser = Parser.htmlParser().setTrackErrors(MAX_PARSE_ERRORS);

		List<String> violations = new ArrayList<>();
		Document document = Jsoup.parse(html, parser);

		for (ParseError error : parser.getErrors()) {
			violations.add(String.format("Malformed HTML at position %s: %s",
					error.getPosition(), error.getErrorMessage()
			));
		}

		for (Element element : document.getAllElements()) {
			checkElement(element, violations);
		}

		if (!violations.isEmpty()) {
			throw new InvalidTemplateHtmlException(violations);
		}
	}

	private void checkElement(Element element, List<String> violations) {
		String tag = element.normalName();
		if (FORBIDDEN_TAGS.contains(tag)) {
			violations.add(String.format("Element <%s> is not allowed", tag));
		}
		for (Attribute attribute : element.attributes()) {
			String name = attribute.getKey().toLowerCase(Locale.ROOT);
			if (isEventAttribute(name)) {
				violations.add(String.format("Event attribute '%s' on tag <%s> is not allowed", name, tag));
			} else if (URL_ATTRIBUTES.contains(name)
					&& hasForbiddenScheme(attribute.getValue())) {
				violations.add(String.format("Script URL attribute '%s' on tag <%s> is not allowed", name, tag));
			}
		}
	}

	private boolean isEventAttribute(String name) {
		return EVENT_ATTRIBUTE_PREFIXES.stream().anyMatch(name::startsWith);
	}

	// o jsoup ja decodifica entidades (&#x09;); aqui tiramos espacos/controles e comparamos em minusculas
	private boolean hasForbiddenScheme(String value) {
		String normalized = IGNORED_URL_CHARS.matcher(value).replaceAll("").toLowerCase(Locale.ROOT);
		return FORBIDDEN_URL_SCHEMES.stream().anyMatch(normalized::startsWith);
	}
}
