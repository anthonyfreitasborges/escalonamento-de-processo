import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LeitorArquivo {

    public static List<Processo> ler(String caminho) throws IOException {
        List<Processo> processos = new ArrayList<>();

        BufferedReader br = new BufferedReader(new FileReader(caminho));
        String linha;

        while ((linha = br.readLine()) != null) {
            linha = linha.trim();

            // ignora linhas vazias ou comentários
            if (linha.isEmpty() || linha.startsWith("#")) continue;

            String[] partes = linha.split(";");

            int pid          = Integer.parseInt(partes[0].trim());
            int chegada      = Integer.parseInt(partes[1].trim());
            int burstTotal   = Integer.parseInt(partes[2].trim());
            int prioridade   = Integer.parseInt(partes[3].trim());

            List<Integer> instantesIO = new ArrayList<>();

            if (partes.length == 5) {
                String[] ios = partes[4].trim().split(",");
                for (String io : ios) {
                    instantesIO.add(Integer.parseInt(io.trim()));
                }
            }

            processos.add(new Processo(pid, chegada, burstTotal, prioridade, instantesIO));
        }

        br.close();
        return processos;
    }
}