#!/usr/bin/env bash
# Sobe o docker compose escolhendo a primeira porta livre para o backend.
#
# O Docker Compose nao tem fallback de porta: se a 8080 estiver ocupada, a subida
# falha. Este script testa 8080, 8081, ... e exporta SERVER_PORT antes de
# chamar o compose. Argumentos extras sao repassados (ex.: ./scripts/dev-up.sh --build).
#
# Uso (Git Bash / WSL / Linux / macOS):
#   ./scripts/dev-up.sh            # sobe tudo
#   ./scripts/dev-up.sh --build    # reconstroi a imagem antes

set -euo pipefail
cd "$(dirname "$0")/.."

first_port="${SERVER_PORT:-8080}"
last_port=$((first_port + 10))

port_busy() {
  # netstat existe no Windows (Git Bash) e no Linux; ss so no Linux.
  if command -v ss >/dev/null 2>&1; then
    ss -ltn 2>/dev/null | awk '{print $4}' | grep -Eq "[:.]$1\$"
  else
    netstat -an 2>/dev/null | grep -Ei "[:.]$1 .*(LISTEN|ESCUTA)" >/dev/null
  fi
}

chosen=""
for ((p = first_port; p <= last_port; p++)); do
  if ! port_busy "$p"; then
    chosen="$p"
    break
  fi
done

if [[ -z "$chosen" ]]; then
  echo "Nenhuma porta livre entre $first_port e $last_port." >&2
  exit 1
fi

if [[ "$chosen" != "$first_port" ]]; then
  echo "Porta $first_port ocupada. Subindo o backend na $chosen. Aponte o frontend para ela."
fi

export SERVER_PORT="$chosen"
docker compose up -d "$@"
echo
echo "Backend:  http://localhost:$chosen"
echo "Swagger:  http://localhost:$chosen/swagger-ui/index.html"
