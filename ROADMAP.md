# Roadmap — MatchVagas (Backend)

Evolução natural do projeto, priorizada da mais óbvia à mais estratégica. Baseada
no estado atual do código: matching por regras (`SugestaoVagaService`), notificações
in-app + e-mail (`NotificacaoService`), fluxo de candidatura com histórico de status,
conformidade LGPD, storage no Supabase, rate limiting, e o que já foi entregue na
branch de segurança (hardening, migrações pós-deploy e paginação).

> Legenda de esforço: 🟢 baixo · 🟡 médio · 🔴 alto

---

## Fase 1 — Fechar o "produção-ready" ✅

**Objetivo:** separar "no ar" de "operável". Continuação direta do trabalho atual.
**Horizonte:** semanas.

- [x] 🟢 **Verificação de e-mail** no cadastro (token de ativação) — evita contas falsas/spam.
- [x] 🟢 **Refresh token** + revogação de sessão — hoje só há JWT de acesso, usuário é deslogado ao expirar.
- [x] 🟢 **CI/CD** (`.github/workflows`) rodando os testes em cada PR (build + test como gate).
- [x] 🟢 **Terminar rollout de paginação** nas listagens de maior volume (candidaturas minhas/empresa, notificações). Restam listas menores/limitadas (pendentes, admins).
- [x] 🟡 **Observabilidade**: Actuator (health público, resto ADMIN) + errorId de correlação nas falhas 500 (gancho para Sentry).

> ✅ Fase 1 implementada na branch `feature/fase1-producao-ready`.

### Próximo passo imediato

- [ ] 🟢 **Mergear em `main`** as branches `feature/fase1-producao-ready` e `fix/seguranca-lgpd` (se ainda pendente). Trabalho entregue e não mergeado é risco parado.

## Fase 2 — Fechar o ciclo de recrutamento dentro da plataforma

**Objetivo:** deixar de ser um CRUD de vagas e virar plataforma de recrutamento.
Hoje a plataforma **abre** o processo (candidatura, status) mas não o **conclui**:
quando a empresa quer chamar para entrevista, a conversa sai da plataforma — e com
ela o usuário. É o que **retém usuário**.
**Horizonte:** 1–2 meses.

Em ordem de implementação:

1. [ ] 🔴 **Mensagens atreladas à candidatura** — não existe entidade de mensagem hoje (maior gap funcional). Escopo contido: não é chat livre, é comunicação dentro do processo seletivo ("convite para entrevista"). É o item que muda a categoria do produto.
2. [ ] 🟡 **Funil de candidaturas (Kanban) para a empresa** — os dados já existem (`HistoricoStatusCandidatura`); é essencialmente uma view agregada por status.
3. [ ] 🟡 **Notificações em tempo real** — completa a experiência das mensagens. SSE é mais simples que WebSocket e suficiente aqui (fluxo só servidor→cliente).
4. [ ] 🟢 **Salvar vaga** (favoritos) e **alertas de vaga** por critérios — menores, encaixam entre as maiores.
5. [ ] 🟡 **Busca melhor**: filtro por faixa salarial, localização/geo, ordenação e full-text (Postgres `tsvector`).

## Fase 2B — Administração do perfil da empresa (paralela à Fase 2)

**Objetivo:** dar à empresa as ferramentas de gestão do dia a dia e fechar o ciclo
de moderação do admin, que hoje existe pela metade (aprovar/rejeitar sem motivo,
sem suspensão, sem re-submissão). Os blocos de moderação e gestão de vagas são
baratos e podem começar antes da Fase 2; o bloco de equipe é pré-requisito lógico
das mensagens empresa↔candidato.
**Horizonte:** intercalado com a Fase 2.

**Moderação (admin da plataforma)** — em ordem:

- [ ] 🟢 **Motivo na rejeição** de empresa + notificação — hoje `rejeitarEmpresa` não registra nem comunica o porquê (`NotificacaoService` já faz in-app + e-mail).
- [ ] 🟢 **Re-submissão** — empresa rejeitada corrige o cadastro e volta à fila, em vez de morrer no `REJEITADA`.
- [ ] 🟢 **Status `SUSPENSA`** no enum `StatusEmpresa` — oculta as vagas sem apagar nada (apagar briga com a auditoria LGPD).
- [ ] 🟢 **Busca/filtros na listagem admin** de empresas (status, ramo, porte, CNPJ).
- [ ] 🟢 **CNPJ por dígito verificador** no cadastro (SEC-04 do audit, se ainda pendente).

**Gestão de vagas (dia a dia do recrutador):**

- [ ] 🟢 **Duplicar vaga**, **encerrar antecipadamente** e **renovar/estender** `dataExpiracao`.
- [ ] 🟢 **Rascunho de vaga** — estado novo no lookup `StatusVaga`, salvar antes de publicar.
- [ ] 🟢 **Notificação de vaga prestes a expirar** (seguir o padrão de scheduler do `RetencaoDadosService`).
- [ ] 🟡 **Templates de vaga** da empresa (descrição/benefícios padrão).

**Equipe (estrutural — pré-requisito das mensagens da Fase 2):**

- [ ] 🟡 **`empresa_membros`** com papéis (`GESTOR`/`RECRUTADOR`) — hoje `Empresas.usuario_id` é `unique`: uma empresa = um único usuário; se o dono da conta sai da empresa, a conta morre.
- [ ] 🟡 **Convite de membros por e-mail** (reaproveitar `EmailService` + fluxo de tokens) e **transferência de gestor**.

**Perfil público / employer branding:**

- [ ] 🟡 **Enriquecer o perfil**: banner/capa, redes sociais, nº de funcionários, ano de fundação, benefícios padrão.
- [ ] 🟢 **Página pública da empresa** — perfil + vagas ativas dela (hoje dá para listar empresas, mas não existe a visão "vagas desta empresa" como página de atração).
- [ ] 🟢 **Selo "verificada"** (CNPJ validado + aprovação do admin).
- [ ] 🟡 **Painel da empresa**: vagas ativas × expiradas, candidaturas novas por vaga — complementa o Kanban da Fase 2 com contadores sobre dados que `HistoricoStatusCandidatura` já tem.

## Fase 2.5 — Matching estruturado (ponte para a inteligência)

**Objetivo:** antes de partir para IA, fazer o `SugestaoVagaService` usar os dados
que o banco **já tem** e hoje ficam fora do score: `Habilidade`, `Formacao` e
`Experiencia`. O score atual compara palavras do objetivo profissional com
título/área/requisitos — superficial. Além de melhorar as sugestões imediatamente,
esta fase cria o dado estruturado que o matching semântico da Fase 3 vai consumir.
**Horizonte:** paralelo ao fim da Fase 2.

1. [ ] 🟡 **Catálogo de habilidades** (`habilidades_catalogo`) — hoje `Habilidade` é texto livre num `@Embeddable` ("Java" ≠ "java"). Catálogo com nome normalizado/único + migração dos dados existentes de `candidato_habilidades` para FK.
2. [ ] 🟡 **Requisitos estruturados na vaga** (`vaga_habilidades`) — habilidade exigida com `nivel_minimo` (enum `NivelHabilidade` existente) e flag `obrigatoria` (eliminatória vs. desejável). Campo `experiencia_minima_anos` em `Vagas`. O campo texto `requisitos` continua como descrição livre.
3. [ ] 🟡 **Score v2** — componentes ponderados (pesos via properties): habilidades atendidas com nível ≥ mínimo, escolaridade (o campo `ordem` de `Escolaridades` já ordena), localização/modalidade, anos de experiência, salário. Detalhe por componente na resposta ("você atende 4 de 5 requisitos") — explica o match na UI.
4. [ ] 🟢 **Materializar scores** (`match_scores`) só quando alertas/ranking exigirem — começar calculando on-demand no endpoint `sugestoes` existente.

## Fase 3 — Inteligência do match (diferencial competitivo)

**Objetivo:** evoluir do matching estruturado para **matching semântico**. Onde o
produto ganha diferencial real frente a um quadro de vagas comum. Consome o dado
estruturado criado na Fase 2.5.
**Horizonte:** após o núcleo estar sólido.

- [ ] 🔴 **Parsing de currículo** (PDF → dados estruturados) — elimina a maior fricção do cadastro e alimenta o matching sem depender de formulário preenchido.
- [ ] 🔴 **Match por embeddings** entre CV e vaga, com score e ranking (em vez de igualdade de campos).
- [ ] 🟡 **Recomendação nos dois sentidos**: vagas para o candidato *e* candidatos para a empresa (ranking de aderência no funil).
- [ ] 🟡 **Recursos assistivos**: resumo automático de perfil, geração de descrição de vaga, triagem inicial.

> **Camada de IA:** usar a API da Anthropic com os modelos **Claude** mais recentes
> (scoring semântico + geração), integrada como serviço à parte para não acoplar o
> core. Confirmar modelos e desenho da integração na documentação oficial antes de
> implementar.

## Fase 4 — Escala e negócio

**Objetivo:** sustentar crescimento e viabilizar receita. Só faz sentido com tração medida.
**Horizonte:** quando houver tração.

- [ ] 🔴 **Monetização**: assinatura/planos para empresas, destaque de vaga paga, acesso ao ranking completo de candidatos por plano.
- [ ] 🟡 **Dashboards/analytics** para a empresa (visualizações da vaga, conversão do funil).
- [ ] 🔴 **Arquitetura para volume**: fila assíncrona para e-mails/notificações (o envio síncrono no `NotificacaoService` vira gargalo), cache, search engine dedicado se necessário.
- [ ] 🔴 **Alcance**: app mobile, i18n, importação de perfil (LinkedIn), integrações ATS.

---

## Sequência recomendada

1. **Merge da Fase 1** em `main` — imediato e barato.
2. **Fase 2B (moderação + gestão de vagas)** — itens 🟢 que fecham ciclos pela metade; bons para intercalar desde já.
3. **Fase 2** é o que retém usuário — prioridade de produto (mensagens primeiro, com o bloco de equipe da 2B como pré-requisito).
4. **Fase 2.5** aprofunda o matching com dados que já existem — pode andar em paralelo ao fim da Fase 2 e é pré-requisito de dado para a Fase 3.
5. **Fase 3** é a aposta de diferencial, assim que o núcleo estiver sólido; a mais alinhada a "usar IA de verdade" no produto.
6. **Fase 4** só com tração medida.

**Caminho crítico:** merge → mensagens empresa↔candidato → Kanban → matching estruturado → IA.
