# Simulador Two-Phase Commit (2PC)

Trabalho de Banco de Dados Distribuído.

O projeto simula o protocolo Two-Phase Commit e compara o 2PC tradicional com
duas variações: Presumed Abort e Presumed Commit. A simulação é em console e
mostra votação, decisão, logs, ACKs, falhas e recuperação.

## Como rodar

No terminal, dentro desta pasta:

```powershell
javac *.java
java Main
```

Ao iniciar, o programa abre um menu interativo:

```text
1 - 2PC tradicional
2 - Presumed Abort
3 - Presumed Commit
4 - Rodar todos os protocolos
0 - Sair
```

Depois de escolher um protocolo, escolha um cenário específico ou rode todos os
cenários daquele protocolo:

```text
1 - Commit com todos YES
2 - Abort com participante NO
3 - Abort por TIMEOUT
4 - Commit com participante READ_ONLY
5 - Falha antes de GLOBAL_COMMIT
6 - Falha após GLOBAL_COMMIT
7 - Participante preparado consulta o coordenador
8 - Rodar todos os cenários deste protocolo
0 - Voltar
```

Se escolher `4 - Rodar todos os protocolos`, o programa abre o mesmo menu de
cenários. Assim, você pode comparar apenas um cenário nos três protocolos ou
rodar a simulação completa.

## Arquivos principais

- `Main.java`: executa os cenários de teste para cada protocolo.
- `Coordinator.java`: representa o coordenador da transação.
- `Participant.java`: representa cada participante.
- `RecoveryManager.java`: aplica a regra de recuperação.
- `ProtocolType.java`: define `TWO_PC`, `PRESUMED_ABORT` e `PRESUMED_COMMIT`.
- `CoordinatorFailurePoint.java`: define os pontos de falha simulados.
- `State.java`: estados possíveis do coordenador e dos participantes.
- `Vote.java`: votos possíveis na fase de votação.
- `ParticipantBehavior.java`: comportamento configurado para cada participante.

## Regras simuladas

### 2PC tradicional

- O coordenador grava a decisão global.
- Participantes preparados recebem `COMMIT` ou `ABORT`.
- Sem decisão global no log, participantes preparados continuam bloqueados.

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

## Cenários de teste

Cada protocolo executa os seguintes cenários:

- Cenário 1: commit com todos os participantes votando `YES`.
- Cenário 2: abort quando um participante vota `NO`.
- Cenário 3: abort por `TIMEOUT` de participante.
- Cenário 4: commit com um participante `READ_ONLY`.
- Cenário 5: falha do coordenador antes de `GLOBAL_COMMIT`.
- Cenário 6: falha do coordenador depois de `GLOBAL_COMMIT`.
- Cenário 7: participante preparado consulta o coordenador durante recuperação.

## READ_ONLY e TIMEOUT

- Participante `READ_ONLY` não gravou alterações, não entra na segunda fase e
  não envia ACK.
- `TIMEOUT` durante a votação faz o coordenador decidir `ABORT`.
- Apenas participantes que votaram `YES` recebem a decisão final.

## Observação

Esta implementação não usa rede, banco real ou threads. O objetivo é demonstrar
o comportamento dos protocolos de commit distribuído e seus casos de falha de
forma didática para apresentação acadêmica.
