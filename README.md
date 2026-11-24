# ReservaLab V3 — implementação de referência

Sistema de reserva de laboratórios (UniBrasil), derivado do documento de engenharia de software V3
(30 RF, 21 RNF, 19 RN, decisões D01–D19).

**Stack:** Java 21 + Spring Boot 3.3 · PostgreSQL 16 · React 18 (Vite) · Docker Compose.

## Como rodar (qualquer PC com Docker)

```bash
docker compose up --build
```

Na primeira vez o build baixa dependências (Maven/npm): leva alguns minutos.

| Serviço | URL |
|---|---|
| Aplicação (React) | http://localhost:3000 |
| API + Swagger (OpenAPI 3) | http://localhost:8080/swagger-ui.html |
| Caixa de e-mails de teste (Mailpit) | http://localhost:8025 |

Para parar: `Ctrl+C` e `docker compose down` (acrescente `-v` para apagar o banco).

## Usuários de demonstração

Senha de todos: `ReservaLab@2026!`

| Perfil | E-mail |
|---|---|
| Coordenador | coordenador@reservalab.local |
| Professor | professor@reservalab.local (turmas PROG1-A e QORG-A) · professora@reservalab.local (BCEL-A) |
| Técnico | tecnico@reservalab.local |
| Analista de TI | ti@reservalab.local |
| Aluno | aluno@reservalab.local · aluno2@reservalab.local |
| Direção | direcao@reservalab.local |

Dados de exemplo (laboratórios, recursos, turmas) são criados no primeiro start. Desligue com `SEED_DEMO=false`.

## Roteiro de teste rápido

1. **Reserva automática (D05):** entre como professor → *Nova reserva* → PROG1-A em LAB-INF-02, daqui a 3+ dias úteis → fica **Confirmada** e o técnico recebe a ordem de preparo (veja no Mailpit).
2. **Conflito (E04):** tente o mesmo horário/lab com outra turma → erro com código e motivo.
3. **Capacidade (E03):** PROG1-A (35 alunos) em LAB-QUI-01 (24 lugares) é recusada.
4. **Análise do técnico:** QORG-A em LAB-QUI-01 (exige acompanhamento) fica **Em análise** → o técnico confirma ou recusa.
5. **Software de terceiros:** reserve LAB-INF-01 com MATLAB (≥5 dias úteis) → técnico *Encaminha à TI* → analista de TI aprova.
6. **Urgência:** reserva abaixo da antecedência com justificativa ≥ 20 caracteres → só o coordenador decide.
7. **Concorrência (RNF04):** a restrição `EXCLUDE USING gist` do PostgreSQL garante 1 sucesso e os demais E06.
8. **Aluno:** veja o ensalamento e o aviso quando o coordenador bloqueia a agenda cancelando a reserva.
9. **Relatório:** coordenador/direção → taxa de ociosidade (TO).

## Arquitetura (Figura 5 do documento)

```
backend/src/main/java/br/unibrasil/reservalab
  domain/      entidades, enums e PoliticaDeReserva (regras RN01–RN08, RN18; sem acesso a banco — D04)
  repository/  interfaces Spring Data (persistência)
  service/     serviços de aplicação (ReservaService, LaboratorioService…) e gateways de e-mail/WhatsApp
  web/         controllers REST, DTOs e tratador de erros (códigos E01–E06)
  config/      segurança (JWT, RBAC), parâmetros P01–P13 e seed
  resources/db/migration/V1__schema.sql   esquema + restrição de exclusão (D03)
frontend/      React + Vite, servido por Nginx (proxy /api → backend)
```

## Mapeamento com o documento

- **RF01–RF30**: implementados, exceto os itens listados em *Limitações*.
- **Parâmetros P01–P13** (Quadro 12): `backend/src/main/resources/application.yml` (ou variáveis de ambiente, ex.: `RESERVALAB_PARAMETROS_ANTECEDENCIA_COM_PREPARO_DIAS_UTEIS=4`).
- **Validação na ordem da Figura 10**: dados (E01) → antecedência (E02) → capacidade (E03) → conflitos (E04) → recursos (E05); gravação com exclusão mútua (E06).
- **Notificações (D08)**: padrão outbox; despacho a cada 10 s, até 3 tentativas; e-mail via SMTP (Mailpit); WhatsApp é um gateway **simulado** (log), só para quem consentiu.
- **Auditoria (RF28)**: tela e tabela `registro_auditoria`.
- **RBAC (Quadro 5)**: `@PreAuthorize` nos endpoints + verificação de propriedade no serviço (anti-IDOR).
- **Segurança (RNF14)**: bcrypt custo 12, senha ≥ 12 caracteres, bloqueio de 15 min após 5 falhas, JWT de 30 min renovado com atividade.

## Limitações conhecidas (evolução sugerida)

- **MFA** para Coordenador e TI (RNF14) **não** implementado; sem SSO/LDAP (D11 / Q06).
- **Feriados**: dias úteis consideram apenas seg–sex (calendário acadêmico de RF25 não implementado); parâmetros só por arquivo (sem tela de edição, como previsto para o MVP).
- `Turma.numeroAlunos` é informado na importação (não derivado das matrículas).
- Hierarquia `Usuario` do diagrama foi modelada como **um único tipo com perfil** (enum), mantendo o RBAC.
- Reservas recorrentes (RF19) **não** implementadas (prioridade “Deveria”); escalonamento automático após P06 não implementado (a pendência expira ao iniciar a aula).
- Reenvio de notificações usa intervalo fixo de 10 s (não crescente); sem testes automatizados no repositório.
- Troque `JWT_SECRET` e as senhas do banco em `docker-compose.yml` antes de qualquer uso real.
