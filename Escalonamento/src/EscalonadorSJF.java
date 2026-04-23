import java.util.List;
import java.util.ArrayList;

public class EscalonadorSJF {

    public void executar(List<Processo> processos) {

        int tempoAtual = 0;

        // Conta quantos processos já terminaram
        int processosConcluidos = 0;

        // Total de processos que temos para executar
        int totalProcessos = processos.size();

        // Lista de processos que já chegaram e estão prontos para executar (fila de prontos)
        List<Processo> filaProntos = new ArrayList<Processo>();

        // Lista separada para guardar os processos que estão bloqueados fazendo I/O.
        // Precisamos dela para saber quais processos ainda existem na simulação
        // mas não podem ser executados agora.
        List<Processo> filaBloqueados = new ArrayList<Processo>();

        // Fica em loop até todos os processos terminarem
        while (processosConcluidos < totalProcessos) {

            // ---------------------------------------------------------
            // PASSO 1: Verifica se algum processo bloqueado terminou
            //          seu I/O e pode voltar para a fila de prontos
            // ---------------------------------------------------------
            // Usamos uma lista auxiliar para não modificar filaBloqueados
            // enquanto ainda estamos percorrendo ela (isso causaria erro em Java)
            List<Processo> voltandoDoIO = new ArrayList<Processo>();

            for (Processo processo : filaBloqueados) {
                // Se o tempo de bloqueio já passou, o processo pode voltar
                if (!processo.estaBloqueado(tempoAtual)) {
                    voltandoDoIO.add(processo);
                }
            }

            // Agora sim removemos da fila de bloqueados e colocamos na fila de prontos
            for (Processo processo : voltandoDoIO) {
                filaBloqueados.remove(processo);
                processo.liberarBloqueio();   // limpa o tempoBloqueioFim dentro do objeto Processo
                filaProntos.add(processo);    // coloca de volta na fila de prontos
            }

            // ---------------------------------------------------------
            // PASSO 2: Verifica quais NOVOS processos chegaram até
            //          o tempo atual e coloca na fila de prontos
            // ---------------------------------------------------------
            for (Processo processo : processos) {

                // Condições para adicionar na fila de prontos:
                // 1) O processo já chegou  (tempoChegada <= tempoAtual)
                // 2) Ainda não foi concluído
                // 3) Ainda não está na fila de prontos
                // 4) Não está na fila de bloqueados (fazendo I/O)
                boolean jaChegou         = processo.getTempoChegada() <= tempoAtual;
                boolean naoTerminou      = !processo.isConcluido();
                boolean naoEstaEmProntos = !filaProntos.contains(processo);
                boolean naoEstaBloqueado = !filaBloqueados.contains(processo);

                if (jaChegou && naoTerminou && naoEstaEmProntos && naoEstaBloqueado) {
                    filaProntos.add(processo);
                }
            }

            // ---------------------------------------------------------
            // PASSO 3: Se a fila de prontos está vazia, a CPU fica
            //          ociosa por 1 unidade de tempo e tenta de novo
            // ---------------------------------------------------------
            if (filaProntos.isEmpty()) {
                tempoAtual++;
                continue; // volta pro início do while
            }

            // ---------------------------------------------------------
            // PASSO 4: Escolhe o processo com MENOR TEMPO RESTANTE
            //          Essa é a regra principal do algoritmo SRTF:
            //          sempre executa quem tem menos CPU pela frente
            // ---------------------------------------------------------
            Processo processoEscolhido = encontrarMenorTempoRestante(filaProntos);

            // ---------------------------------------------------------
            // PASSO 5: Incrementa a espera de todos os outros processos
            //          que estão prontos mas não foram escolhidos.
            //          Eles ficam parados esperando por 1 unidade de tempo.
            // ---------------------------------------------------------
            for (Processo processo : filaProntos) {
                if (processo != processoEscolhido) {
                    processo.incrementarEspera(1);
                }
            }

            // ---------------------------------------------------------
            // PASSO 6: Executa 1 tick de CPU para o processo escolhido.
            //          Incrementamos a CPU acumulada ANTES de checar I/O,
            //          pois o instante de I/O representa o total de CPU
            //          já consumido no momento do disparo.
            //
            //          Exemplo do enunciado:
            //          "instante 10" = após usar 10ms de CPU => cpuAcumulada == 10
            // ---------------------------------------------------------
            processoEscolhido.setCpuAcumulada(processoEscolhido.getCpuAcumulada() + 1);
            tempoAtual++;

            // ---------------------------------------------------------
            // PASSO 7: Verifica se o processo precisa fazer I/O agora.
            //
            //          deveDispararIO() compara cpuAcumulada com os
            //          instantes de I/O definidos no arquivo .txt.
            //          Se bater, o processo deve ser bloqueado.
            // ---------------------------------------------------------
            if (processoEscolhido.deveDispararIO()) {

                // Registra o I/O: define tempoBloqueioFim = tempoAtual + 5
                processoEscolhido.dispararIO(tempoAtual);

                // Remove da fila de prontos pois está bloqueado
                filaProntos.remove(processoEscolhido);

                // Coloca na fila de bloqueados para controlarmos quando ele volta
                filaBloqueados.add(processoEscolhido);

            // ---------------------------------------------------------
            // PASSO 8: Se não houve I/O, verifica se o processo terminou.
            //          Um processo termina quando já usou todo o burstTotal.
            // ---------------------------------------------------------
            } else if (processoEscolhido.getCpuAcumulada() >= processoEscolhido.getBurstTotal()) {

                // Marca o processo como concluído e calcula o turnaround
                processoEscolhido.concluir(tempoAtual);

                // Remove da fila de prontos
                filaProntos.remove(processoEscolhido);

                // Incrementa o contador de processos concluídos
                processosConcluidos++;
            }

        } // fim do while principal

        // Exibe as métricas ao final da simulação
        exibirMetricas(processos, tempoAtual);
    }

    // ------------------------------------------------------------------ //
    // Método auxiliar: encontra o processo com menor tempo restante.       //
    // Tempo restante = burstTotal - cpuAcumulada                           //
    // ------------------------------------------------------------------ //
    private Processo encontrarMenorTempoRestante(List<Processo> filaProntos) {

        // Começa assumindo que o primeiro processo da fila tem o menor tempo restante
        Processo menorProcesso = filaProntos.get(0);

        // Percorre o restante da lista comparando um a um
        for (int i = 1; i < filaProntos.size(); i++) {

            Processo processo = filaProntos.get(i);

            // Se encontramos um processo com MENOS tempo restante, ele passa a ser o escolhido
            if (processo.getTempoRestante() < menorProcesso.getTempoRestante()) {
                menorProcesso = processo;
            }
        }

        return menorProcesso;
    }

    // ------------------------------------------------------------------ //
    // Método auxiliar: exibe as métricas finais da simulação               //
    // ------------------------------------------------------------------ //
    private void exibirMetricas(List<Processo> processos, int tempoTotal) {

        double somaEspera  = 0;
        double somaRetorno = 0;

        System.out.println("\n=== SRTF (Shortest Remaining Time First) ===");
        System.out.printf("%-5s %-10s %-12s%n", "PID", "Espera", "Turnaround");

        for (Processo processo : processos) {
            somaEspera  += processo.getTempoEspera();
            somaRetorno += processo.getTempoRetorno();

            System.out.printf("%-5d %-10d %-12d%n",
                    processo.getPid(),
                    processo.getTempoEspera(),
                    processo.getTempoRetorno());
        }

        int n = processos.size();

        System.out.printf("%nTempo de Espera Médio  : %.2f%n", somaEspera  / n);
        System.out.printf("Turnaround Médio       : %.2f%n",   somaRetorno / n);
        System.out.printf("Throughput             : %.4f processos/u.t.%n", (double) n / tempoTotal);
    }
}