package subastix.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

public final class Json {
    public static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

    private Json() {
    }

    public static void write(HttpServletResponse response, int status, Object body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        MAPPER.writeValue(response.getOutputStream(), body);
    }

    public static void error(HttpServletResponse response, int status, String message) throws IOException {
        write(response, status, Map.of("error", message));
    }
}
