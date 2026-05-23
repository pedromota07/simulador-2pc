# Simulador Two-Phase Commit (2PC)

Trabalho de Banco de Dados Distribuído.

O projeto simula o protocolo Two-Phase Commit, usado para garantir atomicidade em
transações distribuídas. A simulação possui um coordenador e três participantes.

## Como rodar

No terminal, dentro desta pasta:

```powershell
javac *.java
java Main
```

## Arquivos principais

- `Main.java`: executa os cenários de teste.
- `Coordinator.java`: representa o coordenador da transação.
- `Participant.java`: representa cada participante.
- `State.java`: estados possíveis do coordenador e dos participantes.
- `Vote.java`: votos possíveis na fase de votação.
- `ParticipantBehavior.java`: comportamento configurado para cada participante.

## Fluxo do protocolo

1. Fase de votação:
   O coordenador envia `PREPARE` para os participantes.
   Cada participante responde com `YES`, `NO`, `READ_ONLY` ou `TIMEOUT`.

2. Fase de decisão:
   Se todos os participantes ativos votarem `YES` ou `READ_ONLY`, o coordenador
   decide `COMMIT`.
   Se algum participante votar `NO` ou ocorrer `TIMEOUT`, o coordenador decide
   `ABORT`.

## Variantes implementadas

1. Participante `READ_ONLY`:
   O participante informa que não alterou dados. Assim, ele não precisa aplicar
   commit nem rollback, apenas encerra sua participação.

2. Falhas e bloqueio:
   A simulação trata timeout de participante como motivo para abortar a transação.
   Também existe um cenário em que o coordenador falha depois da votação e antes
   da decisão. Nesse caso, os participantes que votaram `YES` ficam bloqueados,
   mostrando uma limitação clássica do 2PC.

## Cenários de teste

- Cenário 1: commit com todos os participantes votando `YES`.
- Cenário 2: abort quando um participante vota `NO`.
- Cenário 3: abort por `TIMEOUT` de participante.
- Cenário 4: commit com um participante `READ_ONLY`.
- Cenário 5: falha do coordenador causando bloqueio dos participantes preparados.

## Observação

Esta implementação é uma simulação em console. Ela não usa rede, banco real ou
threads, pois o foco é demonstrar o comportamento do protocolo 2PC e seus
principais casos de falha.
