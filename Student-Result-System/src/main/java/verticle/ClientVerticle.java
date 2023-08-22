package verticle;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import studentmanagement.Results;
import studentmanagement.StudentData;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClientVerticle extends AbstractVerticle {

    private static final Logger logger = LogManager.getLogger(DBVerticle.class);
    public static String GET_RESULTS_EVENT = "get.results.event";
    private WebClient webClient;
    private String myGrade ;

    @Override
    public void start(Promise<Void> promise){

        webClient = WebClient.create(vertx);

        vertx.eventBus().consumer(GET_RESULTS_EVENT,ar ->{
            JsonObject jsonObject = (JsonObject) ar.body();
            String studentId = jsonObject.getString("id");

            webClient.getAbs("https://www.random.org/integers/?num=1&min=0&max=100&col=1&base=10&format=plain&rnd=new")
                    .send()
                    .onSuccess( res -> {

                        String markStr = res.bodyAsString();
                        int marks = Integer.parseInt(markStr.substring(0, markStr.length() - 1));

                        int temp = marks / 10;
                        switch (temp) {
                            case 0, 1, 2, 3, 4 -> this.myGrade = "F";
                            case 5 -> this.myGrade = "D";
                            case 6 -> this.myGrade = "C";
                            case 7 -> this.myGrade = "B";
                            default -> this.myGrade = "A";
                        }

                        JsonObject resultJson = new JsonObject();
                        resultJson.put("id", studentId);
                        resultJson.put("marks", marks);
                        resultJson.put("grade", myGrade);
                        logger.info("Results generated and have to check if the student ID already exist in the results table.");
                        logger.info(resultJson);

                        vertx.eventBus().request(DBVerticle.ADD_RESULTS_EVENT, resultJson, reply -> {
                            if (reply.succeeded()) {
                                ar.reply(resultJson);
                            } else {
                                ar.fail(500, reply.cause().getMessage());
                            }
                        });

                    })
                    .onFailure(error -> ar.fail(HttpResponseStatus.BAD_GATEWAY.code(), error.getMessage()));
        });
    }

}

