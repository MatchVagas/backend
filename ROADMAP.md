# Roadmap — MatchVagas (Backend)

Evolução natural do projeto, priorizada da mais óbvia à mais estratégica. Baseada
no estado atual do código: matching por regras (`SugestaoVagaService`), notificações
in-app + e-mail (`NotificacaoService`), fluxo de candidatura com histórico de status,
conformidade LGPD, storage no Supabase, rate limiting, e o que já foi entregue na
branch de segurança (hardening, migrações pós-deploy e paginação).

> Legenda de esforço: 🟢 baixo · 🟡 médio · 🔴 alto

---

## Fase 1 — Fechar o "produção-ready"
**Objetivo:** separar "no ar" de "operável". Continuação direta do trabalho atual.
**Horizonte:** semanas.

- [x] 🟢 **Verificação de e-mail** no cadastro (token de ativação) — evita contas falsas/spam.
- [x] 🟢 **Refresh token** + revogação de sessão — hoje só há JWT de acesso, usuário é deslogado ao expirar.
- [x] 🟢 **CI/CD** (`.github/workflows`) rodando os testes em cada PR (build + test como gate).
- [x] 🟢 **Terminar rollout de paginação** nas listagens de maior volume (candidaturas minhas/empresa, notificações). Restam listas menores/limitadas (pendentes, admins).
- [x] 🟡 **Observabilidade**: Actuator (health público, resto ADMIN) + errorId de correlação nas falhas 500 (gancho para Sentry).

> ✅ Fase 1 implementada na branch `feature/fase1-producao-ready`.

## Fase 2 — Tornar o produto usável de verdade
**Objetivo:** deixar de ser um CRUD de vagas e virar plataforma de recrutamento. É o que **retém usuário** — sem comunicação, a empresa sai da plataforma para conversar por fora.
**Horizonte:** 1–2 meses.

- [ ] 🔴 **Comunicação empresa ↔ candidato** — não existe entidade de mensagem hoje (maior gap funcional). Começar simples: mensagem atrelada à candidatura ("convite para entrevista").
- [ ] 🟡 **Notificações em tempo real** (WebSocket/SSE) — o `NotificacaoService` já faz in-app + e-mail; falta o push que evita polling.
- [ ] 🟡 **Funil de candidaturas (Kanban) para a empresa** — os status já existem (`HistoricoStatusCandidatura`); falta a visão de pipeline.
- [ ] 🟡 **Busca melhor**: filtro por faixa salarial, localização/geo, ordenação e full-text (Postgres `tsvector`).
- [ ] 🟢 **Salvar vaga** (favoritos) e **alertas de vaga** por critérios.

## Fase 3 — Inteligência do match (diferencial competitivo)
**Objetivo:** evoluir o matching por regras (idade/salário/área) para **matching semântico**. Onde o produto ganha diferencial real frente a um quadro de vagas comum.
**Horizonte:** após o núcleo estar sólido.

- [ ] 🔴 **Parsing de currículo** (PDF → dados estruturados).
- [ ] 🔴 **Match por embeddings** entre CV e vaga, com score e ranking (em vez de igualdade de campos).
- [ ] 🟡 **Recomendação nos dois sentidos**: vagas para o candidato *e* candidatos para a empresa.
- [ ] 🟡 **Recursos assistivos**: resumo automático de perfil, geração de descrição de vaga, triagem inicial.

> **Camada de IA:** usar a API da Anthropic com os modelos **Claude** mais recentes
> (scoring semântico + geração), integrada como serviço à parte para não acoplar o
> core. Confirmar modelos e desenho da integração na documentação oficial antes de
> implementar.

## Fase 4 — Escala e negócio
**Objetivo:** sustentar crescimento e viabilizar receita. Só faz sentido com tração medida.
**Horizonte:** quando houver tração.

- [ ] 🔴 **Monetização**: assinatura/planos para empresas, destaque de vaga paga.
- [ ] 🟡 **Dashboards/analytics** para a empresa (visualizações da vaga, conversão do funil).
- [ ] 🔴 **Arquitetura para volume**: fila assíncrona para e-mails/notificações (o envio síncrono no `NotificacaoService` vira gargalo), cache, search engine dedicado se necessário.
- [ ] 🔴 **Alcance**: app mobile, i18n, importação de perfil (LinkedIn), integrações ATS.

---

## Sequência recomendada

1. **Fase 1** é pré-requisito e barata — fazer já.
2. **Fase 2** é o que retém usuário — prioridade de produto.
3. **Fase 3** é a aposta de diferencial, assim que o núcleo estiver sólido; a mais alinhada a "usar IA de verdade" no produto.
4. **Fase 4** só com tração medida.
