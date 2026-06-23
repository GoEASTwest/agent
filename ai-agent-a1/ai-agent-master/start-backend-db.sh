#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

load_env_file() {
  if [ ! -f ".env" ]; then
    return
  fi

  local line key value
  while IFS= read -r line || [ -n "$line" ]; do
    line="${line%$'\r'}"
    case "$line" in
      ""|\#*) continue ;;
    esac
    case "$line" in
      *=*) ;;
      *) continue ;;
    esac

    key="${line%%=*}"
    value="${line#*=}"
    key="$(printf '%s' "$key" | tr -d '[:space:]')"

    case "$key" in
      DASHSCOPE_API_KEY|api_key|DASHSCOPE_CHAT_MODEL|DASHSCOPE_COMPATIBLE_BASE_URL|SEARCH_API_KEY|DB_URL|DB_USERNAME|DB_PASSWORD)
        export "$key=$value"
        ;;
    esac
  done < ".env"
}

load_env_file

JAR_PATH="target/ai-agent-0.0.1-SNAPSHOT.jar"
if [ ! -f "$JAR_PATH" ]; then
  chmod +x mvnw
  ./mvnw -DskipTests --no-transfer-progress package
fi

DB_URL="${DB_URL:-jdbc:postgresql://127.0.0.1:5432/ai_agent}"
DB_USERNAME="${DB_USERNAME:-postgres}"
DB_PASSWORD="${DB_PASSWORD:-postgres}"
DASHSCOPE_CHAT_MODEL="${DASHSCOPE_CHAT_MODEL:-qwen3.7-plus}"
DASHSCOPE_COMPATIBLE_BASE_URL="${DASHSCOPE_COMPATIBLE_BASE_URL:-https://dashscope.aliyuncs.com/compatible-mode/v1}"
DASHSCOPE_API_KEY="${DASHSCOPE_API_KEY:-${api_key:-}}"
SEARCH_API_KEY="${SEARCH_API_KEY:-}"

JAVA_ARGS=(
  "-Dapp.persistence.jdbc.enabled=true"
  "-Dapp.vectorstore.pgvector.enabled=false"
  "-Dspring.datasource.url=$DB_URL"
  "-Dspring.datasource.username=$DB_USERNAME"
  "-Dspring.datasource.password=$DB_PASSWORD"
  "-Dspring.ai.dashscope.chat.options.model=$DASHSCOPE_CHAT_MODEL"
  "-Ddashscope.compatible.base-url=$DASHSCOPE_COMPATIBLE_BASE_URL"
)

if [ -n "$DASHSCOPE_API_KEY" ]; then
  JAVA_ARGS+=("-Dspring.ai.dashscope.api-key=$DASHSCOPE_API_KEY")
fi

if [ -n "$SEARCH_API_KEY" ]; then
  JAVA_ARGS+=("-Dsearch-api.api-key=$SEARCH_API_KEY")
fi

echo "Starting backend with PostgreSQL persistence enabled."
echo "Database URL: $DB_URL"
echo "PgVector: disabled"

exec java "${JAVA_ARGS[@]}" -jar "$JAR_PATH"
