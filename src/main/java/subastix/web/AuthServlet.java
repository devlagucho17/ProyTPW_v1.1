package subastix.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import subastix.db.UserRepository;
import subastix.db.UserRepository.AuthenticatedUser;
import subastix.model.User;
import subastix.security.PasswordUtil;
import subastix.security.SessionUser;

@WebServlet("/api/auth/*")
public class AuthServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(AuthServlet.class.getName());
    private static final SecureRandom RANDOM = new SecureRandom();
    private final UserRepository users = new UserRepository();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(true);
        String csrfToken = (String) session.getAttribute(SessionUser.CSRF_ATTRIBUTE);
        if (csrfToken == null) {
            byte[] token = new byte[32];
            RANDOM.nextBytes(token);
            csrfToken = Base64.getUrlEncoder().withoutPadding().encodeToString(token);
            session.setAttribute(SessionUser.CSRF_ATTRIBUTE, csrfToken);
        }
        User user = SessionUser.get(session);
        Json.write(response, HttpServletResponse.SC_OK,
                Map.of("user", user == null ? Map.of() : user, "csrfToken", csrfToken));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getPathInfo();
        try {
            AuthRequest body = Json.MAPPER.readValue(request.getInputStream(), AuthRequest.class);
            if (body == null) {
                Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "Completa los datos de acceso.");
                return;
            }
            if ("/register".equals(action)) {
                register(request, response, body);
            } else if ("/login".equals(action)) {
                login(request, response, body);
            } else if ("/logout".equals(action)) {
                logout(request, response);
            } else {
                Json.error(response, HttpServletResponse.SC_NOT_FOUND, "Acción de autenticación no encontrada.");
            }
        } catch (JsonProcessingException ex) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "El formulario no tiene un formato válido.");
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error de base de datos en autenticación.", ex);
            Json.error(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "No se pudo completar la operación. Inténtalo más tarde.");
        }
    }

    private void register(HttpServletRequest request, HttpServletResponse response, AuthRequest body)
            throws IOException, SQLException {
        String name = trim(body.displayName());
        String email = trim(body.email()).toLowerCase(Locale.ROOT);
        String role = trim(body.role()).toUpperCase(Locale.ROOT);
        char[] password = body.password() == null ? new char[0] : body.password().toCharArray();
        if (name.length() < 2 || name.length() > 80 || !validEmail(email)
                || !("COMPRADOR".equals(role) || "VENDEDOR".equals(role))
                || password.length < 12 || password.length > 128) {
            java.util.Arrays.fill(password, '\0');
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Revisa el nombre, el correo y la contraseña (12 a 128 caracteres).");
            return;
        }
        try {
            User user = users.create(name, email, role, PasswordUtil.hash(password));
            authenticate(request, user);
            writeSession(response, request.getSession(false), user);
        } catch (SQLException ex) {
            if ("23000".equals(ex.getSQLState())) {
                Json.error(response, HttpServletResponse.SC_CONFLICT, "Ese correo ya está registrado.");
                return;
            }
            throw ex;
        } finally {
            java.util.Arrays.fill(password, '\0');
        }
    }

    private void login(HttpServletRequest request, HttpServletResponse response, AuthRequest body)
            throws IOException, SQLException {
        String email = trim(body.email()).toLowerCase(Locale.ROOT);
        char[] password = body.password() == null ? new char[0] : body.password().toCharArray();
        try {
            Optional<AuthenticatedUser> found = users.findByEmail(email);
            if (found.isEmpty() || !PasswordUtil.verify(password, found.get().passwordHash())) {
                Json.error(response, HttpServletResponse.SC_UNAUTHORIZED, "Correo o contraseña incorrectos.");
                return;
            }
            User user = found.get().user();
            authenticate(request, user);
            writeSession(response, request.getSession(false), user);
        } finally {
            java.util.Arrays.fill(password, '\0');
        }
    }

    private void logout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        Json.write(response, HttpServletResponse.SC_OK, Map.of("ok", true));
    }

    private void authenticate(HttpServletRequest request, User user) {
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) oldSession.invalidate();
        HttpSession session = request.getSession(true);
        session.setAttribute(SessionUser.USER_ATTRIBUTE, user);
        byte[] token = new byte[32];
        RANDOM.nextBytes(token);
        session.setAttribute(SessionUser.CSRF_ATTRIBUTE,
                Base64.getUrlEncoder().withoutPadding().encodeToString(token));
    }

    private void writeSession(HttpServletResponse response, HttpSession session, User user) throws IOException {
        Json.write(response, HttpServletResponse.SC_OK, Map.of(
                "user", user,
                "csrfToken", session.getAttribute(SessionUser.CSRF_ATTRIBUTE)));
    }

    private String trim(String value) {
        return value == null ? "" : value.strip();
    }

    private boolean validEmail(String email) {
        return email.length() <= 100 && email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }

    private record AuthRequest(String displayName, String email, String password, String role) {
    }
}
