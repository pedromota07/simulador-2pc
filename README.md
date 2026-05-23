# Simulador Two-Phase Commit (2PC)

Trabalho de Banco de Dados Distribuido.

O projeto simula o protocolo Two-Phase Commit, usado para garantir atomicidade em
transacoes distribuidas. A simulacao possui um coordenador e tres participantes.

## Como rodar

No terminal, dentro desta pasta:

```powershell
javac *.java
java Main
```

## Arquivos principais

- `Main.java`: executa os cenarios de teste.
- `Coordinator.java`: representa o coordenador da transacao.
- `Participant.java`: representa cada participante.
- `State.java`: estados possiveis do coordenador e dos participantes.
- `Vote.java`: votos possiveis na fase de votacao.
- `ParticipantBehavior.java`: comportamento configurado para cada participante.

## Fluxo do protocolo

1. Fase de votacao:
   O coordenador envia `PREPARE` para os participantes.
   Cada participante responde com `YES`, `NO`, `READ_ONLY` ou `TIMEOUT`.

2. Fase de decisao:
   Se todos os participantes ativos votarem `YES` ou `READ_ONLY`, o coordenador
   decide `COMMIT`.
   Se algum participante votar `NO` ou ocorrer `TIMEOUT`, o coordenador decide
   `ABORT`.

## Variantes implementadas

1. Participante `READ_ONLY`:
   O participante informa que nao alterou dados. Assim, ele nao precisa aplicar
   commit nem rollback, apenas encerra sua participacao.

2. Falhas e bloqueio:
   A simulacao trata timeout de participante como motivo para abortar a transacao.
   Tambem existe um cenario em que o coordenador falha depois da votacao e antes
   da decisao. Nesse caso, os participantes que votaram `YES` ficam bloqueados,
   mostrando uma limitacao classica do 2PC.

## Cenarios de teste

- Cenario 1: commit com todos os participantes votando `YES`.
- Cenario 2: abort quando um participante vota `NO`.
- Cenario 3: abort por `TIMEOUT` de participante.
- Cenario 4: commit com um participante `READ_ONLY`.
- Cenario 5: falha do coordenador causando bloqueio dos participantes preparados.

## Observacao

Esta implementacao e uma simulacao em console. Ela nao usa rede, banco real ou
threads, pois o foco e demonstrar o comportamento do protocolo 2PC e seus
principais casos de falha.
