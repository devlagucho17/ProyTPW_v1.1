package subastix.model;

import java.math.BigDecimal;
import java.time.Instant;

public record Auction(
        long id,
        String title,
        String description,
        String category,
        String condition,
        BigDecimal startingPrice,
        BigDecimal currentPrice,
        BigDecimal increment,
        Instant startsAt,
        Instant endsAt,
        String imageUrl,
        String sellerName,
        long bidCount,
        String status,
        long secondsRemaining) {
}
