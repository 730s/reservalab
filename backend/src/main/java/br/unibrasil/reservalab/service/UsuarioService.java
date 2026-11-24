package br.unibrasil.reservalab.service;

import br.unibrasil.reservalab.domain.*;
import br.unibrasil.reservalab.repository.UsuarioRepository;
import br.unibrasil.reservalab.web.Dtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/** UC02 (RF02): gestão de usuários e perfis, restrita ao Analista de TI. */
@Service
@RequiredArgsConstructor
public class UsuarioService {
    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;
    private final AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public List<UsuarioRes> listar() {
        return repo.findAll().stream().sorted(Comparator.comparing(Usuario::getNome)).map(UsuarioRes::of).toList();
    }

    @Transactional(readOnly = true)
    public List<UsuarioRes> porPerfil(Perfil perfil) {
        return repo.findByPerfilAndAtivoTrue(perfil).stream().sorted(Comparator.comparing(Usuario::getNome)).map(UsuarioRes::of).toList();
    }

    @Transactional
    public UsuarioRes criar(UsuarioReq req, Usuario ator) {
        if (repo.findByEmailIgnoreCase(req.email()).isPresent()) throw ApiException.regra("Já existe usuário com este e-mail.");
        validarSenha(req.senha(), true);
        Usuario u = new Usuario();
        aplicar(u, req);
        u.setSenhaHash(encoder.encode(req.senha()));
        repo.save(u);
        auditoria.registrar(ator, "Usuario", u.getId(), "CRIAR", null, u.getEmail() + " / " + u.getPerfil());
        return UsuarioRes.of(u);
    }

    @Transactional
    public UsuarioRes atualizar(Long id, UsuarioReq req, Usuario ator) {
        Usuario u = repo.findById(id).orElseThrow(() -> ApiException.naoEncontrado("Usuário não encontrado."));
        repo.findByEmailIgnoreCase(req.email()).filter(o -> !o.getId().equals(id)).ifPresent(o -> {
            throw ApiException.regra("Já existe usuário com este e-mail.");
        });
        if (u.getId().equals(ator.getId()) && (req.ativo() != null && !req.ativo()))
            throw ApiException.regra("Você não pode desativar a própria conta.");
        String antes = u.getEmail() + " / " + u.getPerfil() + " / ativo=" + u.isAtivo();
        aplicar(u, req);
        if (req.senha() != null && !req.senha().isBlank()) {
            validarSenha(req.senha(), false);
            u.setSenhaHash(encoder.encode(req.senha()));
        }
        repo.save(u);
        auditoria.registrar(ator, "Usuario", u.getId(), "EDITAR", antes, u.getEmail() + " / " + u.getPerfil() + " / ativo=" + u.isAtivo());
        return UsuarioRes.of(u);
    }

    private void aplicar(Usuario u, UsuarioReq req) {
        u.setNome(req.nome().trim());
        u.setEmail(req.email().trim().toLowerCase());
        u.setPerfil(req.perfil());
        u.setTelefone(req.telefone());
        u.setMatricula(req.matricula());
        if (req.consenteWhatsapp() != null) u.setConsenteWhatsapp(req.consenteWhatsapp());
        if (req.ativo() != null) u.setAtivo(req.ativo());
    }

    /** RNF14: senhas com no mínimo 12 caracteres. */
    private void validarSenha(String senha, boolean obrigatoria) {
        if (senha == null || senha.length() < 12)
            throw ApiException.invalido("A senha deve ter no mínimo 12 caracteres.");
    }
}
