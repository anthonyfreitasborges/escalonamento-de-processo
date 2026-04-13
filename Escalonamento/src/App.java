import java.util.List;

public class App {
    public static void main(String[] args) throws Exception {
        String caminhoArquivo = "src/processo.txt";
        List<Processo> processos = LeitorArquivo.ler(caminhoArquivo);

        EscalonadorFCFS escalonador = new EscalonadorFCFS();
        escalonador.executar(processos);
    }
}
