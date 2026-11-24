package br.unibrasil.reservalab.service;

import br.unibrasil.reservalab.domain.*;
import br.unibrasil.reservalab.repository.*;
import br.unibrasil.reservalab.web.Dtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** UC09/UC10 (RF26, RF27): turmas, importação por CSV e ensalamento. */
@Service
@RequiredArgsConstructor
public class TurmaService {
    private final TurmaRepository turmas;
    private final DisciplinaRepository disciplinas;
    private final UsuarioRepository usuarios;
    private final MatriculaRepository matriculas;
    private final ReservaRepository reservas;
    private final AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public List<TurmaRes> listar(Usuario ator) {
        List<Turma> l = ator.getPerfil() == Perfil.PROFESSOR
            ? turmas.findByProfessor_IdOrderByCodigo(ator.getId()) : turmas.findAllByOrderByCodigo();
        return l.stream().map(TurmaRes::of).toList();
    }

    /** RF26: aluno vê só as suas turmas; professor, as suas; coordenador, todas. */
    @Transactional(readOnly = true)
    public List<ReservaRes> ensalamento(Usuario ator) {
        Set<StatusReserva> st = Set.of(StatusReserva.CONFIRMADA, StatusReserva.PENDENTE_ANALISE, StatusReserva.PENDENTE_TI);
        List<Long> ids = switch (ator.getPerfil()) {
            case ALUNO -> matriculas.findByAluno_Id(ator.getId()).stream().map(m -> m.getTurma().getId()).toList();
            case PROFESSOR -> turmas.findByProfessor_IdOrderByCodigo(ator.getId()).stream().map(Turma::getId).toList();
            case COORDENADOR -> turmas.findAll().stream().map(Turma::getId).toList();
            default -> throw ApiException.proibido("Seu perfil não consulta ensalamento.");
        };
        if (ids.isEmpty()) return List.of();
        Set<StatusReserva> visiveis = ator.getPerfil() == Perfil.ALUNO ? Set.of(StatusReserva.CONFIRMADA) : st;
        return reservas.findDasTurmas(ids, visiveis).stream()
            .filter(r -> !r.getData().isBefore(Tempo.hoje())).map(ReservaRes::of).toList();
    }

    /**
     * Layout (D13): codigoDisciplina,nomeDisciplina,cargaHoraria,codigoTurma,periodoLetivo,emailProfessor,
     * numeroAlunos[,emailsAlunos separados por |]. Aceita separador "," ou ";". Relatório de inconsistências por linha.
     */
    @Transactional
    public ImportacaoRes importar(MultipartFile arquivo, Usuario ator) throws IOException {
        List<String> erros = new ArrayList<>();
        int lidas = 0, processadas = 0, mats = 0;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(arquivo.getInputStream(), StandardCharsets.UTF_8))) {
            String linha = br.readLine();
            if (linha == null) throw ApiException.invalido("Arquivo vazio.");
            if (linha.startsWith("\uFEFF")) linha = linha.substring(1);
            String sep = linha.contains(";") ? ";" : ",";
            int n = 1;
            while ((linha = br.readLine()) != null) {
                n++;
                if (linha.isBlank()) continue;
                lidas++;
                String[] c = linha.split(sep, -1);
                if (c.length < 7) { erros.add("Linha " + n + ": esperadas ao menos 7 colunas."); continue; }
                String codDisc = c[0].trim(), nomeDisc = c[1].trim(), codTurma = c[3].trim(), periodo = c[4].trim();
                if (codDisc.isEmpty() || nomeDisc.isEmpty() || codTurma.isEmpty()) {
                    erros.add("Linha " + n + ": código/nome da disciplina e código da turma são obrigatórios."); continue;
                }
                Integer carga = null, qtd;
                try {
                    if (!c[2].isBlank()) carga = Integer.parseInt(c[2].trim());
                    qtd = Integer.parseInt(c[6].trim());
                    if (qtd < 0) throw new NumberFormatException();
                } catch (NumberFormatException ex) { erros.add("Linha " + n + ": carga horária ou número de alunos inválido."); continue; }
                Usuario prof = usuarios.findByEmailIgnoreCase(c[5].trim()).filter(u -> u.getPerfil() == Perfil.PROFESSOR).orElse(null);
                if (prof == null) { erros.add("Linha " + n + ": professor não encontrado (" + c[5].trim() + ")."); continue; }

                Disciplina d = disciplinas.findByCodigo(codDisc).orElseGet(Disciplina::new);
                d.setCodigo(codDisc); d.setNome(nomeDisc); d.setCargaHoraria(carga);
                disciplinas.save(d);
                Turma t = turmas.findByCodigo(codTurma).orElseGet(Turma::new);
                t.setCodigo(codTurma); t.setPeriodoLetivo(periodo); t.setDisciplina(d); t.setProfessor(prof); t.setNumeroAlunos(qtd);
                turmas.save(t);
                processadas++;

                if (c.length > 7 && !c[7].isBlank()) {
                    for (String email : c[7].split("\\|")) {
                        if (email.isBlank()) continue;
                        Usuario aluno = usuarios.findByEmailIgnoreCase(email.trim()).filter(u -> u.getPerfil() == Perfil.ALUNO).orElse(null);
                        if (aluno == null) { erros.add("Linha " + n + ": aluno não encontrado (" + email.trim() + ")."); continue; }
                        if (!matriculas.existsByTurma_IdAndAluno_Id(t.getId(), aluno.getId())) {
                            Matricula m = new Matricula(); m.setTurma(t); m.setAluno(aluno); matriculas.save(m); mats++;
                        }
                    }
                }
            }
        }
        auditoria.registrar(ator, "Turma", null, "IMPORTAR", null,
            "linhas=" + lidas + "; turmas=" + processadas + "; matriculas=" + mats + "; erros=" + erros.size());
        return new ImportacaoRes(lidas, processadas, mats, erros);
    }
}
