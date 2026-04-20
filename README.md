# Call Guard

Call Guard é um aplicativo Android de triagem de chamadas. O princípio é simples: robôs de telemarketing e spam trocam de número a cada tentativa, enquanto pessoas reais ligam do mesmo número. Com base nisso, o app bloqueia qualquer número desconhecido na primeira chamada, e só permite a ligação se o mesmo número chamador se repetir dentro de uma janela de tempo configurável, sinalizando que é um chamador legítimo. Números salvos na agenda do dispositivo são sempre permitidos. Além disso, o app permite criar uma lista negra por sequência numérica: qualquer chamada cujo número contenha a sequência configurada é rejeitada imediatamente.

## Como funciona

```mermaid
flowchart LR
    A([Chamada recebida]) --> CK{Na agenda?}
    CK -- Sim --> E([Permitida])
    CK -- Não --> B{Na lista negra?}
    B -- Sim --> C([Bloqueada])
    B -- Não --> D{Ligou antes\nnos últimos X segundos?}
    D -- Não --> C
    D -- Sim --> E
```

- **Contatos da agenda:** números salvos na agenda são sempre permitidos
- **Lista negra:** qualquer chamada cujo número contenha a sequência configurada é bloqueada imediatamente
- **Primeira chamada:** números desconhecidos são bloqueados na primeira tentativa
- **Repetição:** se o mesmo número ligar novamente dentro da janela de tempo configurada, a chamada é permitida

## Requisitos

- Android 10 (API 29) ou superior
- Permissão de triagem de chamadas (`ROLE_CALL_SCREENING`)
- Permissão de leitura de contatos (`READ_CONTACTS`) — solicitada na primeira execução
- Permissão de leitura do histórico de chamadas (`READ_CALL_LOG`) — solicitada na primeira visita à aba Chamadas

## Instalação

1. Vá em [Releases](../../releases)
2. Faça o download do arquivo `.apk` mais recente
3. Instale no dispositivo (pode ser necessário permitir instalação de fontes desconhecidas)

## Utilização

### Janela de tempo
Em **Configurações**, defina quantos segundos o app aguarda por uma repetição de chamada. Default: 120 segundos.

### Lista negra
Em **Lista Negra**, adicione sequências de dígitos. Qualquer chamada cujo número contenha essa sequência será rejeitada.

> Ex.: a sequência `91234` bloqueia chamadas de `02191234...`

### Histórico
Em **Chamadas**, constam os registros de chamadas liberadas e bloqueadas (chamadas efetuadas não são exibidas). Para contatos salvos na agenda, o nome é exibido junto ao número. Chamadas bloqueadas aparecem em vermelho.

### Notificação persistente
O app mantém uma notificação ativa enquanto o serviço de triagem estiver em execução:

- **Fundo verde / ícone de escudo:** sem novos bloqueios desde a última abertura do app
- **Fundo vermelho / ícone de escudo com !:** indica quantas chamadas foram bloqueadas desde a última abertura do app

Ao abrir o app, o contador é zerado e a notificação volta ao estado verde.

## Compilação do projeto

**Pré-requisitos:**
- Android Studio Hedgehog ou superior
- JDK 11

**Passos:**
1. Clone o repositório
   ```bash
   git clone https://github.com/acdcmaia/call-guard.git
   ```
2. Abra o projeto no Android Studio
3. Faça `Build → Build APK(s)`

## Limitações conhecidas

**MIUI (Xiaomi) e Samsung:** o sistema ignora o serviço de triagem para números salvos nos contatos, aprovando-os automaticamente antes mesmo de consultar o app. O comportamento final é o mesmo — contatos sempre são permitidos.

**Restrição de bateria no MIUI:** pode impedir o funcionamento do serviço. Definir o app como "Sem restrições" em Configurações → Aplicativos → Call Guard → Bateria.

## Licença

Uso pessoal.
