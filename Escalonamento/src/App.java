import java.util.List;

public class App {
    public static void main(String[] args) throws Exception {
        String caminhoArquivo = "src/processo.txt";
        List<Processo> processos = LeitorArquivo.ler(caminhoArquivo);

        EscalonadorFCFS escalonadorFCFS = new EscalonadorFCFS();
     //   escalonadorFCFS.executar(processos);
        EscalonadorSJF escalonadorSJF = new EscalonadorSJF();
       // escalonadorSJF.executar(processos);
        EscalonadorRoundRobin escalonadorRoundRobin = new EscalonadorRoundRobin();
      //  escalonadorRoundRobin.executar(processos);
        
    }
}
