package subastix.model;

import java.math.BigDecimal;
import java.time.Instant;

public record Bid(String bidderName, BigDecimal amount, Instant createdAt) {
}
