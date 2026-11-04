package subastix.security;

import jakarta.servlet.http.HttpSession;
import subastix.model.User;

public final class SessionUser {
    public static final String USER_ATTRIBUTE = "authenticatedUser";
    public static final String CSRF_ATTRIBUTE = "csrfToken";

    private SessionUser() {
    }

    public static User get(HttpSession session) {
        return session == null ? null : (User) session.getAttribute(USER_ATTRIBUTE);
    }
}
