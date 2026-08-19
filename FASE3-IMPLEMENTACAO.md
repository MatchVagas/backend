# Fase 3 — Matching semântico com embeddings abertos (guia de implementação)

> Guia auto-suficiente para implementar a Fase 3 sem assistência. Baseado no estado
> atual do código (branch `feature/fase2-mensagens`): matching por regras em
> `SugestaoVagaService`, entidades `Candidatos`/`Vagas`/`Habilidade`/`Formacao`/`Experiencia`,
> Spring Boot 3.5.13, Java 21, banco Postgres **e** MySQL.

---

## TL;DR (o que vamos construir)

Evoluir o match **por regras** (`SugestaoVagaService`: área/salário/idade via sobreposição de
palavras) para um match **híbrido regras + semântico**, usando um modelo de **embeddings aberto
rodando dentro do próprio backend** (in-process, via Spring AI + ONNX Runtime). **Sem custo de
API** e sem que o currículo saia do servidor (bom p/ LGPD).

- **Indexação:** ao salvar/editar vaga ou perfil de candidato, gerar o vetor do texto e
  **persistir** (tabelas `vaga_embedding` / `candidato_embedding`).
- **Consulta (sugestões):** ler os vetores já persistidos e calcular **similaridade de cosseno**
  em Java — o caminho de consulta **não precisa do modelo**, só de banco + matemática.
- **Portabilidade:** vetor guardado como texto (CSV de floats) — funciona em Postgres **e**
  MySQL (nada de `pgvector`, que é Postgres-only — ver [[busca-melhor-mysql]]).
- **Degradação graciosa:** com a flag `app.embeddings.enabled=false` (padrão em dev/CI),
  o match cai de volta para as regras atuais. **Nada é baixado no CI.**

**Fora de escopo agora** (deixar para Fase 3b / Fase 4):
- Recursos **generativos** (parsear CV→estruturado, resumo de perfil, gerar descrição de vaga) —
  exigem um LLM (Claude pago ou um LLM aberto local tipo Ollama).
- **Embeddings via fornecedor** (Voyage etc.) e **vector DB** dedicado (pgvector/Qdrant) —
  só quando houver escala que a similaridade em memória não aguente.

---

## Decisões de arquitetura (e o porquê)

| Decisão | Por quê |
|---|---|
| Embeddings **in-process** (não serviço à parte) | No Railway Hobby o backend já roda 24/7; o modelo adiciona ~300–500 MB de RAM (~$3–5/mês). Um serviço à parte seria um 2º container always-on. |
| Modelo **aberto multilíngue** pequeno | `all-MiniLM-L6-v2` (padrão, 384d, inglês) funciona out-of-the-box; para PT-BR usar `multilingual-e5-small` ou `paraphrase-multilingual-MiniLM-L12-v2` (384d). |
| Vetor persistido como **TEXT (CSV de floats)** | Portátil Postgres/MySQL, inspecionável. 384 floats ≈ 4 KB. |
| Similaridade **em Java** (cosseno) sobre o conjunto candidato | Portátil e simples; O(n) por consulta é suficiente p/ centenas/milhares de vagas. Índice vetorial fica p/ escala (Fase 4). |
| Camada atrás de uma **porta (`EmbeddingPort`)** + flag | Testes mockam a porta; sem modelo, sem rede no CI; produção liga a flag. |
| Query-path **não usa o modelo** | Só indexação precisa do modelo. Sugerir vagas = ler vetores + cosseno. |

---

## Visão geral do fluxo

```
Salvar/editar Vaga  ──►  IndexacaoEmbeddingService.indexarVaga(v)
Salvar/editar Perfil ─►  IndexacaoEmbeddingService.indexarCandidato(c)
       │                        │
       ▼                        ▼
 TextoEmbeddingBuilder    EmbeddingPort.embed(texto) ──► float[384]
       │                        │
       └───────► EmbeddingCodec.toCsv() ──► TEXT no banco
                            (vaga_embedding / candidato_embedding)

GET /api/candidatos/sugestoes
       │
       ▼
 SugestaoVagaService.sugerirVagas(usuarioId)
   • lê candidato_embedding + vaga_embedding (já persistidos)
   • cosseno(candidato, vaga) → componente semântico (0..60 pts)
   • + regras de salário/idade (as de hoje)
   • fallback p/ regras de texto se algum vetor faltar
```

---

## Passo a passo

### Passo 0 — Dependências (`pom.xml`)

Adicionar o BOM do Spring AI e o **módulo transformers** (usar o módulo, **não** o starter de
auto-config, para controlarmos a criação do bean e evitar download do modelo no CI).

```xml
<!-- dentro de <properties> -->
<spring-ai.version>1.0.3</spring-ai.version>  <!-- confira a versão 1.0.x atual compatível com Boot 3.5 -->

<!-- dentro de <dependencyManagement><dependencies> -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-bom</artifactId>
    <version>${spring-ai.version}</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>

<!-- dentro de <dependencies> -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-transformers</artifactId>
</dependency>
```

> ⚠️ **Verificar:** o artifact `spring-ai-transformers` traz `TransformersEmbeddingModel` +
> ONNX Runtime + tokenizers (DJL). Se o Maven não resolver, adicionar o repositório de
> milestones/releases do Spring AI e conferir o nome/versão atual em
> https://docs.spring.io/spring-ai/reference/api/embeddings/onnx.html . Se preferir o starter
> (`spring-ai-starter-model-transformers`), então **desative a auto-config em teste** com
> `spring.ai.model.embedding=none` no `application-test.properties`, senão o `contextLoads`
> baixa o modelo (lento + precisa de rede no CI).

### Passo 1 — Configuração (`application.properties`)

```properties
# Liga/desliga a camada semântica. PADRÃO false → dev/CI sem modelo, sem rede.
app.embeddings.enabled=${APP_EMBEDDINGS_ENABLED:false}
app.embeddings.dimensao=384
# Limiar mínimo de cosseno para contar como "compatível" (0..1)
app.embeddings.limiar=0.30

# Modelo/tokenizer ONNX (ver Passo 9 para o modelo multilíngue).
# Deixe em branco para usar o default do Spring AI (all-MiniLM-L6-v2, inglês).
app.embeddings.model-uri=
app.embeddings.tokenizer-uri=
app.embeddings.cache-dir=${java.io.tmpdir}/matchvagas-onnx
```

Em produção (Railway), setar `APP_EMBEDDINGS_ENABLED=true`.

### Passo 2 — Porta + adaptador + config guardada por flag

**Porta** (o resto do código só depende disto — nunca do Spring AI direto):

```java
// service/embedding/EmbeddingPort.java
package com.matchvagas.backend.service.embedding;

public interface EmbeddingPort {
    /** true quando há modelo carregado (flag ligada). */
    boolean isAtivo();
    /** Vetor do texto (normalizado). Lança se !isAtivo(). */
    float[] embed(String texto);
    int dimensao();
}
```

**Adaptador** que embrulha o Spring AI:

```java
// service/embedding/TransformersEmbeddingAdapter.java
package com.matchvagas.backend.service.embedding;

import org.springframework.ai.embedding.EmbeddingModel;

public class TransformersEmbeddingAdapter implements EmbeddingPort {

    private final EmbeddingModel model; // org.springframework.ai.embedding.EmbeddingModel
    private final int dimensao;

    public TransformersEmbeddingAdapter(EmbeddingModel model, int dimensao) {
        this.model = model;
        this.dimensao = dimensao;
    }

    @Override public boolean isAtivo() { return true; }
    @Override public int dimensao() { return dimensao; }

    @Override
    public float[] embed(String texto) {
        float[] v = model.embed(texto == null ? "" : texto); // EmbeddingModel.embed(String) -> float[]
        return normalizar(v);
    }

    private static float[] normalizar(float[] v) {
        double norm = 0;
        for (float x : v) norm += (double) x * x;
        norm = Math.sqrt(norm);
        if (norm == 0) return v;
        float[] out = new float[v.length];
        for (int i = 0; i < v.length; i++) out[i] = (float) (v[i] / norm);
        return out;
    }
}
```

**Adaptador desligado** (fallback quando a flag está off — sempre existe um bean):

```java
// service/embedding/DisabledEmbeddingPort.java
package com.matchvagas.backend.service.embedding;

public class DisabledEmbeddingPort implements EmbeddingPort {
    @Override public boolean isAtivo() { return false; }
    @Override public int dimensao() { return 0; }
    @Override public float[] embed(String texto) {
        throw new IllegalStateException("Embeddings desativados (app.embeddings.enabled=false).");
    }
}
```

**Config** — cria o modelo só quando a flag está ligada:

```java
// config/EmbeddingConfig.java
package com.matchvagas.backend.config;

import com.matchvagas.backend.service.embedding.*;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformers.TransformersEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class EmbeddingConfig {

    @Bean
    @ConditionalOnProperty(name = "app.embeddings.enabled", havingValue = "true")
    EmbeddingModel transformersEmbeddingModel(
            @Value("${app.embeddings.model-uri:}") String modelUri,
            @Value("${app.embeddings.tokenizer-uri:}") String tokenizerUri,
            @Value("${app.embeddings.cache-dir}") String cacheDir) throws Exception {

        TransformersEmbeddingModel m = new TransformersEmbeddingModel();
        if (!modelUri.isBlank())     m.setModelResource(modelUri);        // ex.: file:/app/onnx/model.onnx  ou classpath:/onnx/model.onnx
        if (!tokenizerUri.isBlank()) m.setTokenizerResource(tokenizerUri);
        m.setResourceCacheDirectory(cacheDir);
        m.setTokenizerOptions(Map.of("padding", "true"));
        m.afterPropertiesSet();  // baixa/carrega o modelo (primeira vez pode demorar)
        return m;
    }

    @Bean
    @ConditionalOnProperty(name = "app.embeddings.enabled", havingValue = "true")
    EmbeddingPort transformersEmbeddingPort(EmbeddingModel m,
                                            @Value("${app.embeddings.dimensao}") int dim) {
        return new TransformersEmbeddingAdapter(m, dim);
    }

    @Bean
    @ConditionalOnMissingBean(EmbeddingPort.class) // flag off → sempre há um bean desligado
    EmbeddingPort disabledEmbeddingPort() {
        return new DisabledEmbeddingPort();
    }
}
```

> Com a flag `false` (padrão), **só** o `DisabledEmbeddingPort` é criado — o `contextLoads` sobe
> sem baixar nada. Perfeito para o CI.

### Passo 3 — Codec do vetor (portátil) + cosseno

```java
// service/embedding/EmbeddingCodec.java
package com.matchvagas.backend.service.embedding;

public final class EmbeddingCodec {
    private EmbeddingCodec() {}

    public static String toCsv(float[] v) {
        StringBuilder sb = new StringBuilder(v.length * 8);
        for (int i = 0; i < v.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(Float.toString(v[i]));
        }
        return sb.toString();
    }

    public static float[] fromCsv(String csv) {
        if (csv == null || csv.isBlank()) return new float[0];
        String[] parts = csv.split(",");
        float[] v = new float[parts.length];
        for (int i = 0; i < parts.length; i++) v[i] = Float.parseFloat(parts[i]);
        return v;
    }

    /** Cosseno. Se os vetores já vêm normalizados, é só o produto escalar. */
    public static double cosseno(float[] a, float[] b) {
        if (a == null || b == null || a.length == 0 || a.length != b.length) return 0.0;
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            na  += (double) a[i] * a[i];
            nb  += (double) b[i] * b[i];
        }
        if (na == 0 || nb == 0) return 0.0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}
```

### Passo 4 — Construtor de texto (perfil/vaga → string)

```java
// service/embedding/TextoEmbeddingBuilder.java
package com.matchvagas.backend.service.embedding;

import com.matchvagas.backend.entity.*;
import java.util.List;
import java.util.StringJoiner;

public final class TextoEmbeddingBuilder {
    private TextoEmbeddingBuilder() {}

    // NOTA: se usar modelo e5 (multilíngue-e5-*), prefixe:
    //   candidato → "query: " + texto   |   vaga → "passage: " + texto
    // Para MiniLM (default), NÃO prefixe.

    public static String daVaga(Vagas v) {
        StringJoiner sj = new StringJoiner(". ");
        add(sj, v.getTitulo());
        add(sj, v.getAreaAtuacao());
        if (v.getModalidade() != null)  add(sj, v.getModalidade().getDescricao());
        if (v.getTipoVaga() != null)    add(sj, v.getTipoVaga().getDescricao());
        add(sj, v.getDescricao());
        add(sj, v.getRequisitos());
        return sj.toString();
    }

    /** Passe as listas já carregadas (experiências/formações do candidato). */
    public static String doCandidato(Candidatos c, List<Experiencia> exps, List<Formacao> formacoes) {
        StringJoiner sj = new StringJoiner(". ");
        add(sj, c.getObjetivoProfissional());
        add(sj, c.getDisponibilidade());
        if (c.getHabilidades() != null)
            for (Habilidade h : c.getHabilidades()) add(sj, h.getNome());
        if (exps != null)
            for (Experiencia e : exps) { add(sj, e.getCargo()); add(sj, e.getDescricao()); }
        if (formacoes != null)
            for (Formacao f : formacoes) { add(sj, f.getCurso()); add(sj, f.getInstituicao()); }
        return sj.toString();
    }

    private static void add(StringJoiner sj, String s) {
        if (s != null && !s.isBlank()) sj.add(s.trim());
    }
}
```

### Passo 5 — Entidades de embedding (com `ON DELETE CASCADE`)

Mesmo padrão de FK da Fase 2 (`@OnDelete`): remover a vaga/candidato limpa o embedding sozinho.

```java
// entity/VagaEmbedding.java
package com.matchvagas.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "vaga_embedding")
public class VagaEmbedding {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "vaga_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Vagas vaga;

    @Column(name = "vetor", columnDefinition = "TEXT", nullable = false)
    private String vetor;              // CSV de floats

    @Column(name = "modelo", length = 100)  private String modelo;
    @Column(name = "dim")                    private Integer dim;
    @Column(name = "texto_hash", length = 64) private String textoHash; // p/ evitar recomputar
    @Column(name = "atualizado_em")          private LocalDateTime atualizadoEm;

    @PrePersist @PreUpdate void touch() { this.atualizadoEm = LocalDateTime.now(); }
}
```

Criar `CandidatoEmbedding` idêntica, trocando `vaga` por `Candidatos candidato` e a tabela por
`candidato_embedding` / coluna `candidato_id`.

**Repositórios:**

```java
// repository/VagaEmbeddingRepository.java
public interface VagaEmbeddingRepository extends JpaRepository<VagaEmbedding, Long> {
    Optional<VagaEmbedding> findByVagaId(Long vagaId);
    List<VagaEmbedding> findByVagaIdIn(Collection<Long> vagaIds);
}
// repository/CandidatoEmbeddingRepository.java
public interface CandidatoEmbeddingRepository extends JpaRepository<CandidatoEmbedding, Long> {
    Optional<CandidatoEmbedding> findByCandidatoId(Long candidatoId);
}
```

### Passo 6 — Serviço de indexação (gerar + persistir), best-effort

```java
// service/embedding/IndexacaoEmbeddingService.java
package com.matchvagas.backend.service.embedding;

import com.matchvagas.backend.entity.*;
import com.matchvagas.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j @Service @RequiredArgsConstructor
public class IndexacaoEmbeddingService {

    private final EmbeddingPort embeddingPort;
    private final VagaEmbeddingRepository vagaEmbRepo;
    private final CandidatoEmbeddingRepository candEmbRepo;
    private final ExperienciaRepository experienciaRepository;
    private final FormacaoRepository formacaoRepository;

    @Transactional
    public void indexarVaga(Vagas vaga) {
        if (!embeddingPort.isAtivo()) return;               // flag off → no-op
        try {
            String texto = TextoEmbeddingBuilder.daVaga(vaga);
            float[] v = embeddingPort.embed(texto);
            VagaEmbedding e = vagaEmbRepo.findByVagaId(vaga.getId()).orElseGet(VagaEmbedding::new);
            e.setVaga(vaga);
            e.setVetor(EmbeddingCodec.toCsv(v));
            e.setDim(v.length);
            e.setModelo("onnx");
            vagaEmbRepo.save(e);
        } catch (Exception ex) {
            log.warn("Falha ao indexar embedding da vaga {}: {}", vaga.getId(), ex.getMessage());
        }
    }

    @Transactional
    public void indexarCandidato(Candidatos c) {
        if (!embeddingPort.isAtivo()) return;
        try {
            var exps = experienciaRepository.findByCandidatoId(c.getId());
            var forms = formacaoRepository.findByCandidatoId(c.getId());
            String texto = TextoEmbeddingBuilder.doCandidato(c, exps, forms);
            float[] v = embeddingPort.embed(texto);
            CandidatoEmbedding e = candEmbRepo.findByCandidatoId(c.getId()).orElseGet(CandidatoEmbedding::new);
            e.setCandidato(c);
            e.setVetor(EmbeddingCodec.toCsv(v));
            e.setDim(v.length);
            e.setModelo("onnx");
            candEmbRepo.save(e);
        } catch (Exception ex) {
            log.warn("Falha ao indexar embedding do candidato {}: {}", c.getId(), ex.getMessage());
        }
    }
}
```

**Onde chamar (best-effort, não pode quebrar o fluxo):**
- `VagaService.create(...)` e `VagaService.update(...)` → `indexarVaga(salva)` (envolver em try/catch,
  como já faz com `alertaVagaService.notificarNovaVaga`).
- No fluxo de atualização de perfil do candidato (`CandidatoService`) → `indexarCandidato(c)`.
- Ideal: deferir para depois do commit com o `AposCommitExecutor` que já existe (Fase 2), para não
  gerar embedding de algo que sofra rollback.

**Backfill dos registros existentes** — endpoint admin (padrão do `MigracaoController`):

```java
// em AdminService (ou MigracaoService)
@Transactional
public Map<String,Object> backfillEmbeddings() {
    int vagas = 0, cands = 0;
    for (Vagas v : vagaRepository.findAll())      { indexacao.indexarVaga(v); vagas++; }
    for (Candidatos c : candidatoRepository.findAll()) { indexacao.indexarCandidato(c); cands++; }
    return Map.of("vagasIndexadas", vagas, "candidatosIndexados", cands);
}
// Endpoint: POST /api/admin/embeddings/backfill  (hasAuthority ADMIN)
```

### Passo 7 — Match híbrido (evoluir `SugestaoVagaService`)

Ideia: manter salário (25/10) e idade (15) como estão; **substituir** os 60 pts de texto
(área 30 + título 15 + requisitos 15) por um **componente semântico** de até 60 pts baseado no
cosseno. Se algum vetor faltar (ou flag off), **cair de volta** para as regras de texto atuais.

Injete os repositórios de embedding no serviço e adicione:

```java
private static final int PTS_SEMANTICO_MAX = 60;

/** 0..60 pts a partir do cosseno; null se não houver vetores (usar fallback de texto). */
private Integer pontuarSemantico(float[] candVec, Long vagaId,
                                 Map<Long, float[]> vagaVecs, List<String> motivos, double limiar) {
    if (candVec == null) return null;
    float[] vagaVec = vagaVecs.get(vagaId);
    if (vagaVec == null) return null;
    double cos = EmbeddingCodec.cosseno(candVec, vagaVec); // ~[-1,1], normalizados ⇒ [0,1] p/ textos afins
    double sim = Math.max(0.0, cos);
    if (sim < limiar) return 0;
    int pts = (int) Math.round(sim * PTS_SEMANTICO_MAX);
    motivos.add(String.format("Alta compatibilidade com seu perfil (%.0f%%)", sim * 100));
    return pts;
}
```

No `pontuar(vaga, candidato)`, no lugar dos 3 blocos de texto:

```java
Integer semantico = pontuarSemantico(candVec, vaga.getId(), vagaVecs, motivos, limiar);
if (semantico != null) {
    pontuacao += semantico;                 // caminho semântico
} else {
    // FALLBACK: regras de texto atuais (área 30 / título 15 / requisitos 15)
    // ... manter o código de textosSimilares existente ...
}
```

E em `sugerirVagas(...)`, antes do loop, carregar os vetores de uma vez (evita N+1):

```java
double limiar = /* @Value("${app.embeddings.limiar}") */ 0.30;
float[] candVec = candidatoEmbeddingRepository.findByCandidatoId(candidato.getId())
        .map(e -> EmbeddingCodec.fromCsv(e.getVetor())).orElse(null);

List<Long> ids = candidatas.stream().map(Vagas::getId).toList();
Map<Long, float[]> vagaVecs = vagaEmbeddingRepository.findByVagaIdIn(ids).stream()
        .collect(Collectors.toMap(e -> e.getVaga().getId(), e -> EmbeddingCodec.fromCsv(e.getVetor())));
```

> O `SugestaoVagaResponseDTO(vaga, pontuacao, motivos)` **não precisa mudar** — o semântico entra
> na `pontuacao` e num `motivo`. (Opcional: adicionar `Double similaridade` ao DTO.)
> Mantém o filtro `> 15` e o `limit(10)`.

### Passo 8 — (Opcional, Fase 3b) Recomendação no sentido inverso

`sugerirCandidatos(vagaId)` para a empresa: pega o vetor da vaga, ranqueia candidatos por cosseno.

> ⚠️ **LGPD:** mostrar perfis de candidatos à empresa **antes** de qualquer candidatura é dado
> pessoal sem base no fluxo atual (a privacidade hoje é por candidatura, ver `Candidatura`).
> Antes de expor: ou **anonimizar** (só score + resumo), ou exigir **opt-in** do candidato para
> aparecer em buscas de empresas. **Decidir isso antes de implementar.**

### Passo 9 — Modelo multilíngue (PT-BR)

O default (`all-MiniLM-L6-v2`) é anglocêntrico — funciona, mas para PT prefira um multilíngue
(384 dims, compatível com a coluna). Duas formas:

**A) Exportar você mesmo para ONNX** (recomendado, controle total):
```bash
pip install "optimum[exporters]" sentence-transformers
optimum-cli export onnx \
  --model intfloat/multilingual-e5-small \
  --task feature-extraction \
  onnx-e5-small/
# gera model.onnx + tokenizer.json
```
Coloque os arquivos em `src/main/resources/onnx/` (ou num volume no Railway) e aponte:
```properties
app.embeddings.model-uri=classpath:/onnx/model.onnx
app.embeddings.tokenizer-uri=classpath:/onnx/tokenizer.json
```
> **e5:** prefixe o texto com `query: ` (candidato) e `passage: ` (vaga) no `TextoEmbeddingBuilder`.
> **Dimensão:** e5-small = 384 (bate com a config). Se trocar por um modelo de outra dimensão,
> ajuste `app.embeddings.dimensao` **e** re-rode o backfill (vetores de dims diferentes não
> comparam).

**B) Só rodar o default primeiro** (inglês) para validar o pipeline, e trocar o modelo depois.

### Passo 10 — Testes (sem baixar modelo no CI)

- **Cosseno/codec:** teste puro (`EmbeddingCodecTest`) — round-trip CSV e cosseno de vetores
  conhecidos.
- **`SugestaoVagaService`:** injete `@Mock EmbeddingPort` (ou os repositórios de embedding
  retornando vetores fake). Cenários:
  - vetores presentes e afins → alta pontuação semântica.
  - vetores ausentes → cai no fallback de texto (regras atuais). **Os testes atuais continuam
    válidos** se o fallback reproduzir a lógica de hoje.
- **`contextLoads`:** com `app.embeddings.enabled=false` (padrão), só o `DisabledEmbeddingPort`
  é criado — **nenhum download, nenhuma rede.** Confirme que o teto de tempo do teste não muda.
- Ajustar `SugestaoVagaServiceTest` para os novos mocks (repos de embedding + `EmbeddingPort`).

---

## Deploy no Railway (Hobby)

- **Cabe folgado:** teto por serviço muito acima do ~1 GB necessário (RAM não é gargalo).
- **Custo:** RAM ≈ US$10/GB-mês, CPU ≈ US$20/vCPU-mês, cobrado por segundo; Hobby = US$5/mês
  com US$5 de uso incluído. Backend + embeddings 24/7 ≈ **US$13–15/mês**; custo **marginal** dos
  embeddings ≈ **US$3–5/mês** (a RAM extra do modelo).
- **Env vars:** `APP_EMBEDDINGS_ENABLED=true`, e se usar modelo próprio,
  `app.embeddings.model-uri`/`tokenizer-uri` (classpath ou volume).
- **JVM:** limite o heap ciente do container — `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=70`
  (ou `-Xmx768m`). Modelo carrega no boot → +alguns segundos de startup.
- Modelo (~80–470 MB): embutir na imagem (imagem maior, deploy mais lento) **ou** baixar e cachear
  no primeiro boot (`app.embeddings.cache-dir` num volume persistente para não rebaixar a cada deploy).

---

## Checklist de implementação

- [x] Passo 0 — `pom.xml`: BOM Spring AI + `spring-ai-transformers` (Spring AI 1.1.4)
- [x] Passo 1 — properties (`app.embeddings.*`, flag off por padrão)
- [x] Passo 2 — `EmbeddingPort`, `TransformersEmbeddingAdapter`, `DisabledEmbeddingPort`, `EmbeddingConfig`
- [x] Passo 3 — `EmbeddingCodec` (CSV + cosseno)
- [x] Passo 4 — `TextoEmbeddingBuilder`
- [x] Passo 5 — entidades `VagaEmbedding`/`CandidatoEmbedding` + repositórios (`@OnDelete` cascade)
- [x] Passo 6 — `IndexacaoEmbeddingService` + ganchos em `VagaService`/`CandidatoService` + backfill admin
- [x] Passo 7 — match híbrido em `SugestaoVagaService` (semântico + fallback), carregar vetores em lote
- [ ] Passo 8 — (opcional) recomendação inversa (com decisão de LGPD antes)
- [x] Passo 9 — configurar `intfloat/multilingual-e5-small` (384d) e prefixos E5 `query:`/`passage:`
- [x] Passo 10 — testes (codec, híbrido com mocks, contextLoads sem download)
- [x] `./mvnw test` verde localmente (212 testes); ligar `APP_EMBEDDINGS_ENABLED=true` só em homologação/produção
- [x] Marcar no `ROADMAP.md`: parsing de currículo e match semântico implementados
- [ ] Operação — executar `POST /api/admin/migracao/backfill-embeddings` em homologação e produção após habilitar o modelo

---

## Armadilhas conhecidas

- **CI baixando o modelo:** só acontece se a flag ligar OU se usar o *starter* de auto-config.
  Com o módulo `spring-ai-transformers` + bean guardado por flag, o CI fica limpo.
- **Dimensão inconsistente:** trocar de modelo muda a dimensão → vetores antigos ficam
  incomparáveis. Sempre `dim` na tabela + re-backfill ao trocar de modelo.
- **Normalização:** o adaptador normaliza (L2), então cosseno = produto escalar. Se pular a
  normalização, use o cosseno completo do `EmbeddingCodec` (já cobre os dois casos).
- **e5 sem prefixo:** e5 exige `query:`/`passage:`; esquecer degrada a qualidade silenciosamente.
- **RAM no boot:** modelo carrega no `afterPropertiesSet()`. Se o Railway matar por OOM, baixe o
  heap (`MaxRAMPercentage`) e/ou use um modelo menor/quantizado (ONNX int8).
- **Postgres × MySQL:** manter o vetor em `TEXT` (CSV). Não usar `pgvector` no núcleo (Postgres-only).
  Índice vetorial dedicado é assunto de Fase 4.

---

## Depois desta fase (Fase 3b / Fase 4)

- **Generativo (LLM):** parsear CV (PDF→estruturado), resumo de perfil, gerar descrição de vaga,
  justificativa de match em linguagem natural. Precisa de um LLM — Claude (API paga, ver a skill
  `claude-api`) **ou** um LLM aberto local (Ollama). A Anthropic **não** tem API de embeddings.
- **Escala:** quando a similaridade em memória (O(n)) pesar, migrar para um índice vetorial
  (pgvector no Postgres **ou** Qdrant/Milvus como serviço) e/ou embeddings via fornecedor (Voyage).
- **Re-ranking:** um segundo estágio (cross-encoder) sobre o top-N dos embeddings, para precisão.
