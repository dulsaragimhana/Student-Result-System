package verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import studentmanagement.Results;
import studentmanagement.StudentData;

public class MainVerticle extends AbstractVerticle {

    private static final Logger logger = LogManager.getLogger(MainVerticle.class);

    @Override
    public void start(Promise<Void> startPromise){

      /*  vertx.setPeriodic(10000, id -> {
            logger.info("MainVerticle is running.");
        });*/

        // Create a router to handle HTTP requests
        Router router =Router.router(vertx);
        router.route().handler(BodyHandler.create()); // Enable parsing of request bodies

        // Define the route to handle saving student details (POST request)
        router.post("/students").handler(this::addStudent);
        router.post("/results").handler(this::addResults);

        // Define the route to handle fetching student results (GET request)
        router.get("/students/:studentId/results").handler(this::getResults);

        // Define the route to handle deleting student results (DELETE request)
        router.delete("/students/:studentId/delete").handler(this::deleteStudent);

        // Create an HTTP server and pass the router to handle incoming requests
        vertx.deployVerticle(new DBVerticle(),deployResult -> {
            if (deployResult.succeeded()) {
                vertx.createHttpServer()
                        .requestHandler(router)
                        .listen(8081, ar -> {
                            if (ar.succeeded()) {
                                //System.out.println("Server started on port 8081");
                                logger.info("Server started on port 8081");
                            } else {
                                System.err.println("Server startup failed: " + ar.cause());
                                logger.error("Server startup failed.");
                            }
                        });
            } else {
                startPromise.fail(deployResult.cause());
            }
        });

    }
    private void addStudent(RoutingContext routingContext) {
        try{
            HttpServerResponse response = routingContext.response();
            // response.putHeader("content-type", "text/plain");

            JsonObject studentJason = routingContext.getBodyAsJson();
            String id = studentJason.getString("id");
            String name = studentJason.getString("name");

            System.out.println("Student ID: " + id);
            System.out.println("Student Name: " + name);

            logger.info("Student ID: "+id + " Student Name: "+name);

            JsonObject studentDataJson = new JsonObject()
                    .put("id", id)
                    .put("name", name);

            vertx.eventBus().request(DBVerticle.ADD_STUDENT_EVENT, studentDataJson, reply -> {
                if (reply.succeeded()) {
                    //routingContext.response().end
                    response.end("Student data saved");
                    logger.info("Student data saved and put the response in postman.");
                } else {
                    response.setStatusCode(500).end(reply.cause().getMessage());
                    logger.error("Error saving student data due to "+ reply.cause().getMessage());
                }
            });
        }catch (Exception e){
            routingContext.response().setStatusMessage("Bad request in adding student").end();
            logger.error("Bad request in adding student");
        }
    }

    private void addResults(RoutingContext context){
        try{
            HttpServerResponse response = context.response();
            JsonObject studentJason = context.getBodyAsJson();
            String id = studentJason.getString("id");

            vertx.eventBus().request(ClientVerticle.GET_RESULTS_EVENT, new JsonObject()
                    .put("id", id), ar ->{
                if (ar.succeeded()) {
                    JsonObject resultJson = (JsonObject) ar.result().body();
                    //response.end("Result data saved");
                    response.setStatusCode(200).end(resultJson.encodePrettily() + "\nHave to check if the student ID already exist in the results table.");
                    //logger.info("Results taken and have to check if the student ID already exist in the results table.");
                }else {
                    response.setStatusCode(500).end(ar.cause().getMessage());
                    logger.error("Error saving result data due to " + ar.cause().getMessage());
                }
            });
        }catch (Exception e){
            context.response().setStatusMessage("Bad request in adding results").end();
            logger.error("Bad request in adding results");
        }
    }
    private void getResults(RoutingContext routingContext) {
        try{
            HttpServerResponse response = routingContext.response();
            String studentId = routingContext.pathParam("studentId");
            logger.info("Result applied student ID : " + studentId);

            JsonObject newJson = new JsonObject();
            newJson.put("id", studentId);

            vertx.eventBus().request(DBVerticle.SHOW_RESULTS_EVENT, newJson, reply -> {
                if (reply.succeeded()) {
                    JsonObject resultData = (JsonObject) reply.result().body();
                    if (resultData != null) {
                        // If resultData is not null, send it as the response
                        response
                                .putHeader("content-type", "application/json")
                                .end(resultData.encodePrettily());
                        logger.info("Result data taken successfully and showed in postman.");
                    } else {
                        response.setStatusCode(404) // Not Found
                                .end("No result data found for the given student ID "+studentId);
                        logger.error("No result data found for the given student ID " +studentId);
                    }
                } else {
                    response.setStatusCode(500) // Internal Server Error
                            .end("Error retrieving result data");
                    logger.info("Error retrieving result data");
                }
            });
        }catch (Exception e){
            routingContext.response().setStatusMessage("Bad request in getting results").end();
            logger.error("Bad request in getting results");
        }
    }

    private void deleteStudent(RoutingContext routingContext) {
        try{
            HttpServerResponse response = routingContext.response();
            String studentId = routingContext.request().getParam("studentId");
            logger.info("Data deletion applied student ID : " + studentId);

            JsonObject newJson = new JsonObject();
            newJson.put("id", studentId);

            vertx.eventBus().request(DBVerticle.DELETE_STUDENT_EVENT, newJson, reply -> {
                if (reply.succeeded()) {
                    response.end("Student data for ID "+ studentId+" deleted.");
                    logger.info("Student data for ID "+ studentId+" deleted.");
                } else {
                    response.setStatusCode(500).end("Error deleting student data.");
                    logger.error("Error deleting student data of ID "+studentId +" due to "+reply.cause().getMessage());
                }
            });
        }catch (Exception e){
            routingContext.response().setStatusMessage("Bad request in deleting student").end();
            logger.error("Bad request in deleting student");
        }
    }

}

