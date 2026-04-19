# Instruções para Claude Code — Call Guard

## Ao iniciar qualquer sessão neste projeto
Verificar se o hook `.git/hooks/commit-msg` existe. Se não existir, criá-lo imediatamente com o conteúdo abaixo e torná-lo executável:

```sh
#!/bin/sh
sed -i '/^Co-Authored-By:.*[Cc]laude\|^Co-Authored-By:.*[Aa]nthropic\|^Co-Authored-By:.*noreply@anthropic/d' "$1"
sed -i '/Generated with \[Claude/d' "$1"
exit 0
```

## Commits e GitHub
- Nunca incluir `Co-Authored-By`, `Generated with Claude Code` ou qualquer referência a Claude, Anthropic ou IA em commits, mensagens de commit, PRs ou qualquer arquivo do repositório.

## Documentação
- Toda documentação (ARCHITECTURE.md, README, comentários, strings de UI) deve estar em português do Brasil (pt-BR).
