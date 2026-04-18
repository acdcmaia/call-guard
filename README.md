# Call Guard

Aplicativo Android de triagem de chamadas. Bloqueia automaticamente chamadas de números desconhecidos e permite configurar uma lista negra por sequência numérica.

## Como funciona

```mermaid
flowchart LR
    A([Chamada recebida]) --> B{Na lista negra?}
    B -- Sim --> C([Bloqueada])
    B -- Não --> D{Já ligou antes\nnesta janela?}
    D -- Não --> C
    D -- Sim --> E([Permitida])
```

- **Lista negra:** qualquer chamada cujo número contenha a sequência configurada é bloqueada imediatamente
- **Primeira chamada:** números desconhecidos são bloqueados na primeira tentativa
- **Repetição:** se o mesmo número ligar novamente dentro da janela de tempo configurada, a chamada é permitida

## Requisitos

- Android 10 (API 29) ou superior
- Permissão de triagem de chamadas (`ROLE_CALL_SCREENING`)

## Instalação

1. Vai a [Releases](../../releases)
2. Faz download do ficheiro `.apk` mais recente
3. Instala no dispositivo (pode ser necessário permitir instalação de fontes desconhecidas em Definições → Segurança)

## Utilização

### Janela de tempo
Em **Configurações**, define quantos segundos o app aguarda por uma repetição de chamada. Default: 120 segundos.

### Lista negra
Em **Lista Negra**, adiciona sequências de dígitos a bloquear. Qualquer chamada cujo número contenha essa sequência será rejeitada.

> Ex.: a sequência `98181` bloqueia chamadas de `021XXXXXXXX`

### Histórico
Em **Chamadas**, vê o registo de todas as chamadas processadas. Chamadas bloqueadas aparecem a vermelho.

## Compilar o projeto

**Pré-requisitos:**
- Android Studio Hedgehog ou superior
- JDK 11

**Passos:**
1. Clona o repositório
   ```bash
   git clone https://github.com/acdcmaia/call-guard.git
   ```
2. Abre o projeto no Android Studio
3. Faz `Build → Build APK(s)`

## Limitações conhecidas

**MIUI (Xiaomi):** o sistema ignora o serviço de triagem para números guardados nos contactos, aprovando-os automaticamente. O app funciona corretamente apenas para números não guardados na agenda.

## Licença

Uso pessoal.
