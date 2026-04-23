import java.util.List;
import java.util.ArrayList;

public class EscalonadorRoundRobin {

    public void executar(List<Processo> processos) {

        int tempoAtual = 0;

        // Conta quantos processos já terminaram
        int processosConcluidos = 0;

        // Total de processos que temos para executar
        int totalProcessos = processos.size();

        // Lista de processos prontos para executar (fila circular)
        List<Processo> filaProntos = new ArrayList<Processo>();

        // Lista de processos bloqueados fazendo I/O
        List<Processo> filaBloqueados = new ArrayList<Processo>();

        // Fica em loop até todos os processos terminarem
        while (processosConcluidos < totalProcessos) {

            // ---------------------------------------------------------
            // PASSO 1: Verifica se algum processo bloqueado terminou
            //          seu I/O e pode voltar para a fila de prontos
            // ---------------------------------------------------------
            List<Processo> voltandoDoIO = new ArrayList<Processo>();

            for (Processo processo : filaBloqueados) {
                if (!processo.estaBloqueado(tempoAtual)) {
                    voltandoDoIO.add(processo);
                }
            }

            for (Processo processo : voltandoDoIO) {
                filaBloqueados.remove(processo);
                processo.liberarBloqueio();
                filaProntos.add(processo); // volta para o FIM da fila (comportamento circular)
            }

            // ---------------------------------------------------------
            // PASSO 2: Verifica quais novos processos chegaram e
            //          adiciona no fim da fila de prontos
            // ---------------------------------------------------------
            for (Processo processo : processos) {
                boolean jaChegou         = processo.getTempoChegada() <= tempoAtual;
                boolean naoTerminou      = !processo.isConcluido();
                boolean naoEstaEmProntos = !filaProntos.contains(processo);
                boolean naoEstaBloqueado = !filaBloqueados.contains(processo);

                if (jaChegou && naoTerminou && naoEstaEmProntos && naoEstaBloqueado) {
                    filaProntos.add(processo);
                }
            }

            // ---------------------------------------------------------
            // PASSO 3: Se a fila de prontos está vazia, CPU fica ociosa
            // ---------------------------------------------------------
            if (filaProntos.isEmpty()) {
                tempoAtual++;
                continue;
            }

            // ---------------------------------------------------------
            // PASSO 4: Calcula o quantum da rodada atual.
            //
            //          No Round Robin com Quantum por Predição, o quantum
            //          NÃO é fixo. Ele é recalculado a cada troca de
            //          contexto como sendo a MENOR média exponencial (τ)
            //          entre todos os processos que estão na fila de prontos.
            //
            //          Isso significa: damos a cada processo uma fatia de
            //          tempo igual ao menor surto previsto na fila.
            //          Processos "curtos" influenciam o quantum para baixo,
            //          evitando que processos longos monopolizem a CPU.
            // ---------------------------------------------------------
            int quantum = calcularQuantum(filaProntos);

            // ---------------------------------------------------------
            // PASSO 5: Pega o primeiro processo da fila (cabeça da fila)
            //          No Round Robin sempre executa quem está na frente
            // ---------------------------------------------------------
            Processo processoEscolhido = filaProntos.get(0);

            // ---------------------------------------------------------
            // PASSO 6: Executa o processo tick a tick durante o quantum.
            //          A cada tick verificamos:
            //          - Se chegou novo processo (entra no fim da fila)
            //          - Se disparou I/O (bloqueia e sai da fila)
            //          - Se terminou (conclui e sai da fila)
            //          Se nenhum dos dois aconteceu e o quantum acabou,
            //          o processo vai pro fim da fila (preempção).
            // ---------------------------------------------------------
            int ticksExecutados = 0; // quantos ticks esse processo executou nessa rodada

            while (ticksExecutados < quantum) {

                // Verifica se chegou algum processo novo durante a execução
                for (Processo processo : processos) {
                    boolean jaChegou         = processo.getTempoChegada() <= tempoAtual;
                    boolean naoTerminou      = !processo.isConcluido();
                    boolean naoEstaEmProntos = !filaProntos.contains(processo);
                    boolean naoEstaBloqueado = !filaBloqueados.contains(processo);

                    if (jaChegou && naoTerminou && naoEstaEmProntos && naoEstaBloqueado) {
                        filaProntos.add(processo); // entra no FIM da fila
                    }
                }

                // Verifica se algum processo terminou o I/O durante a execução
                List<Processo> voltandoDoIOInterno = new ArrayList<Processo>();
                for (Processo processo : filaBloqueados) {
                    if (!processo.estaBloqueado(tempoAtual)) {
                        voltandoDoIOInterno.add(processo);
                    }
                }
                for (Processo processo : voltandoDoIOInterno) {
                    filaBloqueados.remove(processo);
                    processo.liberarBloqueio();
                    filaProntos.add(processo); // volta para o FIM da fila
                }

                // Incrementa a espera de todos os outros processos prontos
                for (Processo processo : filaProntos) {
                    if (processo != processoEscolhido) {
                        processo.incrementarEspera(1);
                    }
                }

                // Executa 1 tick de CPU
                processoEscolhido.setCpuAcumulada(processoEscolhido.getCpuAcumulada() + 1);
                tempoAtual++;
                ticksExecutados++;

                // Verifica se disparou I/O após esse tick
                if (processoEscolhido.deveDispararIO()) {

                    // Atualiza tau com base nos ticks que executou nessa rodada
                    // (aprendizado: quanto tempo realmente usou antes do I/O)
                    processoEscolhido.atualizarTau(ticksExecutados);

                    // Registra o I/O e bloqueia o processo
                    processoEscolhido.dispararIO(tempoAtual);

                    // Sai da fila de prontos e vai para a fila de bloqueados
                    filaProntos.remove(processoEscolhido);
                    filaBloqueados.add(processoEscolhido);

                    // Interrompe o while interno pois o processo saiu da CPU
                    break;
                }

                // Verifica se o processo terminou
                if (processoEscolhido.getCpuAcumulada() >= processoEscolhido.getBurstTotal()) {

                    // Atualiza tau com o tempo que executou nessa última rodada
                    processoEscolhido.atualizarTau(ticksExecutados);

                    // Conclui o processo
                    processoEscolhido.concluir(tempoAtual);

                    // Remove da fila de prontos
                    filaProntos.remove(processoEscolhido);

                    processosConcluidos++;

                    // Interrompe o while interno pois o processo terminou
                    break;
                }

            } // fim do while interno (quantum esgotado ou processo saiu)

            // ---------------------------------------------------------
            // PASSO 7: Se o processo ainda está na fila de prontos e
            //          ainda está na posição 0 (ou seja, não foi removido
            //          por I/O ou conclusão), significa que o quantum
            //          acabou e ele deve ir para o FIM da fila.
            //          Isso é a PREEMPÇÃO do Round Robin.
            //
            //          Também atualizamos o tau com os ticks que executou
            //          nessa rodada (aprendizado para o próximo quantum).
            // ---------------------------------------------------------
            if (!filaProntos.isEmpty() && filaProntos.get(0) == processoEscolhido) {

                // Atualiza a média exponencial com os ticks executados nessa rodada
                processoEscolhido.atualizarTau(ticksExecutados);

                // Remove do início e coloca no fim da fila (preempção circular)
                filaProntos.remove(0);
                filaProntos.add(processoEscolhido);
            }

        } // fim do while principal

        exibirMetricas(processos, tempoAtual);
    }

    // ------------------------------------------------------------------ //
    // Método auxiliar: calcula o quantum como a MENOR tau (média           //
    // exponencial) entre os processos da fila de prontos.                  //
    //                                                                      //
    // tau representa a previsão do próximo surto de CPU do processo.       //
    // Usar o menor tau como quantum favorece processos curtos e evita      //
    // que processos longos segurem a CPU por muito tempo.                  //
    //                                                                      //
    // O resultado é convertido para int (arredondado para cima com ceil)   //
    // pois o tempo da simulação é em unidades inteiras.                    //
    // ------------------------------------------------------------------ //
    private int calcularQuantum(List<Processo> filaProntos) {

        // Começa com o tau do primeiro processo como referência
        double menorTau = filaProntos.get(0).getTau();

        // Percorre os demais processos da fila procurando o menor tau
        for (int i = 1; i < filaProntos.size(); i++) {
            double tauAtual = filaProntos.get(i).getTau();
            if (tauAtual < menorTau) {
                menorTau = tauAtual;
            }
        }

        // Garante que o quantum seja pelo menos 1 para não travar a simulação
        int quantum = (int) Math.ceil(menorTau);
        if (quantum < 1) {
            quantum = 1;
        }

        return quantum;
    }

    // ------------------------------------------------------------------ //
    // Método auxiliar: exibe as métricas finais da simulação               //
    // ------------------------------------------------------------------ //
    private void exibirMetricas(List<Processo> processos, int tempoTotal) {

        double somaEspera  = 0;
        double somaRetorno = 0;

        System.out.println("\n=== Round Robin com Quantum por Predição (α=0.5, τ0=10) ===");
        System.out.printf("%-5s %-10s %-12s %-10s%n", "PID", "Espera", "Turnaround", "Tau Final");

        for (Processo processo : processos) {
            somaEspera  += processo.getTempoEspera();
            somaRetorno += processo.getTempoRetorno();

            System.out.printf("%-5d %-10d %-12d %-10.2f%n",
                    processo.getPid(),
                    processo.getTempoEspera(),
                    processo.getTempoRetorno(),
                    processo.getTau()); // mostra o tau final para fins didáticos
        }

        int n = processos.size();

        System.out.printf("%nTempo de Espera Médio  : %.2f%n", somaEspera  / n);
        System.out.printf("Turnaround Médio       : %.2f%n",   somaRetorno / n);
        System.out.printf("Throughput             : %.4f processos/u.t.%n", (double) n / tempoTotal);
    }
}