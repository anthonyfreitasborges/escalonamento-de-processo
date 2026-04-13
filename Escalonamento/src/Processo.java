import java.util.List;
import java.util.ArrayList;

public class Processo {

    // --- Dados estáticos (lidos do arquivo) ---
    private final int pid;
    private final int tempoChegada;
    private final int burstTotal;
    private final int prioridade;
    private final List<Integer> instantesIO; // momentos de CPU acumulado que disparam I/O

    // --- Estado dinâmico da simulação ---
    private int cpuAcumulada;        // quanto de CPU já consumiu ao todo
    private int tempoEspera;         // tempo total esperando na fila de prontos
    private int tempoRetorno;        // turnaround = conclusão - chegada
    private int tempoBloqueioFim;    // instante em que sai do bloqueio de I/O (-1 = não bloqueado)
    private boolean concluido;

    // --- Média exponencial (Round-Robin com Quantum por Predição) ---
    private static final double ALPHA = 0.5;
    private static final double TAU_INICIAL = 10.0;
    private double tau;              // previsão atual do próximo surto

    // --- Controle de I/O já disparados ---
    private final List<Integer> ioDisparados; // quais instantes já foram usados

    public Processo(int pid, int tempoChegada, int burstTotal, int prioridade, List<Integer> instantesIO) {
        this.pid = pid;
        this.tempoChegada = tempoChegada;
        this.burstTotal = burstTotal;
        this.prioridade = prioridade;
        this.instantesIO = new ArrayList<>(instantesIO);

        this.cpuAcumulada = 0;
        this.tempoEspera = 0;
        this.tempoRetorno = 0;
        this.tempoBloqueioFim = -1;
        this.concluido = false;
        this.tau = TAU_INICIAL;
        this.ioDisparados = new ArrayList<>();
    }

    // ------------------------------------------------------------------ //
    //  Lógica de I/O                                                       //
    // ------------------------------------------------------------------ //

    /**
     * Verifica se o processo deve disparar I/O agora.
     * Um instante de I/O é disparado quando cpuAcumulada atinge aquele valor
     * e ainda não foi disparado antes.
     */
    public boolean deveDispararIO() {
        for (int instante : instantesIO) {
            if (instante == cpuAcumulada && !ioDisparados.contains(instante)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Registra o disparo de I/O no instante atual de simulação.
     * Bloqueia o processo por 5 unidades de tempo.
     */
    public void dispararIO(int tempoAtual) {
        for (int instante : instantesIO) {
            if (instante == cpuAcumulada && !ioDisparados.contains(instante)) {
                ioDisparados.add(instante);
                tempoBloqueioFim = tempoAtual + 5;
                return;
            }
        }
    }

    /** Retorna true se o processo ainda está bloqueado no instante dado. */
    public boolean estaBloqueado(int tempoAtual) {
        return tempoBloqueioFim != -1 && tempoAtual < tempoBloqueioFim;
    }

    /** Libera o bloqueio (chamado quando o processo volta para a fila de prontos). */
    public void liberarBloqueio() {
        tempoBloqueioFim = -1;
    }

    // ------------------------------------------------------------------ //
    //  Média exponencial                                                    //
    // ------------------------------------------------------------------ //

    /**
     * Atualiza tau após um surto de CPU de duração real "duracaoSurto".
     * Deve ser chamado ao fim de cada fatia executada.
     */
    public void atualizarTau(double duracaoSurto) {
        tau = ALPHA * duracaoSurto + (1 - ALPHA) * tau;
    }

    // ------------------------------------------------------------------ //
    //  Tempo restante                                                       //
    // ------------------------------------------------------------------ //

    public int getTempoRestante() {
        return burstTotal - cpuAcumulada;
    }

    public boolean isConcluido() {
        return concluido;
    }

    public void concluir(int tempoAtual) {
        this.concluido = true;
        this.tempoRetorno = tempoAtual - tempoChegada;
    }

    // ------------------------------------------------------------------ //
    //  Getters e Setters                                                    //
    // ------------------------------------------------------------------ //

    public int getPid()                  { return pid; }
    public int getTempoChegada()         { return tempoChegada; }
    public int getBurstTotal()           { return burstTotal; }
    public int getPrioridade()           { return prioridade; }
    public List<Integer> getInstantesIO(){ return instantesIO; }

    public int getCpuAcumulada()         { return cpuAcumulada; }
    public void setCpuAcumulada(int v)   { this.cpuAcumulada = v; }

    public int getTempoEspera()          { return tempoEspera; }
    public void setTempoEspera(int v)    { this.tempoEspera = v; }
    public void incrementarEspera(int v) { this.tempoEspera += v; }

    public int getTempoRetorno()         { return tempoRetorno; }

    public int getTempoBloqueioFim()     { return tempoBloqueioFim; }

    public double getTau()               { return tau; }

    @Override
    public String toString() {
        return String.format("Processo{pid=%d, chegada=%d, burst=%d, prioridade=%d, cpu=%d, tau=%.1f}",
                pid, tempoChegada, burstTotal, prioridade, cpuAcumulada, tau);
    }
}