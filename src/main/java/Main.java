import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try {
            int porta = 8080;
            ServidorConversor.iniciarServidor(porta);
        } catch (IOException e) {
            System.err.println("Erro ao iniciar o servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
