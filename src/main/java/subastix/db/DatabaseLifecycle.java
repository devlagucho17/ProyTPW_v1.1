package subastix.db;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class DatabaseLifecycle implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent event) {
        event.getServletContext().getSessionCookieConfig().setHttpOnly(true);
        event.getServletContext().getSessionCookieConfig().setSecure(
                Boolean.parseBoolean(System.getenv("COOKIE_SECURE")));
        event.getServletContext().setSessionTimeout(30);
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        Database.close();
    }
}
