# Checklist de Deploy — MatchVagas (Segurança/LGPD)

> Branch: `fix/seguranca-lgpd`. O **código** das 20 correções de segurança/LGPD está
> pronto e verificado. Este checklist cobre os passos **operacionais** (fora do código)
> obrigatórios antes de subir em produção. Execute na ordem.

---

## 0. Pré-requisitos

- [ ] Acesso ao painel do provedor de hospedagem (Render) para setar env vars.
- [ ] Acesso ao painel do Supabase (Storage + Settings > API).
- [ ] Acesso ao banco de produção (psql / cliente SQL) para conferência de schema e backfill.
- [ ] `openssl`, `git` e `java`/`mvn` disponíveis localmente.

---

## 1. Gerar os segredos fortes

```bash
# JWT (SEC-01) — mínimo 32 bytes; use 64
openssl rand -base64 64        # → JWT_SECRET

# CPF em repouso (LGPD-04)
openssl rand -base64 32        # → CPF_ENCRYPTION_KEY   (AES-256-GCM)
openssl rand -base64 32        # → CPF_HASH_SECRET      (HMAC-SHA256 de unicidade)
```

- [ ] `JWT_SECRET` gerado (não pode conter `change`/`changeme`/`troque_em_producao`).
- [ ] `CPF_ENCRYPTION_KEY` gerado e **guardado em cofre** (trocar depois invalida os CPFs já cifrados).
- [ ] `CPF_HASH_SECRET` gerado e guardado (trocar depois invalida os `cpf_hash`).

> ⚠️ Guarde `CPF_ENCRYPTION_KEY` e `CPF_HASH_SECRET` num secret manager. Perdê-los =
> perder acesso aos CPFs cifrados.

---

## 2. Configurar variáveis de ambiente no provedor (Render)

### Obrigatórias (sem elas → fallback inseguro / boot abortado com `APP_ENV=prod`)

- [ ] `APP_ENV=prod`  ← ativa o `StartupSecretsValidator` (aborta o boot se segredo faltar/fraco)
- [ ] `JWT_SECRET=<passo 1>`
- [ ] `CPF_ENCRYPTION_KEY=<passo 1>`
- [ ] `CPF_HASH_SECRET=<passo 1>`

### CORS (SEC-08) — sem isso o default é `*` e credenciais desligadas

- [ ] `CORS_ALLOWED_ORIGINS=https://app.matchvagas.com,https://www.matchvagas.com` (domínios reais do front)
- [ ] `CORS_ALLOW_CREDENTIALS=true` **somente** se usar cookies e com origens explícitas acima (a API usa JWT no header, então geralmente mantenha `false`)

### Rate limiting atrás de proxy

- [ ] `APP_TRUSTED_PROXY_COUNT=1` (Render = 1 reverse proxy; ajuste se houver CDN/LB extra)

### Banco de dados

- [ ] `DATABASE_URL`, `DATABASE_PORT`, `DATABASE_NAME`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `DATABASE_PREFIX`, `DATABASE_DRIVER`, `DATABASE_DIALECT`, `DATABASE_PREPARED_STATEMENTS`

### Supabase Storage

- [ ] `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY`
- [ ] `SUPABASE_BUCKET=curriculos`
- [ ] `SUPABASE_IMAGES_BUCKET=imagens-perfil`

### E-mail (Resend é o padrão; Render bloqueia SMTP de saída)

- [ ] `RESEND_API_KEY`, `RESEND_FROM=noreply@seudominio.com` (remetente verificado)
- [ ] (manter `MAIL_USE_GMAIL=false`)

### Retenção LGPD (opcionais — já têm default)

- [ ] `LGPD_RETENCAO_ENABLED=true`
- [ ] `LGPD_RETENCAO_INATIVIDADE_ANOS=2`
- [ ] `LGPD_RETENCAO_CURRICULO_ANOS=1`

---

## 3. Rotacionar credenciais vazadas (CRÍTICO)

O `.env` foi commitado no histórico (`git show d9edd70:.env`) e o `.env` atual contém
`SUPABASE_SERVICE_ROLE_KEY` e `MAIL_PASSWORD` reais.

- [ ] Rotacionar `DATABASE_PASSWORD` no provedor do banco.
- [ ] Rotacionar `SUPABASE_SERVICE_ROLE_KEY` (Supabase > Settings > API > roll key).
- [ ] Rotacionar `MAIL_PASSWORD` / criar nova senha de app / nova `RESEND_API_KEY`.
- [ ] Rotacionar qualquer outro segredo presente no `.env`.
- [ ] Confirmar que `.env` está no `.gitignore`.
- [ ] Limpar o histórico do git (BFG ou git-filter-repo):
  ```bash
  # backup antes!
  git filter-repo --path .env --invert-paths --force
  # depois: force-push coordenado + todos reclonam
  ```

---

## 4. Supabase — bucket privado (LGPD-08)

- [ ] Tornar o bucket de imagens (`imagens-perfil`) **privado** no painel do Supabase.
- [ ] Backfill dos registros antigos de foto/logo: valores antigos guardam a **URL pública
      completa**; o código novo espera o **object path**. Rodar a migração (idempotente):
  ```bash
  curl -X POST https://<host>/api/admin/migracao/normalizar-urls-imagens \
       -H "Authorization: Bearer <TOKEN_ADMIN>"
  # resposta: { operacao, total, atualizados, jaProcessados, semValor, erros, mensagens }
  ```
  - [ ] `erros == 0` na resposta; se houver, ver `mensagens` para diagnóstico.
- [ ] Validar: abrir a URL pública antiga deve dar 403; endpoint de foto/logo deve
      retornar URL assinada válida.

> Obs.: a migração apenas reescreve a **referência** (URL → path) no banco; ela **não move**
> o arquivo entre buckets. Se os arquivos estiverem num bucket público diferente, faça o
> re-upload para o bucket privado (`imagens-perfil`) antes/depois conforme necessário.

---

## 5. Backfill de CPF (LGPD-04)

CPFs existentes estão em texto puro com `cpf_hash` nulo. São lidos normalmente (compat.
de legado), mas a checagem de unicidade só cobre registros com hash.

- [ ] Rodar a migração de backfill (idempotente) — cifra o CPF em repouso e popula `cpf_hash`:
  ```bash
  curl -X POST https://<host>/api/admin/migracao/backfill-cpf \
       -H "Authorization: Bearer <TOKEN_ADMIN>"
  # resposta: { operacao, total, atualizados, jaProcessados, semValor, erros, mensagens }
  ```
  - [ ] `erros == 0` na resposta.
- [ ] Conferir: `SELECT count(*) FROM candidatos WHERE cpf_hash IS NULL AND cpf IS NOT NULL;` → deve ser 0.
- [ ] Conferir que os CPFs estão cifrados no banco: `SELECT cpf FROM candidatos LIMIT 3;`
      → devem começar com o prefixo `enc:v1:`.

> ⚠️ Rode este backfill **depois** de já ter setado `CPF_ENCRYPTION_KEY`/`CPF_HASH_SECRET`
> definitivos — trocá-los após o backfill invalida todos os CPFs cifrados e hashes.

---

## 6. Schema (ddl-auto=update)

Com `spring.jpa.hibernate.ddl-auto=update`, o Hibernate cria/altera tabelas no boot.

- [ ] Subir primeiro em **homologação** e conferir:
  - [ ] tabela `auditoria_acesso` criada.
  - [ ] coluna `cpf_hash` adicionada em `candidatos`.
  - [ ] coluna `cpf` alargada (comporta o texto cifrado em Base64).
- [ ] Conferir que nenhuma coluna existente foi perdida/renomeada indevidamente.

---

## 7. Deploy e smoke test

- [ ] Base do PR correta: `feature/notificacoes-candidatura` (ou já mesclada na `main`),
      para o diff conter só as mudanças de segurança.
- [ ] Build local OK: `./mvnw clean verify`
- [ ] Deploy em homologação com `APP_ENV=prod` → **boot NÃO deve abortar** (confirma segredos ok).
- [ ] Smoke test (usar os `.http` em `requests/`):
  - [ ] `POST /register` e `POST /login` retornam JWT.
  - [ ] `POST /esqueceu-senha` → e-mail chega (Resend).
  - [ ] Upload de currículo/foto → URL assinada funciona; URL pública direta dá 403.
  - [ ] `GET /meus-dados` (exportação LGPD-05) retorna os dados do titular.
  - [ ] Rate limiting responde 429 após o limite (teste no login).
  - [ ] Requisição de origem NÃO listada no CORS é bloqueada.
- [ ] Promover para produção.

---

## 8. Pós-deploy

- [ ] Confirmar logs em nível INFO (sem CPF/e-mail/token vazando).
- [ ] Confirmar job mensal de retenção (LGPD-09) agendado.
- [ ] Revogar acessos temporários usados no deploy.
- [ ] Atualizar a nota de memória `seguranca-lgpd-deploy` marcando o que foi concluído.
