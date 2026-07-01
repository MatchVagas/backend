package com.matchvagas.backend.service;

import com.matchvagas.backend.dto.MigracaoResultadoDTO;
import com.matchvagas.backend.entity.Candidatos;
import com.matchvagas.backend.entity.Empresas;
import com.matchvagas.backend.repository.CandidatoRepository;
import com.matchvagas.backend.repository.EmpresaRepository;
import com.matchvagas.backend.util.CpfCrypto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Migrações operacionais pós-deploy (segurança/LGPD)")
class MigracaoServiceTest {

    @Mock CandidatoRepository candidatoRepository;
    @Mock EmpresaRepository   empresaRepository;

    @InjectMocks MigracaoService migracaoService;

    private static final String CPF = "52998224725";
    private static final String URL_PUBLICA =
            "https://xyz.supabase.co/storage/v1/object/public/imagens-perfil/candidatos/42/abc.png";
    private static final String PATH = "candidatos/42/abc.png";

    private Candidatos candidato(Long id, String cpf, String cpfHash) {
        Candidatos c = new Candidatos();
        c.setId(id);
        c.setCpf(cpf);
        c.setCpfHash(cpfHash);
        return c;
    }

    // ═══════════════════════════════════════════════════════════
    //  BACKFILL DE CPF (LGPD-04)
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("backfillCpf")
    class BackfillCpf {

        @Test
        @DisplayName("candidato legado (sem hash) deve receber o hash e ser salvo")
        void candidatoLegadoRecebeHash() {
            Candidatos legado = candidato(1L, CPF, null);
            when(candidatoRepository.findAll()).thenReturn(List.of(legado));

            MigracaoResultadoDTO r = migracaoService.backfillCpf();

            assertThat(legado.getCpfHash()).isEqualTo(CpfCrypto.hash(CPF));
            verify(candidatoRepository).save(legado);
            assertThat(r.operacao()).isEqualTo("backfill-cpf");
            assertThat(r.total()).isEqualTo(1);
            assertThat(r.atualizados()).isEqualTo(1);
            assertThat(r.jaProcessados()).isZero();
            assertThat(r.semValor()).isZero();
            assertThat(r.erros()).isZero();
        }

        @Test
        @DisplayName("candidato que já tem hash deve ser ignorado (idempotência)")
        void candidatoComHashEhIgnorado() {
            Candidatos jaProcessado = candidato(2L, CPF, CpfCrypto.hash(CPF));
            when(candidatoRepository.findAll()).thenReturn(List.of(jaProcessado));

            MigracaoResultadoDTO r = migracaoService.backfillCpf();

            verify(candidatoRepository, never()).save(any());
            assertThat(r.atualizados()).isZero();
            assertThat(r.jaProcessados()).isEqualTo(1);
        }

        @Test
        @DisplayName("candidato sem CPF deve ser contabilizado em semValor, sem salvar")
        void candidatoSemCpf() {
            Candidatos semCpf = candidato(3L, null, null);
            Candidatos cpfEmBranco = candidato(4L, "  ", null);
            when(candidatoRepository.findAll()).thenReturn(List.of(semCpf, cpfEmBranco));

            MigracaoResultadoDTO r = migracaoService.backfillCpf();

            verify(candidatoRepository, never()).save(any());
            assertThat(r.semValor()).isEqualTo(2);
            assertThat(r.atualizados()).isZero();
        }

        @Test
        @DisplayName("falha ao salvar deve contabilizar erro e continuar os demais")
        void falhaAoSalvarNaoInterrompe() {
            Candidatos comErro = candidato(5L, CPF, null);
            Candidatos ok       = candidato(6L, CPF, null);
            when(candidatoRepository.findAll()).thenReturn(List.of(comErro, ok));
            when(candidatoRepository.save(comErro)).thenThrow(new RuntimeException("db down"));

            MigracaoResultadoDTO r = migracaoService.backfillCpf();

            verify(candidatoRepository).save(ok);
            assertThat(r.total()).isEqualTo(2);
            assertThat(r.atualizados()).isEqualTo(1);
            assertThat(r.erros()).isEqualTo(1);
            assertThat(r.mensagens()).hasSize(1);
            assertThat(r.mensagens().get(0)).contains("id=5").contains("db down");
        }

        @Test
        @DisplayName("mistura de estados deve totalizar corretamente")
        void contadoresMistos() {
            when(candidatoRepository.findAll()).thenReturn(List.of(
                    candidato(1L, CPF, null),                 // atualizado
                    candidato(2L, CPF, CpfCrypto.hash(CPF)),  // já processado
                    candidato(3L, null, null)                 // sem valor
            ));

            MigracaoResultadoDTO r = migracaoService.backfillCpf();

            assertThat(r.total()).isEqualTo(3);
            assertThat(r.atualizados()).isEqualTo(1);
            assertThat(r.jaProcessados()).isEqualTo(1);
            assertThat(r.semValor()).isEqualTo(1);
            assertThat(r.erros()).isZero();
            verify(candidatoRepository, times(1)).save(any());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  NORMALIZAÇÃO DE URLs DE IMAGEM (LGPD-08)
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("normalizarUrlsImagens")
    class NormalizarUrls {

        private Empresas empresa(Long id, String logoUrl) {
            Empresas e = new Empresas();
            e.setId(id);
            e.setLogoUrl(logoUrl);
            return e;
        }

        @Test
        @DisplayName("URL pública completa de foto deve virar object path")
        void fotoUrlPublicaViraPath() {
            Candidatos c = new Candidatos();
            c.setId(1L);
            c.setFotoPerfilUrl(URL_PUBLICA);
            when(candidatoRepository.findAll()).thenReturn(List.of(c));
            when(empresaRepository.findAll()).thenReturn(List.of());

            MigracaoResultadoDTO r = migracaoService.normalizarUrlsImagens();

            assertThat(c.getFotoPerfilUrl()).isEqualTo(PATH);
            verify(candidatoRepository).save(c);
            assertThat(r.operacao()).isEqualTo("normalizar-urls-imagens");
            assertThat(r.atualizados()).isEqualTo(1);
        }

        @Test
        @DisplayName("logo com URL pública deve virar object path")
        void logoUrlPublicaViraPath() {
            String urlLogo = "https://xyz.supabase.co/storage/v1/object/public/imagens-perfil/empresas/7/logo.png";
            Empresas emp = empresa(7L, urlLogo);
            when(candidatoRepository.findAll()).thenReturn(List.of());
            when(empresaRepository.findAll()).thenReturn(List.of(emp));

            MigracaoResultadoDTO r = migracaoService.normalizarUrlsImagens();

            assertThat(emp.getLogoUrl()).isEqualTo("empresas/7/logo.png");
            verify(empresaRepository).save(emp);
            assertThat(r.atualizados()).isEqualTo(1);
        }

        @Test
        @DisplayName("valor que já é object path deve ser ignorado (idempotência)")
        void pathJaNormalizadoEhIgnorado() {
            Candidatos c = new Candidatos();
            c.setId(1L);
            c.setFotoPerfilUrl(PATH);
            when(candidatoRepository.findAll()).thenReturn(List.of(c));
            when(empresaRepository.findAll()).thenReturn(List.of());

            MigracaoResultadoDTO r = migracaoService.normalizarUrlsImagens();

            assertThat(c.getFotoPerfilUrl()).isEqualTo(PATH); // inalterado
            verify(candidatoRepository, never()).save(any());
            assertThat(r.jaProcessados()).isEqualTo(1);
            assertThat(r.atualizados()).isZero();
        }

        @Test
        @DisplayName("registros sem imagem devem ser contabilizados em semValor")
        void semImagem() {
            Candidatos c = new Candidatos();
            c.setId(1L);
            c.setFotoPerfilUrl(null);
            Empresas emp = empresa(2L, "   ");
            when(candidatoRepository.findAll()).thenReturn(List.of(c));
            when(empresaRepository.findAll()).thenReturn(List.of(emp));

            MigracaoResultadoDTO r = migracaoService.normalizarUrlsImagens();

            verify(candidatoRepository, never()).save(any());
            verify(empresaRepository, never()).save(any());
            assertThat(r.semValor()).isEqualTo(2);
            assertThat(r.atualizados()).isZero();
        }

        @Test
        @DisplayName("total soma candidatos e empresas avaliados")
        void totalSomaAmbos() {
            Candidatos c = new Candidatos();
            c.setId(1L);
            c.setFotoPerfilUrl(URL_PUBLICA);
            when(candidatoRepository.findAll()).thenReturn(List.of(c));
            when(empresaRepository.findAll()).thenReturn(List.of(empresa(2L, PATH)));

            MigracaoResultadoDTO r = migracaoService.normalizarUrlsImagens();

            assertThat(r.total()).isEqualTo(2);
            assertThat(r.atualizados()).isEqualTo(1);   // candidato
            assertThat(r.jaProcessados()).isEqualTo(1); // empresa (já path)
        }
    }
}
