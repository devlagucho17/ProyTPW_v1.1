package subastix.web;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/images/*")
public class ImageServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(ImageServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathInfo();
        if (path == null || !path.matches("/[0-9a-fA-F-]{36}\\.(jpg|png)")) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Path directory = ImageStorage.uploadDirectory();
        Path image = directory.resolve(path.substring(1)).normalize();
        if (!image.getParent().equals(directory) || !Files.isRegularFile(image)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String contentType = path.endsWith(".png") ? "image/png" : "image/jpeg";
        response.setContentType(contentType);
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Cache-Control", "public, max-age=86400");
        try (var output = response.getOutputStream()) {
            Files.copy(image, output);
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "No se pudo servir una imagen de subasta.", ex);
            throw ex;
        }
    }
}
