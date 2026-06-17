# Simulador de Commit Distribuído

Trabalho de Banco de Dados Distribuído.

O projeto simula protocolos de commit distribuído em console. Ele compara o 2PC
tradicional, Presumed Abort, Presumed Commit e Three-Phase Commit (3PC).

A simulação mostra votação, decisão, logs, ACKs, falhas e recuperação. Ela não
usa rede real, banco real, threads, timeout real ou eleição de coordenador. O
objetivo é demonstrar o comportamento dos protocolos de forma didática.

## Como rodar

No terminal, dentro desta pasta:

```powershell
javac *.java
java Main
```

Se o Java não estiver no PATH nesta máquina, use o JDK local:

```powershell
& "C:\Users\labor\.jdks\ms-17.0.15\bin\javac.exe" *.java
& "C:\Users\labor\.jdks\ms-17.0.15\bin\java.exe" Main
```

Ao iniciar, o programa abre um menu interativo:

```text
1 - 2PC tradicional
2 - Presumed Abort
3 - Presumed Commit
4 - Three-Phase Commit
5 - Rodar todos os protocolos
0 - Sair
```

## Arquivos principais

- `Main.java`: menu e cenários de teste.
- `Coordinator.java`: coordenador da transação e fluxo dos protocolos.
- `Participant.java`: comportamento dos participantes.
- `RecoveryManager.java`: regras de recuperação do coordenador e dos participantes.
- `ProtocolType.java`: protocolos disponíveis.
- `CoordinatorFailurePoint.java`: pontos de falha simulados.
- `State.java`: estados possíveis do coordenador e dos participantes.
- `Vote.java`: votos possíveis na fase de votação.
- `ParticipantBehavior.java`: comportamento configurado para cada participante.

## Protocolos simulados

### 2PC tradicional

- O coordenador envia `PREPARE`.
- Participantes votam `YES`, `NO`, `READ_ONLY` ou simulam `TIMEOUT`.
- Se todos os ativos votam `YES` ou `READ_ONLY`, o coordenador grava
  `GLOBAL_COMMIT` e envia `COMMIT`.
- Se houver `NO` ou `TIMEOUT`, o coordenador grava `GLOBAL_ABORT` e envia
  `ABORT`.
- Sem decisão global no log, participante que ficou em `READY` pode bloquear.

### Presumed Abort

- O coordenador não precisa gravar registro inicial antes do `PREPARE`.
- `COMMIT` precisa de `GLOBAL_COMMIT` no log.
- Participantes preparados enviam ACK de `COMMIT`.
- `ABORT` não precisa de `GLOBAL_ABORT` nem ACK.
- Se não houver `GLOBAL_COMMIT`, a recuperação presume `ABORT`.

### Presumed Commit

- O coordenador grava `COMMIT_INIT` antes de enviar `PREPARE`.
- `COMMIT` precisa de `GLOBAL_COMMIT` no log.
- `COMMIT` não exige ACK dos participantes.
- `ABORT` exige ACK dos participantes preparados antes de esquecer.
- Se houver `COMMIT_INIT` sem `GLOBAL_COMMIT`, a transação está pendente; a
  simulação aplica a política segura de recuperar como `ABORT`.
- A ausência de informação só presume `COMMIT` quando a transação já foi
  esquecida com segurança.

### Three-Phase Commit

- O 3PC possui três fases: `PREPARE`, `PRE_COMMIT` e `DO_COMMIT`.
- O coordenador grava `START` antes de enviar `PREPARE`.
- Se houver `NO` ou `TIMEOUT`, o coordenador grava `GLOBAL_ABORT` e envia
  `ABORT`.
- Se todos os ativos votam `YES` ou `READ_ONLY`, o coordenador grava
  `PRE_COMMIT` e envia `PRE_COMMIT` apenas aos participantes que votaram `YES`.
- O coordenador espera ACK dos participantes em `PRE_COMMIT` antes de gravar
  `GLOBAL_COMMIT`.
- Depois de gravar `GLOBAL_COMMIT`, o coordenador envia `DO_COMMIT`.
- Participantes `READ_ONLY` não recebem `PRE_COMMIT` nem `DO_COMMIT`.
- Na recuperação, `PRE_COMMIT` sem `GLOBAL_COMMIT` permite continuar o protocolo:
  o coordenador grava `GLOBAL_COMMIT`, reenvia `DO_COMMIT` e finaliza como
  `COMMITTED`.
- Participante em `PRE_COMMITTED` pode avançar para `COMMIT`; participante que
  ficou apenas em `READY`, sem `PRE_COMMIT`, pode abortar.

## Cenários

Para 2PC, Presumed Abort e Presumed Commit:

- Cenário 1: commit com todos os participantes votando `YES`.
- Cenário 2: abort quando um participante vota `NO`.
- Cenário 3: abort por `TIMEOUT` de participante.
- Cenário 4: commit com um participante `READ_ONLY`.
- Cenário 5: falha antes de `GLOBAL_COMMIT`.
- Cenário 6: falha depois de `GLOBAL_COMMIT`.
- Cenário 7: participante preparado consulta recuperação.

Para 3PC:

- Cenário 1: commit normal.
- Cenário 2: abort por `NO`.
- Cenário 3: abort por `TIMEOUT`.
- Cenário 4: participante `READ_ONLY`.
- Cenário 5: falha antes de `PRE_COMMIT`.
- Cenário 6: falha depois de `PRE_COMMIT`.
- Cenário 7: falha antes de `DO_COMMIT`.

## READ_ONLY e TIMEOUT

- Participante `READ_ONLY` não grava alterações, não entra nas fases finais e
  não recebe `PRE_COMMIT`, `COMMIT` nem `DO_COMMIT`.
- `TIMEOUT` durante a votação faz o coordenador decidir `ABORT`.
- Apenas participantes que votaram `YES` recebem as mensagens finais do
  protocolo.
