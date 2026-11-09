package com.matchvagas.backend.security;

import com.matchvagas.backend.service.CustomUserDetailsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Segurança — filtro de autenticação JWT")
class JwtAuthenticationFilterTest {

    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock CustomUserDetailsService userDetailsService;
    @InjectMocks JwtAuthenticationFilter filter;

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    private MockFilterChain executar(String authorization) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (authorization != null) request.addHeader("Authorization", authorization);
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        return chain;
    }

    @Test
    @DisplayName("token válido autentica com o id do usuário e o perfil como authority")
    void autenticaTokenValido() throws Exception {
        when(jwtTokenProvider.validateToken("tok")).thenReturn(true);
        when(jwtTokenProvider.getEmailFromToken("tok")).thenReturn("ana@exemplo.com");
        when(jwtTokenProvider.getUserIdFromToken("tok")).thenReturn("42");
        when(userDetailsService.loadUserByUsername("ana@exemplo.com")).thenReturn(User.withUsername("ana@exemplo.com")
                .password("x").authorities(List.of(new SimpleGrantedAuthority("EMPRESA"))).build());

        MockFilterChain chain = executar("Bearer tok");

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getName()).isEqualTo("42");
        assertThat(auth.getAuthorities()).extracting(Object::toString).containsExactly("EMPRESA");
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("sem header, ou com esquema diferente de Bearer, segue sem autenticar")
    void semToken() throws Exception {
        executar(null);
        executar("Basic dXNlcjpzZW5oYQ==");

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtTokenProvider, userDetailsService);
    }

    @Test
    @DisplayName("token inválido segue sem autenticar")
    void tokenInvalido() throws Exception {
        when(jwtTokenProvider.validateToken("ruim")).thenReturn(false);

        MockFilterChain chain = executar("Bearer ruim");

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
        verifyNoInteractions(userDetailsService);
    }

    @Test
    @DisplayName("token válido de usuário desativado segue sem autenticar")
    void usuarioDesativado() throws Exception {
        when(jwtTokenProvider.validateToken("tok")).thenReturn(true);
        when(jwtTokenProvider.getEmailFromToken("tok")).thenReturn("ana@exemplo.com");
        when(userDetailsService.loadUserByUsername("ana@exemplo.com")).thenReturn(User.withUsername("ana@exemplo.com")
                .password("x").authorities(List.of(new SimpleGrantedAuthority("EMPRESA"))).disabled(true).build());

        MockFilterChain chain = executar("Bearer tok");

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("token de usuário que não existe mais segue sem autenticar")
    void usuarioRemovido() throws Exception {
        when(jwtTokenProvider.validateToken("tok")).thenReturn(true);
        when(jwtTokenProvider.getEmailFromToken("tok")).thenReturn("sumiu@exemplo.com");
        when(userDetailsService.loadUserByUsername(anyString()))
                .thenThrow(new UsernameNotFoundException("não encontrado"));

        MockFilterChain chain = executar("Bearer tok");

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }
}
