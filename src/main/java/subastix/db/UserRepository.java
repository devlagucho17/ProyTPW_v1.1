package subastix.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import javax.sql.DataSource;
import subastix.model.User;

public class UserRepository {
    private final DataSource dataSource;

    public UserRepository() {
        this(Database.getDataSource());
    }

    UserRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Optional<AuthenticatedUser> findByEmail(String email) throws SQLException {
        String sql = """
                SELECT id_usuario, nombre, email, password_hash, rol, saldo_disponible
                FROM usuarios WHERE email = ?
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return Optional.empty();
                User user = mapUser(result);
                return Optional.of(new AuthenticatedUser(user, result.getString("password_hash")));
            }
        }
    }

    public User create(String displayName, String email, String role, String passwordHash) throws SQLException {
        String sql = "INSERT INTO usuarios (nombre, email, password_hash, rol) VALUES (?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, displayName);
            statement.setString(2, email);
            statement.setString(3, passwordHash);
            statement.setString(4, role);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("No se pudo obtener el ID del usuario creado.");
                return new User(keys.getLong(1), displayName, email, role, java.math.BigDecimal.ZERO);
            }
        }
    }

    private User mapUser(ResultSet result) throws SQLException {
        return new User(result.getLong("id_usuario"), result.getString("nombre"),
                result.getString("email"), result.getString("rol"),
                result.getBigDecimal("saldo_disponible"));
    }

    public record AuthenticatedUser(User user, String passwordHash) {
    }
}
