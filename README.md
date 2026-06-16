# Simulador Two-Phase Commit (2PC)

Trabalho de Banco de Dados Distribuido.

O projeto simula o protocolo Two-Phase Commit e compara o 2PC tradicional com
duas variacoes: Presumed Abort e Presumed Commit. A simulacao e em console e
mostra votacao, decisao, logs, ACKs, falhas e recuperacao.

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

Depois de escolher um protocolo, escolha um cenario especifico ou rode todos os
cenarios daquele protocolo:

```text
1 - Commit com todos YES
2 - Abort com participante NO
3 - Abort por TIMEOUT
4 - Commit com participante READ_ONLY
5 - Falha antes de GLOBAL_COMMIT
6 - Falha apos GLOBAL_COMMIT
7 - Participante preparado consulta o coordenador
8 - Rodar todos os cenarios deste protocolo
0 - Voltar
```

Se escolher `4 - Rodar todos os protocolos`, o programa abre o mesmo menu de
cenarios. Assim, voce pode comparar apenas um cenario nos tres protocolos ou
rodar a simulacao completa.

A saida de cada cenario mostra separadamente o protocolo, o cenario e o id da
transacao para facilitar a leitura durante a apresentacao.

A saida usa cores ANSI apenas nos resultados/estados dos cenarios. Caso o
terminal nao renderize cores corretamente, rode com a variavel `NO_COLOR`
definida para desativar essa formatacao.

## Arquivos principais

- `Main.java`: executa os cenarios de teste para cada protocolo.
- `Coordinator.java`: representa o coordenador da transacao.
- `Participant.java`: representa cada participante.
- `RecoveryManager.java`: aplica a regra de recuperacao.
- `ProtocolType.java`: define `TWO_PC`, `PRESUMED_ABORT` e `PRESUMED_COMMIT`.
- `CoordinatorFailurePoint.java`: define os pontos de falha simulados.
- `State.java`: estados possiveis do coordenador e dos participantes.
- `Vote.java`: votos possiveis na fase de votacao.
- `ParticipantBehavior.java`: comportamento configurado para cada participante.

## Regras simuladas

### 2PC tradicional

- O coordenador grava a decisao global.
- Participantes preparados recebem `COMMIT` ou `ABORT`.
- Sem decisao global no log, participantes preparados continuam bloqueados.

### Presumed Abort

- O coordenador nao precisa gravar registro inicial antes do `PREPARE`.
- `COMMIT` precisa de `GLOBAL_COMMIT` no log.
- Participantes preparados enviam ACK de `COMMIT`.
- `ABORT` nao precisa de `GLOBAL_ABORT` nem ACK.
- Se nao houver `GLOBAL_COMMIT`, a recuperacao presume `ABORT`.

### Presumed Commit

- O coordenador grava `COMMIT_INIT` antes de enviar `PREPARE`.
- `COMMIT` precisa de `GLOBAL_COMMIT` no log.
- `COMMIT` nao exige ACK dos participantes.
- `ABORT` exige ACK dos participantes preparados antes de esquecer.
- Se houver `COMMIT_INIT` sem `GLOBAL_COMMIT`, a transacao esta pendente; a
  simulacao aplica a politica segura de recuperar como `ABORT`.
- A ausencia de informacao so presume `COMMIT` quando a transacao ja foi
  esquecida com seguranca.

## Cenarios de teste

Cada protocolo executa os seguintes cenarios:

- Cenario 1: commit com todos os participantes votando `YES`.
- Cenario 2: abort quando um participante vota `NO`.
- Cenario 3: abort por `TIMEOUT` de participante.
- Cenario 4: commit com um participante `READ_ONLY`.
- Cenario 5: falha do coordenador antes de `GLOBAL_COMMIT`.
- Cenario 6: falha do coordenador depois de `GLOBAL_COMMIT`.
- Cenario 7: participante preparado consulta o coordenador durante recuperacao.

## READ_ONLY e TIMEOUT

- Participante `READ_ONLY` nao gravou alteracoes, nao entra na segunda fase e
  nao envia ACK.
- `TIMEOUT` durante a votacao faz o coordenador decidir `ABORT`.
- Apenas participantes que votaram `YES` recebem a decisao final.

## Observacao

Esta implementacao nao usa rede, banco real ou threads. O objetivo e demonstrar
o comportamento dos protocolos de commit distribuido e seus casos de falha de
forma didatica para apresentacao academica.
