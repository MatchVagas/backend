#!/usr/bin/env bash
# Seed de dados de teste — ambiente remoto
# Uso: bash scripts/seed-remote.sh

set -euo pipefail

BASE_URL="https://backend-tgi8.onrender.com"
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'

log()  { echo -e "${CYAN}[INFO]${NC}  $*"; }
ok()   { echo -e "${GREEN}[OK]${NC}    $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC}  $*"; }
fail() { echo -e "${RED}[ERRO]${NC}  $*"; }

# Wrapper curl: retorna body, falha silenciosa no HTTP error
api() {
  local method=$1 path=$2; shift 2
  curl -s -X "$method" "$BASE_URL$path" \
    -H "Content-Type: application/json" \
    "$@"
}

# ──────────────────────────────────────────────────────────────
# 1. LOOKUP — capturar IDs de referência
# ──────────────────────────────────────────────────────────────
log "Buscando dados de lookup..."

TIPO_VAGA_ID=$(api GET /api/lookup/vagas/tipos | python3 -c "import sys,json; d=json.load(sys.stdin); print(d[0]['id'])" 2>/dev/null || echo "")
MODALIDADE_ID=$(api GET /api/lookup/vagas/modalidades | python3 -c "import sys,json; d=json.load(sys.stdin); print(d[0]['id'])" 2>/dev/null || echo "")
ESCOLARIDADE_ID=$(api GET /api/lookup/vagas/escolaridades | python3 -c "import sys,json; d=json.load(sys.stdin); print(d[0]['id'])" 2>/dev/null || echo "")
PORTE_ID=$(api GET /api/lookup/vagas/portes | python3 -c "import sys,json; d=json.load(sys.stdin); print(d[0]['id'])" 2>/dev/null || echo "")
RAMO_ID=$(api GET /api/lookup/vagas/ramos | python3 -c "import sys,json; d=json.load(sys.stdin); print(d[0]['id'])" 2>/dev/null || echo "")
STATUS_VAGA_ID=$(api GET /api/lookup/vagas/status | python3 -c "
import sys,json
d=json.load(sys.stdin)
ativa=[x for x in d if x.get('descricao','').lower()=='ativa']
print(ativa[0]['id'] if ativa else d[0]['id'])
" 2>/dev/null || echo "")
CIDADE_ID=$(api GET /api/localizacao/cidades | python3 -c "import sys,json; d=json.load(sys.stdin); print(d[0]['id'])" 2>/dev/null || echo "")

if [[ -z "$TIPO_VAGA_ID" || -z "$MODALIDADE_ID" || -z "$CIDADE_ID" ]]; then
  fail "Não foi possível buscar lookups. Verifique se o backend está acessível em $BASE_URL"
  exit 1
fi

ok "tipo_vaga=$TIPO_VAGA_ID  modalidade=$MODALIDADE_ID  escolaridade=$ESCOLARIDADE_ID"
ok "porte=$PORTE_ID  ramo=$RAMO_ID  status_vaga=$STATUS_VAGA_ID  cidade=$CIDADE_ID"

# ──────────────────────────────────────────────────────────────
# 2. REGISTRAR USUÁRIOS
# ──────────────────────────────────────────────────────────────
log "Registrando usuários..."

register_user() {
  local nome=$1 email=$2 senha=$3 nascimento=$4 tipo=$5
  local body
  body=$(api POST /api/auth/register -d "{
    \"nome\": \"$nome\",
    \"email\": \"$email\",
    \"senha\": \"$senha\",
    \"dataNascimento\": \"${nascimento}T00:00:00\",
    \"tipoUsuario\": \"$tipo\"
  }")
  local status
  status=$(echo "$body" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('id','já existe'))" 2>/dev/null || echo "erro")
  echo "$status"
}

# Candidatos
register_user "Ana Lima"       "ana.lima@seed.com"       "Seed@123" "1998-04-10" "CANDIDATO" > /dev/null
register_user "Bruno Costa"    "bruno.costa@seed.com"    "Seed@123" "1993-07-22" "CANDIDATO" > /dev/null
register_user "Carla Mendes"   "carla.mendes@seed.com"   "Seed@123" "2000-01-05" "CANDIDATO" > /dev/null

# Empresas
register_user "Rafael Gestor"  "rafael.gestor@seed.com"  "Seed@123" "1985-09-15" "EMPRESA"   > /dev/null
register_user "Fernanda Sousa" "fernanda.sousa@seed.com" "Seed@123" "1980-03-28" "EMPRESA"   > /dev/null

# Admin
register_user "Admin Seed"     "admin.seed@seed.com"     "Seed@123" "1979-11-20" "ADMIN"     > /dev/null

ok "Usuários registrados (ou já existentes)"

# ──────────────────────────────────────────────────────────────
# 3. LOGIN — capturar tokens
# ──────────────────────────────────────────────────────────────
log "Fazendo login..."

login() {
  local email=$1 senha=$2
  api POST /api/auth/login -d "{\"email\": \"$email\", \"senha\": \"$senha\"}" \
    | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('token',''))" 2>/dev/null || echo ""
}

TOKEN_ANA=$(login "ana.lima@seed.com" "Seed@123")
TOKEN_BRUNO=$(login "bruno.costa@seed.com" "Seed@123")
TOKEN_CARLA=$(login "carla.mendes@seed.com" "Seed@123")
TOKEN_RAFAEL=$(login "rafael.gestor@seed.com" "Seed@123")
TOKEN_FERNANDA=$(login "fernanda.sousa@seed.com" "Seed@123")
TOKEN_ADMIN=$(login "admin.seed@seed.com" "Seed@123")

for tok in TOKEN_ANA TOKEN_BRUNO TOKEN_CARLA TOKEN_RAFAEL TOKEN_FERNANDA TOKEN_ADMIN; do
  if [[ -z "${!tok}" ]]; then
    fail "Falha no login para $tok — verifique o registro anterior"
    exit 1
  fi
done

ok "Tokens obtidos para todos os usuários"

# ──────────────────────────────────────────────────────────────
# 4. PERFIS DE CANDIDATO
# ──────────────────────────────────────────────────────────────
log "Criando perfis de candidato..."

gen_cpf() {
  python3 -c "
import random
def gen():
    n = [random.randint(0,9) for _ in range(9)]
    for j in range(2):
        s = sum((10+j-i)*n[i] for i in range(9+j))
        r = (s*10) % 11
        n.append(0 if r >= 10 else r)
    cpf = ''.join(map(str, n))
    return f'{cpf[:3]}.{cpf[3:6]}.{cpf[6:9]}-{cpf[9:]}'
print(gen())
"
}

create_candidato() {
  local token=$1 cpf=$2 resumo=$3 disponibilidade=$4 pretensao=$5
  local resp
  resp=$(curl -s -X POST "$BASE_URL/api/candidatos" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $token" \
    -d "{
      \"cpf\": \"$cpf\",
      \"resumoProfissional\": \"$resumo\",
      \"disponibilidade\": \"$disponibilidade\",
      \"pretensaoSalarial\": $pretensao
    }")
  echo "$resp" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('id','já existe'))" 2>/dev/null || echo "?"
}

ID_CAND_ANA=$(create_candidato   "$TOKEN_ANA"   "529.982.247-25" \
  "Desenvolvedora frontend com 4 anos de experiência em React e TypeScript." \
  "Imediata" 6500)

ID_CAND_BRUNO=$(create_candidato "$TOKEN_BRUNO" "$(gen_cpf)" \
  "Engenheiro de dados especializado em pipelines ETL, Spark e AWS." \
  "30 dias" 9000)

ID_CAND_CARLA=$(create_candidato "$TOKEN_CARLA" "390.533.447-05" \
  "Analista de QA com foco em automação de testes usando Cypress e Playwright." \
  "15 dias" 5500)

ok "Candidatos criados — ids: $ID_CAND_ANA | $ID_CAND_BRUNO | $ID_CAND_CARLA"

# ──────────────────────────────────────────────────────────────
# 5. EMPRESAS
# ──────────────────────────────────────────────────────────────
log "Criando empresas..."

create_empresa() {
  local token=$1 cnpj=$2 razao=$3 fantasia=$4 descricao=$5
  local resp
  resp=$(curl -s -X POST "$BASE_URL/api/empresas" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $token" \
    -d "{
      \"cnpj\": \"$cnpj\",
      \"razaoSocial\": \"$razao\",
      \"nomeFantasia\": \"$fantasia\",
      \"descricao\": \"$descricao\",
      \"porteId\": $PORTE_ID,
      \"ramoId\": $RAMO_ID,
      \"site\": \"https://www.$(echo "$fantasia" | tr '[:upper:]' '[:lower:]' | tr ' ' '-').com.br\"
    }")
  echo "$resp" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('id',''))" 2>/dev/null || echo ""
}

EMPRESA_RAFAEL_ID=$(create_empresa "$TOKEN_RAFAEL" \
  "12.345.678/0001-95" "Inova Tech Soluções Ltda" "InovaTech" \
  "Startup de inovação em software e inteligência artificial.")

EMPRESA_FERNANDA_ID=$(create_empresa "$TOKEN_FERNANDA" \
  "98.765.432/0001-00" "Conecta Digital SA" "ConectaDigital" \
  "Consultoria em transformação digital para médias e grandes empresas.")

# Fallback: buscar empresa existente se a criação retornar vazio (já existe ou PENDENTE)
if [[ -z "$EMPRESA_RAFAEL_ID" ]]; then
  EMPRESA_RAFAEL_ID=$(curl -s "$BASE_URL/api/empresas" \
    | python3 -c "import sys,json; d=json.load(sys.stdin); print(d[0]['id'] if d else '')" 2>/dev/null || echo "1")
fi
if [[ -z "$EMPRESA_FERNANDA_ID" ]]; then
  EMPRESA_FERNANDA_ID=$(curl -s "$BASE_URL/api/empresas" \
    | python3 -c "import sys,json; d=json.load(sys.stdin); print(d[-1]['id'] if d else '1')" 2>/dev/null || echo "1")
fi

ok "Empresas — InovaTech id=$EMPRESA_RAFAEL_ID | ConectaDigital id=$EMPRESA_FERNANDA_ID"

# Admin aprova empresas (necessário para poder criar vagas)
log "Aprovando empresas..."
for EMP_ID in "$EMPRESA_RAFAEL_ID" "$EMPRESA_FERNANDA_ID"; do
  STATUS=$(curl -s -X PATCH "$BASE_URL/api/admin/empresas/$EMP_ID/aprovar" \
    -H "Authorization: Bearer $TOKEN_ADMIN" \
    | python3 -c "import sys,json; print(json.load(sys.stdin).get('status','?'))" 2>/dev/null || echo "?")
  ok "Empresa $EMP_ID aprovada -> $STATUS"
done

# ──────────────────────────────────────────────────────────────
# 6. VAGAS
# ──────────────────────────────────────────────────────────────
log "Criando vagas..."

create_vaga() {
  local token=$1 empresa_id=$2 titulo=$3 descricao=$4 requisitos=$5 area=$6 sal_min=$7 sal_max=$8
  curl -s -X POST "$BASE_URL/api/vagas" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $token" \
    -d "{
      \"empresaId\": $empresa_id,
      \"titulo\": \"$titulo\",
      \"descricao\": \"$descricao\",
      \"requisitos\": \"$requisitos\",
      \"tipoVagaId\": $TIPO_VAGA_ID,
      \"modalidadeId\": $MODALIDADE_ID,
      \"salarioMinimo\": $sal_min,
      \"salarioMaximo\": $sal_max,
      \"beneficios\": \"VR, VT, Plano de Saude, Home Office\",
      \"cargaHoraria\": \"40h semanais\",
      \"idadeMinima\": 18,
      \"idadeMaxima\": 55,
      \"escolaridadeId\": $ESCOLARIDADE_ID,
      \"areaAtuacao\": \"$area\",
      \"dataExpiracao\": \"2026-12-31T23:59:59\",
      \"statusVagaId\": $STATUS_VAGA_ID,
      \"numeroVagas\": 2,
      \"cidadeId\": $CIDADE_ID
    }" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('id',''))" 2>/dev/null || echo ""
}

VAGA1=$(create_vaga "$TOKEN_RAFAEL" "$EMPRESA_RAFAEL_ID" \
  "Desenvolvedor Frontend React Pleno" \
  "Buscamos desenvolvedor React com experiência em TypeScript e testes unitários para compor nosso time de produto." \
  "React, TypeScript, Jest, Git, REST APIs" \
  "Tecnologia da Informação" 6000 9500)

VAGA2=$(create_vaga "$TOKEN_RAFAEL" "$EMPRESA_RAFAEL_ID" \
  "Engenheiro de Machine Learning" \
  "Vaga para cientista/engenheiro ML para atuar em modelos de recomendação e NLP em produção." \
  "Python, TensorFlow ou PyTorch, MLflow, Docker, SQL" \
  "Inteligência Artificial" 10000 16000)

VAGA3=$(create_vaga "$TOKEN_FERNANDA" "$EMPRESA_FERNANDA_ID" \
  "Analista de QA Automacao" \
  "Profissional de QA para estruturar estratégia de automação de testes end-to-end e de API." \
  "Cypress, Playwright, Postman, CI/CD, Git" \
  "Qualidade de Software" 5500 8000)

VAGA4=$(create_vaga "$TOKEN_FERNANDA" "$EMPRESA_FERNANDA_ID" \
  "Engenheiro de Dados Senior" \
  "Responsável por projetar e manter pipelines de dados em larga escala usando cloud AWS." \
  "Apache Spark, Airflow, AWS (S3/Glue/Redshift), Python, dbt" \
  "Engenharia de Dados" 12000 18000)

VAGA5=$(create_vaga "$TOKEN_ADMIN" "$EMPRESA_RAFAEL_ID" \
  "UX Designer Junior" \
  "Designer de UX para atuar na jornada de produtos digitais, com foco em pesquisa e prototipação." \
  "Figma, pesquisa com usuarios, prototipacao, Design System" \
  "Design de Produto" 3500 5500)

ok "Vagas criadas — IDs: $VAGA1 | $VAGA2 | $VAGA3 | $VAGA4 | $VAGA5"

# ──────────────────────────────────────────────────────────────
# 7. CANDIDATURAS
# ──────────────────────────────────────────────────────────────
log "Criando candidaturas..."

apply() {
  local token=$1 vaga_id=$2
  [[ -z "$vaga_id" ]] && return
  curl -s -X POST "$BASE_URL/api/candidaturas" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $token" \
    -d "{
      \"vagaId\": $vaga_id,
      \"compartilharObjetivoProfissional\": true,
      \"compartilharDisponibilidade\": true,
      \"compartilharPretensaoSalarial\": true,
      \"compartilharExperiencias\": true,
      \"compartilharFormacoes\": true,
      \"compartilharTelefone\": false,
      \"compartilharEndereco\": false
    }" > /dev/null
}

# Ana se candidata a frontend e UX
apply "$TOKEN_ANA"   "$VAGA1"
apply "$TOKEN_ANA"   "$VAGA5"

# Bruno se candidata a engenheiro de dados e ML
apply "$TOKEN_BRUNO" "$VAGA4"
apply "$TOKEN_BRUNO" "$VAGA2"

# Carla se candidata a QA e frontend
apply "$TOKEN_CARLA" "$VAGA3"
apply "$TOKEN_CARLA" "$VAGA1"

ok "Candidaturas criadas"

# ──────────────────────────────────────────────────────────────
# RESUMO FINAL
# ──────────────────────────────────────────────────────────────
echo ""
echo -e "${GREEN}══════════════════════════════════════════════════${NC}"
echo -e "${GREEN}  SEED CONCLUIDO — $BASE_URL${NC}"
echo -e "${GREEN}══════════════════════════════════════════════════${NC}"
echo ""
echo "  USUARIOS (senha: Seed@123)"
echo "    Candidatos : ana.lima@seed.com | bruno.costa@seed.com | carla.mendes@seed.com"
echo "    Empresas   : rafael.gestor@seed.com | fernanda.sousa@seed.com"
echo "    Admin      : admin.seed@seed.com"
echo ""
echo "  EMPRESAS"
echo "    InovaTech      id=$EMPRESA_RAFAEL_ID"
echo "    ConectaDigital id=$EMPRESA_FERNANDA_ID"
echo ""
echo "  VAGAS"
echo "    $VAGA1 - Desenvolvedor Frontend React Pleno   (InovaTech)"
echo "    $VAGA2 - Engenheiro de Machine Learning       (InovaTech)"
echo "    $VAGA3 - Analista de QA Automacao             (ConectaDigital)"
echo "    $VAGA4 - Engenheiro de Dados Senior           (ConectaDigital)"
echo "    $VAGA5 - UX Designer Junior                   (InovaTech)"
echo ""
echo "  CANDIDATURAS"
echo "    Ana   -> Vagas $VAGA1, $VAGA5"
echo "    Bruno -> Vagas $VAGA4, $VAGA2"
echo "    Carla -> Vagas $VAGA3, $VAGA1"
echo ""
