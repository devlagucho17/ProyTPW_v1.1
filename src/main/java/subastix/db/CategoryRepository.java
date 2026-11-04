package subastix.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

public class CategoryRepository {
    private final DataSource dataSource;

    public CategoryRepository() {
        this(Database.getDataSource());
    }

    CategoryRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Category> findAll() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT nombre, descripcion FROM categorias ORDER BY nombre");
             ResultSet results = statement.executeQuery()) {
            List<Category> categories = new ArrayList<>();
            while (results.next()) {
                categories.add(new Category(results.getString("nombre"), results.getString("descripcion")));
            }
            return categories;
        }
    }

    public record Category(String name, String description) {
    }
}
