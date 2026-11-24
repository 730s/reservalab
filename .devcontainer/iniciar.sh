#!/usr/bin/env bash
# Sobe o ReservaLab dentro do Codespace e deixa as portas públicas
# (para abrir o link em qualquer computador sem login no GitHub).
set -u

echo "Aguardando o Docker ficar pronto..."
for i in $(seq 1 60); do
  docker info >/dev/null 2>&1 && break
  sleep 2
done

echo "Subindo os serviços (na primeira vez o build leva alguns minutos)..."
docker compose up -d --build

# Tenta deixar as portas públicas. Se não conseguir, faça pela aba "Portas"
# (botão direito na porta -> Visibilidade da porta -> Pública).
if [ -n "${CODESPACE_NAME:-}" ]; then
  gh codespace ports visibility 3000:public 8025:public 8080:public -c "$CODESPACE_NAME" \
    || echo "AVISO: não deu para tornar as portas públicas automaticamente; faça pela aba Portas."
fi

echo "Aguardando o backend responder..."
for i in $(seq 1 90); do
  if curl -fs -o /dev/null http://localhost:3000/api/health; then
    echo "ReservaLab no ar! Abra a porta 3000 na aba Portas."
    exit 0
  fi
  sleep 5
done
echo "O backend ainda não respondeu. Veja os logs com: docker compose logs backend"
