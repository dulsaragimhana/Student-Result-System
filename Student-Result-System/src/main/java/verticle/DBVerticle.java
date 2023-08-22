package verticle;

import io.vertx.core.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Tuple;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Row;
import io.vertx.core.json.JsonObject;
import studentmanagement.Results;
import studentmanagement.StudentData;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DBVerticle extends AbstractVerticle {

    private static final Logger logger = LogManager.getLogger(DBVerticle.class);
    public static final String ADD_STUDENT_EVENT = "add.student.event";
    public static final String ADD_RESULTS_EVENT = "add.results.event";
    public static final String SHOW_RESULTS_EVENT = "show.results.event";
    public static final String DELETE_STUDENT_EVENT = "delete.student.event";

    private PgPool pgPool;

    @Override
    public void start(Promise<Void> startPromise) {

       /* vertx.setPeriodic(10000, id -> {
            logger.info("DBVerticle is running.");
        });*/

        EventBus eventBusStudent = vertx.eventBus();

        eventBusStudent.consumer(ADD_STUDENT_EVENT, message -> {
            JsonObject studentData = (JsonObject) message.body();
            saveStudent(studentData, result -> {
                if (result.succeeded()) {
                    message.reply("Student data saved successfully");
                    //logger.info("Student data saved successfully");
                } else {
                    message.fail(500, result.cause().getMessage());
                    //logger.error(result.cause().getMessage());
                }
            });

        });

        EventBus eventBusResult = vertx.eventBus();

        eventBusResult.consumer(ADD_RESULTS_EVENT, message -> {
            JsonObject results = (JsonObject) message.body();
            saveResults(results, result -> {
                if (result.succeeded()) {
                    message.reply("Result data saved successfully");
                    //logger.info("Result data saved successfully");
                } else {
                    message.fail(500, result.cause().getMessage());
                    //logger.error("Error saving result data");
                }
            });
        });

        EventBus eventBusResultShow = vertx.eventBus();

        eventBusResultShow.consumer(SHOW_RESULTS_EVENT, message -> {
            JsonObject show = (JsonObject) message.body();
            showResults(show, result -> {
                if (result.succeeded()) {
                    JsonObject resultData = result.result(); // Assuming the result of showResults is a JsonObject
                    message.reply(resultData);
                    logger.info("Result data got successfully");
                } else {
                    message.fail(500, result.cause().getMessage());
                    logger.error("Error getting result data");
                }
            });
        });

        EventBus eventBusDelete = vertx.eventBus();

        eventBusDelete.consumer(DELETE_STUDENT_EVENT, message -> {
            JsonObject delete = (JsonObject) message.body();
            deleteStudent(delete, result -> {
                if (result.succeeded()) {
                    message.reply("Student data deleted successfully");
                    logger.info("Student data deleted successfully");
                } else {
                    message.fail(500, result.cause().getMessage());
                    logger.error("Error deleting student data ");
                }
            });
        });

        PgConnectOptions connectOptions = new PgConnectOptions()
                .setHost("localhost")
                .setPort(5432)
                .setDatabase("students")
                .setUser("postgres")
                .setPassword("admin");

        PoolOptions poolOptions = new PoolOptions()
                .setMaxSize(10);

        pgPool = PgPool.pool(vertx, connectOptions, poolOptions);

        startPromise.complete();
    }

    // Save student data to the database
    private void saveStudent(JsonObject studentData, io.vertx.core.Handler<io.vertx.core.AsyncResult<Void>> resultHandler) {
        // Extract ID and name from the studentData JsonObject
        String studentId = studentData.getString("id");
        String studentName = studentData.getString("name");

        // Prepare the parameters to be used in the query
        Tuple params = Tuple.of(studentId, studentName);

        // Define the SQL query with placeholders
        String insertQuery = "INSERT INTO students (id, name) VALUES ($1, $2)";

        // Execute the query with the provided parameters
        pgPool.preparedQuery(insertQuery)
                .execute(params, ar -> {
                    if (ar.succeeded()) {
                        // If the query execution succeeds, call the resultHandler with a success outcome
                        resultHandler.handle(io.vertx.core.Future.succeededFuture());
                        logger.info("New student data saved successfully for the given ID " +studentId);
                    } else {
                        // If the query execution fails, call the resultHandler with a failure outcome and the cause
                        resultHandler.handle(io.vertx.core.Future.failedFuture(ar.cause()));
                        logger.error(io.vertx.core.Future.failedFuture(ar.cause()));
                    }
                });
    }

    private void saveResults(JsonObject results, io.vertx.core.Handler<io.vertx.core.AsyncResult<Void>> resultHandler) {

        String studentId = results.getString("id");
        String marks= results.getString("marks");
        String grade = results.getString("grade");

        String checkQuery = "SELECT COUNT(*) FROM results WHERE id = $1";
        Tuple checkParams = Tuple.of(studentId);

        pgPool.preparedQuery(checkQuery)
                .execute(checkParams, checkResult -> {
                    if (checkResult.succeeded()) {
                        Row row = checkResult.result().iterator().next();
                        int count = row.getInteger(0);

                        if (count > 0) {
                            // Result for the provided student ID already exists
                            resultHandler.handle(io.vertx.core.Future.succeededFuture());
                            logger.info("Results already saved for the given ID " + studentId);
                        } else {
                            // Prepare the parameters and query for insertion
                            Tuple insertParams = Tuple.of(studentId, marks, grade);
                            String insertQuery = "INSERT INTO results (id, marks, grade) VALUES ($1, $2, $3)";

                            pgPool.preparedQuery(insertQuery)
                                    .execute(insertParams, insertResult -> {
                                        if (insertResult.succeeded()) {
                                            resultHandler.handle(io.vertx.core.Future.succeededFuture());
                                            logger.info("New results data saved successfully for the given ID " + studentId);
                                        } else {
                                            resultHandler.handle(io.vertx.core.Future.failedFuture(insertResult.cause()));
                                            logger.error("Error in results data saving.");
                                        }
                                    });
                        }
                    } else {
                        resultHandler.handle(io.vertx.core.Future.failedFuture(checkResult.cause()));
                        logger.error("Error in checking for same ID in results table.");
                    }
                });
    }

    private void showResults(JsonObject studentData, io.vertx.core.Handler<io.vertx.core.AsyncResult<JsonObject>> resultHandler) {
        // Define the SQL query to retrieve results for a given student ID
        String studentId = studentData.getString("id");
        String selectQuery = "SELECT * FROM results WHERE id = $1";

        // Execute the query with the provided student ID as parameter
        pgPool.preparedQuery(selectQuery)
                .execute(Tuple.of(studentId), ar -> {
                    if (ar.succeeded()) {
                        // If the query execution succeeds and returns a result row
                        RowSet<Row> rows = ar.result();
                        if (rows.iterator().hasNext()) {
                            Row row = rows.iterator().next();
                            JsonObject resultData = new JsonObject()
                                    .put("id", row.getString("id"))
                                    .put("marks", row.getString("marks"))
                                    .put("grade", row.getString("grade"));

                            logger.info("Result data write to a jason successfully");
                            logger.info(resultData);

                            resultHandler.handle(io.vertx.core.Future.succeededFuture(resultData));

                        } else {
                            // No matching result found
                            resultHandler.handle(io.vertx.core.Future.succeededFuture(null));
                        }
                    } else {
                        // If the query execution fails, call the resultHandler with a failure outcome and the cause
                        resultHandler.handle(io.vertx.core.Future.failedFuture(ar.cause()));
                    }
                });
    }

    private void deleteStudent(JsonObject studentData, io.vertx.core.Handler<io.vertx.core.AsyncResult<Void>> resultHandler) {
        // Define the SQL query to delete student data based on ID
        String studentId = studentData.getString("id");
        String deleteQuery = "DELETE FROM students WHERE id = $1";

        // Execute the delete query with the provided student ID as parameter
        pgPool.preparedQuery(deleteQuery)
                .execute(Tuple.of(studentId), ar -> {
                    if (ar.succeeded()) {
                        int affectedRows = ar.result().rowCount();
                        if (affectedRows > 0) {
                            resultHandler.handle(io.vertx.core.Future.succeededFuture());
                        } else {
                            resultHandler.handle(io.vertx.core.Future.failedFuture("Student not found"));
                        }
                    } else {
                        resultHandler.handle(io.vertx.core.Future.failedFuture(ar.cause()));
                    }
                });
    }

}
