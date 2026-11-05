package subastix.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.DateTimeException;
import java.sql.SQLException;
import java.time.Instant;
import java.nio.file.Files;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import subastix.db.AuctionRepository;
import subastix.db.AuctionRepository.BidRejectedException;
import subastix.db.CategoryRepository;
import subastix.model.Auction;
import subastix.model.User;
import subastix.security.SessionUser;

@WebServlet("/api/auctions/*")
@MultipartConfig(maxFileSize = 10 * 1024 * 1024, maxRequestSize = 11 * 1024 * 1024)
public class AuctionServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(AuctionServlet.class.getName());
    private final AuctionRepository auctions = new AuctionRepository();
    private final CategoryRepository categories = new CategoryRepository();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String path = request.getPathInfo();
            if (path == null || "/".equals(path)) {
                List<Auction> results = auctions.findAll();
                Json.write(response, HttpServletResponse.SC_OK, results);
                return;
            }
            if ("/categories".equals(path)) {
                Json.write(response, HttpServletResponse.SC_OK, categories.findAll());
                return;
            }
            String[] parts = path.split("/");
            if (parts.length == 2 && isPositiveId(parts[1])) {
                long id = Long.parseLong(parts[1]);
                Auction auction = auctions.findById(id).orElse(null);
                if (auction == null) {
                    Json.error(response, HttpServletResponse.SC_NOT_FOUND, "La subasta no existe.");
                    return;
                }
                Json.write(response, HttpServletResponse.SC_OK, Map.of(
                        "auction", auction,
                        "bids", auctions.findRecentBids(id, 20)));
                return;
            }
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "La ruta solicitada no existe.");
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error de base de datos al consultar subastas.", ex);
            Json.error(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "No se pudo consultar la base de datos. Verifica MySQL y la configuración.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = SessionUser.get(request.getSession(false));
        if (user == null) {
            Json.error(response, HttpServletResponse.SC_UNAUTHORIZED, "Inicia sesión para continuar.");
            return;
        }
        String path = request.getPathInfo();
        try {
            String[] parts = path == null ? new String[0] : path.split("/");
            if (parts.length == 3 && isPositiveId(parts[1]) && "bids".equals(parts[2])) {
                placeBid(request, response, user, Long.parseLong(parts[1]));
                return;
            }
            if (path == null || "/".equals(path)) {
                if (!"VENDEDOR".equals(user.role()) && !"ADMIN".equals(user.role())) {
                    Json.error(response, HttpServletResponse.SC_FORBIDDEN,
                            "Para publicar una subasta necesitas una cuenta de vendedor.");
                    return;
                }
                createAuction(request, response, user);
                return;
            }
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "La ruta solicitada no existe.");
        } catch (JsonProcessingException ex) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "El formulario no tiene un formato válido.");
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error de base de datos al guardar una subasta o puja.", ex);
            Json.error(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "No se pudo guardar la operación. Verifica MySQL e inténtalo de nuevo.");
        } catch (BidRejectedException ex) {
            Json.error(response, ex.getStatus(), ex.getMessage());
        }
    }

    private void placeBid(HttpServletRequest request, HttpServletResponse response, User user, long auctionId)
            throws IOException, SQLException, BidRejectedException {
        BidRequest body = Json.MAPPER.readValue(request.getInputStream(), BidRequest.class);
        if (body == null || body.amount() == null || body.amount().scale() > 2
                || body.amount().precision() > 10 || body.amount().signum() <= 0) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "Indica una puja válida.");
            return;
        }
        Auction auction = auctions.placeBid(auctionId, user.id(), body.amount());
        Json.write(response, HttpServletResponse.SC_OK, Map.of("auction", auction));
    }

    private void createAuction(HttpServletRequest request, HttpServletResponse response, User user)
            throws IOException, SQLException, ServletException {
        if (request.getContentType() == null
                || !request.getContentType().toLowerCase(Locale.ROOT).startsWith("multipart/form-data")) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "El formulario de publicación no es válido.");
            return;
        }
        String title = trim(request.getParameter("title"));
        String description = trim(request.getParameter("description"));
        String category = trim(request.getParameter("category"));
        String condition = trim(request.getParameter("condition"));
        BigDecimal startingPrice;
        BigDecimal increment;
        Instant startsAt;
        Instant endsAt;
        try {
            startingPrice = new BigDecimal(request.getParameter("startingPrice"));
            increment = new BigDecimal(request.getParameter("increment"));
            startsAt = Instant.parse(request.getParameter("startsAt"));
            endsAt = Instant.parse(request.getParameter("endsAt"));
        } catch (NumberFormatException | DateTimeException | NullPointerException ex) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "Revisa los precios y fechas de la subasta.");
            return;
        }
        if (title.isBlank() || title.length() > 80 || description.isBlank() || description.length() > 2000
                || category.isBlank() || category.length() > 50 || condition.isBlank() || condition.length() > 40
                || !Set.of("Nuevo", "Usado · Excelente", "Usado · Bueno").contains(condition)
                || !validPrice(startingPrice) || !validPrice(increment)) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "Revisa los datos obligatorios de la subasta.");
            return;
        }
        Instant now = Instant.now();
        if (!startsAt.isAfter(now) || !endsAt.isAfter(startsAt)) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "El inicio debe ser futuro y la finalización debe ser posterior al inicio.");
            return;
        }
        ImageStorage.StoredImage image;
        try {
            image = ImageStorage.store(request.getPart("image"));
        } catch (ImageStorage.InvalidImageException ex) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            return;
        }
        try {
            Auction created = auctions.create(user.id(), title, description, category, condition,
                    startingPrice, increment, startsAt, endsAt, image == null ? null : image.url());
            Json.write(response, HttpServletResponse.SC_CREATED, Map.of("auction", created));
        } catch (SQLException ex) {
            if (image != null) {
                try {
                    Files.deleteIfExists(image.path());
                } catch (IOException cleanupError) {
                    LOGGER.log(Level.WARNING, "No se pudo limpiar una imagen de subasta no guardada.", cleanupError);
                    ex.addSuppressed(cleanupError);
                }
            }
            throw ex;
        }
    }

    private boolean validPrice(BigDecimal value) {
        return value != null && value.signum() > 0 && value.scale() <= 2 && value.precision() <= 10;
    }

    private String trim(String value) {
        return value == null ? "" : value.strip();
    }

    private boolean isPositiveId(String value) {
        return value.matches("[1-9][0-9]{0,18}");
    }

    private record BidRequest(BigDecimal amount) {
    }

}
