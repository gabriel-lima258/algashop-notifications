package com.algaworks.algashop.notifications.infrastructure.template;

import com.algaworks.algashop.notifications.application.email.EmailTemplateRenderer;
import com.algaworks.algashop.notifications.application.email.TemplateRenderException;
import org.springframework.expression.EvaluationException;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.exceptions.TemplateEngineException;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.util.Locale;
import java.util.Map;

/**
 * Adapter da porta EmailTemplateRenderer: preenche o assunto e o corpo de um template com as variaveis
 * do comando SendEmail, usando Thymeleaf fora do Spring MVC.
 *
 * - StringTemplateResolver: o template vem do banco como texto, nao de um arquivo em /templates.
 * - Duas engines: TEXT para o assunto (texto puro, sem tags) e HTML para o corpo (escapa as variaveis).
 * - assertRenderable: usado no cadastro do template; reprova erro de sintaxe e tolera falta de valor.
 */
@Component
public class EmailTemplateRendererThymeleafImpl implements EmailTemplateRenderer {

	// locale fixo: define moeda e separadores do #numbers (os contratos e templates estao em ingles)
	private static final Locale LOCALE = Locale.US;

	// uma engine por modo: o modo decide como o template e lido e se a saida e escapada como HTML
	private final TemplateEngine subjectTemplateEngine = createEngine(TemplateMode.TEXT);
	private final TemplateEngine bodyTemplateEngine = createEngine(TemplateMode.HTML);

	@Override
	public String renderSubject(String subjectTemplate, Map<String, Object> variables) {
		return process(variables, subjectTemplateEngine, subjectTemplate);
	}

	@Override
	public String renderBody(String bodyTemplate, Map<String, Object> variables) {
		return process(variables, bodyTemplateEngine, bodyTemplate);
	}

	// renderiza sem variaveis: erro de sintaxe reprova; erro de avaliacao (ex.: ${order.id} com order nulo)
	// so quer dizer que falta valor, e o valor so chega no envio. Assunto e corpo sao checados separados,
	// para a falta de valor no assunto nao esconder um erro de sintaxe no corpo.
	@Override
	public void assertRenderable(String subjectTemplate, String bodyTemplate) {
		assertRenderable(subjectTemplateEngine, subjectTemplate);
		assertRenderable(bodyTemplateEngine, bodyTemplate);
	}

	private void assertRenderable(TemplateEngine engine, String template) {
		try {
			process(Map.of(), engine, template);
		} catch (TemplateRenderException e) {
			if (!isEvaluationFailure(e)) {
				throw e;
			}
		}
	}

	private static TemplateEngine createEngine(TemplateMode mode) {
		StringTemplateResolver resolver = new StringTemplateResolver();
		resolver.setTemplateMode(mode);
		// sem cache: a chave seria o proprio texto do template, e cada versao editada ocuparia memoria
		resolver.setCacheable(false);

		SpringTemplateEngine engine = new SpringTemplateEngine();
		engine.setTemplateResolver(resolver);
		return engine;
	}

	private String process(Map<String, Object> variables, TemplateEngine engine, String template) {
		try {
			// o Context guarda os valores dinamicos que as expressoes ${...} vao ler
			Context context = new Context(LOCALE, variables);
			return engine.process(template, context);
		} catch (TemplateEngineException e) {
			throw new TemplateRenderException("Failed to render email: " + e.getMessage(), e);
		}
	}

	// o Thymeleaf embrulha a excecao do SpEL; procura a causa na cadeia
	private boolean isEvaluationFailure(TemplateRenderException e) {
		for (Throwable t = e; t != null; t = t.getCause()) {
			if (t instanceof EvaluationException) {
				return true;
			}
		}
		return false;
	}
}
