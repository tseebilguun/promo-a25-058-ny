package mn.unitel.campaign.database;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.sql.Connection;
import java.sql.SQLException;

@ApplicationScoped
public class DBPromoObject {
    Logger logger = Logger.getLogger(DBPromoObject.class);

    private final HikariDataSource _ds = new HikariDataSource();

    public DBPromoObject(@ConfigProperty(name = "db.driver-class-name") String _driverClassName,
                         @ConfigProperty(name = "db.connection-string") String _connString,
                         @ConfigProperty(name = "db.username") String _username,
                         @ConfigProperty(name = "db.password") String _password,
                         @ConfigProperty(name = "db.min-conn") int _minConn,
                         @ConfigProperty(name = "db.max-conn") int _maxConn) {
        logger.info("Initializing database connection ...");

        _ds.setJdbcUrl(_connString);
        _ds.setUsername(_username);
        _ds.setPassword(_password);
        _ds.setMinimumIdle(_minConn);
        _ds.setMaximumPoolSize(_maxConn);
        _ds.setDriverClassName(_driverClassName);
        _ds.setAutoCommit(true);
    }

    public Connection getConnection() throws SQLException {
        logger.debug("getting database connection");
        return _ds.getConnection();
    }

    @PreDestroy
    public void closeDataSource() {
        try {
            logger.info("Closing database connection pool");
            _ds.close();
        } catch (Exception e) {
            logger.error("Error while closing the database connection pool", e);
        }
    }
}
