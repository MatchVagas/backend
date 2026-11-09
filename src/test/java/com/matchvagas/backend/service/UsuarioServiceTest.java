package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.UsuarioResponseDTO;
import com.matchvagas.backend.dto.UsuariosRequestDTO;
import com.matchvagas.backend.entity.Usuarios;
import com.matchvagas.backend.entity.Usuarios.TipoUsuario;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.mapper.UsuarioMapper;
import com.matchvagas.backend.repository.UsuariosRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Admin — gestão de usuários")
class UsuarioServiceTest {

    @Mock UsuariosRepository repository;
    @Mock UsuarioMapper mapper;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks UsuarioService service;

    private UsuariosRequestDTO dto(String email, String senha, TipoUsuario tipo, Boolean aceitou,
                                   LocalDateTime nascimento) {
        return new UsuariosRequestDTO("Ana", email, senha, nascimento, tipo, true, aceitou);
    }

    private Usuarios usuario(String email) {
        Usuarios u = new Usuarios();
        u.setId(1L);
        u.setEmail(email);
        return u;
    }

    @Test
    @DisplayName("cria com senha cifrada, papel definido, consentimento LGPD e idade calculada")
    void cria() {
        var request = dto("ana@x.com", "Senha@123", TipoUsuario.ADMIN, true, LocalDateTime.now().minusYears(30));
        Usuarios entidade = new Usuarios();
        when(repository.existsByEmail("ana@x.com")).thenReturn(false);
        when(mapper.toEntity(request)).thenReturn(entidade);
        when(passwordEncoder.encode("Senha@123")).thenReturn("hash");
        when(repository.save(entidade)).thenReturn(entidade);
        when(mapper.toResponseDTO(entidade)).thenReturn(mock(UsuarioResponseDTO.class));

        service.create(request);

        assertThat(entidade.getSenha()).isEqualTo("hash");
        assertThat(entidade.getTipoUsuario()).isEqualTo(TipoUsuario.ADMIN);
        assertThat(entidade.getConsentimentoLgpdEm()).isNotNull();
        assertThat(entidade.getIdade()).isEqualTo(30);
    }

    @Test
    @DisplayName("sem aceitar os termos não cria")
    void exigeTermos() {
        assertThatThrownBy(() -> service.create(dto("a@x.com", "s", TipoUsuario.CANDIDATO, false, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("termos");
        assertThatThrownBy(() -> service.create(dto("a@x.com", "s", TipoUsuario.CANDIDATO, null, null)))
                .isInstanceOf(BusinessException.class);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("e-mail já cadastrado não cria")
    void emailDuplicado() {
        when(repository.existsByEmail("a@x.com")).thenReturn(true);

        assertThatThrownBy(() -> service.create(dto("a@x.com", "s", TipoUsuario.CANDIDATO, true, null)))
                .hasMessageContaining("Email já cadastrado");
    }

    @Test
    @DisplayName("atualiza: troca de e-mail para um em uso é rejeitada")
    void atualizaEmailEmUso() {
        when(repository.findById(1L)).thenReturn(Optional.of(usuario("ana@x.com")));
        when(repository.existsByEmail("bia@x.com")).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, dto("bia@x.com", null, null, true, null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("atualiza: mantém a senha quando vem em branco e troca o papel quando informado")
    void atualizaSenhaEmBranco() {
        Usuarios existente = usuario("ana@x.com");
        existente.setSenha("hash-antigo");
        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(repository.save(existente)).thenReturn(existente);

        service.update(1L, dto("ana@x.com", "  ", TipoUsuario.EMPRESA, true, null));

        ArgumentCaptor<Usuarios> captor = ArgumentCaptor.forClass(Usuarios.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getSenha()).isEqualTo("hash-antigo");
        assertThat(captor.getValue().getTipoUsuario()).isEqualTo(TipoUsuario.EMPRESA);
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("busca, remoção e registro de acesso de usuário inexistente retornam 404")
    void naoEncontrado() {
        when(repository.findById(9L)).thenReturn(Optional.empty());
        when(repository.findByEmail("x@x.com")).thenReturn(Optional.empty());
        when(repository.existsById(9L)).thenReturn(false);

        assertThatThrownBy(() -> service.findById(9L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.findByEmail("x@x.com")).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.delete(9L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.registrarAcesso("x@x.com")).isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).deleteById(any());
    }

    @Test
    @DisplayName("remove usuário existente e registra o último acesso")
    void removeERegistraAcesso() {
        Usuarios ana = usuario("ana@x.com");
        when(repository.existsById(1L)).thenReturn(true);
        when(repository.findByEmail("ana@x.com")).thenReturn(Optional.of(ana));

        service.delete(1L);
        service.registrarAcesso("ana@x.com");

        verify(repository).deleteById(1L);
        assertThat(ana.getDataUltimoAcesso()).isNotNull();
    }
}
