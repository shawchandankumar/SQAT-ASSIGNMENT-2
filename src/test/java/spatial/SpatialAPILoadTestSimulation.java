package spatial;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class SpatialAPILoadTestSimulation extends Simulation {

    // http configuration
    private final HttpProtocolBuilder httpProtocol = http
            .baseUrl("http://54.226.34.214:8081")
            .acceptHeader("application/json");


    // Runtime parameters
    private static final int USER_COUNT = Integer.parseInt(System.getProperty("USERS", "5"));
    private static final int RAMP_DURATION = Integer.parseInt(System.getProperty("RAMP_DURATION", "10"));

    // data feeder of user
    private static FeederBuilder.FileBased<Object> userJsonFeeder = jsonFile("data/authUserData.json");

    @Override
    public void before() {
        System.out.printf("Running test with %d users%n", USER_COUNT);
        System.out.printf("Ramping users over %d seconds%n", RAMP_DURATION);
    }


    // Scenario builder

    // 1. Signup (Register a user)
    // if user doesn't exist, creates new user
    private static ChainBuilder signup =
            feed(userJsonFeeder)
                    .exec(http("Signup")
                    .post("/signup")
                    .body(ElFileBody("body/authBodyTemplate.json"))
                    .asJson());

    // 2. Login (Authenticate)
    private static ChainBuilder login =
                    exec(http("Login")
                    .post("/login")
                    .body(ElFileBody("body/authBodyTemplate.json")).asJson()
                    .check(jmesPath("token").saveAs("jwtToken")));

    // 3. Create new post
    private static ChainBuilder createPost =
            exec(http("Create New Post")
                    .post("/post/create")
                    .header("Authorization", "Bearer #{jwtToken}")
                    .body(ElFileBody("body/newPostTemplate.json")).asJson());

    // 4. Get all nearby post sorted by distance
    private static ChainBuilder getPost =
            exec(http("Get nearby distance post")
                    .post("/post/near")
                    .header("Authorization", "Bearer #{jwtToken}")
                    .body(ElFileBody("body/getNearbyPostTemplate.json")).asJson()
                    .check(jsonPath("$.posts[0]._id").saveAs("postId")));

    // 5. Comment on a post
    private static ChainBuilder addComment =
            exec(http("Comment on a post")
                    .post("/post/createComment")
                    .header("Authorization", "Bearer #{jwtToken}")
                    .body(ElFileBody("body/newCommentTemplate.json")).asJson());

    // Defining Scenarios to execute a use case
    private ScenarioBuilder scn = scenario("Spatial API Stress test")
            .exec(signup)
            .pause(2)
            .exec(login)
            .pause(2)
            .exec(createPost)
            .pause(2)
            .exec(getPost)
            .pause(2)
            .exec(addComment);

    // Load test
    {
        setUp(
                scn.injectOpen(
                        nothingFor(5),
                        rampUsers(USER_COUNT).during(RAMP_DURATION)
                )
        ).protocols(httpProtocol);
    }

}
