package com.zamaz.mcp.performance

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._
import scala.concurrent.duration._
import scala.util.Random

class DebateAPILoadTest extends Simulation {

  // Configuration
  val baseUrl = System.getProperty("base.url", "http://localhost:8080")
  val jwtToken = System.getProperty("jwt.token", "test-token")
  val users = Integer.getInteger("users", 100)
  val rampDuration = Integer.getInteger("ramp.duration", 60)
  val testDuration = Integer.getInteger("test.duration", 300)

  // HTTP Configuration
  val httpProtocol = http
    .baseUrl(baseUrl)
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .authorizationHeader(s"Bearer $jwtToken")
    .userAgentHeader("Gatling Performance Test")

  // Feeders for test data
  val organizationFeeder = Iterator.continually(Map(
    "orgName" -> s"PerfTest Org ${Random.alphanumeric.take(10).mkString}",
    "orgDescription" -> "Performance test organization"
  ))

  val debateFeeder = Iterator.continually(Map(
    "debateTitle" -> s"Performance Test Debate ${Random.alphanumeric.take(10).mkString}",
    "debateTopic" -> "AI Ethics in Healthcare",
    "maxRounds" -> Random.nextInt(5) + 3,
    "responseTimeout" -> 30000,
    "maxResponseLength" -> Random.nextInt(500) + 300
  ))

  // Scenarios
  val createOrganizationScenario = scenario("Create Organization")
    .feed(organizationFeeder)
    .exec(http("Create Organization")
      .post("/api/organization/create")
      .body(StringBody(
        """{
          |  "name": "${orgName}",
          |  "description": "${orgDescription}",
          |  "plan": "PROFESSIONAL"
          |}""".stripMargin))
      .check(status.is(200))
      .check(jsonPath("$.id").saveAs("orgId"))
      .check(responseTimeInMillis.lessThan(1000)))
    .pause(1, 3)

  val createDebateScenario = scenario("Create and Run Debate")
    .exec(session => {
      // First create an organization
      session.set("orgId", s"perf-org-${Random.alphanumeric.take(10).mkString}")
    })
    .feed(debateFeeder)
    .exec(http("Create Debate")
      .post("/api/debate/create")
      .body(StringBody(
        """{
          |  "title": "${debateTitle}",
          |  "topic": "${debateTopic}",
          |  "organizationId": "${orgId}",
          |  "participants": [
          |    {
          |      "name": "Claude",
          |      "position": "PRO",
          |      "aiProvider": "CLAUDE",
          |      "model": "claude-3-opus-20240229"
          |    },
          |    {
          |      "name": "GPT-4",
          |      "position": "CON",
          |      "aiProvider": "OPENAI",
          |      "model": "gpt-4"
          |    }
          |  ],
          |  "config": {
          |    "maxRounds": ${maxRounds},
          |    "responseTimeout": ${responseTimeout},
          |    "maxResponseLength": ${maxResponseLength}
          |  }
          |}""".stripMargin))
      .check(status.is(200))
      .check(jsonPath("$.id").saveAs("debateId"))
      .check(responseTimeInMillis.lessThan(2000)))
    .pause(2, 5)
    .exec(http("Start Debate")
      .post("/api/debate/${debateId}/start")
      .check(status.is(200))
      .check(responseTimeInMillis.lessThan(1000)))
    .pause(5, 10)
    .repeat(3) {
      exec(http("Get Debate Status")
        .get("/api/debate/${debateId}")
        .check(status.is(200))
        .check(jsonPath("$.status").in("IN_PROGRESS", "COMPLETED"))
        .check(responseTimeInMillis.lessThan(500)))
        .pause(3, 5)
    }

  val browseDebatesScenario = scenario("Browse Debates")
    .exec(http("List Organizations")
      .get("/api/organization/list")
      .queryParam("page", 0)
      .queryParam("size", 20)
      .check(status.is(200))
      .check(jsonPath("$.content[0].id").saveAs("browseOrgId"))
      .check(responseTimeInMillis.lessThan(1000)))
    .pause(1, 2)
    .exec(http("List Debates")
      .get("/api/debate/list")
      .queryParam("organizationId", "${browseOrgId}")
      .queryParam("page", 0)
      .queryParam("size", 20)
      .check(status.is(200))
      .check(jsonPath("$.content[0].id").optional.saveAs("browseDebateId"))
      .check(responseTimeInMillis.lessThan(1000)))
    .pause(1, 2)
    .doIf(session => session.contains("browseDebateId")) {
      exec(http("Get Debate Details")
        .get("/api/debate/${browseDebateId}")
        .check(status.is(200))
        .check(responseTimeInMillis.lessThan(500)))
        .pause(2, 4)
        .exec(http("Get Debate Messages")
          .get("/api/debate/${browseDebateId}/messages")
          .queryParam("page", 0)
          .queryParam("size", 50)
          .check(status.is(200))
          .check(responseTimeInMillis.lessThan(1000)))
    }

  val searchDebatesScenario = scenario("Search Debates")
    .exec(http("Search by Topic")
      .get("/api/debate/search")
      .queryParam("topic", "AI")
      .queryParam("page", 0)
      .queryParam("size", 10)
      .check(status.is(200))
      .check(responseTimeInMillis.lessThan(1500)))
    .pause(2, 4)
    .exec(http("Search by Status")
      .get("/api/debate/search")
      .queryParam("status", "COMPLETED")
      .queryParam("page", 0)
      .queryParam("size", 10)
      .check(status.is(200))
      .check(responseTimeInMillis.lessThan(1500)))

  val websocketScenario = scenario("WebSocket Connection")
    .exec(ws("Connect to Debate WebSocket")
      .connect("/ws/debate")
      .subprotocol("debate-protocol")
      .header("Authorization", s"Bearer $jwtToken"))
    .pause(1)
    .exec(ws("Subscribe to Debate")
      .sendText("""{"action": "subscribe", "debateId": "test-debate-123"}""")
      .await(30 seconds) {
        ws.checkTextMessage("Subscription confirmed")
      })
    .pause(30, 60)
    .exec(ws("Close WebSocket").close)

  // Load Scenarios
  val loadScenarios = Seq(
    createOrganizationScenario.inject(
      rampUsers(users / 10) during (rampDuration seconds),
      constantUsersPerSec(1) during (testDuration seconds)
    ),
    createDebateScenario.inject(
      rampUsers(users / 5) during (rampDuration seconds),
      constantUsersPerSec(2) during (testDuration seconds)
    ),
    browseDebatesScenario.inject(
      rampUsers(users * 6 / 10) during (rampDuration seconds),
      constantUsersPerSec(10) during (testDuration seconds)
    ),
    searchDebatesScenario.inject(
      rampUsers(users * 2 / 10) during (rampDuration seconds),
      constantUsersPerSec(5) during (testDuration seconds)
    ),
    websocketScenario.inject(
      rampUsers(users / 10) during (rampDuration seconds),
      constantConcurrentUsers(users / 20) during (testDuration seconds)
    )
  )

  // Setup
  setUp(loadScenarios: _*)
    .protocols(httpProtocol)
    .assertions(
      global.responseTime.max.lt(5000),
      global.responseTime.mean.lt(1000),
      global.responseTime.percentile3.lt(2000),
      global.successfulRequests.percent.gt(95),
      details("Create Debate").responseTime.mean.lt(2000),
      details("Get Debate Status").responseTime.mean.lt(500),
      details("List Debates").responseTime.mean.lt(1000)
    )
}