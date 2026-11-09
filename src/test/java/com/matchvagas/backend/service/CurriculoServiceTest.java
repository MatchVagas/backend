package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.CurriculoParseResult;
import com.matchvagas.backend.dto.CurriculoResponseDTO;
import com.matchvagas.backend.entity.Candidatos;
import com.matchvagas.backend.entity.Candidatura;
import com.matchvagas.backend.entity.Curriculos;
import com.matchvagas.backend.entity.Empresas;
import com.matchvagas.backend.entity.Vagas;
import com.matchvagas.backend.exception.BusinessException;
import com.matchvagas.backend.exception.ResourceNotFoundException;
import com.matchvagas.backend.mapper.CurriculoMapper;
import com.matchvagas.backend.repository.CandidatoRepository;
import com.matchvagas.backend.repository.CandidaturaRepository;
import com.matchvagas.backend.repository.CurriculoRepository;
import com.matchvagas.backend.repository.EmpresaRepository;
import com.matchvagas.backend.service.embedding.IndexacaoEmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RF007 — Currículo do candidato")
class CurriculoServiceTest {

    private static final String PDF = "application/pdf";
    private static final byte[] CONTEUDO_PDF = "%PDF-1.7 conteúdo".getBytes();

    @Mock CandidatoRepository candidatoRepository;
    @Mock CurriculoRepository curriculoRepository;
    @Mock CandidaturaRepository candidaturaRepository;
    @Mock EmpresaRepository empresaRepository;
    @Mock CurriculoMapper curriculoMapper;
    @Mock SupabaseStorageService supabaseStorageService;
    @Mock AuditoriaService auditoriaService;
    @Mock CurriculoParserService curriculoParserService;
    @Mock IndexacaoEmbeddingService indexacaoEmbeddingService;
    @Spy AposCommitExecutor aposCommitExecutor = new AposCommitExecutor();
    @InjectMocks CurriculoService service;

    private Candidatos candidato;

    @BeforeEach
    void setUp() {
        candidato = new Candidatos();
        candidato.setId(10L);
    }

    private CurriculoResponseDTO dtoBase() {
        return new CurriculoResponseDTO(1L, 10L, "cv.pdf", "10/x.pdf", null, null, "PDF", null, null, null);
    }

    @Test
    @DisplayName("upload extrai o texto, envia ao storage, salva e reindexa o candidato")
    void upload() {
        var arquivo = new MockMultipartFile("arquivo", "Meu CV.PDF", PDF, CONTEUDO_PDF);
        var parse = new CurriculoParseResult("texto extraído", null, null, null, List.of("java"), List.of(), List.of());
        when(curriculoParserService.parse(CONTEUDO_PDF)).thenReturn(parse);
        when(curriculoParserService.toJson(parse)).thenReturn("{\"competencias\":[\"java\"]}");
        when(candidatoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(candidato));
        when(curriculoRepository.findByCandidatoId(10L)).thenReturn(Optional.empty());
        when(curriculoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(curriculoMapper.toResponseDTO(any())).thenReturn(dtoBase());

        var resposta = service.upload(1L, arquivo);

        ArgumentCaptor<Curriculos> captor = ArgumentCaptor.forClass(Curriculos.class);
        verify(curriculoRepository).save(captor.capture());
        Curriculos salvo = captor.getValue();
        assertThat(salvo.getCaminhoArquivo()).startsWith("10/").endsWith(".pdf");
        assertThat(salvo.getFormatoArquivo()).isEqualTo("PDF");
        assertThat(salvo.getTextoExtraido()).isEqualTo("texto extraído");
        assertThat(salvo.getParseadoEm()).isNotNull();
        assertThat(candidato.getCurriculo()).isSameAs(salvo);
        assertThat(resposta.urlArquivo()).isNull();
        verify(supabaseStorageService).upload(eq(salvo.getCaminhoArquivo()), eq(CONTEUDO_PDF), eq(PDF));
        verify(indexacaoEmbeddingService).indexarCandidato(candidato);
    }

    @Test
    @DisplayName("novo upload remove o arquivo anterior do storage e reaproveita o registro")
    void substituiAnterior() {
        Curriculos anterior = new Curriculos();
        anterior.setCaminhoArquivo("10/antigo.pdf");
        when(curriculoParserService.parse(any())).thenReturn(
                new CurriculoParseResult("t", null, null, null, List.of(), List.of(), List.of()));
        when(candidatoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(candidato));
        when(curriculoRepository.findByCandidatoId(10L)).thenReturn(Optional.of(anterior));
        when(curriculoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(curriculoMapper.toResponseDTO(any())).thenReturn(dtoBase());

        service.upload(1L, new MockMultipartFile("arquivo", "cv.pdf", PDF, CONTEUDO_PDF));

        verify(supabaseStorageService).deletar("10/antigo.pdf");
        verify(curriculoRepository).save(anterior);
    }

    @Test
    @DisplayName("rejeita arquivo vazio, grande, de tipo não aceito ou com conteúdo forjado")
    void validaArquivo() {
        assertThatThrownBy(() -> service.upload(1L, new MockMultipartFile("arquivo", new byte[0])))
                .hasMessageContaining("Nenhum arquivo");
        assertThatThrownBy(() -> service.upload(1L,
                new MockMultipartFile("arquivo", "cv.pdf", PDF, new byte[5 * 1024 * 1024 + 1])))
                .hasMessageContaining("5 MB");
        assertThatThrownBy(() -> service.upload(1L,
                new MockMultipartFile("arquivo", "cv.png", "image/png", CONTEUDO_PDF)))
                .hasMessageContaining("Formato não aceito");
        assertThatThrownBy(() -> service.upload(1L,
                new MockMultipartFile("arquivo", "cv.pdf", PDF, "MZ executável".getBytes())))
                .hasMessageContaining("não corresponde");
        verifyNoInteractions(supabaseStorageService, curriculoRepository);
    }

    @Test
    @DisplayName("busca devolve URL assinada; sem currículo retorna 404")
    void busca() {
        Curriculos curriculo = new Curriculos();
        curriculo.setCaminhoArquivo("10/x.pdf");
        when(candidatoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(candidato));
        when(curriculoRepository.findByCandidatoId(10L))
                .thenReturn(Optional.of(curriculo))
                .thenReturn(Optional.empty());
        when(curriculoMapper.toResponseDTO(curriculo)).thenReturn(dtoBase());
        when(supabaseStorageService.gerarUrlAssinada("10/x.pdf", 3600)).thenReturn("https://assinada");

        assertThat(service.buscar(1L).urlArquivo()).isEqualTo("https://assinada");
        assertThatThrownBy(() -> service.buscar(1L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("download do candidato redireciona (302) para a URL assinada")
    void download() {
        Curriculos curriculo = new Curriculos();
        curriculo.setCaminhoArquivo("10/x.pdf");
        when(candidatoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(candidato));
        when(curriculoRepository.findByCandidatoId(10L)).thenReturn(Optional.of(curriculo));
        when(supabaseStorageService.gerarUrlAssinada("10/x.pdf", 3600)).thenReturn("https://assinada");

        var resposta = service.download(1L);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(resposta.getHeaders().getFirst(HttpHeaders.LOCATION)).isEqualTo("https://assinada");
    }

    private Candidatura candidatura(Long empresaId, boolean compartilha) {
        Empresas empresa = new Empresas();
        empresa.setId(empresaId);
        Vagas vaga = new Vagas();
        vaga.setEmpresas(empresa);
        Curriculos curriculo = new Curriculos();
        curriculo.setCaminhoArquivo("10/x.pdf");
        candidato.setCurriculo(curriculo);
        Candidatura c = new Candidatura();
        c.setVaga(vaga);
        c.setCandidato(candidato);
        c.setCompartilharCurriculo(compartilha);
        return c;
    }

    private void empresaDoUsuario(Long usuarioId, Long empresaId) {
        Empresas empresa = new Empresas();
        empresa.setId(empresaId);
        when(empresaRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(empresa));
    }

    @Test
    @DisplayName("empresa dona da vaga baixa o currículo autorizado, com auditoria")
    void downloadEmpresa() {
        empresaDoUsuario(5L, 20L);
        when(candidaturaRepository.findById(7L)).thenReturn(Optional.of(candidatura(20L, true)));
        when(supabaseStorageService.gerarUrlAssinada("10/x.pdf", 3600)).thenReturn("https://assinada");

        assertThat(service.downloadParaEmpresa(7L, 5L).getStatusCode()).isEqualTo(HttpStatus.FOUND);
        verify(auditoriaService).registrar(5L, 10L, "CURRICULO", "READ");
    }

    @Test
    @DisplayName("empresa de outra vaga, ou sem autorização do candidato, não baixa")
    void downloadEmpresaBloqueado() {
        empresaDoUsuario(5L, 20L);
        when(candidaturaRepository.findById(7L)).thenReturn(Optional.of(candidatura(99L, true)));
        when(candidaturaRepository.findById(8L)).thenReturn(Optional.of(candidatura(20L, false)));

        assertThatThrownBy(() -> service.downloadParaEmpresa(7L, 5L)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.downloadParaEmpresa(8L, 5L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("não autorizou");
        verify(supabaseStorageService, never()).gerarUrlAssinada(anyString(), anyInt());
        verifyNoInteractions(auditoriaService);
    }

    @Test
    @DisplayName("usuário sem empresa não baixa currículo de candidatura")
    void downloadSemEmpresa() {
        when(empresaRepository.findByUsuarioId(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.downloadParaEmpresa(7L, 5L)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("remove o currículo do storage e do perfil")
    void deleta() {
        Curriculos curriculo = new Curriculos();
        curriculo.setCaminhoArquivo("10/x.pdf");
        candidato.setCurriculo(curriculo);
        when(candidatoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(candidato));
        when(curriculoRepository.findByCandidatoId(10L)).thenReturn(Optional.of(curriculo));

        service.deletar(1L);

        verify(supabaseStorageService).deletar("10/x.pdf");
        verify(curriculoRepository).delete(curriculo);
        assertThat(candidato.getCurriculo()).isNull();
    }
}
