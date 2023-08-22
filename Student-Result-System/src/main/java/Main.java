import io.vertx.core.Vertx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studentmanagement.StudentData;
import verticle.ClientVerticle;
import verticle.MainVerticle;
import verticle.DBVerticle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {

    private static final Logger logger = LogManager.getLogger(MainVerticle.class);

    public static void main(String[] args) {

        logger.info("Starting the Application");
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new MainVerticle());
        vertx.deployVerticle(new DBVerticle());
        vertx.deployVerticle(new ClientVerticle());
    }
}
