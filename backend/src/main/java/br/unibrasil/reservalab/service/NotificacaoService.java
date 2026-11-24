package br.unibrasil.reservalab.service;

import br.unibrasil.reservalab.domain.*;
import br.unibrasil.reservalab.repository.NotificacaoRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * Notificações assíncronas com padrão outbox (D08, RNF09): o registro é gravado na mesma transação
 * da operação; o despacho ocorre depois, com até 3 tentativas. Falha de envio nunca desfaz a reserva.
 */
@Service
@RequiredArgsConstructor
public class NotificacaoService {
    private static final Logger log = LoggerFactory.getLogger(NotificacaoService.class);
    private static final int MAX_TENTATIVAS = 3;

    private final NotificacaoRepository repo;
    private final List<GatewayMensagem> gateways;

    @Transactional
    public void notificar(Usuario dest, String assunto, String mensagem) {
        if (dest == null || !dest.isAtivo()) return;
        salvar(dest, CanalNotificacao.EMAIL, assunto, mensagem);
        if (dest.isConsenteWhatsapp() && dest.getTelefone() != null && !dest.getTelefone().isBlank())
            salvar(dest, CanalNotificacao.WHATSAPP, assunto, mensagem);
    }

    @Transactional
    public void notificarTodos(Collection<Usuario> destinatarios, String assunto, String mensagem) {
        for (Usuario u : new LinkedHashSet<>(destinatarios)) notificar(u, assunto, mensagem);
    }

    private void salvar(Usuario dest, CanalNotificacao canal, String assunto, String mensagem) {
        Notificacao n = new Notificacao();
        n.setDestinatario(dest);
        n.setCanal(canal);
        n.setAssunto(assunto);
        n.setMensagem(mensagem);
        n.setCriadaEm(Instant.now());
        repo.save(n);
    }

    @Transactional(readOnly = true)
    public List<Notificacao> doUsuario(Long usuarioId) {
        return repo.findTop100ByDestinatario_IdOrderByCriadaEmDesc(usuarioId);
    }

    @Scheduled(fixedDelay = 10_000, initialDelay = 15_000)
    @Transactional
    public void despachar() {
        for (Notificacao n : repo.findTop50ByStatusAndTentativasLessThanOrderByCriadaEmAsc(StatusEnvio.PENDENTE, MAX_TENTATIVAS)) {
            try {
                GatewayMensagem g = gateways.stream().filter(x -> x.canal() == n.getCanal()).findFirst()
                    .orElseThrow(() -> new IllegalStateException("Sem gateway para " + n.getCanal()));
                g.enviar(n);
                n.setStatus(StatusEnvio.ENVIADA);
                n.setEnviadaEm(Instant.now());
            } catch (Exception ex) {
                n.setTentativas(n.getTentativas() + 1);
                if (n.getTentativas() >= MAX_TENTATIVAS) n.setStatus(StatusEnvio.FALHA);
                log.warn("Falha ao enviar notificação {} (tentativa {}): {}", n.getId(), n.getTentativas(), ex.getMessage());
            }
        }
    }
}
