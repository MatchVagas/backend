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

- [x] 🔴 **Comunicação empresa ↔ candidato** — entidade `Mensagem` atrelada à candidatura (a candidatura é o thread; participantes = candidato dono + gestor da empresa da vaga). REST em `/api/mensagens` (enviar, listar paginado, marcar lidas, contagens), notificação in-app/e-mail ao destinatário e limpeza de FK nos fluxos de exclusão. Falta apenas o push em tempo real (item abaixo).
- [x] 🟡 **Notificações em tempo real** (SSE) — `RealtimeService` mantém um stream por usuário em `GET /api/realtime/stream` (auth por header JWT), com heartbeat anti-timeout. `NotificacaoService` emite o evento `notificacao` (badge) e `MensagemService` o evento `mensagem` (conversa aberta atualiza na hora). Escolhido SSE em vez de WebSocket por ser push unidirecional sobre a stack stateless existente.
- [x] 🟡 **Funil de candidaturas (Kanban) para a empresa** — `GET /api/candidaturas/empresa/vaga/{vagaId}/kanban` devolve o board: coluna "Novas" (sem status) + status na ordem canônica do fluxo, cada uma com seus cards (com filtro de privacidade). Mover card reusa o PATCH de status existente.
- [x] 🟡 **Busca melhor**: `GET /api/vagas/busca` com filtros de texto livre (título/descrição/requisitos/área), área, tipo, modalidade, escolaridade, cidade, estado, empresa e faixa salarial, ordenação via `?sort=` e só ativas/não expiradas por padrão. Implementada com **JPA Specifications (Criteria API)** — portátil entre Postgres e MySQL, sem `tsvector`/`MATCH AGAINST`. Full-text real dedicado fica para a Fase 4.
- [x] 🟢 **Salvar vaga** (favoritos) e **alertas de vaga** por critérios. Favoritos em `/api/favoritos` (salvar idempotente, listar, remover). Alertas em `/api/alertas` (CRUD + ativar/desativar); ao publicar uma vaga ATIVA, `VagaService` casa os alertas ativos e notifica os candidatos (reusa notificação in-app + e-mail + realtime). FKs das tabelas novas com `ON DELETE CASCADE`.

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
