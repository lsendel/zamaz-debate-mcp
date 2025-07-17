package com.zamaz.mcp.loadtest

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import scala.util.Random

class DebateSystemLoadTest extends Simulation {

  // Configuration
  val baseUrl = sys.env.getOrElse("GATEWAY_URL", "http://localhost:8080")
  val users = sys.env.getOrElse("USERS", "100").toInt
  val rampDuration = sys.env.getOrElse("RAMP_DURATION", "60").toInt
  val testDuration = sys.env.getOrElse("TEST_DURATION", "300").toInt

  // HTTP Configuration
  val httpProtocol = http
    .baseUrl(baseUrl)
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("MCP-LoadTest/1.0")

  // Test data
  val organizations = (1 to 10).map(i => s"org-$i")
  val debateTopics = List(
    "AI Safety", "Climate Change", "Space Exploration", 
    "Quantum Computing", "Renewable Energy"
  )

  // Authentication helper
  def authenticate(orgId: String) = {
    exec(
      http("Authenticate")
        .post("/api/v1/auth/login")
        .body(StringBody(s"""{"username": "loadtest-$orgId", "password": "test123", "organizationId": "$orgId"}"""))
        .check(jsonPath("$.token").saveAs("authToken"))
        .check(jsonPath("$.userId").saveAs("userId"))
    )
  }

  // Scenarios
  val browseDebates = scenario("Browse Debates")
    .exec(session => {
      val orgId = organizations(Random.nextInt(organizations.length))
      session.set("organizationId", orgId)
    })
    .exec(authenticate("${organizationId}"))
    .exec(
      http("List Debates")
        .get("/api/v1/debates")
        .header("Authorization", "Bearer ${authToken}")
        .header("X-Organization-ID", "${organizationId}")
        .check(status.is(200))
    )
    .pause(2, 5)
    .exec(
      http("Get Debate Details")
        .get("/api/v1/debates/${debateId}")
        .header("Authorization", "Bearer ${authToken}")
        .header("X-Organization-ID", "${organizationId}")
        .check(status.is(200))
    )

  val createDebate = scenario("Create Debate")
    .exec(session => {
      val orgId = organizations(Random.nextInt(organizations.length))
      val topic = debateTopics(Random.nextInt(debateTopics.length))
      session
        .set("organizationId", orgId)
        .set("debateTopic", topic)
    })
    .exec(authenticate("${organizationId}"))
    .exec(
      http("Create Debate")
        .post("/api/v1/debates")
        .header("Authorization", "Bearer ${authToken}")
        .header("X-Organization-ID", "${organizationId}")
        .body(StringBody("""
          {
            "title": "Load Test Debate - ${debateTopic}",
            "topic": "${debateTopic}",
            "description": "Generated debate for load testing",
            "maxRounds": 5,
            "format": "standard",
            "settings": {
              "turnTimeoutSeconds": 300,
              "allowAudience": true
            }
          }
        """))
        .check(status.is(201))
        .check(jsonPath("$.id").saveAs("newDebateId"))
    )
    .pause(1, 3)
    .exec(
      http("Add AI Participant")
        .post("/api/v1/debates/${newDebateId}/participants")
        .header("Authorization", "Bearer ${authToken}")
        .header("X-Organization-ID", "${organizationId}")
        .body(StringBody("""
          {
            "type": "AI",
            "name": "Claude",
            "modelProvider": "anthropic",
            "modelName": "claude-3-opus-20240229"
          }
        """))
        .check(status.is(201))
    )

  val participateInDebate = scenario("Participate in Debate")
    .exec(session => {
      val orgId = organizations(Random.nextInt(organizations.length))
      session.set("organizationId", orgId)
    })
    .exec(authenticate("${organizationId}"))
    .exec(
      http("Get Active Debates")
        .get("/api/v1/debates?status=IN_PROGRESS")
        .header("Authorization", "Bearer ${authToken}")
        .header("X-Organization-ID", "${organizationId}")
        .check(jsonPath("$[0].id").saveAs("activeDebateId"))
    )
    .exec(
      http("Submit Response")
        .post("/api/v1/debates/${activeDebateId}/responses")
        .header("Authorization", "Bearer ${authToken}")
        .header("X-Organization-ID", "${organizationId}")
        .body(StringBody("""
          {
            "content": "This is my perspective on the topic...",
            "participantId": "${userId}"
          }
        """))
        .check(status.in(201, 200))
    )

  val searchContent = scenario("Search Content")
    .exec(session => {
      val orgId = organizations(Random.nextInt(organizations.length))
      val searchTerm = debateTopics(Random.nextInt(debateTopics.length))
      session
        .set("organizationId", orgId)
        .set("searchTerm", searchTerm)
    })
    .exec(authenticate("${organizationId}"))
    .exec(
      http("Search Debates")
        .get("/api/v1/search")
        .queryParam("q", "${searchTerm}")
        .queryParam("type", "debate")
        .header("Authorization", "Bearer ${authToken}")
        .header("X-Organization-ID", "${organizationId}")
        .check(status.is(200))
    )

  // Load test setup
  setUp(
    browseDebates.inject(
      rampUsers(users * 6 / 10) during (rampDuration seconds)
    ),
    createDebate.inject(
      rampUsers(users * 2 / 10) during (rampDuration seconds)
    ),
    participateInDebate.inject(
      rampUsers(users * 1 / 10) during (rampDuration seconds)
    ),
    searchContent.inject(
      rampUsers(users * 1 / 10) during (rampDuration seconds)
    )
  ).protocols(httpProtocol)
    .maxDuration(testDuration seconds)
    .assertions(
      global.responseTime.max.lt(5000),
      global.responseTime.mean.lt(1000),
      global.successfulRequests.percent.gt(95),
      details("Create Debate").responseTime.mean.lt(2000)
    )
}