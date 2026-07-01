package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.*;
import com.matchvagas.backend.entity.Empresas;
import com.matchvagas.backend.entity.Usuarios;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.mapper.UsuarioMapper;
import com.matchvagas.backend.repository.EmpresaRepository;
import com.matchvagas.backend.repository.PorteRepository;
import com.matchvagas.backend.repository.RamoAtuacaoRepository;
import com.matchvagas.backend.repository.UsuariosRepository;
import com.matchvagas.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuariosRepository usuariosRepository;
    private final UsuarioMapper usuariosMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmpresaRepository empresaRepository;
    private final PorteRepository porteRepository;
    private final RamoAtuacaoRepository ramoAtuacaoRepository;
    private final EmailVerificationService emailVerificationService;

    /**
     * RF001 - Cadastro de novo usuário (Candidato, Empresa ou Admin)
     */
    @Transactional
    public UsuarioResponseDTO register(UsuariosRequestDTO request) {

        if (!Boolean.TRUE.equals(request.aceitouTermos())) {
            throw new BusinessException("É necessário aceitar os termos de uso e a política de privacidade.");
        }

        if (usuariosRepository.existsByEmail(request.email())) {
            throw new BusinessException("Já existe um usuário cadastrado com este email.");
        }

        Usuarios usuario = usuariosMapper.toEntity(request);
        usuario.setSenha(passwordEncoder.encode(request.senha()));
        // SEC: cadastro público SEMPRE cria CANDIDATO. O papel jamais é definido
        // pelo cliente — empresas usam /register-empresa e ADMIN só pelo canal
        // restrito (/api/usuarios, protegido por hasAuthority('ADMIN')).
        usuario.setTipoUsuario(Usuarios.TipoUsuario.CANDIDATO);
        usuario.registrarConsentimento();
        // Conta nasce não verificada: o login fica bloqueado até confirmar o e-mail.
        usuario.setEmailVerificado(false);

        // Calcula idade a partir da dataNascimento
        if (request.dataNascimento() != null) {
            int idade = java.time.Period.between(
                request.dataNascimento().toLocalDate(),
                java.time.LocalDate.now()
            ).getYears();
            usuario.setIdade(idade);
        }

        // EMPRESA users começam ativos; a aprovação é controlada por empresa.status (PENDENTE/APROVADA)
        // A barreira para publicar vagas fica em VagaService — não no login

        Usuarios salvo = usuariosRepository.save(usuario);
        emailVerificationService.enviarVerificacao(salvo);
        return usuariosMapper.toDTO(salvo);
    }

    /**
     * Cadastro atômico de empresa: cria usuário + empresa numa única transação.
     * Se a criação da empresa falhar por qualquer motivo, o usuário também é revertido.
     */
    @Transactional
    public AuthResponse registerEmpresa(RegisterEmpresaRequestDTO dto) {
        if (!Boolean.TRUE.equals(dto.aceitouTermos())) {
            throw new BusinessException("É necessário aceitar os termos de uso e a política de privacidade.");
        }
        if (usuariosRepository.existsByEmail(dto.email())) {
            throw new BusinessException("Já existe um usuário cadastrado com este e-mail.");
        }
        if (empresaRepository.findByCnpjContainingIgnoreCase(dto.cnpj()).isPresent()) {
            throw new BusinessException("Já existe uma empresa cadastrada com este CNPJ.");
        }

        // 1. Cria o usuário
        Usuarios usuario = new Usuarios();
        usuario.setNome(dto.nome());
        usuario.setEmail(dto.email());
        usuario.setSenha(passwordEncoder.encode(dto.senha()));
        usuario.setTipoUsuario(Usuarios.TipoUsuario.EMPRESA);
        usuario.setAtivo(true);
        usuario.registrarConsentimento();
        if (dto.dataNascimento() != null) {
            usuario.setDataNascimento(
                java.util.Date.from(dto.dataNascimento()
                    .atZone(java.time.ZoneId.systemDefault()).toInstant()));
            usuario.setIdade(java.time.Period.between(
                dto.dataNascimento().toLocalDate(),
                java.time.LocalDate.now()).getYears());
        }
        Usuarios usuarioSalvo = usuariosRepository.save(usuario);

        // 2. Cria a empresa vinculada ao usuário (lança exceção se porte/ramo não existirem)
        Empresas empresa = new Empresas();
        empresa.setUsuario(usuarioSalvo);
        empresa.setCnpj(dto.cnpj());
        empresa.setRazaoSocial(dto.razaoSocial());
        empresa.setNomeFantasia(dto.nomeFantasia());
        empresa.setDescricao(dto.descricao());
        empresa.setSite(dto.site());
        empresa.setStatus(Empresas.StatusEmpresa.PENDENTE);
        empresa.setPorte(porteRepository.findById(dto.porteId())
                .orElseThrow(() -> new ResourceNotFoundException("Porte não encontrado")));
        empresa.setRamoAtuacao(ramoAtuacaoRepository.findById(dto.ramoId())
                .orElseThrow(() -> new ResourceNotFoundException("Ramo de atuação não encontrado")));
        empresaRepository.save(empresa);

        // 3. Gera token e retorna — app já entra autenticado
        String token = jwtTokenProvider.generateToken(usuarioSalvo);
        return new AuthResponse(token, "Bearer", usuarioSalvo.getId(),
                usuarioSalvo.getNome(), usuarioSalvo.getEmail(), "EMPRESA");
    }

    /**
     * RF002 - Autenticação (Login)
     */
    public AuthResponse login(LoginRequestDTO request) {

        Usuarios usuario = usuariosRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Email ou senha inválidos."));

        if (!passwordEncoder.matches(request.senha(), usuario.getSenha())) {
            throw new BadCredentialsException("Email ou senha inválidos.");
        }

        if (Boolean.FALSE.equals(usuario.getAtivo())) {
            throw new BadCredentialsException("Usuário está inativo. Contate o administrador.");
        }

        // E-mail não verificado bloqueia o login. NULO = conta legada (verificada).
        if (Boolean.FALSE.equals(usuario.getEmailVerificado())) {
            throw new BadCredentialsException(
                    "E-mail não verificado. Confira sua caixa de entrada ou solicite um novo link.");
        }

        String token = jwtTokenProvider.generateToken(usuario);

        // Usa o tipoUsuario real — não mais hardcoded
        String perfil = usuario.getTipoUsuario() != null
                ? usuario.getTipoUsuario().name()
                : "CANDIDATO";

        return new AuthResponse(
                token,
                "Bearer",
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                perfil
        );
    }
}
