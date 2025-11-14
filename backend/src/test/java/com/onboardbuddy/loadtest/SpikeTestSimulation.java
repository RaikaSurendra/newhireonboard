package com.onboardbuddy.loadtest;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

/**
 * Spike test simulation - Sudden traffic spikes
 * Tests system behavior under sudden load increases
 */
public class SpikeTestSimulation extends Simulation {

    HttpProtocolBuilder httpProtocol = http
        .baseUrl("http://localhost:8080/api")
        .acceptHeader("application/json")
        .contentTypeHeader("application/json")
        .userAgentHeader("Gatling Spike Test");

    FeederBuilder<String> userFeeder = csv("users.csv").circular();

    ScenarioBuilder spikeScenario = scenario("Spike Test Scenario")
        .feed(userFeeder)
        .exec(http("Login")
            .post("/auth/login")
            .body(StringBody("{\"email\": \"#{email}\", \"password\": \"#{password}\"}"))
            .check(status().is(200))
            .check(jsonPath("$.data.token").saveAs("authToken"))
        )
        .pause(Duration.ofMillis(100))
        .exec(http("Get Dashboard Data")
            .get("/users")
            .header("Authorization", "Bearer #{authToken}")
            .check(status().is(200))
        );

    {
        setUp(
            spikeScenario.injectOpen(
                // Normal load
                constantUsersPerSec(10).during(Duration.ofMinutes(1)),
                // Sudden spike
                atOnceUsers(500),
                // Back to normal
                constantUsersPerSec(10).during(Duration.ofMinutes(1)),
                // Another spike
                atOnceUsers(1000),
                // Recovery
                constantUsersPerSec(5).during(Duration.ofMinutes(1))
            )
        ).protocols(httpProtocol)
         .assertions(
             global().responseTime().max().lt(15000),
             global().successfulRequests().percent().gt(85.0)
         );
    }
}
