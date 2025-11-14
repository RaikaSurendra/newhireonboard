package com.onboardbuddy.loadtest;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

/**
 * Basic load test simulation for OnboardBuddy API
 * Tests authentication and basic CRUD operations
 */
public class BasicSimulation extends Simulation {

    // HTTP Protocol Configuration
    HttpProtocolBuilder httpProtocol = http
        .baseUrl("http://localhost:8080/api")
        .acceptHeader("application/json")
        .contentTypeHeader("application/json")
        .userAgentHeader("Gatling Load Test");

    // Feeder for test data
    FeederBuilder<String> userFeeder = csv("users.csv").circular();

    // Scenario 1: Authentication Flow
    ScenarioBuilder authScenario = scenario("Authentication Flow")
        .feed(userFeeder)
        .exec(http("Login")
            .post("/auth/login")
            .body(StringBody("{\"email\": \"#{email}\", \"password\": \"#{password}\"}"))
            .check(status().is(200))
            .check(jsonPath("$.data.token").saveAs("authToken"))
        )
        .pause(1, 3)
        .exec(http("Get Current User")
            .get("/auth/me")
            .header("Authorization", "Bearer #{authToken}")
            .check(status().is(200))
        )
        .pause(1, 2)
        .exec(http("Logout")
            .post("/auth/logout")
            .header("Authorization", "Bearer #{authToken}")
            .check(status().is(200))
        );

    // Scenario 2: User Operations
    ScenarioBuilder userScenario = scenario("User Operations")
        .feed(userFeeder)
        .exec(http("Login")
            .post("/auth/login")
            .body(StringBody("{\"email\": \"#{email}\", \"password\": \"#{password}\"}" ))
            .check(status().is(200))
            .check(jsonPath("$.data.token").saveAs("authToken"))
        )
        .pause(1)
        .exec(http("Get All Users")
            .get("/users")
            .header("Authorization", "Bearer #{authToken}")
            .check(status().is(200))
        )
        .pause(1, 2)
        .exec(http("Get User By ID")
            .get("/users/#{userId}")
            .header("Authorization", "Bearer #{authToken}")
            .check(status().in(200, 404))
        );

    // Scenario 3: Onboarding Plan Operations
    ScenarioBuilder planScenario = scenario("Onboarding Plan Operations")
        .feed(userFeeder)
        .exec(http("Login")
            .post("/auth/login")
            .body(StringBody("{\"email\": \"#{email}\", \"password\": \"#{password}\"}" ))
            .check(status().is(200))
            .check(jsonPath("$.data.token").saveAs("authToken"))
        )
        .pause(1)
        .exec(http("Get All Plans")
            .get("/plans")
            .header("Authorization", "Bearer #{authToken}")
            .check(status().is(200))
        )
        .pause(1, 2)
        .exec(http("Get My Plans")
            .get("/plans/my-plans")
            .header("Authorization", "Bearer #{authToken}")
            .check(status().is(200))
        );

    // Load Test Configuration
    {
        setUp(
            // Ramp up users gradually
            authScenario.injectOpen(
                rampUsers(50).during(Duration.ofSeconds(30)),
                constantUsersPerSec(10).during(Duration.ofMinutes(2))
            ),
            userScenario.injectOpen(
                rampUsers(30).during(Duration.ofSeconds(30)),
                constantUsersPerSec(5).during(Duration.ofMinutes(2))
            ),
            planScenario.injectOpen(
                rampUsers(20).during(Duration.ofSeconds(30)),
                constantUsersPerSec(3).during(Duration.ofMinutes(2))
            )
        ).protocols(httpProtocol)
         .assertions(
             global().responseTime().max().lt(5000),
             global().responseTime().percentile3().lt(1000),
             global().successfulRequests().percent().gt(95.0)
         );
    }
}
