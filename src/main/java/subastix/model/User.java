package subastix.model;

import java.math.BigDecimal;

public record User(long id, String displayName, String email, String role, BigDecimal balance) {
}
