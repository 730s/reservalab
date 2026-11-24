package br.unibrasil.reservalab.service;

import br.unibrasil.reservalab.domain.*;
import br.unibrasil.reservalab.repository.AuditoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/** RF28/RNF19: trilha de auditoria (autor, data/hora, ação, dados anteriores e posteriores). */
@Service
@RequiredArgsConstructor
public class AuditoriaService {
    private final AuditoriaRepository repo;

    @Transactional
    public void registrar(Usuario autor, String entidade, Long id, String acao, String antes, String depois) {
        RegistroAuditoria a = new RegistroAuditoria();
        if (autor != null) { a.setAutorId(autor.getId()); a.setAutorNome(autor.getNome()); }
        else a.setAutorNome("Sistema");
        a.setEntidade(entidade);
        a.setEntidadeId(id);
        a.setAcao(acao);
        a.setDadosAnteriores(antes);
        a.setDadosPosteriores(depois);
        a.setOcorridoEm(Instant.now());
        repo.save(a);
    }

    @Transactional(readOnly = true)
    public List<RegistroAuditoria> recentes() { return repo.findTop200ByOrderByOcorridoEmDesc(); }
}
