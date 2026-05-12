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

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // ← habilita @PreAuthorize, @PostAuthorize, @Secured
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz

                // ── Público — sem token ───────────────────────────────────
                .requestMatchers("/api/auth/**").permitAll()

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
