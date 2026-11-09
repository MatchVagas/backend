package com.matchvagas.backend.service;

import com.matchvagas.backend.entity.Usuarios;
import com.matchvagas.backend.entity.Usuarios.TipoUsuario;
import com.matchvagas.backend.repository.UsuariosRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Segurança — carregamento do usuário para o Spring Security")
class CustomUserDetailsServiceTest {

    @Mock UsuariosRepository usuariosRepository;
    @InjectMocks CustomUserDetailsService service;

    private Usuarios usuario(TipoUsuario tipo, Boolean ativo) {
        Usuarios u = new Usuarios();
        u.setEmail("ana@exemplo.com");
        u.setSenha("$2a$hash");
        u.setTipoUsuario(tipo);
        u.setAtivo(ativo);
        return u;
    }

    @Test
    @DisplayName("usa o tipo do usuário como authority")
    void authorityPeloTipo() {
        when(usuariosRepository.findByEmail("ana@exemplo.com"))
                .thenReturn(Optional.of(usuario(TipoUsuario.ADMIN, true)));

        var details = service.loadUserByUsername("ana@exemplo.com");

        assertThat(details.getAuthorities()).extracting(Object::toString).containsExactly("ADMIN");
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.getPassword()).isEqualTo("$2a$hash");
    }

    @Test
    @DisplayName("sem tipo definido assume CANDIDATO")
    void semTipoAssumeCandidato() {
        when(usuariosRepository.findByEmail("ana@exemplo.com"))
                .thenReturn(Optional.of(usuario(null, true)));

        assertThat(service.loadUserByUsername("ana@exemplo.com").getAuthorities())
                .extracting(Object::toString).containsExactly("CANDIDATO");
    }

    @Test
    @DisplayName("usuário inativo fica desabilitado; ativo nulo (conta legada) segue habilitado, como no login")
    void inativoDesabilitado() {
        when(usuariosRepository.findByEmail("ana@exemplo.com"))
                .thenReturn(Optional.of(usuario(TipoUsuario.CANDIDATO, false)))
                .thenReturn(Optional.of(usuario(TipoUsuario.CANDIDATO, null)));

        assertThat(service.loadUserByUsername("ana@exemplo.com").isEnabled()).isFalse();
        assertThat(service.loadUserByUsername("ana@exemplo.com").isEnabled()).isTrue();
    }

    @Test
    @DisplayName("e-mail desconhecido lança UsernameNotFoundException")
    void emailDesconhecido() {
        when(usuariosRepository.findByEmail("x@exemplo.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("x@exemplo.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
