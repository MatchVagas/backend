package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.RegisterEmpresaRequestDTO;
import com.matchvagas.backend.entity.Empresas;
import com.matchvagas.backend.entity.Porte;
import com.matchvagas.backend.entity.RamoAtuacao;
import com.matchvagas.backend.entity.Usuarios;
import com.matchvagas.backend.entity.Usuarios.TipoUsuario;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.mapper.UsuarioMapper;
import com.matchvagas.backend.repository.EmpresaRepository;
import com.matchvagas.backend.repository.PorteRepository;
import com.matchvagas.backend.repository.RamoAtuacaoRepository;
import com.matchvagas.backend.repository.UsuariosRepository;
import com.matchvagas.backend.security.JwtTokenProvider;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RF001 — Cadastro de empresa")
class AuthServiceRegisterEmpresaTest {

    @Mock UsuariosRepository usuariosRepository;
    @Mock UsuarioMapper usuariosMapper;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock EmailVerificationService emailVerificationService;
    @Mock RefreshTokenService refreshTokenService;
    @Mock EmpresaRepository empresaRepository;
    @Mock PorteRepository porteRepository;
    @Mock RamoAtuacaoRepository ramoAtuacaoRepository;
    @InjectMocks AuthService authService;

    private RegisterEmpresaRequestDTO request(LocalDateTime nascimento, Boolean aceitou) {
        return new RegisterEmpresaRequestDTO("Carlos", "rh@tech.com", "Senha@123", nascimento,
                "11.222.333/0001-81", "Tech LTDA", "Tech", null, 1L, 2L, null, aceitou);
    }

    private void prepararCadastro() {
        when(usuariosRepository.existsByEmail("rh@tech.com")).thenReturn(false);
        when(empresaRepository.findByCnpjContainingIgnoreCase("11.222.333/0001-81")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Senha@123")).thenReturn("hash");
        when(usuariosRepository.save(any())).thenAnswer(inv -> {
            Usuarios u = inv.getArgument(0);
            u.setId(7L);
            return u;
        });
        when(porteRepository.findById(1L)).thenReturn(Optional.of(new Porte()));
        when(ramoAtuacaoRepository.findById(2L)).thenReturn(Optional.of(new RamoAtuacao()));
    }

    @Test
    @DisplayName("cria a empresa pendente, envia a confirmação de e-mail e não devolve token")
    void exigeConfirmacaoDeEmail() {
        prepararCadastro();

        var resposta = authService.registerEmpresa(request(LocalDateTime.of(1985, 3, 1, 0, 0), true));

        ArgumentCaptor<Usuarios> usuario = ArgumentCaptor.forClass(Usuarios.class);
        verify(usuariosRepository).save(usuario.capture());
        assertThat(usuario.getValue().getEmailVerificado()).isFalse();
        assertThat(usuario.getValue().getTipoUsuario()).isEqualTo(TipoUsuario.EMPRESA);
        verify(emailVerificationService).enviarVerificacao(usuario.getValue());

        ArgumentCaptor<Empresas> empresa = ArgumentCaptor.forClass(Empresas.class);
        verify(empresaRepository).save(empresa.capture());
        assertThat(empresa.getValue().getStatus()).isEqualTo(Empresas.StatusEmpresa.PENDENTE);

        assertThat(resposta.token()).isNull();
        assertThat(resposta.refreshToken()).isNull();
        assertThat(resposta.usuarioId()).isEqualTo(7L);
        assertThat(resposta.perfil()).isEqualTo("EMPRESA");
        verifyNoInteractions(jwtTokenProvider, refreshTokenService);
    }

    @Test
    @DisplayName("data de nascimento é opcional")
    void semDataDeNascimento() {
        prepararCadastro();

        authService.registerEmpresa(request(null, true));

        ArgumentCaptor<Usuarios> usuario = ArgumentCaptor.forClass(Usuarios.class);
        verify(usuariosRepository).save(usuario.capture());
        assertThat(usuario.getValue().getDataNascimento()).isNull();
        assertThat(usuario.getValue().getIdade()).isNull();
    }

    @Test
    @DisplayName("sem aceitar os termos, ou com e-mail ou CNPJ já cadastrados, não cria")
    void validacoes() {
        assertThatThrownBy(() -> authService.registerEmpresa(request(null, false)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("termos");

        when(usuariosRepository.existsByEmail("rh@tech.com")).thenReturn(true);
        assertThatThrownBy(() -> authService.registerEmpresa(request(null, true)))
                .hasMessageContaining("e-mail");

        when(usuariosRepository.existsByEmail("rh@tech.com")).thenReturn(false);
        when(empresaRepository.findByCnpjContainingIgnoreCase("11.222.333/0001-81"))
                .thenReturn(Optional.of(new Empresas()));
        assertThatThrownBy(() -> authService.registerEmpresa(request(null, true)))
                .hasMessageContaining("CNPJ");

        verify(usuariosRepository, never()).save(any());
        verifyNoInteractions(emailVerificationService);
    }
}
