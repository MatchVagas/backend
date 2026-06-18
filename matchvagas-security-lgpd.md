# Relatório de Segurança e Conformidade LGPD — MatchVagas Backend

**Data da análise:** Junho/2026  
**Versão analisada:** Spring Boot 3.5.x / Java 21  
**Ambiente:** Backend REST — `com.matchvagas.backend`

---

## Sumário executivo

O projeto está bem estruturado e adota práticas razoáveis de segurança (JWT, BCrypt, Spring Security com `@PreAuthorize`). No entanto, foram identificadas **11 vulnerabilidades de segurança** e **9 lacunas de conformidade com a LGPD**, que vão de gravidade crítica a baixa. Nenhuma delas inviabiliza o projeto, mas algumas precisam de correção antes de qualquer lançamento em produção.

---

## 1. Vulnerabilidades de Segurança

### 🔴 CRÍTICAS

---

#### SEC-01 — Chave JWT em texto puro no `application.properties`

**Arquivo:** `src/main/resources/application.properties`  
**Linha relevante:**
```properties
jwt.secret=matchvagas_chave_secreta_minimo_256bits_troque_em_producao!
```

**Problema:** A chave secreta está commitada no repositório. Qualquer pessoa com acesso ao código pode forjar tokens JWT válidos, comprometendo toda a autenticação do sistema.

**Correção:**
```properties
# application.properties — apenas placeholder
jwt.secret=${JWT_SECRET}
```
Injetar via variável de ambiente em produção (`JWT_SECRET=<valor gerado com openssl rand -base64 64>`).

---

#### SEC-02 — Ausência de validação de propriedade do recurso em `LocalizacaoController`

**Arquivo:** `LocalizacaoController.java`

**Problema:** O controller não tem `@PreAuthorize` nos endpoints de escrita (`POST /paises`, `PUT /estados/{id}`, `DELETE /cidades/{id}`, etc.). O arquivo `SecurityConfig` protege esses endpoints com `.hasAuthority("ADMIN")`, mas a regra está configurada apenas na cadeia do Spring Security e não há validação explícita no controller. Se a ordem das regras no `HttpSecurity` for alterada por algum commit futuro, todos os endpoints de escrita de localização ficam desprotegidos.

**Correção:**
```java
@PostMapping("/paises")
@PreAuthorize("hasAuthority('ADMIN')")
public ResponseEntity<PaisResponseDTO> criarPais(...) { ... }
```
Adicionar `@PreAuthorize("hasAuthority('ADMIN')")` em todos os endpoints de escrita dos controllers `LocalizacaoController` e `LookupVagaController` / `LookupSistemaController`.

---

#### SEC-03 — Spring Session JDBC habilitado sem configuração de limpeza

**Arquivo:** `application.properties`
```properties
spring.session.jdbc.initialize-schema=always
```

**Problema:** O projeto usa JWT stateless, mas `spring-session-jdbc` está declarado como dependência (`pom.xml`). Isso cria uma tabela `SPRING_SESSION` no banco que nunca é limpa. Em JWT puro, sessions JDBC são desnecessárias e representam uma superfície de ataque adicional (session fixation).

**Correção:** Remover a dependência `spring-session-jdbc` do `pom.xml` e a linha do `application.properties`, ou configurar `spring.session.store-type=none`.

---

### 🟠 ALTAS

---

#### SEC-04 — CNPJ e CPF validados apenas por regex/formato, não por dígitos verificadores

**Arquivos:** `EmpresaRequestDTO.java`, `CandidatoRequestDTO.java`

**Problema:** O CNPJ é validado somente por regex de formato. O CPF usa `@CPF` do Hibernate Validator (correto), mas o CNPJ não possui validação de dígitos verificadores — um CNPJ como `11.111.111/1111-11` (formato válido, dígitos inválidos) seria aceito.

**Correção no `EmpresaService` (validação programática):**
```java
// Adicionar método de validação de CNPJ
private boolean cnpjValido(String cnpj) {
    String digits = cnpj.replaceAll("[^0-9]", "");
    if (digits.length() != 14) return false;
    // ... algoritmo módulo 11
}
```

---

#### SEC-05 — Upload de arquivo sem verificação de magic bytes (somente MIME type)

**Arquivo:** `CurriculoService.java`, `FotoPerfilService.java`

**Problema:** A validação de tipo de arquivo usa apenas `arquivo.getContentType()`, que é o header `Content-Type` enviado pelo cliente — facilmente forjável. Um atacante pode renomear um `.exe` para `.pdf` e fazer upload com `Content-Type: application/pdf`.

**Correção:**
```java
// Usar Apache Tika ou verificar magic bytes manualmente
private boolean verificarMagicBytes(byte[] bytes, String tipoEsperado) {
    // PDF: começa com %PDF (25 50 44 46)
    if ("application/pdf".equals(tipoEsperado)) {
        return bytes.length >= 4 
            && bytes[0] == 0x25 && bytes[1] == 0x50
            && bytes[2] == 0x44 && bytes[3] == 0x46;
    }
    // ... outros tipos
}
```
Alternativa: adicionar `org.apache.tika:tika-core` ao `pom.xml`.

---

#### SEC-06 — Ausência de rate limiting nos endpoints de autenticação

**Arquivo:** `AuthController.java`

**Problema:** Os endpoints `/api/auth/login`, `/api/auth/register` e `/api/auth/esqueceu-senha` não têm proteção contra força bruta. Um atacante pode testar senhas indefinidamente.

**Correção:** Adicionar `spring-boot-starter-data-redis` + `bucket4j-spring-boot-starter` ou implementar throttle via `@Aspect`:
```java
@Component
@Aspect
public class RateLimitAspect {
    private final Cache<String, AtomicInteger> tentativas = 
        Caffeine.newBuilder().expireAfterWrite(15, TimeUnit.MINUTES).build();
    
    @Around("@annotation(RateLimit)")
    public Object limitar(ProceedingJoinPoint pjp) throws Throwable {
        // bloquear após 5 tentativas por IP em 15 min
    }
}
```

---

#### SEC-07 — Tokens de reset de senha não invalidados corretamente após uso em consulta

**Arquivo:** `PasswordResetService.java`

**Problema:** O método `verificarCodigo` retorna o token sem invalidá-lo. Se o token vazar (log, intercepção), pode ser reutilizado. Além disso, `deleteExpiredAndUsed` é chamado apenas em `solicitarRedefinicao`, não automaticamente.

**Correção:**
```java
@Transactional
public String verificarCodigo(String email, String codigo) {
    PasswordResetToken resetToken = tokenRepository
            .findValidByCodigo(email, codigo, LocalDateTime.now())
            .orElseThrow(() -> new BusinessException("Código inválido ou expirado."));
    // Gerar novo token único para a etapa de redefinição
    String tokenRedefinicao = UUID.randomUUID().toString();
    resetToken.setToken(tokenRedefinicao);
    tokenRepository.save(resetToken);
    return tokenRedefinicao;
}
```

---

### 🟡 MÉDIAS

---

#### SEC-08 — `@CrossOrigin(origins = "*")` no `AuthController`

**Arquivo:** `AuthController.java`

**Problema:** `@CrossOrigin(origins = "*")` sobrescreve a política CORS global definida em `SecurityConfig` apenas para o controller de autenticação, permitindo qualquer origem. Em produção, isso deve ser restrito aos domínios do frontend.

**Correção:** Remover o `@CrossOrigin` do controller e manter apenas a configuração global em `SecurityConfig`, trocando `config.setAllowedOriginPatterns(List.of("*"))` pelos domínios reais.

---

#### SEC-09 — Logs de debug expõem informações sensíveis

**Arquivo:** `application.properties`
```properties
logging.level.com.matchvagas=DEBUG
```

**Problema:** Em modo DEBUG, o Spring loga queries SQL (incluindo parâmetros), headers HTTP e dados de sessão, o que pode expor senhas, CPFs e tokens em logs de produção.

**Correção:**
```properties
# Produção
logging.level.com.matchvagas=INFO
logging.level.org.springframework.security=WARN
```

---

#### SEC-10 — Ausência de headers de segurança HTTP

**Arquivo:** `SecurityConfig.java`

**Problema:** Não há configuração de headers como `X-Content-Type-Options`, `X-Frame-Options`, `Content-Security-Policy`, `Strict-Transport-Security`. Isso abre brechas para clickjacking e MIME sniffing.

**Correção:**
```java
http.headers(headers -> headers
    .frameOptions(frame -> frame.deny())
    .xssProtection(xss -> xss.block(true))
    .contentTypeOptions(Customizer.withDefaults())
    .httpStrictTransportSecurity(hsts -> 
        hsts.maxAgeInSeconds(31536000).includeSubdomains(true))
);
```

---

#### SEC-11 — Endpoint `/api/usuarios` sem restrição de role

**Arquivo:** `SecurityConfig.java` + `UsuarioController.java`

**Problema:** O `SecurityConfig` termina com `.anyRequest().authenticated()`, o que significa que qualquer usuário autenticado (inclusive CANDIDATOs) pode chamar `GET /api/usuarios`, `POST /api/usuarios`, `PUT /api/usuarios/{id}` e `DELETE /api/usuarios/{id}`. O Swagger documenta esses endpoints como acessíveis, e um CANDIDATO poderia listar todos os usuários do sistema.

**Correção:**
```java
// Em SecurityConfig, adicionar ANTES de .anyRequest().authenticated():
.requestMatchers("/api/usuarios/**").hasAuthority("ADMIN")
```

---

## 2. Conformidade com a LGPD

A **Lei Geral de Proteção de Dados (Lei nº 13.709/2018)** se aplica plenamente ao MatchVagas, que coleta e processa dados pessoais de candidatos (CPF, data de nascimento, currículo, pretensão salarial, endereço, telefone, gênero, foto) e de gestores de empresa.

---

### 🔴 CRÍTICAS

---

#### LGPD-01 — Ausência de mecanismo de consentimento explícito e registrável

**Impacto:** Art. 7º, I — O tratamento de dados deve ser baseado em hipótese legal (consentimento, execução de contrato, legítimo interesse, etc.).

**Problema:** O cadastro (`POST /api/auth/register`) não coleta, registra nem armazena qualquer manifestação de consentimento do titular. Não há `consentimento_aceito_em`, `versão_política` ou campo equivalente na entidade `Usuarios`.

**Correção:**  
Adicionar ao `Usuarios`:
```java
@Column(name = "consentimento_lgpd_em")
private LocalDateTime consentimentoLgpdEm;

@Column(name = "versao_politica_privacidade", length = 10)
private String versaoPoliticaPrivacidade;
```
E ao `UsuariosRequestDTO`:
```java
@NotNull(message = "Aceite dos termos de uso é obrigatório")
Boolean aceitouTermos;
```

---

#### LGPD-02 — Ausência de endpoint de exclusão de conta (direito ao esquecimento)

**Impacto:** Art. 18, VI — O titular tem direito à eliminação dos dados pessoais tratados com o seu consentimento.

**Problema:** Não existe endpoint que permita ao próprio candidato ou gestor de empresa excluir sua conta e todos os dados associados (candidato, currículo no Supabase, foto, candidaturas, histórico, telefones, endereço).

**Correção:**
```java
// Em CandidatoController
@DeleteMapping("/minha-conta")
@PreAuthorize("hasAuthority('CANDIDATO')")
@Operation(summary = "Excluir minha conta e todos os dados (direito ao esquecimento - LGPD Art. 18, VI)")
public ResponseEntity<Void> excluirMinhaConta(Authentication auth) {
    Long usuarioId = Long.parseLong(auth.getName());
    candidatoService.excluirConta(usuarioId); // apaga dados, arquivos no Supabase, etc.
    return ResponseEntity.noContent().build();
}
```

---

### 🟠 ALTAS

---

#### LGPD-03 — Dados sensíveis sem anonimização nos logs

**Impacto:** Art. 46 — medidas de segurança técnicas para proteger os dados pessoais.

**Problema:** Em modo DEBUG, campos como CPF, email, pretensão salarial e nome podem aparecer nos logs do Hibernate (`spring.jpa.show-sql` e level DEBUG). Com o nível `com.matchvagas=DEBUG` configurado em produção, qualquer exceção loga o DTO completo (incluindo CPF).

**Correção:**  
Anotar campos sensíveis com `@JsonProperty(access = WRITE_ONLY)` nos DTOs onde necessário, e assegurar que o nível de log seja `INFO` em produção (ver SEC-09). Considerar mascaramento de CPF nos logs:
```java
log.debug("Candidato {} criado", candidato.getId()); // sem CPF
```

---

#### LGPD-04 — CPF armazenado em texto puro

**Impacto:** Art. 46 — medidas técnicas para proteger dados pessoais sensíveis.

**Arquivo:** `Candidatos.java`
```java
@Column(name = "cpf", nullable = false, unique = true, length = 20)
private String cpf;
```

**Problema:** CPF é dado pessoal identificador direto. Armazenado em texto puro no banco, qualquer acesso não autorizado ao banco (SQL injection, backup vazado, acesso por DBA) expõe todos os CPFs.

**Correção (mínima):** Hash irreversível com sal para consulta por igualdade:
```java
// Armazenar hash SHA-256 do CPF para verificar unicidade
// Armazenar CPF criptografado com AES-256 para exibição ao próprio titular
```
Ou usar colunas com criptografia transparente no banco (PostgreSQL pgcrypto / MariaDB encryption at rest).

---

#### LGPD-05 — Ausência de endpoint para portabilidade de dados

**Impacto:** Art. 18, V — O titular tem direito à portabilidade dos dados a outro fornecedor de serviço.

**Problema:** Não existe endpoint que permita ao candidato exportar todos os seus dados em formato estruturado (JSON/CSV).

**Correção:**
```java
@GetMapping("/meus-dados")
@PreAuthorize("hasAuthority('CANDIDATO')")
@Operation(summary = "Exportar todos os meus dados (portabilidade - LGPD Art. 18, V)")
public ResponseEntity<MeusDadosExportDTO> exportarMeusDados(Authentication auth) { ... }
```
O `MeusDadosExportDTO` deve incluir: dados do usuário, candidato, endereço, telefone, experiências, formações, habilidades, candidaturas, histórico de status.

---

### 🟡 MÉDIAS

---

#### LGPD-06 — Ausência de trilha de auditoria para acesso a dados pessoais

**Impacto:** Art. 37 — O controlador deve manter registro das operações de tratamento.

**Problema:** Existe `HistoricoStatusCandidatura` para status, mas não há log de auditoria geral para operações de leitura/escrita em dados pessoais (quem acessou o perfil de qual candidato, quando a empresa baixou o currículo, etc.).

**Correção:**  
Criar entidade `AuditoriaAcesso`:
```java
@Entity
@Table(name = "auditoria_acesso")
public class AuditoriaAcesso {
    @Id @GeneratedValue private Long id;
    private Long usuarioSolicitanteId;
    private Long recursoId;
    private String tipoRecurso; // "CANDIDATO", "CURRICULO", "EMPRESA"
    private String operacao;    // "READ", "UPDATE", "DELETE"
    private LocalDateTime dataHora;
    private String ipOrigem;
}
```

---

#### LGPD-07 — Dados de geolocalização implícitos sem finalidade declarada

**Impacto:** Art. 9º — O titular deve ser informado da finalidade do tratamento.

**Problema:** O sistema armazena endereço completo do candidato (logradouro, número, bairro, CEP, cidade, estado). A finalidade desse dado não é clara — a plataforma é de vagas, não de entrega. Além disso, o candidato pode optar por não compartilhar o endereço com a empresa (`compartilharEndereco`), mas o dado ainda fica armazenado permanentemente.

**Correção:**  
Documentar formalmente a finalidade do endereço (ex.: "personalização de vagas por localidade") e implementar retenção mínima — se o candidato não tiver candidaturas ativas há X meses, pseudoanonimizar o endereço.

---

#### LGPD-08 — Foto de perfil em bucket público do Supabase

**Impacto:** Art. 46 — medidas de segurança adequadas ao risco.

**Arquivo:** `SupabaseStorageService.java`
```java
public String getPublicUrl(String objectPath) {
    return supabaseUrl + "/storage/v1/object/public/" + imagesBucket + "/" + objectPath;
}
```

**Problema:** As fotos de perfil são salvas em um bucket público, acessíveis por qualquer URL sem autenticação. Qualquer pessoa que adivinhe ou obtenha o path pode acessar a foto de qualquer candidato.

**Correção:**  
Mover fotos para bucket privado e gerar URLs assinadas com expiração (como já é feito para o currículo):
```java
public String getUrlFoto(String objectPath) {
    return gerarUrlAssinada(objectPath, 3600); // reutilizar método existente
}
```
E ajustar `supabase.storage.images-bucket` para bucket privado.

---

#### LGPD-09 — Ausência de política de retenção de dados

**Impacto:** Art. 15 — O tratamento de dados pessoais deve ser encerrado quando a finalidade for alcançada.

**Problema:** Não há nenhum mecanismo de retenção ou exclusão automática de dados. Candidaturas reprovadas, contas inativas há anos, currículos de candidatos que abandonaram a plataforma — todos ficam indefinidamente no banco e no Supabase.

**Correção:**  
Implementar job agendado com `@Scheduled`:
```java
@Scheduled(cron = "0 0 2 1 * ?") // todo dia 1 do mês às 2h
@Transactional
public void limparDadosExpirados() {
    LocalDateTime limite = LocalDateTime.now().minusYears(2);
    // Pseudoanonimizar usuários inativos há mais de 2 anos
    // Excluir tokens de reset expirados
    // Remover currículos de candidatos sem candidatura ativa há 1 ano
}
```

---

## 3. Tabela de Priorização

| ID | Tipo | Severidade | Esforço | Prioridade |
|----|------|-----------|---------|-----------|
| SEC-01 | Segurança | 🔴 Crítica | Baixo | **P0** |
| LGPD-01 | LGPD | 🔴 Crítica | Médio | **P0** |
| LGPD-02 | LGPD | 🔴 Crítica | Médio | **P0** |
| SEC-02 | Segurança | 🔴 Crítica | Baixo | **P1** |
| SEC-03 | Segurança | 🔴 Crítica | Baixo | **P1** |
| SEC-04 | Segurança | 🟠 Alta | Médio | **P1** |
| SEC-05 | Segurança | 🟠 Alta | Médio | **P1** |
| SEC-06 | Segurança | 🟠 Alta | Médio | **P1** |
| SEC-07 | Segurança | 🟠 Alta | Baixo | **P1** |
| LGPD-03 | LGPD | 🟠 Alta | Baixo | **P1** |
| LGPD-04 | LGPD | 🟠 Alta | Alto | **P2** |
| LGPD-05 | LGPD | 🟠 Alta | Médio | **P2** |
| SEC-08 | Segurança | 🟡 Média | Baixo | **P2** |
| SEC-09 | Segurança | 🟡 Média | Baixo | **P2** |
| SEC-10 | Segurança | 🟡 Média | Baixo | **P2** |
| SEC-11 | Segurança | 🟡 Média | Baixo | **P2** |
| LGPD-06 | LGPD | 🟡 Média | Alto | **P3** |
| LGPD-07 | LGPD | 🟡 Média | Médio | **P3** |
| LGPD-08 | LGPD | 🟡 Média | Baixo | **P2** |
| LGPD-09 | LGPD | 🟡 Média | Alto | **P3** |

---

## 4. O que está bem feito

Antes de encerrar, vale registrar o que o projeto já implementa corretamente:

- **BCrypt com strength 12** — custo computacional adequado para senhas.
- **JWT stateless** — sem estado de sessão no servidor.
- **`@PreAuthorize` no nível do método** — dupla camada de controle de acesso além do `SecurityConfig`.
- **Preferências de privacidade de candidatura** — o sistema de `compartilharXxx` é uma implementação elegante e conforme com o espírito da LGPD (art. 18 — controle pelo titular).
- **FluxoStatusCandidatura** — validação de transições de estado garante integridade do processo seletivo.
- **GlobalExceptionHandler** — trata exceções sem expor stack traces.
- **Validação de CPF com `@CPF`** — usa algoritmo de dígitos verificadores.
- **Upload para Supabase com path único por UUID** — evita colisões e enumeração de arquivos.
- **`HistoricoStatusCandidatura`** — trilha de auditoria para mudanças de status (parcialmente atende ao art. 37 da LGPD).
- **Seed script com aprovação de empresa por admin** — fluxo de aprovação protege contra empresas fraudulentas.

---

*Relatório gerado por análise estática do código-fonte. Recomenda-se revisão complementar com ferramentas automatizadas (OWASP Dependency Check, SonarQube, Snyk) antes do lançamento em produção.*
