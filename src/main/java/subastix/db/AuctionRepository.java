package subastix.db;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import subastix.model.Auction;
import subastix.model.Bid;

public class AuctionRepository {
    private static final String SELECT_AUCTION = """
            SELECT s.id_subasta AS id, p.titulo AS title, p.descripcion AS description,
                   c.nombre AS category, p.estado_producto AS product_condition,
                   s.precio_base AS starting_price, s.precio_actual AS current_price,
                   s.incremento_minimo AS bid_increment, s.fecha_inicio AS starts_at,
                   s.fecha_fin AS ends_at, p.imagen_url AS image_url,
                   COALESCE(v.nombre, 'Vendedor') AS seller_name, s.estado AS auction_state,
                   (SELECT COUNT(*) FROM pujas b WHERE b.id_subasta = s.id_subasta) AS bid_count
            FROM subastas s
            JOIN productos p ON p.id_producto = s.id_producto
            JOIN categorias c ON c.id_categoria = p.id_categoria
            JOIN usuarios v ON v.id_usuario = p.id_vendedor
            """;

    private final DataSource dataSource;

    public AuctionRepository() {
        this(Database.getDataSource());
    }

    AuctionRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Auction> findAll() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     SELECT_AUCTION + " ORDER BY s.fecha_fin ASC, s.id_subasta ASC");
             ResultSet results = statement.executeQuery()) {
            List<Auction> auctions = new ArrayList<>();
            while (results.next()) auctions.add(mapAuction(results));
            return auctions;
        }
    }

    public Optional<Auction> findById(long id) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     SELECT_AUCTION + " WHERE s.id_subasta = ?")) {
            statement.setLong(1, id);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? Optional.of(mapAuction(results)) : Optional.empty();
            }
        }
    }

    public List<Bid> findRecentBids(long auctionId, int limit) throws SQLException {
        String sql = """
                SELECT u.nombre, b.monto, b.fecha_puja
                FROM pujas b JOIN usuarios u ON u.id_usuario = b.id_usuario
                WHERE b.id_subasta = ?
                ORDER BY b.fecha_puja DESC, b.id_puja DESC
                LIMIT ?
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, auctionId);
            statement.setInt(2, limit);
            try (ResultSet results = statement.executeQuery()) {
                List<Bid> bids = new ArrayList<>();
                while (results.next()) {
                    bids.add(new Bid(maskName(results.getString("nombre")),
                            results.getBigDecimal("monto"), results.getTimestamp("fecha_puja").toInstant()));
                }
                return bids;
            }
        }
    }

    public Auction create(long sellerId, String title, String description, String category,
                          String condition, BigDecimal startingPrice, BigDecimal increment,
                          Instant startsAt, Instant endsAt, String imageUrl) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long categoryId;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT id_categoria FROM categorias WHERE nombre = ?")) {
                    statement.setString(1, category);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) throw new SQLException("La categoría seleccionada ya no existe.");
                        categoryId = result.getLong("id_categoria");
                    }
                }

                int productId;
                String insertProduct = """
                        INSERT INTO productos
                            (id_vendedor, id_categoria, titulo, descripcion, imagen_url, estado_producto)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """;
                try (PreparedStatement statement = connection.prepareStatement(
                        insertProduct, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setLong(1, sellerId);
                    statement.setLong(2, categoryId);
                    statement.setString(3, title);
                    statement.setString(4, description);
                    statement.setString(5, imageUrl);
                    statement.setString(6, conditionCode(condition));
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("No se pudo obtener el producto creado.");
                        productId = keys.getInt(1);
                    }
                }

                int auctionId;
                String insertAuction = """
                        INSERT INTO subastas
                            (id_producto, precio_base, precio_actual, incremento_minimo,
                             fecha_inicio, fecha_fin, estado)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """;
                try (PreparedStatement statement = connection.prepareStatement(
                        insertAuction, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setInt(1, productId);
                    statement.setBigDecimal(2, startingPrice);
                    statement.setBigDecimal(3, startingPrice);
                    statement.setBigDecimal(4, increment);
                    statement.setTimestamp(5, Timestamp.from(startsAt));
                    statement.setTimestamp(6, Timestamp.from(endsAt));
                    statement.setString(7, startsAt.isAfter(Instant.now()) ? "PROGRAMADA" : "ACTIVA");
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("No se pudo obtener la subasta creada.");
                        auctionId = keys.getInt(1);
                    }
                }
                connection.commit();
                return findById(auctionId).orElseThrow(
                        () -> new SQLException("No se pudo cargar la subasta creada."));
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public Auction placeBid(long auctionId, long bidderId, BigDecimal amount)
            throws SQLException, BidRejectedException {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                BigDecimal currentPrice;
                BigDecimal increment;
                Instant startsAt;
                Instant endsAt;
                String status;
                long sellerId;
                String lockSql = """
                        SELECT s.precio_actual, s.incremento_minimo, s.fecha_inicio, s.fecha_fin,
                               s.estado, p.id_vendedor
                        FROM subastas s JOIN productos p ON p.id_producto = s.id_producto
                        WHERE s.id_subasta = ?
                        FOR UPDATE
                        """;
                try (PreparedStatement statement = connection.prepareStatement(lockSql)) {
                    statement.setLong(1, auctionId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) throw new BidRejectedException("La subasta no existe.", 404);
                        currentPrice = result.getBigDecimal("precio_actual");
                        increment = result.getBigDecimal("incremento_minimo");
                        startsAt = result.getTimestamp("fecha_inicio").toInstant();
                        endsAt = result.getTimestamp("fecha_fin").toInstant();
                        status = result.getString("estado");
                        sellerId = result.getLong("id_vendedor");
                    }
                }

                Instant now = Instant.now();
                if (!now.isBefore(endsAt) || "FINALIZADA".equals(status) || "CANCELADA".equals(status)) {
                    throw new BidRejectedException("La subasta ya finalizó.", 409);
                }
                if (now.isBefore(startsAt)) {
                    throw new BidRejectedException("La subasta todavía no ha comenzado.", 409);
                }
                if (sellerId == bidderId) {
                    throw new BidRejectedException("No puedes pujar por tu propia subasta.", 403);
                }
                BigDecimal minimum = currentPrice.add(increment);
                if (amount.compareTo(minimum) < 0) {
                    throw new BidRejectedException("La puja mínima es " + minimum.toPlainString() + ".", 409);
                }

                Instant newEnd = endsAt;
                if (endsAt.toEpochMilli() - now.toEpochMilli() <= 60_000) {
                    newEnd = endsAt.plusSeconds(120);
                }
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO pujas (id_subasta, id_usuario, monto) VALUES (?, ?, ?)")) {
                    insert.setLong(1, auctionId);
                    insert.setLong(2, bidderId);
                    insert.setBigDecimal(3, amount);
                    insert.executeUpdate();
                }
                try (PreparedStatement update = connection.prepareStatement("""
                        UPDATE subastas
                        SET precio_actual = ?, fecha_fin = ?, id_ganador_actual = ?,
                            estado = 'ACTIVA', version = version + 1
                        WHERE id_subasta = ?
                        """)) {
                    update.setBigDecimal(1, amount);
                    update.setTimestamp(2, Timestamp.from(newEnd));
                    update.setLong(3, bidderId);
                    update.setLong(4, auctionId);
                    update.executeUpdate();
                }
                connection.commit();
                return findById(auctionId).orElseThrow(
                        () -> new SQLException("La subasta no se encontró después de registrar la puja."));
            } catch (SQLException | BidRejectedException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private Auction mapAuction(ResultSet result) throws SQLException {
        Instant startsAt = result.getTimestamp("starts_at").toInstant();
        Instant endsAt = result.getTimestamp("ends_at").toInstant();
        String dbStatus = result.getString("auction_state");
        Instant now = Instant.now();
        String status;
        if ("CANCELADA".equals(dbStatus) || "FINALIZADA".equals(dbStatus) || !now.isBefore(endsAt)) {
            status = "ended";
        } else if (now.isBefore(startsAt)) {
            status = "soon";
        } else {
            status = "live";
        }
        Instant countdownTarget = "soon".equals(status) ? startsAt : endsAt;
        long secondsRemaining = Math.max(0, countdownTarget.getEpochSecond() - now.getEpochSecond());
        return new Auction(
                result.getLong("id"),
                result.getString("title"),
                result.getString("description"),
                result.getString("category"),
                conditionLabel(result.getString("product_condition")),
                result.getBigDecimal("starting_price"),
                result.getBigDecimal("current_price"),
                result.getBigDecimal("bid_increment"),
                startsAt,
                endsAt,
                result.getString("image_url"),
                result.getString("seller_name"),
                result.getLong("bid_count"),
                status,
                secondsRemaining);
    }

    private String conditionCode(String condition) throws SQLException {
        return switch (condition) {
            case "Nuevo" -> "NUEVO";
            case "Usado · Excelente" -> "USADO_EXCELENTE";
            case "Usado · Bueno" -> "USADO_BUENO";
            default -> throw new SQLException("El estado del producto no es válido.");
        };
    }

    private String conditionLabel(String condition) {
        return switch (condition) {
            case "USADO_EXCELENTE" -> "Usado · Excelente";
            case "USADO_BUENO" -> "Usado · Bueno";
            default -> "Nuevo";
        };
    }

    private String maskName(String name) {
        String first = name.strip().split("\\s+")[0];
        if (first.length() <= 1) return first + "***";
        return first.substring(0, 1) + "***" + first.substring(first.length() - 1);
    }

    public static class BidRejectedException extends Exception {
        private final int status;

        public BidRejectedException(String message, int status) {
            super(message);
            this.status = status;
        }

        public int getStatus() {
            return status;
        }
    }
}
