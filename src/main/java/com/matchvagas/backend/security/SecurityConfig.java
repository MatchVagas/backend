package com.matchvagas.backend.security;

import com.matchvagas.backend.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // ← habilita @PreAuthorize, @PostAuthorize, @Secured
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    // ── Configuração de CORS (SEC-08) ──────────────────────────────────────────
    // Em produção, defina CORS_ALLOWED_ORIGINS com a lista explícita de domínios
    // (ex.: https://app.matchvagas.com) e, se precisar de cookies, CORS_ALLOW_CREDENTIALS=true.
    @Value("${cors.allowed-origins:*}")
    private String allowedOrigins;
    @Value("${cors.allowed-methods:GET,POST,PUT,PATCH,DELETE,OPTIONS}")
    private String allowedMethods;
    @Value("${cors.allowed-headers:Content-Type,Authorization}")
    private String allowedHeaders;
    @Value("${cors.allow-credentials:false}")
    private boolean allowCredentials;
    @Value("${cors.max-age:3600}")
    private long maxAge;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            // ── Headers de segurança HTTP (SEC-10) ────────────────────────────
            .headers(headers -> headers
                    .frameOptions(frame -> frame.deny())                 // anti-clickjacking (X-Frame-Options)
                    .contentTypeOptions(opts -> {})                      // X-Content-Type-Options: nosniff
                    .httpStrictTransportSecurity(hsts -> hsts            // força HTTPS (apenas em conexões seguras)
                            .includeSubDomains(true)
                            .maxAgeInSeconds(31536000))
                    .referrerPolicy(ref -> ref.policy(
                            org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter
                                    .ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)))
            .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz

                // ── Público — sem token ───────────────────────────────────
                .requestMatchers("/api/auth/**").permitAll()

                // Actuator: health é público (health check do provedor); o resto é ADMIN.
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/actuator/**").hasAuthority("ADMIN")

                // Swagger UI — todos os caminhos que o Springdoc usa
                .requestMatchers(
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs",
                    "/v3/api-docs/**",
                    "/v3/api-docs.yaml",
                    "/swagger-resources",
                    "/swagger-resources/**",
                    "/webjars/**"
                ).permitAll()

                // Vagas — leitura pública (RF007)
                .requestMatchers(HttpMethod.GET, "/api/vagas/**").permitAll()

                // Empresas — leitura pública
                .requestMatchers(HttpMethod.GET, "/api/empresas/**").permitAll()

                // Localização — leitura pública (País, Estado, Cidade)
                .requestMatchers(HttpMethod.GET, "/api/localizacao/**").permitAll()

                // Lookup — leitura pública (TipoVaga, Modalidade, etc.)
                .requestMatchers(HttpMethod.GET, "/api/lookup/**").permitAll()

                // ── Somente ADMIN ─────────────────────────────────────────
                // Administração global
                .requestMatchers("/api/admin/**").hasAuthority("ADMIN")

                // Gerenciamento de usuários — restrito a ADMIN (SEC-11)
                .requestMatchers("/api/usuarios/**").hasAuthority("ADMIN")

                // Escrita nos lookups e localização
                .requestMatchers(HttpMethod.POST,   "/api/localizacao/**").hasAuthority("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/localizacao/**").hasAuthority("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/localizacao/**").hasAuthority("ADMIN")
                .requestMatchers(HttpMethod.POST,   "/api/lookup/**").hasAuthority("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/lookup/**").hasAuthority("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/lookup/**").hasAuthority("ADMIN")

                // ── Empresas — escrita restrita a EMPRESA ou ADMIN ────────
                .requestMatchers(HttpMethod.POST,   "/api/empresas/**").hasAnyAuthority("EMPRESA", "ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/empresas/**").hasAnyAuthority("EMPRESA", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/empresas/**").hasAnyAuthority("EMPRESA", "ADMIN")

                // ── Vagas — escrita restrita a EMPRESA ou ADMIN ───────────
                .requestMatchers(HttpMethod.POST,   "/api/vagas/**").hasAnyAuthority("EMPRESA", "ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/vagas/**").hasAnyAuthority("EMPRESA", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/vagas/**").hasAnyAuthority("EMPRESA", "ADMIN")

                // ── Candidatos — acesso restrito a CANDIDATO ─────────────
                .requestMatchers("/api/candidatos/**").hasAuthority("CANDIDATO")

                // ── Candidaturas — visualização por empresa restrita a EMPRESA ──
                .requestMatchers(HttpMethod.GET, "/api/candidaturas/empresa").hasAuthority("EMPRESA")
                .requestMatchers(HttpMethod.POST, "/api/candidaturas/**").hasAuthority("CANDIDATO")

                // ── Qualquer outro endpoint requer autenticação ───────────
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> origins = parseCsv(allowedOrigins);
        boolean wildcard = origins.contains("*");

        CorsConfiguration config = new CorsConfiguration();

        if (wildcard) {
            // A spec do CORS PROÍBE "*" junto com credenciais. Como a API é
            // baseada em JWT no header Authorization (sem cookies), o uso seguro
            // do curinga é com credenciais DESLIGADAS — assim nenhuma origem
            // arbitrária consegue fazer requisições autenticadas por cookie.
            config.setAllowedOriginPatterns(List.of("*"));
            config.setAllowCredentials(false);
        } else {
            // Lista explícita de origens confiáveis: credenciais podem ser ligadas.
            config.setAllowedOrigins(origins);
            config.setAllowCredentials(allowCredentials);
        }

        config.setAllowedMethods(parseCsv(allowedMethods));
        config.setAllowedHeaders(parseCsv(allowedHeaders));
        config.setMaxAge(maxAge);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private static List<String> parseCsv(String csv) {
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
