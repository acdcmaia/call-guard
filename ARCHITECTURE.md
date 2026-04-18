# Call Guard — Arquitetura

Aplicativo Android de triagem de chamadas. Bloqueia automaticamente chamadas de números desconhecidos e permite configurar uma lista negra baseada em sequências numéricas.

**Regra principal:** uma chamada é permitida somente se o número chamador já tiver ligado anteriormente dentro da janela de tempo configurada. Chamadas de números na lista negra são sempre bloqueadas.

---

## Estrutura do projeto

Projeto Android de módulo único (`app`), compilado com SDK 36, mínimo SDK 29 (Android 10).

```
call-guard/
├── app/src/main/
│   ├── AndroidManifest.xml
│   └── java/com/acdcmaia/callguard/
│       ├── CallGuardApp.kt          # Application — inicializa repositórios
│       ├── MainActivity.kt          # Entrada + gestão do role de triagem
│       ├── data/
│       │   ├── db/                  # Room: entidades, DAOs, base de dados
│       │   ├── CallRepository.kt    # Blacklist + chamadas recentes
│       │   ├── CallLogRepository.kt # Histórico mesclado (Room + sistema)
│       │   ├── SettingsRepository.kt# Preferências (DataStore)
│       │   └── CallHistoryItem.kt   # Modelo de exibição do histórico
│       ├── service/
│       │   ├── CallGuardScreeningService.kt  # Triagem de chamadas
│       │   ├── CallGuardForegroundService.kt # Serviço de notificação persistente
│       │   └── PhoneStateReceiver.kt         # Diagnóstico de estado de chamada
│       └── ui/
│           ├── Navigation.kt        # Bottom navigation (3 abas)
│           ├── calls/               # Histórico de chamadas
│           ├── blacklist/           # Lista negra
│           └── settings/            # Configurações
```

---

## Componentes principais

### `CallGuardApp`
Classe `Application`. Inicializa e expõe como singletons:
- `AppDatabase` (Room)
- `CallRepository`
- `CallLogRepository`
- `SettingsRepository`

### `MainActivity`
- Verifica se o app possui o `ROLE_CALL_SCREENING` do Android
- Se não tiver, exibe tela pedindo permissão via `RoleManager`
- Se tiver, inicia o `CallGuardForegroundService`
- Recebe o intent `android.telecom.action.POST_CALL` (enviado pelo telecom após cada chamada) via `onNewIntent()`, que dispara nova verificação do role
- Usa `launchMode="singleTop"` para evitar instâncias duplicadas ao receber o POST_CALL

### `CallGuardScreeningService`
Implementa `CallScreeningService` do Android. É vinculado pelo framework telecom a cada chamada recebida.

- Executa a lógica de triagem em `Dispatchers.IO` com `coroutineScope { async }` para carregar blacklist e configurações em paralelo
- Sempre chama `respondToCall()`, inclusive em caso de exceção (fallback: permitir)
- Usa `setDisallowCall(true)` + `setRejectCall(true)` para bloquear: o chamador recebe sinal de ocupado imediatamente

### `CallGuardForegroundService`
Serviço de notificação persistente (`foregroundServiceType="dataSync"`). Mantém o processo do app vivo para garantir que o `CallGuardScreeningService` seja vinculado pelo telecom sem ser morto pelo sistema.

### `PhoneStateReceiver`
Receiver de `android.intent.action.PHONE_STATE`. Adicionado como ferramenta de diagnóstico para confirmar se chamadas chegam ao processo do app, independentemente do `CallScreeningService`.

---

## Camada de dados

### Room — `callguard.db`

| Tabela | Entidade | Descrição |
|---|---|---|
| `blacklist_patterns` | `BlacklistPattern` | Sequências de dígitos a bloquear |
| `recent_calls` | `RecentCall` | Registo de cada chamada processada pelo app |

**`BlacklistPattern`**
```
id (PK, autoincrement)
pattern: String   — sequência de dígitos (ex: "98181")
label: String     — descrição opcional
```

**`RecentCall`**
```
id (PK, autoincrement)
number: String
timestamp: Long
allowed: Boolean
blockReason: BlockReason?  — BLACKLIST | FIRST_CALL | null (se permitida)
```

### DataStore — `settings`

| Chave | Tipo | Default | Descrição |
|---|---|---|---|
| `window_seconds` | Int | 300 | Janela de tempo em segundos (anterior: `window_minutes` — migrado) |

### `CallLogRepository`
Agrega duas fontes para o histórico:
1. **Log do sistema** (`CallLog.Calls`) — todas as chamadas registadas pelo Android
2. **Room** (`recent_calls`) — chamadas processadas pelo app

Entradas do Room sem correspondência no log do sistema (chamadas bloqueadas ainda não registadas pelo Android) aparecem imediatamente como `appOnly = true`, garantindo atualização em tempo real.

---

## Fluxo de triagem de chamada

```mermaid
flowchart TD
    A([Chamada recebida]) --> B{handle nulo?}
    B -- Sim --> Z1([Permitir — fallback])
    B -- Não --> C[Carregar blacklist e\nwindow_seconds em paralelo]
    C --> D{Dígitos do número\ncontêm algum padrão\nda blacklist?}
    D -- Sim --> E[Gravar RecentCall\nblockReason=BLACKLIST]
    E --> Z2([Rejeitar\nsetDisallowCall + setRejectCall])
    D -- Não --> F[Consultar chamadas\ndo mesmo número\ndentro da janela]
    F --> G{Há chamadas\nrecentes?}
    G -- Não --> H[Gravar RecentCall\nblockReason=FIRST_CALL]
    H --> Z3([Rejeitar\nsetDisallowCall + setRejectCall])
    G -- Sim --> I[Gravar RecentCall\nallowed=true]
    I --> Z4([Permitir])
```

---

## Fluxo de atualização do histórico

```mermaid
flowchart LR
    A[CallGuardScreeningService\ninsere RecentCall no Room] --> B[Room Flow emite\nnova lista]
    B --> C[RecentCallsViewModel\ncollect → refresh]
    C --> D[getMergedHistory:\nRoom + log do sistema]
    D --> E[UI atualiza\nem tempo real]

    F[Chamada termina] --> G[Android regista\nno CallLog do sistema]
    G --> H[repeatOnLifecycle RESUMED\ntrigger no próximo foco]
    H --> D
```

---

## Lógica de correspondência da lista negra

A correspondência **não usa regex**. É uma busca de substring sobre os dígitos do número:

```
número chamador : "+5521XXXXXXXX"
dígitos extraídos: "5521XXXXXXXX"
padrão na blacklist: "98181"
resultado: BLOQUEADO  ✓  ("5521XXXXXXXX".contains("98181"))
```

Wildcards não são suportados. O campo da lista negra aceita apenas dígitos.

---

## Navegação

Bottom navigation com 3 abas:

| Rota | Ecrã | Descrição |
|---|---|---|
| `calls` | `RecentCallsScreen` | Histórico de chamadas (start destination); chamadas bloqueadas em vermelho |
| `blacklist` | `BlacklistScreen` | Gerir sequências bloqueadas; suporta adicionar, editar e remover |
| `settings` | `SettingsScreen` | Janela de tempo em segundos; build info no ícone ⓘ |

---

## Permissões e role

| Permissão | Motivo |
|---|---|
| `ROLE_CALL_SCREENING` | Obrigatório para o `CallScreeningService` ser vinculado pelo telecom |
| `READ_PHONE_STATE` | Receber broadcast de estado de chamada (`PhoneStateReceiver`) |
| `READ_CALL_LOG` | Ler o log de chamadas do sistema para o histórico |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_DATA_SYNC` | Manter o serviço persistente ativo |
| `POST_NOTIFICATIONS` | Notificação do `CallGuardForegroundService` |

O role é solicitado via `RoleManager.createRequestRoleIntent`. Cada reinstalação revoga o role e gera novo UID, apagando a base de dados Room.

---

## Decisões de arquitetura

**`coroutineScope { async }` no serviço de triagem**
Blacklist e configurações são carregadas em paralelo dentro de `coroutineScope`. Usar `CoroutineScope(Dispatchers.IO).async` criaria scopes não gerenciados; `coroutineScope` garante concorrência estruturada e propagação de exceções.

**Fallback always-allow**
Qualquer exceção não tratada durante a triagem resulta em `respondToCall` com resposta padrão (permitir). O Android impõe um timeout; não chamar `respondToCall` também deixaria a chamada passar, mas geraria ANR no serviço.

**`setDisallowCall + setRejectCall`**
Bloquear apenas com `setDisallowCall` silencia a chamada do lado do destinatário mas o chamador continua a ouvir toque. Adicionar `setRejectCall` envia sinal de ocupado imediatamente, encerrando a chamada no lado do chamador também.

**Histórico com dupla fonte**
O log do sistema só regista chamadas após o fim da ligação. Para mostrar chamadas bloqueadas imediatamente, o `CallLogRepository` inclui registos do Room sem correspondência no log do sistema (`appOnly = true`). Quando o log do sistema eventualmente regista a entrada, ela substitui a entrada `appOnly` no próximo refresh.

**`launchMode="singleTop"` na MainActivity**
O telecom envia `POST_CALL` para a Activity após cada chamada. Sem `singleTop`, cada intent criaria uma nova instância da Activity. Com `singleTop`, o intent é entregue via `onNewIntent()` na instância existente.

**Tooltips com posicionamento abaixo do campo**
O Material3 `TooltipDefaults` não expõe controlo de posição vertical. É usado um `PopupPositionProvider` customizado que posiciona o balão em `anchorBounds.bottom`, garantindo que não ultrapassa a largura da janela. Aplicado nos campos "Segundos" (Configurações) e "Sequência de dígitos" (Lista Negra).

---

## Limitação confirmada — MIUI

Em dispositivos Xiaomi com MIUI, o framework telecom **ignora o `CallScreeningService` para números guardados nos contactos**, aprovando-os automaticamente sem invocar `onScreenCall`.

**Confirmação:** testado adicionando e removendo o mesmo número da agenda. Com o número na agenda, `onScreenCall` nunca é invocado (logcat do `system_server` mostra `[Allow, contact exists]`, zero logs `CallGuard`). Com o número removido da agenda, o serviço de triagem é invocado normalmente e o bloqueio funciona.

**Implicação:** o app funciona corretamente para números desconhecidos (não guardados nos contactos). Chamadas de contactos guardados passam sempre, independentemente da blacklist ou da janela de tempo.

**Workaround possível:** não existe solução via `CallScreeningService` — é um bypass do próprio MIUI. A restrição de bateria do MIUI também pode impedir o binding do serviço — definir o app como "Sem restrições" em Configurações → Aplicativos → Call Guard → Bateria.
