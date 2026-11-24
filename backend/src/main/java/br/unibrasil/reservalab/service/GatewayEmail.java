package br.unibrasil.reservalab.service;

import br.unibrasil.reservalab.domain.CanalNotificacao;
import br.unibrasil.reservalab.domain.Notificacao;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GatewayEmail implements GatewayMensagem {
    private final JavaMailSender mail;

    @Override public CanalNotificacao canal() { return CanalNotificacao.EMAIL; }

    @Override
    public void enviar(Notificacao n) {
        SimpleMailMessage m = new SimpleMailMessage();
        m.setFrom("reservalab@unibrasil.local");
        m.setTo(n.getDestinatario().getEmail());
        m.setSubject("[ReservaLab] " + n.getAssunto());
        m.setText(n.getMensagem());
        mail.send(m);
    }
}
