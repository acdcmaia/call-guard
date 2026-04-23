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
- As permissões abaixo são solicitadas no onboarding, na primeira execução:
  - Triagem de chamadas (`ROLE_CALL_SCREENING`)
  - Leitura de contatos (`READ_CONTACTS`)
  - Leitura do histórico de chamadas (`READ_CALL_LOG`)
  - Notificações (`POST_NOTIFICATIONS`) — Android 13 ou superior
  - Isenção de otimização de bateria (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`)
  - Instalação de aplicativos de fontes desconhecidas (permite update automático) — concedida em **Configurações → Aplicativos → [app usado para abrir o APK] → Instalar apps desconhecidos**

## Instalação

1. Vá em [Releases](../../releases)
2. Faça o download do arquivo `.apk` mais recente
3. Abra o arquivo no dispositivo para iniciar a instalação
4. Após a instalação, abra o Call Guard e siga o onboarding para conceder as permissões necessárias

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

### Início automático
O app inicia automaticamente após a reinicialização do dispositivo. Em fabricantes com restrição de início automático (Xiaomi, Samsung, Huawei e outros), uma tela de configuração é exibida na primeira execução com atalho direto para as configurações do fabricante.

O status do início automático também pode ser verificado em **Configurações → Início automático do aplicativo**, destacado em vermelho enquanto não estiver configurado.

### Atualizações
Em **Configurações → Verificar atualizações**, o app consulta automaticamente a disponibilidade de uma nova versão. Ao detectar, baixa e instala o APK diretamente sem sair do app.

### Sobre
Em **Configurações → Sobre**, são exibidos a versão, a data de build e o desenvolvedor.

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

**Início automático em MIUI, Samsung, Huawei e outros:** alguns fabricantes bloqueiam o início automático de aplicativos após reinicialização. O app exibe uma tela de configuração na primeira execução com atalho para as configurações do fabricante. Sem essa configuração, o serviço de triagem não inicia automaticamente após reiniciar o dispositivo.

## Licença

Uso pessoal.
