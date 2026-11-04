package subastix.db;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

public class AccountRepository {
    private final DataSource dataSource;

    public AccountRepository() {
        this(Database.getDataSource());
    }

    AccountRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<AccountBid> findBids(long userId) throws SQLException {
        String sql = """
                SELECT s.id_subasta AS id, p.titulo AS title, s.precio_actual AS current_price,
                       s.fecha_fin AS ends_at, p.imagen_url AS image_url,
                       MAX(my_bid.monto) AS my_bid,
                       CASE WHEN MAX(my_bid.monto) = s.precio_actual THEN 1 ELSE 0 END AS leading
                FROM pujas my_bid
                JOIN subastas s ON s.id_subasta = my_bid.id_subasta
                JOIN productos p ON p.id_producto = s.id_producto
                WHERE my_bid.id_usuario = ?
                GROUP BY s.id_subasta, p.titulo, s.precio_actual, s.fecha_fin, p.imagen_url
                ORDER BY s.fecha_fin ASC
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet results = statement.executeQuery()) {
                List<AccountBid> bids = new ArrayList<>();
                while (results.next()) {
                    bids.add(new AccountBid(results.getLong("id"), results.getString("title"),
                            results.getBigDecimal("my_bid"), results.getBigDecimal("current_price"),
                            results.getTimestamp("ends_at").toInstant().toString(),
                            results.getString("image_url"), results.getBoolean("leading")));
                }
                return bids;
            }
        }
    }

    public record AccountBid(long auctionId, String title, BigDecimal myBid, BigDecimal currentPrice,
                             String endsAt, String imageUrl, boolean leading) {
    }
}
