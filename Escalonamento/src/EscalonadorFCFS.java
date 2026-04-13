import java.util.List;
import java.util.Comparator;


public class EscalonadorFCFS {

    public void executar(List<Processo> processos) {
        // ordena por tempo de chegada
       processos.sort(Comparator.comparingInt(Processo::getTempoChegada));

        int tempoAtual = 0;
    
        for (Processo p : processos) {

            // CPU fica ociosa se o processo ainda não chegou
            if (tempoAtual < p.getTempoChegada()) {
                tempoAtual = p.getTempoChegada();
            }

            // tempo de espera = quanto ficou na fila antes de executar
            p.incrementarEspera(tempoAtual - p.getTempoChegada());

            // simula a execução tick a tick para tratar I/O
            while (!p.isConcluido()) {

                // verifica se deve disparar I/O antes de executar
                if (p.deveDispararIO()) {
                    p.dispararIO(tempoAtual);
                    tempoAtual = p.getTempoBloqueioFim(); // avança o tempo até sair do bloqueio
                    p.liberarBloqueio();
                }

                // executa 1 unidade de CPU
                p.setCpuAcumulada(p.getCpuAcumulada() + 1);
                tempoAtual++;

                // verifica se terminou
                if (p.getCpuAcumulada() >= p.getBurstTotal()) {
                    p.concluir(tempoAtual);
                }
            }
        }

        exibirMetricas(processos, tempoAtual);
    }

    private void exibirMetricas(List<Processo> processos, int tempoTotal) {
        double somaEspera    = 0;
        double somaRetorno   = 0;

        System.out.println("\n=== FCFS ===");
        System.out.printf("%-5s %-10s %-12s%n", "PID", "Espera", "Turnaround");

        for (Processo p : processos) {
            somaEspera  += p.getTempoEspera();
            somaRetorno += p.getTempoRetorno();
            System.out.printf("%-5d %-10d %-12d%n",
                    p.getPid(), p.getTempoEspera(), p.getTempoRetorno());
        }

        int n = processos.size();
        System.out.printf("%nTempo de Espera Médio  : %.2f%n", somaEspera  / n);
        System.out.printf("Turnaround Médio       : %.2f%n", somaRetorno / n);
        System.out.printf("Throughput             : %.4f processos/u.t.%n", (double) n / tempoTotal);
    }
}