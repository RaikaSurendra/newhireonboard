package com.onboardbuddy.loadtest;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

/**
 * Stress test simulation - Push system to its limits
 * Gradually increases load to find breaking point
 */
public class StressTestSimulation extends Simulation {

    HttpProtocolBuilder httpProtocol = http
        .baseUrl("http://localhost:8080/api")
        .acceptHeader("application/json")
        .contentTypeHeader("application/json")
        .userAgentHeader("Gatling Stress Test");

    FeederBuilder<String> userFeeder = csv("users.csv").circular();

    ScenarioBuilder stressScenario = scenario("Stress Test Scenario")
        .feed(userFeeder)
        .exec(http("Login")
            .post("/auth/login")
            .body(StringBody("{\"email\": \"#{email}\", \"password\": \"#{password}\"}"))
            .check(status().is(200))
            .check(jsonPath("$.data.token").saveAs("authToken"))
        )
        .pause(Duration.ofMillis(500))
        .repeat(5).on(
            exec(http("Get Users")
                .get("/users")
                .header("Authorization", "Bearer #{authToken}")
                .check(status().is(200))
            )
            .pause(Duration.ofMillis(200))
            .exec(http("Get Plans")
                .get("/plans")
                .header("Authorization", "Bearer #{authToken}")
                .check(status().is(200))
            )
            .pause(Duration.ofMillis(200))
        );

    {
        setUp(
            stressScenario.injectOpen(
                // Gradually increase load
                rampUsers(100).during(Duration.ofSeconds(60)),
                constantUsersPerSec(20).during(Duration.ofMinutes(2)),
                rampUsers(200).during(Duration.ofSeconds(60)),
                constantUsersPerSec(40).during(Duration.ofMinutes(2)),
                rampUsers(300).during(Duration.ofSeconds(60)),
                constantUsersPerSec(60).during(Duration.ofMinutes(2))
            )
        ).protocols(httpProtocol)
         .assertions(
             global().responseTime().max().lt(10000),
             global().successfulRequests().percent().gt(90.0)
         );
    }
}
