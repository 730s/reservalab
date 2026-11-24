package br.unibrasil.reservalab.service;

import br.unibrasil.reservalab.domain.CanalNotificacao;
import br.unibrasil.reservalab.domain.Notificacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Simulação do WhatsApp Business API (opcional e com consentimento, D08). Troque pela integração real. */
@Component
public class GatewayWhatsApp implements GatewayMensagem {
    private static final Logger log = LoggerFactory.getLogger(GatewayWhatsApp.class);

    @Override public CanalNotificacao canal() { return CanalNotificacao.WHATSAPP; }

    @Override
    public void enviar(Notificacao n) {
        log.info("[WhatsApp simulado] para {} ({}): {}", n.getDestinatario().getNome(),
            n.getDestinatario().getTelefone(), n.getAssunto());
    }
}
