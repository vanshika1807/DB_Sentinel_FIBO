package com.dbSentinel.DB_Sentinel.services;

import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

@Component
public class IncidentNotifier {

    // Correct FIBO microservice endpoint for POST
    private final String FIBO_URL = "http://127.0.0.1:8000/generate";

    private final ObjectMapper mapper = new ObjectMapper();

    public void sendIncident(String logText) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(FIBO_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setDoOutput(true);

            // JSON body must match FastAPI's GenerateRequest model
            Map<String, String> body = new HashMap<>();
            body.put("prompt", logText); // <-- key must be "prompt"

            String json = mapper.writeValueAsString(body);
            byte[] out = json.getBytes(StandardCharsets.UTF_8);

            conn.setFixedLengthStreamingMode(out.length);
            conn.connect();

            try (OutputStream os = conn.getOutputStream()) {
                os.write(out);
            }

            int responseCode = conn.getResponseCode();
            System.out.println("FIBO Response Code: " + responseCode);

            // Read response body
            try (InputStream is = (responseCode < 400 ? conn.getInputStream() : conn.getErrorStream());
                 Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name())) {

                String response = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
                System.out.println("FIBO Response Body: " + response);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
