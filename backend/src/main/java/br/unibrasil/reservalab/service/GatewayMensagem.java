package br.unibrasil.reservalab.service;

import br.unibrasil.reservalab.domain.CanalNotificacao;
import br.unibrasil.reservalab.domain.Notificacao;

/** Porta de saída para provedores de mensageria (camada de infraestrutura). */
public interface GatewayMensagem {
    CanalNotificacao canal();
    void enviar(Notificacao n) throws Exception;
}
