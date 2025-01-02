package com.mongodb.starter.student;

import java.util.Base64;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class UserService {

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Value("${base.url}")
    private String baseUrl;

    public String getUserRoleById(String userId, String token) {
        String url = baseUrl + userId;
        try {
            JsonNode userNode = webClientBuilder.build()
                    .get()
                    .uri(url)
                    .headers(headers -> headers.set("Authorization", token))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(); // Sincrónico, evita en entornos reactivos

            if (userNode != null && userNode.has("role")) {
                return userNode.get("role").asText();
            } else {
                throw new RuntimeException("Role not found for user ID: " + userId);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving user role: " + e.getMessage(), e);
        }
    }

    public String extractUserId(String token) {
        try {
            // Eliminar cualquier espacio extra
            token = token.trim();

            // Verificar si el token tiene el prefijo "Bearer"
            if (token.startsWith("Bearer ")) {
                token = token.substring(7); // Eliminar el prefijo "Bearer "
            } else {
                throw new RuntimeException("Token no contiene el prefijo 'Bearer'");
            }

            // Verificar que el token tenga el formato adecuado
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new RuntimeException("Token malformado: El token debe tener 3 partes separadas por puntos");
            }

            // Decodificar el payload (segunda parte del token)
            String payload = new String(Base64.getDecoder().decode(parts[1]));
            System.out.println("Payload del token: " + payload); // Verifica el contenido del payload
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> payloadMap = mapper.readValue(payload, Map.class);
            System.err.println(payloadMap.get("sub").toString());
            return payloadMap.get("sub").toString();

        } catch (Exception e) {
            System.err.println("Error al procesar el token: " + e.getMessage());
            throw new RuntimeException("Token inválido o malformado: " + e.getMessage());
        }
    }

}
