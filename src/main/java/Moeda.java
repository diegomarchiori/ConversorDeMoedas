import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Moeda {
    private String code;
    private String name;

    public Moeda(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Moeda{" +
                "code='" + code + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    public static List<Moeda> listarMoedas() {
        List<Moeda> listagemMoedas = new ArrayList<>();
        try {
            String url = "https://api.fxratesapi.com/currencies";
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                System.out.println("Erro " + conn.getResponseCode() + " ao obter dados da URL " + url);
                return null;
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                output.append(line);
            }
            br.close();
            conn.disconnect();

            Gson gson = new Gson();
            Type tipoMapa = new TypeToken<Map<String, Moeda>>() {}.getType();
            Map<String, Moeda> mapaMoedas = gson.fromJson(output.toString(), tipoMapa);

            listagemMoedas.addAll(mapaMoedas.values());

        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return listagemMoedas;
    }
}
