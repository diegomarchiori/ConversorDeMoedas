import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServidorConversor {

    private static final Gson gson = new Gson();

    public static void iniciarServidor(int porta) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(porta), 0);

        server.createContext("/", new IndexHandler());

        server.createContext("/api/moedas", new MoedasHandler());

        server.createContext("/api/converter", new ConverterHandler());

        server.setExecutor(null);
        server.start();

        System.out.println("Servidor Java iniciado com sucesso!");
        System.out.println("Acesse no navegador: http://localhost:" + porta);
        System.out.println("====================================================");
    }

    static class IndexHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            InputStream is = getClass().getResourceAsStream("/index.html");
            if (is == null) {
                String resposta = "Arquivo index.html nao encontrado em src/main/resources.";
                exchange.sendResponseHeaders(404, resposta.length());
                OutputStream os = exchange.getResponseBody();
                os.write(resposta.getBytes());
                os.close();
                return;
            }

            byte[] bytes = is.readAllBytes();
            is.close();

            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    static class MoedasHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            configurarCORS(exchange);

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            List<Moeda> moedas = Moeda.listarMoedas();
            String json = gson.toJson(moedas);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    static class ConverterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            configurarCORS(exchange);

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = extrairParametros(query);

            String from = params.get("from");
            String to = params.get("to");
            String amountStr = params.get("amount");

            if (from == null || to == null || amountStr == null) {
                enviarRespostaJson(exchange, 400, "{\"erro\":\"Parametros 'from', 'to' e 'amount' sao obrigatorios.\"}");
                return;
            }

            try {
                double amount = Double.parseDouble(amountStr);
                Conversao resultado = converterNoBackend(from, to, amount);

                String jsonResposta = gson.toJson(resultado);
                enviarRespostaJson(exchange, 200, jsonResposta);
            } catch (Exception e) {
                e.printStackTrace();
                enviarRespostaJson(exchange, 500, "{\"erro\":\"" + e.getMessage() + "\"}");
            }
        }

        private Conversao converterNoBackend(String from, String to, double amount) throws IOException {
            String url = "https://api.fxratesapi.com/convert?from=" + from + "&to=" + to + "&amount=" + amount + "&format=json";

            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                throw new IOException("Erro da API externa: HTTP " + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
            conn.disconnect();

            return gson.fromJson(sb.toString(), Conversao.class);
        }

        private Map<String, String> extrairParametros(String query) {
            Map<String, String> params = new HashMap<>();
            if (query == null || query.isBlank()) return params;

            for (String param : query.split("&")) {
                String[] par = param.split("=");
                if (par.length == 2) {
                    params.put(URLDecoder.decode(par[0], StandardCharsets.UTF_8),
                               URLDecoder.decode(par[1], StandardCharsets.UTF_8));
                }
            }
            return params;
        }

        private void enviarRespostaJson(HttpExchange exchange, int status, String json) throws IOException {
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(status, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    private static void configurarCORS(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }
}
