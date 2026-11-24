package br.unibrasil.reservalab.service;

import br.unibrasil.reservalab.domain.*;
import br.unibrasil.reservalab.repository.*;
import br.unibrasil.reservalab.web.Dtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/** UC03/UC04/UC05 (RF03-RF07). Exclusão sempre lógica (D07). */
@Service
@RequiredArgsConstructor
public class LaboratorioService {
    private final LaboratorioRepository labs;
    private final RecursoRepository recursos;
    private final UsuarioRepository usuarios;
    private final ReservaRepository reservas;
    private final AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public List<LabRes> listar(String q, Integer capacidadeMinima, Long recursoId, StatusLaboratorio status) {
        return labs.findAll().stream()
            .filter(l -> q == null || q.isBlank()
                || l.getIdentificacao().toLowerCase().contains(q.toLowerCase())
                || (l.getLocalizacao() != null && l.getLocalizacao().toLowerCase().contains(q.toLowerCase())))
            .filter(l -> capacidadeMinima == null || l.getCapacidadeMaxima() >= capacidadeMinima)
            .filter(l -> recursoId == null || l.quantidadeDe(recursoId) > 0)
            .filter(l -> status == null || l.getStatus() == status)
            .sorted(Comparator.comparing(Laboratorio::getIdentificacao))
            .map(LabRes::of).toList();
    }

    @Transactional(readOnly = true)
    public LabRes obter(Long id) { return LabRes.of(buscar(id)); }

    @Transactional
    public LabRes criar(LabReq req, Usuario ator) {
        String ident = req.identificacao().trim().toUpperCase();
        if (labs.findByIdentificacaoIgnoreCase(ident).isPresent())
            throw ApiException.regra("Já existe um laboratório com a identificação " + ident + " (RN04).");
        validarHorario(req);
        Laboratorio l = new Laboratorio();
        l.setIdentificacao(ident);
        aplicar(l, req);
        labs.save(l);
        if (req.recursos() != null) definirRecursosInterno(l, req.recursos());
        auditoria.registrar(ator, "Laboratorio", l.getId(), "CRIAR", null, resumo(l));
        return LabRes.of(l);
    }

    @Transactional
    public LabRes editar(Long id, LabReq req, Usuario ator) {
        Laboratorio l = buscar(id);
        validarHorario(req);
        String ident = req.identificacao().trim().toUpperCase();
        labs.findByIdentificacaoIgnoreCase(ident).filter(o -> !o.getId().equals(id)).ifPresent(o -> {
            throw ApiException.regra("Já existe um laboratório com a identificação " + ident + " (RN04).");
        });
        // RF04: reduzir lotação exige tratar as reservas futuras cuja turma deixaria de caber
        if (req.capacidadeMaxima() < l.getCapacidadeMaxima()) {
            List<Reserva> afetadas = reservas.futurasDoLaboratorio(id, Tempo.hoje(), StatusReserva.OCUPANTES).stream()
                .filter(r -> r.getTurma().getNumeroAlunos() > req.capacidadeMaxima()).toList();
            if (!afetadas.isEmpty())
                throw ApiException.e("E04", 409, "Reduzir a lotação afetaria " + afetadas.size()
                    + " reserva(s) futura(s): " + descrever(afetadas) + ". Cancele ou remaneje antes.");
        }
        String antes = resumo(l);
        l.setIdentificacao(ident);
        aplicar(l, req);
        if (req.recursos() != null) definirRecursosInterno(l, req.recursos());
        labs.save(l);
        auditoria.registrar(ator, "Laboratorio", l.getId(), "EDITAR", antes, resumo(l));
        return LabRes.of(l);
    }

    /** RF05: inativação bloqueada enquanto houver reservas futuras ativas. */
    @Transactional
    public LabRes inativar(Long id, Usuario ator) {
        Laboratorio l = buscar(id);
        List<Reserva> futuras = reservas.futurasDoLaboratorio(id, Tempo.hoje(), StatusReserva.OCUPANTES);
        if (!futuras.isEmpty())
            throw ApiException.e("E04", 409, "Existem " + futuras.size() + " reserva(s) futura(s) ativa(s): "
                + descrever(futuras) + ". Cancele ou remaneje antes de inativar.");
        String antes = resumo(l);
        l.setStatus(StatusLaboratorio.INATIVO);
        labs.save(l);
        auditoria.registrar(ator, "Laboratorio", l.getId(), "INATIVAR", antes, resumo(l));
        return LabRes.of(l);
    }

    /** Técnico altera apenas o status operacional (ATIVO/MANUTENCAO) dos laboratórios sob sua responsabilidade. */
    @Transactional
    public LabRes alterarStatus(Long id, StatusLaboratorio novo, Usuario ator) {
        if (novo == StatusLaboratorio.INATIVO) {
            if (ator.getPerfil() != Perfil.COORDENADOR) throw ApiException.proibido("Somente o coordenador inativa laboratórios.");
            return inativar(id, ator);
        }
        Laboratorio l = buscar(id);
        exigirResponsavel(l, ator);
        String antes = resumo(l);
        l.setStatus(novo);
        labs.save(l);
        auditoria.registrar(ator, "Laboratorio", l.getId(), "STATUS", antes, resumo(l));
        return LabRes.of(l);
    }

    @Transactional
    public LabRes definirRecursos(Long id, List<LabRecursoReq> itens, Usuario ator) {
        Laboratorio l = buscar(id);
        exigirResponsavel(l, ator);
        String antes = resumo(l);
        definirRecursosInterno(l, itens);
        labs.save(l);
        auditoria.registrar(ator, "Laboratorio", l.getId(), "RECURSOS", antes, resumo(l));
        return LabRes.of(l);
    }

    // ---------------------------------------------------------------- internos

    private Laboratorio buscar(Long id) {
        return labs.findById(id).orElseThrow(() -> ApiException.naoEncontrado("Laboratório não encontrado."));
    }

    private void exigirResponsavel(Laboratorio l, Usuario ator) {
        if (ator.getPerfil() == Perfil.COORDENADOR) return;
        if (!l.tecnicoResponsavel(ator) && !l.getTecnicos().isEmpty())
            throw ApiException.proibido("Você não é responsável por este laboratório.");
    }

    private void validarHorario(LabReq req) {
        if (!req.horaFechamento().isAfter(req.horaAbertura()))
            throw ApiException.invalido("O horário de fechamento deve ser posterior ao de abertura.");
    }

    private void aplicar(Laboratorio l, LabReq req) {
        l.setLocalizacao(req.localizacao());
        l.setCapacidadeMaxima(req.capacidadeMaxima());
        l.setHoraAbertura(req.horaAbertura());
        l.setHoraFechamento(req.horaFechamento());
        l.setStatus(req.status() != null ? req.status() : StatusLaboratorio.ATIVO);
        l.setExigeAcompanhamentoTecnico(Boolean.TRUE.equals(req.exigeAcompanhamentoTecnico()));
        if (req.tecnicoIds() != null) {
            l.getTecnicos().clear();
            usuarios.findAllById(req.tecnicoIds()).stream()
                .filter(u -> u.getPerfil() == Perfil.TECNICO).forEach(l.getTecnicos()::add);
        }
    }

    private void definirRecursosInterno(Laboratorio l, List<LabRecursoReq> itens) {
        Map<Long, Integer> novo = new LinkedHashMap<>();
        for (LabRecursoReq i : itens) if (i.quantidade() > 0) novo.merge(i.recursoId(), i.quantidade(), Integer::sum);
        l.getRecursos().removeIf(rl -> !novo.containsKey(rl.getRecurso().getId()));
        for (RecursoDoLaboratorio rl : l.getRecursos()) {
            Integer q = novo.remove(rl.getRecurso().getId());
            if (q != null) rl.setQuantidade(q);
        }
        for (Map.Entry<Long, Integer> e : novo.entrySet()) {
            Recurso rec = recursos.findById(e.getKey()).orElseThrow(() -> ApiException.invalido("Recurso inexistente."));
            RecursoDoLaboratorio rl = new RecursoDoLaboratorio();
            rl.setLaboratorio(l);
            rl.setRecurso(rec);
            rl.setQuantidade(e.getValue());
            l.getRecursos().add(rl);
        }
    }

    private String descrever(List<Reserva> rs) {
        return rs.stream().limit(5).map(r -> "#" + r.getId() + " (" + r.getData() + " " + r.getHoraInicio() + ")")
            .reduce((a, b) -> a + ", " + b).orElse("") + (rs.size() > 5 ? "..." : "");
    }

    private String resumo(Laboratorio l) {
        return "id=" + l.getIdentificacao() + "; capacidade=" + l.getCapacidadeMaxima() + "; status=" + l.getStatus()
            + "; recursos=" + l.getRecursos().size();
    }
}
