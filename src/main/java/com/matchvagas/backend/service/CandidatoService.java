package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.CandidatoRequestDTO;
import com.matchvagas.backend.dto.CandidatoResponseDTO;
import com.matchvagas.backend.dto.LocalizacaoRequestDTO;
import com.matchvagas.backend.dto.MeusDadosExportDTO;
import com.matchvagas.backend.dto.TelefonesRequestDTO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import com.matchvagas.backend.entity.Candidatos;
import com.matchvagas.backend.entity.Candidatura;
import com.matchvagas.backend.entity.Endereco;
import com.matchvagas.backend.entity.Telefones;
import com.matchvagas.backend.entity.TipoTelefone;
import com.matchvagas.backend.entity.Usuarios;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.mapper.CandidatoMapper;
import com.matchvagas.backend.repository.CandidatoRepository;
import com.matchvagas.backend.repository.CandidaturaRepository;
import com.matchvagas.backend.repository.MensagemRepository;
import com.matchvagas.backend.repository.ExperienciaRepository;
import com.matchvagas.backend.repository.FormacaoRepository;
import com.matchvagas.backend.repository.HistoricoStatusCandidaturaRepository;
import com.matchvagas.backend.repository.NotificacaoRepository;
import com.matchvagas.backend.repository.PasswordResetTokenRepository;
import com.matchvagas.backend.repository.TelefoneRepository;
import com.matchvagas.backend.repository.TipoTelefoneRepository;
import com.matchvagas.backend.repository.UsuariosRepository;
import com.matchvagas.backend.util.CpfCrypto;
import com.matchvagas.backend.service.embedding.IndexacaoEmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CandidatoService {

    private final CandidatoRepository candidatoRepository;
    private final UsuariosRepository usuariosRepository;
    private final CandidatoMapper candidatoMapper;
    private final TelefoneRepository telefoneRepository;
    private final TipoTelefoneRepository tipoTelefoneRepository;
    private final CandidaturaRepository candidaturaRepository;
    private final MensagemRepository mensagemRepository;
    private final HistoricoStatusCandidaturaRepository historicoRepository;
    private final NotificacaoRepository notificacaoRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final CurriculoService curriculoService;
    private final FotoPerfilService fotoPerfilService;
    private final ExperienciaRepository experienciaRepository;
    private final FormacaoRepository formacaoRepository;
    private final SupabaseStorageService supabaseStorageService;
    private final IndexacaoEmbeddingService indexacaoEmbeddingService;
    private final AposCommitExecutor aposCommitExecutor;

    private static final int URL_FOTO_EXPIRACAO_SEGUNDOS = 3600; // 1 hora

    // RF003 — Buscar perfil do candidato pelo ID do usuário autenticado
    @Transactional(readOnly = true)
    public CandidatoResponseDTO findByUsuarioId(Long usuarioId) {
        Candidatos candidato = candidatoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Perfil de candidato não encontrado para o usuário ID: " + usuarioId));
        return comFotoAssinada(candidatoMapper.toResponseDTO(candidato));
    }

    @Transactional(readOnly = true)
    public CandidatoResponseDTO findById(Long id) {
        Candidatos candidato = candidatoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidato não encontrado com ID: " + id));
        return comFotoAssinada(candidatoMapper.toResponseDTO(candidato));
    }

    // RF003 — Criar perfil de candidato vinculado ao usuário autenticado
    @Transactional
    public CandidatoResponseDTO create(Long usuarioId, CandidatoRequestDTO dto) {
        if (candidatoRepository.findByUsuarioId(usuarioId).isPresent()) {
            throw new BusinessException("Já existe um perfil de candidato para este usuário.");
        }

        if (dto.cpf() != null && !dto.cpf().isBlank()
                && candidatoRepository.findByCpfHash(CpfCrypto.hash(dto.cpf())).isPresent()) {
            throw new BusinessException("CPF já cadastrado para outro candidato.");
        }

        Usuarios usuario = usuariosRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + usuarioId));

        atualizarDadosPessoais(dto, usuario);

        Candidatos candidato = candidatoMapper.toEntity(dto);
        candidato.setUsuario(usuario);

        if (dto.localizacao() != null) {
            candidato.setEndereco(toEndereco(dto.localizacao()));
        }

        if (dto.telefone() != null) {
            vincularTelefone(dto.telefone(), usuario);
        }

        Candidatos salvo = candidatoRepository.save(candidato);
        aposCommitExecutor.executar(() -> indexacaoEmbeddingService.indexarCandidato(salvo));
        return comFotoAssinada(candidatoMapper.toResponseDTO(salvo));
    }

    // RF003 — Atualizar perfil do candidato
    @Transactional
    public CandidatoResponseDTO update(Long usuarioId, CandidatoRequestDTO dto) {
        Candidatos candidato = candidatoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Perfil de candidato não encontrado para o usuário ID: " + usuarioId));

        atualizarDadosPessoais(dto, candidato.getUsuario());

        if (dto.cpf() != null && !dto.cpf().isBlank()) {
            candidatoRepository.findByCpfHash(CpfCrypto.hash(dto.cpf()))
                    .filter(c -> !c.getId().equals(candidato.getId()))
                    .ifPresent(c -> { throw new BusinessException("CPF já cadastrado para outro candidato."); });
            candidato.setCpf(dto.cpf());
        }
        candidato.setObjetivoProfissional(dto.resumoProfissional());
        candidato.setDisponibilidade(dto.disponibilidade());
        candidato.setPretensaoSalarial(dto.pretensaoSalarial());
        if (dto.genero() != null) candidato.setGenero(dto.genero());

        if (dto.localizacao() != null) {
            if (candidato.getEndereco() == null) {
                candidato.setEndereco(toEndereco(dto.localizacao()));
            } else {
                atualizarEndereco(candidato.getEndereco(), dto.localizacao());
            }
        }

        if (dto.telefone() != null) {
            vincularTelefone(dto.telefone(), candidato.getUsuario());
        }

        Candidatos salvo = candidatoRepository.save(candidato);
        aposCommitExecutor.executar(() -> indexacaoEmbeddingService.indexarCandidato(salvo));
        return comFotoAssinada(candidatoMapper.toResponseDTO(salvo));
    }

    /**
     * LGPD Art. 18, V — Portabilidade / acesso facilitado.
     * Exporta todos os dados pessoais do candidato em formato estruturado.
     */
    @Transactional(readOnly = true)
    public MeusDadosExportDTO exportarDados(Long usuarioId) {
        Candidatos candidato = candidatoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Perfil de candidato não encontrado para o usuário ID: " + usuarioId));
        Usuarios usuario = candidato.getUsuario();

        MeusDadosExportDTO.Usuario usuarioDto = new MeusDadosExportDTO.Usuario(
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getDataCadastro(),
                usuario.getIdade(),
                usuario.getConsentimentoLgpdEm(),
                usuario.getVersaoPoliticaPrivacidade());

        MeusDadosExportDTO.Endereco enderecoDto = null;
        Endereco e = candidato.getEndereco();
        if (e != null) {
            enderecoDto = new MeusDadosExportDTO.Endereco(
                    e.getLogradouro(), e.getNumero(), e.getCompleto(),
                    e.getBairro(), e.getCidade(), e.getEstado(), e.getCep());
        }

        List<MeusDadosExportDTO.Habilidade> habilidades = candidato.getHabilidades() == null
                ? List.of()
                : candidato.getHabilidades().stream()
                        .map(h -> new MeusDadosExportDTO.Habilidade(
                                h.getNome(), h.getNivel() != null ? h.getNivel().name() : null))
                        .toList();

        List<MeusDadosExportDTO.Telefone> telefones = usuario.getTelefones() == null
                ? List.of()
                : usuario.getTelefones().stream()
                        .map(t -> new MeusDadosExportDTO.Telefone(
                                t.getNumero(), t.isWpp(),
                                t.getTipoTelefone() != null ? t.getTipoTelefone().getNome() : null))
                        .toList();

        MeusDadosExportDTO.Candidato candidatoDto = new MeusDadosExportDTO.Candidato(
                candidato.getCpf(),
                candidato.getObjetivoProfissional(),
                candidato.getDisponibilidade(),
                candidato.getPretensaoSalarial(),
                candidato.getGenero() != null ? candidato.getGenero().name() : null,
                enderecoDto,
                habilidades,
                telefones);

        List<MeusDadosExportDTO.Experiencia> experiencias =
                experienciaRepository.findByCandidatoId(candidato.getId()).stream()
                        .map(x -> new MeusDadosExportDTO.Experiencia(
                                x.getEmpresa(), x.getCargo(), x.getDescricao(),
                                x.getDataInicio(), x.getDataFim()))
                        .toList();

        List<MeusDadosExportDTO.Formacao> formacoes =
                formacaoRepository.findByCandidatoId(candidato.getId()).stream()
                        .map(f -> new MeusDadosExportDTO.Formacao(
                                f.getInstituicao(), f.getCurso(), f.getNivel(),
                                f.getDataInicio(), f.getDataFim()))
                        .toList();

        List<MeusDadosExportDTO.Candidatura> candidaturas =
                candidaturaRepository.findByCandidatoId(candidato.getId()).stream()
                        .map(c -> new MeusDadosExportDTO.Candidatura(
                                c.getVaga() != null ? c.getVaga().getTitulo() : null,
                                c.getVaga() != null && c.getVaga().getEmpresas() != null
                                        ? c.getVaga().getEmpresas().getNomeFantasia() : null,
                                c.getStatus() != null ? c.getStatus().getStatus() : null,
                                c.getDataCandidatura()))
                        .toList();

        return new MeusDadosExportDTO(
                LocalDateTime.now(), usuarioDto, candidatoDto,
                experiencias, formacoes, candidaturas);
    }

    /**
     * LGPD Art. 18, VI — Direito ao esquecimento.
     * Exclui de forma permanente a conta do candidato e todos os dados associados:
     * candidaturas e seu histórico, currículo e foto no Supabase, notificações,
     * tokens de redefinição de senha, endereço, telefones e o próprio usuário.
     */
    @Transactional
    public void excluirConta(Long usuarioId) {
        Candidatos candidato = candidatoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Perfil de candidato não encontrado para o usuário ID: " + usuarioId));

        // 1. Candidaturas + conversas + histórico de status (FKs para candidatura)
        List<Candidatura> candidaturas = candidaturaRepository.findByCandidatoId(candidato.getId());
        for (Candidatura candidatura : candidaturas) {
            mensagemRepository.deleteByCandidaturaId(candidatura.getId());
            historicoRepository.deleteAll(
                    historicoRepository.findByCandidaturaIdOrderByDataHoraDesc(candidatura.getId()));
        }
        candidaturaRepository.deleteAll(candidaturas);

        // 2. Arquivos no Supabase (currículo e foto), removidos via serviços existentes
        if (candidato.getCurriculo() != null) {
            curriculoService.deletar(usuarioId);
        }
        if (candidato.getFotoPerfilUrl() != null) {
            fotoPerfilService.deletarCandidato(usuarioId);
        }

        // 3. Notificações e tokens de redefinição de senha do usuário
        notificacaoRepository.deleteAll(
                notificacaoRepository.findByUsuarioIdOrderByDataEnvioDesc(usuarioId));
        passwordResetTokenRepository.deleteByUsuarioId(usuarioId);

        // 4. Remove o candidato — cascateia endereço, currículo e o próprio usuário
        candidatoRepository.delete(candidato);
    }

    /**
     * Substitui o object path da foto (armazenado na entidade) por uma URL
     * assinada temporária, já que o bucket de imagens é privado (LGPD-08).
     */
    private CandidatoResponseDTO comFotoAssinada(CandidatoResponseDTO dto) {
        if (dto.fotoPerfilUrl() == null || dto.fotoPerfilUrl().isBlank()) {
            return dto;
        }
        String url = supabaseStorageService.gerarUrlAssinadaImagem(
                dto.fotoPerfilUrl(), URL_FOTO_EXPIRACAO_SEGUNDOS);
        return new CandidatoResponseDTO(
                dto.id(), dto.nome(), dto.email(), dto.dataNascimento(), dto.cpf(),
                dto.objetivoProfissional(), dto.disponibilidade(), dto.pretensaoSalarial(),
                dto.genero(), url, dto.telefone(), dto.localizacao());
    }

    private void atualizarDadosPessoais(CandidatoRequestDTO dto, Usuarios usuario) {
        if (dto.nomeCompleto() != null && !dto.nomeCompleto().isBlank()) {
            usuario.setNome(dto.nomeCompleto());
        }
        if (dto.email() != null && !dto.email().isBlank() && !dto.email().equalsIgnoreCase(usuario.getEmail())) {
            if (usuariosRepository.existsByEmail(dto.email())) {
                throw new BusinessException("Já existe um usuário cadastrado com este email.");
            }
            usuario.setEmail(dto.email());
        }
        if (dto.dataNascimento() != null) {
            usuario.setDataNascimento(java.sql.Date.valueOf(dto.dataNascimento()));
            usuario.setIdade(Period.between(dto.dataNascimento(), LocalDate.now()).getYears());
        }
    }

    private Endereco toEndereco(LocalizacaoRequestDTO loc) {
        return new Endereco(
                loc.logradouro(),
                loc.numero(),
                loc.complemento() != null ? loc.complemento() : "",
                loc.estado(),
                loc.cidade(),
                loc.bairro(),
                loc.cep()
        );
    }

    private void atualizarEndereco(Endereco endereco, LocalizacaoRequestDTO loc) {
        endereco.setLogradouro(loc.logradouro());
        endereco.setNumero(loc.numero());
        endereco.setCompleto(loc.complemento() != null ? loc.complemento() : "");
        endereco.setEstado(loc.estado());
        endereco.setCidade(loc.cidade());
        endereco.setBairro(loc.bairro());
        endereco.setCep(loc.cep());
    }

    private void vincularTelefone(TelefonesRequestDTO dto, Usuarios usuario) {
        TipoTelefone tipo = tipoTelefoneRepository.findById(dto.tipoTelefoneId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tipo de telefone não encontrado: " + dto.tipoTelefoneId()));

        Telefones telefone = telefoneRepository.findByNumero(dto.numero())
                .orElseGet(() -> {
                    Telefones novo = new Telefones();
                    novo.setNumero(dto.numero());
                    novo.setTipoTelefone(tipo);
                    novo.setWpp(dto.wpp());
                    return telefoneRepository.save(novo);
                });

        if (usuario.getTelefones() == null) {
            usuario.setTelefones(new ArrayList<>());
        }
        if (!usuario.getTelefones().contains(telefone)) {
            usuario.getTelefones().add(telefone);
            usuariosRepository.save(usuario);
        }
    }
}
