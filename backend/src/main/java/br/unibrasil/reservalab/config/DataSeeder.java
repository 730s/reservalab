package br.unibrasil.reservalab.config;

import br.unibrasil.reservalab.domain.*;
import br.unibrasil.reservalab.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;

/** Dados de demonstração (desative com SEED_DEMO=false). Senha de todos: ReservaLab@2026! */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "reservalab.seed-demo", havingValue = "true")
public class DataSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    public static final String SENHA = "ReservaLab@2026!";

    private final UsuarioRepository usuarios;
    private final LaboratorioRepository labs;
    private final RecursoRepository recursos;
    private final DisciplinaRepository disciplinas;
    private final TurmaRepository turmas;
    private final MatriculaRepository matriculas;
    private final ReservaRepository reservas;
    private final PasswordEncoder encoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarios.count() > 0) return;
        String hash = encoder.encode(SENHA);

        Usuario coord = usr("Carla Coordenadora", "coordenador@reservalab.local", Perfil.COORDENADOR, "C001", hash);
        Usuario prof1 = usr("Paulo Professor", "professor@reservalab.local", Perfil.PROFESSOR, "P001", hash);
        Usuario prof2 = usr("Patrícia Professora", "professora@reservalab.local", Perfil.PROFESSOR, "P002", hash);
        Usuario tec = usr("Tiago Técnico", "tecnico@reservalab.local", Perfil.TECNICO, "T001", hash);
        usr("Igor Analista de TI", "ti@reservalab.local", Perfil.ANALISTA_TI, "I001", hash);
        Usuario aluno1 = usr("Ana Aluna", "aluno@reservalab.local", Perfil.ALUNO, "2026001", hash);
        Usuario aluno2 = usr("Bruno Aluno", "aluno2@reservalab.local", Perfil.ALUNO, "2026002", hash);
        usr("Diana Direção", "direcao@reservalab.local", Perfil.DIRECAO, "D001", hash);

        Recurso pc = rec("Computador", TipoRecurso.EQUIPAMENTO, false, null);
        Recurso proj = rec("Projetor", TipoRecurso.EQUIPAMENTO, false, null);
        Recurso micro = rec("Microscópio óptico", TipoRecurso.EQUIPAMENTO, false, null);
        Recurso capela = rec("Capela de exaustão", TipoRecurso.EQUIPAMENTO, false, null);
        Recurso reagentes = rec("Kit de reagentes", TipoRecurso.INSUMO, false, null);
        Recurso python = rec("Python", TipoRecurso.SOFTWARE, false, "3.12");
        Recurso matlab = rec("MATLAB", TipoRecurso.SOFTWARE, true, "R2025b");

        Laboratorio inf1 = lab("LAB-INF-01", "Bloco B, sala 101", 40, "07:30", "22:30", false, tec);
        item(inf1, pc, 40); item(inf1, proj, 1); item(inf1, python, 40); item(inf1, matlab, 40);
        Laboratorio inf2 = lab("LAB-INF-02", "Bloco B, sala 102", 25, "07:30", "22:30", false, tec);
        item(inf2, pc, 25); item(inf2, proj, 1); item(inf2, python, 25);
        Laboratorio qui = lab("LAB-QUI-01", "Bloco C, térreo", 24, "08:00", "18:00", true, tec);
        item(qui, capela, 4); item(qui, reagentes, 24); item(qui, proj, 1);
        Laboratorio bio = lab("LAB-BIO-01", "Bloco C, 1º andar", 20, "08:00", "18:00", false, tec);
        item(bio, micro, 20); item(bio, proj, 1);
        labs.saveAll(java.util.List.of(inf1, inf2, qui, bio));

        Disciplina prog = disc("PROG1", "Programação I", 80);
        Disciplina qorg = disc("QORG", "Química Orgânica Experimental", 60);
        Disciplina bcel = disc("BCEL", "Biologia Celular", 60);
        Turma tProg = turma("PROG1-A", "2026/2", prog, prof1, 35);
        turma("QORG-A", "2026/2", qorg, prof1, 22);
        Turma tBio = turma("BCEL-A", "2026/2", bcel, prof2, 18);
        mat(tProg, aluno1); mat(tProg, aluno2); mat(tBio, aluno1);

        Reserva r = new Reserva();
        r.setLaboratorio(inf1);
        r.setTurma(tProg);
        r.setSolicitante(prof1);
        r.setCriadaPor(prof1);
        r.setData(Tempo.somarDiasUteis(Tempo.agora(), 4).toLocalDate());
        r.setHoraInicio(LocalTime.of(8, 0));
        r.setHoraFim(LocalTime.of(10, 0));
        r.setStatus(StatusReserva.CONFIRMADA);
        r.setCriadaEm(Instant.now());
        r.setAtualizadaEm(Instant.now());
        reservas.save(r);

        log.info("Dados de demonstração criados. Usuários: coordenador@, professor@, professora@, tecnico@, ti@, aluno@, aluno2@, direcao@reservalab.local — senha: {}", SENHA);
    }

    private Usuario usr(String nome, String email, Perfil perfil, String matricula, String hash) {
        Usuario u = new Usuario();
        u.setNome(nome); u.setEmail(email); u.setPerfil(perfil); u.setMatricula(matricula); u.setSenhaHash(hash);
        return usuarios.save(u);
    }

    private Recurso rec(String nome, TipoRecurso tipo, boolean terceiros, String versao) {
        Recurso r = new Recurso();
        r.setNome(nome); r.setTipo(tipo); r.setSoftwareTerceiros(terceiros); r.setVersao(versao);
        return recursos.save(r);
    }

    private Laboratorio lab(String id, String loc, int cap, String abre, String fecha, boolean acomp, Usuario tecnico) {
        Laboratorio l = new Laboratorio();
        l.setIdentificacao(id); l.setLocalizacao(loc); l.setCapacidadeMaxima(cap);
        l.setHoraAbertura(LocalTime.parse(abre)); l.setHoraFechamento(LocalTime.parse(fecha));
        l.setExigeAcompanhamentoTecnico(acomp);
        l.getTecnicos().add(tecnico);
        return l;
    }

    private void item(Laboratorio l, Recurso r, int qtd) {
        RecursoDoLaboratorio rl = new RecursoDoLaboratorio();
        rl.setLaboratorio(l); rl.setRecurso(r); rl.setQuantidade(qtd);
        l.getRecursos().add(rl);
    }

    private Disciplina disc(String cod, String nome, int carga) {
        Disciplina d = new Disciplina();
        d.setCodigo(cod); d.setNome(nome); d.setCargaHoraria(carga);
        return disciplinas.save(d);
    }

    private Turma turma(String cod, String periodo, Disciplina d, Usuario prof, int alunos) {
        Turma t = new Turma();
        t.setCodigo(cod); t.setPeriodoLetivo(periodo); t.setDisciplina(d); t.setProfessor(prof); t.setNumeroAlunos(alunos);
        return turmas.save(t);
    }

    private void mat(Turma t, Usuario aluno) {
        Matricula m = new Matricula();
        m.setTurma(t); m.setAluno(aluno);
        matriculas.save(m);
    }
}
