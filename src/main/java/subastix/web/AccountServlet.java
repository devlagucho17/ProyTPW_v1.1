package subastix.web;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import subastix.db.AccountRepository;
import subastix.model.User;
import subastix.security.SessionUser;

@WebServlet("/api/account")
public class AccountServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(AccountServlet.class.getName());
    private final AccountRepository account = new AccountRepository();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = SessionUser.get(request.getSession(false));
        if (user == null) {
            Json.error(response, HttpServletResponse.SC_UNAUTHORIZED, "Inicia sesión para ver tu cuenta.");
            return;
        }
        try {
            Json.write(response, HttpServletResponse.SC_OK, account.findBids(user.id()));
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error de base de datos al consultar la cuenta.", ex);
            Json.error(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "No se pudo consultar la actividad de tu cuenta.");
        }
    }
}
