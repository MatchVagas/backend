#!/usr/bin/env bash
# Seed de dados de teste — ambiente remoto
# Uso: bash scripts/seed-remote.sh

set -euo pipefail

BASE_URL="https://backend-production-7396d.up.railway.app"
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
# 1. REGISTRAR USUÁRIOS
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
register_user "Marcelo Tavares" "marcelo.tavares@seed.com" "Seed@123" "1982-02-14" "EMPRESA" > /dev/null
register_user "Juliana Ramos"   "juliana.ramos@seed.com"   "Seed@123" "1988-06-30" "EMPRESA" > /dev/null

# Admin
register_user "Admin Seed"     "admin.seed@seed.com"     "Seed@123" "1979-11-20" "ADMIN"     > /dev/null

ok "Usuários registrados (ou já existentes)"

# ──────────────────────────────────────────────────────────────
# 2. LOGIN — capturar tokens
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
TOKEN_MARCELO=$(login "marcelo.tavares@seed.com" "Seed@123")
TOKEN_JULIANA=$(login "juliana.ramos@seed.com" "Seed@123")
TOKEN_ADMIN=$(login "admin.seed@seed.com" "Seed@123")

for tok in TOKEN_ANA TOKEN_BRUNO TOKEN_CARLA TOKEN_RAFAEL TOKEN_FERNANDA TOKEN_MARCELO TOKEN_JULIANA TOKEN_ADMIN; do
  if [[ -z "${!tok}" ]]; then
    fail "Falha no login para $tok — verifique o registro anterior"
    exit 1
  fi
done

ok "Tokens obtidos para todos os usuários"

# ──────────────────────────────────────────────────────────────
# 3. LOOKUPS — garantir dados de referência (idempotente)
# ──────────────────────────────────────────────────────────────
log "Garantindo dados de lookup..."

# Busca um item de lookup cujo $match_field == $match_value (case-insensitive);
# se não existir, cria via POST (ADMIN) com $payload. Retorna o ID em ambos os casos.
ensure_lookup() {
  local get_path=$1 post_path=$2 match_field=$3 match_value=$4 payload=$5
  local id
  id=$(api GET "$get_path" | python3 -c "
import sys, json
d = json.load(sys.stdin)
field, value = sys.argv[1], sys.argv[2]
match = [x for x in d if str(x.get(field, '')).lower() == value.lower()]
print(match[0]['id'] if match else '')
" "$match_field" "$match_value" 2>/dev/null || echo "")

  if [[ -z "$id" ]]; then
    id=$(curl -s -X POST "$BASE_URL$post_path" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $TOKEN_ADMIN" \
      -d "$payload" | python3 -c "import sys,json; print(json.load(sys.stdin).get('id',''))" 2>/dev/null || echo "")
  fi
  echo "$id"
}

# Tipos de vaga
TIPO_VAGA_ID=$(ensure_lookup "/api/lookup/vagas/tipos" "/api/lookup/vagas/tipos" "descricao" "CLT" '{"descricao":"CLT"}')
TIPO_VAGA_PJ_ID=$(ensure_lookup "/api/lookup/vagas/tipos" "/api/lookup/vagas/tipos" "descricao" "PJ"       '{"descricao":"PJ"}')
TIPO_VAGA_ESTAGIO_ID=$(ensure_lookup "/api/lookup/vagas/tipos" "/api/lookup/vagas/tipos" "descricao" "Estágio" '{"descricao":"Estágio"}')

# Modalidades
MODALIDADE_ID=$(ensure_lookup "/api/lookup/vagas/modalidades" "/api/lookup/vagas/modalidades" "descricao" "Remoto" '{"descricao":"Remoto"}')
MODALIDADE_PRESENCIAL_ID=$(ensure_lookup "/api/lookup/vagas/modalidades" "/api/lookup/vagas/modalidades" "descricao" "Presencial" '{"descricao":"Presencial"}')
MODALIDADE_HIBRIDO_ID=$(ensure_lookup "/api/lookup/vagas/modalidades" "/api/lookup/vagas/modalidades" "descricao" "Híbrido"    '{"descricao":"Híbrido"}')

# Escolaridades
ESCOLARIDADE_ID=$(ensure_lookup "/api/lookup/vagas/escolaridades" "/api/lookup/vagas/escolaridades" "nome" "Ensino Superior" '{"nome":"Ensino Superior","ordem":4}')
ensure_lookup "/api/lookup/vagas/escolaridades" "/api/lookup/vagas/escolaridades" "nome" "Ensino Médio"   '{"nome":"Ensino Médio","ordem":2}'   > /dev/null
ensure_lookup "/api/lookup/vagas/escolaridades" "/api/lookup/vagas/escolaridades" "nome" "Pós-Graduação" '{"nome":"Pós-Graduação","ordem":5}' > /dev/null

# Portes de empresa
PORTE_ID=$(ensure_lookup "/api/lookup/vagas/portes" "/api/lookup/vagas/portes" "descricao" "Pequena" '{"descricao":"Pequena"}')
PORTE_MEDIA_ID=$(ensure_lookup "/api/lookup/vagas/portes" "/api/lookup/vagas/portes" "descricao" "Média"  '{"descricao":"Média"}')
PORTE_GRANDE_ID=$(ensure_lookup "/api/lookup/vagas/portes" "/api/lookup/vagas/portes" "descricao" "Grande" '{"descricao":"Grande"}')
ensure_lookup "/api/lookup/vagas/portes" "/api/lookup/vagas/portes" "descricao" "MEI"    '{"descricao":"MEI"}'    > /dev/null

# Ramos de atuação
RAMO_ID=$(ensure_lookup "/api/lookup/vagas/ramos" "/api/lookup/vagas/ramos" "descricao" "Tecnologia da Informação" '{"descricao":"Tecnologia da Informação"}')
RAMO_EDUCACAO_ID=$(ensure_lookup "/api/lookup/vagas/ramos" "/api/lookup/vagas/ramos" "descricao" "Educação" '{"descricao":"Educação"}')
RAMO_FINANCEIRO_ID=$(ensure_lookup "/api/lookup/vagas/ramos" "/api/lookup/vagas/ramos" "descricao" "Financeiro" '{"descricao":"Financeiro"}')
ensure_lookup "/api/lookup/vagas/ramos" "/api/lookup/vagas/ramos" "descricao" "Saúde"    '{"descricao":"Saúde"}'    > /dev/null

# Status de vaga
STATUS_VAGA_ID=$(ensure_lookup "/api/lookup/vagas/status" "/api/lookup/vagas/status" "descricao" "Ativa" '{"descricao":"Ativa"}')
ensure_lookup "/api/lookup/vagas/status" "/api/lookup/vagas/status" "descricao" "Pausada"   '{"descricao":"Pausada"}'   > /dev/null
ensure_lookup "/api/lookup/vagas/status" "/api/lookup/vagas/status" "descricao" "Encerrada" '{"descricao":"Encerrada"}' > /dev/null

# Status de candidatura
ensure_lookup "/api/lookup/sistema/status-candidatura" "/api/lookup/sistema/status-candidatura" "status" "Em Análise"          '{"status":"Em Análise"}'          > /dev/null
ensure_lookup "/api/lookup/sistema/status-candidatura" "/api/lookup/sistema/status-candidatura" "status" "Aprovado"            '{"status":"Aprovado"}'            > /dev/null
ensure_lookup "/api/lookup/sistema/status-candidatura" "/api/lookup/sistema/status-candidatura" "status" "Reprovado"           '{"status":"Reprovado"}'           > /dev/null
ensure_lookup "/api/lookup/sistema/status-candidatura" "/api/lookup/sistema/status-candidatura" "status" "Entrevista Agendada" '{"status":"Entrevista Agendada"}' > /dev/null

# Tipos de telefone
ensure_lookup "/api/lookup/sistema/tipos-telefone" "/api/lookup/sistema/tipos-telefone" "nome" "Celular"  '{"nome":"Celular"}'  > /dev/null
ensure_lookup "/api/lookup/sistema/tipos-telefone" "/api/lookup/sistema/tipos-telefone" "nome" "WhatsApp" '{"nome":"WhatsApp"}' > /dev/null
ensure_lookup "/api/lookup/sistema/tipos-telefone" "/api/lookup/sistema/tipos-telefone" "nome" "Fixo"     '{"nome":"Fixo"}'     > /dev/null

# Departamentos
ensure_lookup "/api/lookup/sistema/departamentos" "/api/lookup/sistema/departamentos" "nome" "Recursos Humanos" \
  '{"nome":"Recursos Humanos","descricao":"Gestão de pessoas, recrutamento e seleção"}' > /dev/null
ensure_lookup "/api/lookup/sistema/departamentos" "/api/lookup/sistema/departamentos" "nome" "Tecnologia da Informação" \
  '{"nome":"Tecnologia da Informação","descricao":"Infraestrutura, sistemas e suporte"}' > /dev/null
ensure_lookup "/api/lookup/sistema/departamentos" "/api/lookup/sistema/departamentos" "nome" "Financeiro" \
  '{"nome":"Financeiro","descricao":"Controladoria, contas a pagar e a receber"}' > /dev/null

# Tipos de notificação (GET público, POST restrito a ADMIN)
ensure_lookup "/api/lookup/notificacoes/tipos" "/api/lookup/notificacoes/tipos" "status" "Candidatura"  '{"status":"Candidatura"}'  > /dev/null
ensure_lookup "/api/lookup/notificacoes/tipos" "/api/lookup/notificacoes/tipos" "status" "Vaga"         '{"status":"Vaga"}'         > /dev/null
ensure_lookup "/api/lookup/notificacoes/tipos" "/api/lookup/notificacoes/tipos" "status" "Sistema"      '{"status":"Sistema"}'      > /dev/null

# Localização — País, Estado e Cidade
PAIS_ID=$(ensure_lookup "/api/localizacao/paises" "/api/localizacao/paises" "nome" "Brasil" '{"nome":"Brasil","codigoIso":"BR"}')
ESTADO_ID=$(ensure_lookup "/api/localizacao/estados" "/api/localizacao/estados" "uf" "SP" "{\"nome\":\"São Paulo\",\"uf\":\"SP\",\"paisId\":$PAIS_ID}")
CIDADE_ID=$(ensure_lookup "/api/localizacao/cidades" "/api/localizacao/cidades" "nome" "São Paulo" "{\"nome\":\"São Paulo\",\"estadoId\":$ESTADO_ID}")

if [[ -z "$TIPO_VAGA_ID" || -z "$MODALIDADE_ID" || -z "$CIDADE_ID" ]]; then
  fail "Não foi possível criar/obter lookups. Verifique se o backend está acessível em $BASE_URL"
  exit 1
fi

ok "tipo_vaga=$TIPO_VAGA_ID  modalidade=$MODALIDADE_ID  escolaridade=$ESCOLARIDADE_ID"
ok "porte=$PORTE_ID  ramo=$RAMO_ID  status_vaga=$STATUS_VAGA_ID  cidade=$CIDADE_ID"

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
  local token=$1 cnpj=$2 razao=$3 fantasia=$4 descricao=$5 porte_id=$6 ramo_id=$7
  local resp
  resp=$(curl -s -X POST "$BASE_URL/api/empresas" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $token" \
    -d "{
      \"cnpj\": \"$cnpj\",
      \"razaoSocial\": \"$razao\",
      \"nomeFantasia\": \"$fantasia\",
      \"descricao\": \"$descricao\",
      \"porteId\": $porte_id,
      \"ramoId\": $ramo_id,
      \"site\": \"https://www.$(echo "$fantasia" | tr '[:upper:]' '[:lower:]' | tr ' ' '-').com.br\"
    }")
  echo "$resp" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('id',''))" 2>/dev/null || echo ""
}

# Busca o ID de uma empresa já cadastrada pelo CNPJ (fallback quando a criação retorna vazio)
find_empresa_by_cnpj() {
  local cnpj=$1
  curl -s "$BASE_URL/api/empresas" \
    | python3 -c "
import sys, json
d = json.load(sys.stdin)
cnpj = sys.argv[1]
match = [x for x in d if x.get('cnpj') == cnpj]
print(match[0]['id'] if match else '')
" "$cnpj" 2>/dev/null || echo ""
}

EMPRESA_RAFAEL_ID=$(create_empresa "$TOKEN_RAFAEL" \
  "12.345.678/0001-95" "Inova Tech Soluções Ltda" "InovaTech" \
  "Startup de inovação em software e inteligência artificial." \
  "$PORTE_ID" "$RAMO_ID")

EMPRESA_FERNANDA_ID=$(create_empresa "$TOKEN_FERNANDA" \
  "98.765.432/0001-00" "Conecta Digital SA" "ConectaDigital" \
  "Consultoria em transformação digital para médias e grandes empresas." \
  "$PORTE_ID" "$RAMO_ID")

EMPRESA_MARCELO_ID=$(create_empresa "$TOKEN_MARCELO" \
  "11.222.333/0001-44" "TechFin Pagamentos Ltda" "TechFin" \
  "Fintech especializada em meios de pagamento e crédito digital." \
  "$PORTE_MEDIA_ID" "$RAMO_FINANCEIRO_ID")

EMPRESA_JULIANA_ID=$(create_empresa "$TOKEN_JULIANA" \
  "22.333.444/0001-55" "Vereda Educação SA" "Vereda" \
  "Edtech focada em cursos online de tecnologia e idiomas." \
  "$PORTE_GRANDE_ID" "$RAMO_EDUCACAO_ID")

# Fallback: buscar empresa existente pelo CNPJ se a criação retornar vazio (já existe ou PENDENTE)
[[ -z "$EMPRESA_RAFAEL_ID" ]]   && EMPRESA_RAFAEL_ID=$(find_empresa_by_cnpj "12.345.678/0001-95")
[[ -z "$EMPRESA_FERNANDA_ID" ]] && EMPRESA_FERNANDA_ID=$(find_empresa_by_cnpj "98.765.432/0001-00")
[[ -z "$EMPRESA_MARCELO_ID" ]]  && EMPRESA_MARCELO_ID=$(find_empresa_by_cnpj "11.222.333/0001-44")
[[ -z "$EMPRESA_JULIANA_ID" ]]  && EMPRESA_JULIANA_ID=$(find_empresa_by_cnpj "22.333.444/0001-55")

ok "Empresas — InovaTech id=$EMPRESA_RAFAEL_ID | ConectaDigital id=$EMPRESA_FERNANDA_ID | TechFin id=$EMPRESA_MARCELO_ID | Vereda id=$EMPRESA_JULIANA_ID"

# Admin aprova empresas (necessário para poder criar vagas)
log "Aprovando empresas..."
for EMP_ID in "$EMPRESA_RAFAEL_ID" "$EMPRESA_FERNANDA_ID" "$EMPRESA_MARCELO_ID" "$EMPRESA_JULIANA_ID"; do
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
  local token=$1 empresa_id=$2 titulo=$3 descricao=$4 requisitos=$5 area=$6 sal_min=$7 sal_max=$8 tipo_vaga_id=$9 modalidade_id=${10}
  curl -s -X POST "$BASE_URL/api/vagas" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $token" \
    -d "{
      \"empresaId\": $empresa_id,
      \"titulo\": \"$titulo\",
      \"descricao\": \"$descricao\",
      \"requisitos\": \"$requisitos\",
      \"tipoVagaId\": $tipo_vaga_id,
      \"modalidadeId\": $modalidade_id,
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
  "Tecnologia da Informação" 6000 9500 "$TIPO_VAGA_ID" "$MODALIDADE_ID")

VAGA2=$(create_vaga "$TOKEN_RAFAEL" "$EMPRESA_RAFAEL_ID" \
  "Engenheiro de Machine Learning" \
  "Vaga para cientista/engenheiro ML para atuar em modelos de recomendação e NLP em produção." \
  "Python, TensorFlow ou PyTorch, MLflow, Docker, SQL" \
  "Inteligência Artificial" 10000 16000 "$TIPO_VAGA_ID" "$MODALIDADE_ID")

VAGA3=$(create_vaga "$TOKEN_FERNANDA" "$EMPRESA_FERNANDA_ID" \
  "Analista de QA Automacao" \
  "Profissional de QA para estruturar estratégia de automação de testes end-to-end e de API." \
  "Cypress, Playwright, Postman, CI/CD, Git" \
  "Qualidade de Software" 5500 8000 "$TIPO_VAGA_ID" "$MODALIDADE_ID")

VAGA4=$(create_vaga "$TOKEN_FERNANDA" "$EMPRESA_FERNANDA_ID" \
  "Engenheiro de Dados Senior" \
  "Responsável por projetar e manter pipelines de dados em larga escala usando cloud AWS." \
  "Apache Spark, Airflow, AWS (S3/Glue/Redshift), Python, dbt" \
  "Engenharia de Dados" 12000 18000 "$TIPO_VAGA_ID" "$MODALIDADE_ID")

VAGA5=$(create_vaga "$TOKEN_ADMIN" "$EMPRESA_RAFAEL_ID" \
  "UX Designer Junior" \
  "Designer de UX para atuar na jornada de produtos digitais, com foco em pesquisa e prototipação." \
  "Figma, pesquisa com usuarios, prototipacao, Design System" \
  "Design de Produto" 3500 5500 "$TIPO_VAGA_ID" "$MODALIDADE_ID")

VAGA6=$(create_vaga "$TOKEN_MARCELO" "$EMPRESA_MARCELO_ID" \
  "Analista Financeiro Pleno" \
  "Responsável por conciliação de pagamentos, fluxo de caixa e relatórios financeiros para a área de fintech." \
  "Excel avançado, ERP financeiro, conciliação bancária, fluxo de caixa" \
  "Financeiro" 4500 7000 "$TIPO_VAGA_ID" "$MODALIDADE_PRESENCIAL_ID")

VAGA7=$(create_vaga "$TOKEN_MARCELO" "$EMPRESA_MARCELO_ID" \
  "Desenvolvedor Backend Java" \
  "Desenvolvimento e manutenção de microsserviços Java/Spring Boot para a plataforma de pagamentos." \
  "Java, Spring Boot, PostgreSQL, Docker, Kafka" \
  "Tecnologia da Informação" 8000 13000 "$TIPO_VAGA_PJ_ID" "$MODALIDADE_HIBRIDO_ID")

VAGA8=$(create_vaga "$TOKEN_JULIANA" "$EMPRESA_JULIANA_ID" \
  "Professor de Inglês Online" \
  "Ministrar aulas particulares e em grupo de inglês para alunos de diferentes níveis em plataforma online." \
  "Fluência em inglês, didática, experiência com ensino online" \
  "Educação" 2500 4500 "$TIPO_VAGA_PJ_ID" "$MODALIDADE_ID")

VAGA9=$(create_vaga "$TOKEN_JULIANA" "$EMPRESA_JULIANA_ID" \
  "Coordenador Pedagógico" \
  "Coordenação da equipe pedagógica, planejamento curricular e acompanhamento da qualidade dos cursos oferecidos." \
  "Pedagogia ou licenciatura, gestão de equipes, planejamento curricular" \
  "Educação" 6000 9000 "$TIPO_VAGA_ID" "$MODALIDADE_PRESENCIAL_ID")

VAGA10=$(create_vaga "$TOKEN_FERNANDA" "$EMPRESA_FERNANDA_ID" \
  "Estágio em Marketing Digital" \
  "Apoio nas campanhas de marketing digital, redes sociais, criação de conteúdo e análise de métricas." \
  "Cursando Marketing, Publicidade ou áreas afins, Canva, redes sociais" \
  "Marketing" 1800 2200 "$TIPO_VAGA_ESTAGIO_ID" "$MODALIDADE_HIBRIDO_ID")

ok "Vagas criadas — IDs: $VAGA1 | $VAGA2 | $VAGA3 | $VAGA4 | $VAGA5 | $VAGA6 | $VAGA7 | $VAGA8 | $VAGA9 | $VAGA10"

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
echo "    Empresas   : rafael.gestor@seed.com | fernanda.sousa@seed.com | marcelo.tavares@seed.com | juliana.ramos@seed.com"
echo "    Admin      : admin.seed@seed.com"
echo ""
echo "  EMPRESAS"
echo "    InovaTech      id=$EMPRESA_RAFAEL_ID"
echo "    ConectaDigital id=$EMPRESA_FERNANDA_ID"
echo "    TechFin        id=$EMPRESA_MARCELO_ID"
echo "    Vereda         id=$EMPRESA_JULIANA_ID"
echo ""
echo "  VAGAS"
echo "    $VAGA1  - Desenvolvedor Frontend React Pleno   (InovaTech)"
echo "    $VAGA2  - Engenheiro de Machine Learning       (InovaTech)"
echo "    $VAGA3  - Analista de QA Automacao             (ConectaDigital)"
echo "    $VAGA4  - Engenheiro de Dados Senior           (ConectaDigital)"
echo "    $VAGA5  - UX Designer Junior                   (InovaTech)"
echo "    $VAGA6  - Analista Financeiro Pleno            (TechFin)"
echo "    $VAGA7  - Desenvolvedor Backend Java           (TechFin)"
echo "    $VAGA8  - Professor de Ingles Online           (Vereda)"
echo "    $VAGA9  - Coordenador Pedagogico               (Vereda)"
echo "    $VAGA10 - Estagio em Marketing Digital         (ConectaDigital)"
echo ""
echo "  CANDIDATURAS"
echo "    Ana   -> Vagas $VAGA1, $VAGA5"
echo "    Bruno -> Vagas $VAGA4, $VAGA2"
echo "    Carla -> Vagas $VAGA3, $VAGA1"
echo ""
