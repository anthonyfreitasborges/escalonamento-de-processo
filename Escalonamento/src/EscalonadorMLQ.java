import java.util.List;
import java.util.ArrayList;

public class EscalonadorMLQ {

    // Quantum fixo para a Fila 1 (Alta Prioridade) que usa Round-Robin.
    // Diferente do Round Robin com predição, aqui o quantum não muda.
    private static final int QUANTUM_FILA_ALTA_PRIORIDADE = 4;

    public void executar(List<Processo> processos) {

        int tempoAtual = 0;

        // Conta quantos processos já terminaram
        int processosConcluidos = 0;

        // Total de processos que temos para executar
        int totalProcessos = processos.size();

        // ---------------------------------------------------------
        // FILA 1 - Alta Prioridade: escalonada por Round-Robin
        // Processos com prioridade >= 2 entram aqui.
        // A fila é circular: processo que esgota o quantum vai pro fim.
        // ---------------------------------------------------------
        List<Processo> filaAltaPrioridade = new ArrayList<Processo>();

        // ---------------------------------------------------------
        // FILA 2 - Baixa Prioridade: escalonada por FCFS
        // Processos com prioridade < 2 entram aqui.
        // Só executa quando a Fila 1 estiver completamente vazia.
        // ---------------------------------------------------------
        List<Processo> filaBaixaPrioridade = new ArrayList<Processo>();

        // Lista de processos bloqueados fazendo I/O (de qualquer fila)
        List<Processo> filaBloqueados = new ArrayList<Processo>();

        // Controla quantos ticks o processo atual já executou nessa rodada
        // (usado para saber quando o quantum do Round-Robin acabou)
        int ticksDaRodadaAtual = 0;

        // Guarda qual processo está executando agora
        // null significa que nenhum processo foi escolhido ainda
        Processo processoExecutando = null;

        // Fica em loop até todos os processos terminarem
        while (processosConcluidos < totalProcessos) {

            // ---------------------------------------------------------
            // PASSO 1: Verifica se algum processo bloqueado terminou
            //          o I/O e o devolve para a fila correta de origem
            // ---------------------------------------------------------
            liberarProcessosBloqueados(filaBloqueados, filaAltaPrioridade, filaBaixaPrioridade, tempoAtual);

            // ---------------------------------------------------------
            // PASSO 2: Verifica quais novos processos chegaram e os
            //          distribui na fila correta conforme a prioridade
            // ---------------------------------------------------------
            distribuirProcessosNasFilas(processos, filaAltaPrioridade, filaBaixaPrioridade, filaBloqueados, tempoAtual);

            // ---------------------------------------------------------
            // PASSO 3: Escolhe qual processo vai executar nesse tick.
            //
            //          REGRA DO MLQ:
            //          A Fila 1 (alta prioridade) sempre tem preferência.
            //          Se a Fila 1 tiver algo, pegamos da Fila 1.
            //          Só usamos a Fila 2 se a Fila 1 estiver vazia.
            //
            //          Se o processo que estava executando ainda é o
            //          primeiro da fila correta, ele continua executando.
            //          Se não, escolhemos o novo primeiro da fila.
            // ---------------------------------------------------------
            Processo processoEscolhido = null;

            if (!filaAltaPrioridade.isEmpty()) {

                // Verifica se o processo que estava rodando ainda é o primeiro da fila alta.
                // Se não for (foi preemptado ou terminou), começa uma nova rodada.
                if (processoExecutando != filaAltaPrioridade.get(0)) {
                    processoEscolhido = filaAltaPrioridade.get(0);
                    ticksDaRodadaAtual = 0; // reinicia a contagem do quantum
                } else {
                    processoEscolhido = processoExecutando; // continua o mesmo processo
                }

            } else if (!filaBaixaPrioridade.isEmpty()) {

                // Fila 1 vazia: executa da Fila 2 (FCFS - primeiro da fila)
                processoEscolhido = filaBaixaPrioridade.get(0);
                ticksDaRodadaAtual = 0; // FCFS não usa quantum, mas zeramos por segurança

            } else {

                // Nenhuma fila tem processo pronto: CPU fica ociosa
                processoExecutando = null;
                tempoAtual++;
                continue; // volta pro início do while
            }

            // Atualiza o processo que está executando agora
            processoExecutando = processoEscolhido;

            // ---------------------------------------------------------
            // PASSO 4: Incrementa a espera de todos os outros processos
            //          que estão prontos mas não foram escolhidos.
            //          Isso vale para processos nas duas filas.
            // ---------------------------------------------------------
            incrementarEsperaDosOutros(processoEscolhido, filaAltaPrioridade, filaBaixaPrioridade);

            // ---------------------------------------------------------
            // PASSO 5: Executa 1 tick de CPU para o processo escolhido
            // ---------------------------------------------------------
            processoEscolhido.setCpuAcumulada(processoEscolhido.getCpuAcumulada() + 1);
            tempoAtual++;
            ticksDaRodadaAtual++;

            // ---------------------------------------------------------
            // PASSO 6: Verifica se o processo disparou I/O após esse tick
            // ---------------------------------------------------------
            if (processoEscolhido.deveDispararIO()) {

                // Bloqueia o processo por 5 unidades de tempo
                processoEscolhido.dispararIO(tempoAtual);

                // Remove da fila em que estava e manda para bloqueados
                filaAltaPrioridade.remove(processoEscolhido);
                filaBaixaPrioridade.remove(processoEscolhido);
                filaBloqueados.add(processoEscolhido);

                // Reseta o controle da rodada pois esse processo saiu
                processoExecutando = null;
                ticksDaRodadaAtual = 0;

                continue; // volta pro início do while
            }

            // ---------------------------------------------------------
            // PASSO 7: Verifica se o processo terminou
            // ---------------------------------------------------------
            if (processoEscolhido.getCpuAcumulada() >= processoEscolhido.getBurstTotal()) {

                // Marca o processo como concluído
                processoEscolhido.concluir(tempoAtual);

                // Remove da fila em que estava
                filaAltaPrioridade.remove(processoEscolhido);
                filaBaixaPrioridade.remove(processoEscolhido);

                processosConcluidos++;

                // Reseta o controle da rodada pois esse processo saiu
                processoExecutando = null;
                ticksDaRodadaAtual = 0;

                continue; // volta pro início do while
            }

            // ---------------------------------------------------------
            // PASSO 8: Verifica preempção por quantum (só para Fila 1)
            //
            //          Se o processo está na Fila 1 e já executou
            //          QUANTUM ticks seguidos, ele sofre preempção:
            //          vai para o FIM da fila (comportamento circular).
            //
            //          Processos da Fila 2 (FCFS) não sofrem preempção
            //          por quantum, apenas por chegada de processo
            //          de alta prioridade (tratado no Passo 3).
            // ---------------------------------------------------------
            if (filaAltaPrioridade.contains(processoEscolhido)
                    && ticksDaRodadaAtual >= QUANTUM_FILA_ALTA_PRIORIDADE) {

                // Remove do início e coloca no fim da fila (preempção circular)
                filaAltaPrioridade.remove(processoEscolhido);
                filaAltaPrioridade.add(processoEscolhido);

                // Reseta a contagem de ticks para a próxima rodada
                processoExecutando = null;
                ticksDaRodadaAtual = 0;
            }

        } // fim do while principal

        exibirMetricas(processos, tempoAtual);
    }

    // ------------------------------------------------------------------ //
    // Método: distribui os processos que chegaram nas filas corretas       //
    //                                                                      //
    // Separação por prioridade:                                            //
    //   prioridade >= 2  →  Fila 1 (Alta Prioridade) - Round Robin        //
    //   prioridade <  2  →  Fila 2 (Baixa Prioridade) - FCFS              //
    // ------------------------------------------------------------------ //
    private void distribuirProcessosNasFilas(
            List<Processo> todosProcessos,
            List<Processo> filaAltaPrioridade,
            List<Processo> filaBaixaPrioridade,
            List<Processo> filaBloqueados,
            int tempoAtual) {

        for (Processo processo : todosProcessos) {

            boolean jaChegou         = processo.getTempoChegada() <= tempoAtual;
            boolean naoTerminou      = !processo.isConcluido();
            boolean naoEstaEmAlta    = !filaAltaPrioridade.contains(processo);
            boolean naoEstaEmBaixa   = !filaBaixaPrioridade.contains(processo);
            boolean naoEstaBloqueado = !filaBloqueados.contains(processo);

            if (jaChegou && naoTerminou && naoEstaEmAlta && naoEstaEmBaixa && naoEstaBloqueado) {

                // Coloca na fila correta conforme a prioridade
                if (processo.getPrioridade() >= 2) {
                    filaAltaPrioridade.add(processo);
                } else {
                    filaBaixaPrioridade.add(processo);
                }
            }
        }
    }

    // ------------------------------------------------------------------ //
    // Método: libera processos que terminaram o I/O e os devolve          //
    //         para a fila de origem correta (alta ou baixa prioridade)    //
    // ------------------------------------------------------------------ //
    private void liberarProcessosBloqueados(
            List<Processo> filaBloqueados,
            List<Processo> filaAltaPrioridade,
            List<Processo> filaBaixaPrioridade,
            int tempoAtual) {

        // Lista auxiliar para não modificar filaBloqueados enquanto percorremos ela
        List<Processo> voltandoDoIO = new ArrayList<Processo>();

        for (Processo processo : filaBloqueados) {
            if (!processo.estaBloqueado(tempoAtual)) {
                voltandoDoIO.add(processo);
            }
        }

        for (Processo processo : voltandoDoIO) {
            filaBloqueados.remove(processo);
            processo.liberarBloqueio();

            // Devolve para a fila de origem conforme a prioridade do processo
            if (processo.getPrioridade() >= 2) {
                filaAltaPrioridade.add(processo); // volta para o fim da fila alta
            } else {
                filaBaixaPrioridade.add(processo); // volta para o fim da fila baixa
            }
        }
    }

    // ------------------------------------------------------------------ //
    // Método: incrementa a espera de todos os processos prontos,          //
    //         exceto o processo que está executando agora                 //
    // ------------------------------------------------------------------ //
    private void incrementarEsperaDosOutros(
            Processo processoExecutando,
            List<Processo> filaAltaPrioridade,
            List<Processo> filaBaixaPrioridade) {

        for (Processo processo : filaAltaPrioridade) {
            if (processo != processoExecutando) {
                processo.incrementarEspera(1);
            }
        }

        for (Processo processo : filaBaixaPrioridade) {
            if (processo != processoExecutando) {
                processo.incrementarEspera(1);
            }
        }
    }

    // ------------------------------------------------------------------ //
    // Método: exibe as métricas finais da simulação                       //
    // ------------------------------------------------------------------ //
    private void exibirMetricas(List<Processo> processos, int tempoTotal) {

        double somaEspera  = 0;
        double somaRetorno = 0;

        System.out.println("\n=== Multilevel Queue (MLQ) ===");
        System.out.println("Fila 1 (Alta Prioridade - Round Robin, quantum=" + QUANTUM_FILA_ALTA_PRIORIDADE + "): prioridade >= 2");
        System.out.println("Fila 2 (Baixa Prioridade - FCFS): prioridade < 2");
        System.out.println();
        System.out.printf("%-5s %-10s %-12s %-10s %-6s%n", "PID", "Espera", "Turnaround", "Prioridade", "Fila");

        for (Processo processo : processos) {
            somaEspera  += processo.getTempoEspera();
            somaRetorno += processo.getTempoRetorno();

            String nomeDaFila = processo.getPrioridade() >= 2 ? "Alta" : "Baixa";

            System.out.printf("%-5d %-10d %-12d %-10d %-6s%n",
                    processo.getPid(),
                    processo.getTempoEspera(),
                    processo.getTempoRetorno(),
                    processo.getPrioridade(),
                    nomeDaFila);
        }

        int n = processos.size();

        System.out.printf("%nTempo de Espera Médio  : %.2f%n", somaEspera  / n);
        System.out.printf("Turnaround Médio       : %.2f%n",   somaRetorno / n);
        System.out.printf("Throughput             : %.4f processos/u.t.%n", (double) n / tempoTotal);
    }
}