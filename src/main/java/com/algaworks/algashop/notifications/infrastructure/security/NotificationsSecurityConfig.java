package com.algaworks.algashop.notifications.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Resource server: quem EXIGE e VALIDA o token. O authorization server emite; este servico nunca ve
 * senha nem segredo, so a chave publica que baixa do /oauth2/jwks do issuer.
 *
 * O filtro so diz "tem que estar autenticado"; QUAL escopo cada endpoint exige fica no proprio metodo,
 * com as meta-anotacoes de SecurityAnnotations. @EnableMethodSecurity e o que liga esse @PreAuthorize:
 * sem ele as anotacoes ficam no codigo sem efeito nenhum - e nada avisa.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class NotificationsSecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				// API de maquina com token no header: sem cookie de sessao, nao ha o que o CSRF proteger
				.csrf(AbstractHttpConfigurer::disable)
				.cors(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/health/**", "/actuator/info/**").permitAll()
						.anyRequest().authenticated())
				// JWT validado pelo issuer-uri; o claim "scope" vira as autoridades SCOPE_<escopo>
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
		return http.build();
	}
}
