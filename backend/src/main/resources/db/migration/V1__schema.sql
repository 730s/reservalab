-- ReservaLab V3 - esquema relacional
-- D03/RNF04: restrição de exclusão garante, no SGBD, que não existam duas reservas
-- ativas sobrepostas no mesmo laboratório, mesmo sob concorrência.
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE usuario (
  id                 BIGSERIAL PRIMARY KEY,
  matricula          VARCHAR(30),
  nome               VARCHAR(120) NOT NULL,
  email              VARCHAR(160) NOT NULL UNIQUE,
  telefone           VARCHAR(20),
  senha_hash         VARCHAR(100) NOT NULL,
  perfil             VARCHAR(20)  NOT NULL,
  ativo              BOOLEAN      NOT NULL DEFAULT TRUE,
  consente_whatsapp  BOOLEAN      NOT NULL DEFAULT FALSE,
  tentativas_falhas  INT          NOT NULL DEFAULT 0,
  bloqueado_ate      TIMESTAMPTZ
);

CREATE TABLE laboratorio (
  id                              BIGSERIAL PRIMARY KEY,
  identificacao                   VARCHAR(40) NOT NULL UNIQUE,
  localizacao                     VARCHAR(120),
  capacidade_maxima               INT  NOT NULL CHECK (capacidade_maxima > 0),
  hora_abertura                   TIME NOT NULL,
  hora_fechamento                 TIME NOT NULL,
  status                          VARCHAR(20) NOT NULL DEFAULT 'ATIVO',
  exige_acompanhamento_tecnico    BOOLEAN NOT NULL DEFAULT FALSE,
  CHECK (hora_fechamento > hora_abertura)
);

CREATE TABLE laboratorio_tecnico (
  laboratorio_id BIGINT NOT NULL REFERENCES laboratorio(id),
  usuario_id     BIGINT NOT NULL REFERENCES usuario(id),
  PRIMARY KEY (laboratorio_id, usuario_id)
);

CREATE TABLE recurso (
  id                  BIGSERIAL PRIMARY KEY,
  nome                VARCHAR(120) NOT NULL,
  tipo                VARCHAR(20)  NOT NULL,
  software_terceiros  BOOLEAN NOT NULL DEFAULT FALSE,
  versao              VARCHAR(40)
);

CREATE TABLE recurso_laboratorio (
  id             BIGSERIAL PRIMARY KEY,
  laboratorio_id BIGINT NOT NULL REFERENCES laboratorio(id),
  recurso_id     BIGINT NOT NULL REFERENCES recurso(id),
  quantidade     INT NOT NULL CHECK (quantidade >= 0),
  UNIQUE (laboratorio_id, recurso_id)
);

CREATE TABLE disciplina (
  id             BIGSERIAL PRIMARY KEY,
  codigo         VARCHAR(30) NOT NULL UNIQUE,
  nome           VARCHAR(160) NOT NULL,
  carga_horaria  INT
);

CREATE TABLE turma (
  id              BIGSERIAL PRIMARY KEY,
  codigo          VARCHAR(40) NOT NULL UNIQUE,
  periodo_letivo  VARCHAR(20),
  disciplina_id   BIGINT NOT NULL REFERENCES disciplina(id),
  professor_id    BIGINT NOT NULL REFERENCES usuario(id),
  numero_alunos   INT NOT NULL CHECK (numero_alunos >= 0)
);

CREATE TABLE matricula (
  id        BIGSERIAL PRIMARY KEY,
  turma_id  BIGINT NOT NULL REFERENCES turma(id),
  aluno_id  BIGINT NOT NULL REFERENCES usuario(id),
  situacao  VARCHAR(20) NOT NULL DEFAULT 'ATIVA',
  UNIQUE (turma_id, aluno_id)
);

CREATE TABLE reserva (
  id                      BIGSERIAL PRIMARY KEY,
  laboratorio_id          BIGINT NOT NULL REFERENCES laboratorio(id),
  turma_id                BIGINT NOT NULL REFERENCES turma(id),
  solicitante_id          BIGINT NOT NULL REFERENCES usuario(id),
  criada_por_id           BIGINT NOT NULL REFERENCES usuario(id),
  data                    DATE NOT NULL,
  hora_inicio             TIME NOT NULL,
  hora_fim                TIME NOT NULL,
  status                  VARCHAR(20) NOT NULL,
  origem                  VARCHAR(20) NOT NULL DEFAULT 'PORTAL',
  urgente                 BOOLEAN NOT NULL DEFAULT FALSE,
  justificativa_urgencia  VARCHAR(500),
  exige_preparo           BOOLEAN NOT NULL DEFAULT FALSE,
  software_terceiros      BOOLEAN NOT NULL DEFAULT FALSE,
  motivo_decisao          VARCHAR(500),
  cancelamento_tardio     BOOLEAN NOT NULL DEFAULT FALSE,
  lembrete_professor_enviado BOOLEAN NOT NULL DEFAULT FALSE,
  lembrete_tecnico_enviado   BOOLEAN NOT NULL DEFAULT FALSE,
  criada_em               TIMESTAMPTZ NOT NULL,
  atualizada_em           TIMESTAMPTZ NOT NULL,
  versao                  BIGINT NOT NULL DEFAULT 0,
  CHECK (hora_fim > hora_inicio),
  periodo tsrange GENERATED ALWAYS AS (tsrange(data + hora_inicio, data + hora_fim, '[)')) STORED,
  CONSTRAINT reserva_sem_sobreposicao EXCLUDE USING gist (laboratorio_id WITH =, periodo WITH &&)
    WHERE (status IN ('CONFIRMADA','PENDENTE_ANALISE','PENDENTE_TI'))
);
CREATE INDEX idx_reserva_data ON reserva (data, laboratorio_id);
CREATE INDEX idx_reserva_turma ON reserva (turma_id, data);

CREATE TABLE reserva_recurso (
  reserva_id  BIGINT NOT NULL REFERENCES reserva(id) ON DELETE CASCADE,
  recurso_id  BIGINT NOT NULL REFERENCES recurso(id),
  quantidade  INT NOT NULL CHECK (quantidade > 0),
  PRIMARY KEY (reserva_id, recurso_id)
);

CREATE TABLE bloqueio_agenda (
  id              BIGSERIAL PRIMARY KEY,
  laboratorio_id  BIGINT NOT NULL REFERENCES laboratorio(id),
  data            DATE NOT NULL,
  hora_inicio     TIME NOT NULL,
  hora_fim        TIME NOT NULL,
  motivo          VARCHAR(300) NOT NULL,
  criado_por_id   BIGINT NOT NULL REFERENCES usuario(id),
  CHECK (hora_fim > hora_inicio)
);

CREATE TABLE validacao_ti (
  id           BIGSERIAL PRIMARY KEY,
  reserva_id   BIGINT NOT NULL REFERENCES reserva(id),
  analista_id  BIGINT NOT NULL REFERENCES usuario(id),
  resultado    VARCHAR(20) NOT NULL,
  parecer      VARCHAR(500),
  validada_em  TIMESTAMPTZ NOT NULL
);

-- Padrão outbox (D08): a notificação é gravada na mesma transação da reserva e enviada depois.
CREATE TABLE notificacao (
  id               BIGSERIAL PRIMARY KEY,
  destinatario_id  BIGINT NOT NULL REFERENCES usuario(id),
  canal            VARCHAR(20) NOT NULL,
  assunto          VARCHAR(200) NOT NULL,
  mensagem         TEXT NOT NULL,
  status           VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
  tentativas       INT NOT NULL DEFAULT 0,
  criada_em        TIMESTAMPTZ NOT NULL,
  enviada_em       TIMESTAMPTZ
);
CREATE INDEX idx_notif_dest ON notificacao (destinatario_id, criada_em DESC);
CREATE INDEX idx_notif_status ON notificacao (status);

CREATE TABLE registro_auditoria (
  id                 BIGSERIAL PRIMARY KEY,
  autor_id           BIGINT,
  autor_nome         VARCHAR(120),
  entidade           VARCHAR(40) NOT NULL,
  entidade_id        BIGINT,
  acao               VARCHAR(40) NOT NULL,
  dados_anteriores   TEXT,
  dados_posteriores  TEXT,
  ocorrido_em        TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_audit_em ON registro_auditoria (ocorrido_em DESC);
