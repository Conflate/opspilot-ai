OpsPilot AI is an AI-assisted operational ticket triage platform built with Java Spring Boot, PostgreSQL, Docker, and Gemini.

The project is designed as an internal operations tool. Tickets are created, triaged by AI, reviewed by a human, and tracked through audit logs. AI does not directly modify ticket state without human approval.

## Why This Project Exists

Operational teams often receive unclear, repetitive, or poorly categorized tickets. Manual triage creates delay, inconsistent prioritization, and duplicate work.

OpsPilot AI demonstrates how AI can be integrated into a reliable backend workflow without giving the model uncontrolled authority. The system uses AI for decision support while preserving human review, traceability, validation, and auditability.

## Core Workflow

```text

Create ticket

    -> Run AI triage

    -> Store AI recommendation

    -> Move ticket to PENDING_REVIEW

    -> Human approves or rejects recommendation

    -> Ticket state is updated

    -> Audit log records the action

```

## Features

- Ticket creation and retrieval

- AI-assisted triage through a replaceable AiClient interface

- Fake AI client for local development and tests

- Gemini AI client for real model-based triage

- Human approval and rejection workflow

- Audit logging for ticket creation, AI triage, approval, rejection, and duplicate checks

- Duplicate ticket detection using explainable scoring

- Dashboard summary endpoint

- Global API error handling

- Unit tests for ticket, triage, approval, duplicate detection, and dashboard services

## Tech Stack

- Java 17

- Spring Boot

- Spring Web

- Spring Data JPA

- PostgreSQL

- Docker Compose

- Gemini API

- JUnit 5

- Mockito

- Maven

## Architecture

```text

Client / API Consumer

        |

        v

Spring Boot REST API

        |

        v

Service Layer

TicketService / AiTriageService / ApprovalService / AuditService

        |

        +--------------------+

        |                    |

        v                    v

PostgreSQL             AiClient Interface

                             |

                             +--> FakeAiClient

                             +--> GeminiAiClient

```

## Package Structure

```text

com.opspilot

  ai          -> AI client interface, fake AI client, Gemini client

  approval    -> human approval/rejection workflow

  audit       -> audit log entity, service, and API

  common      -> API errors and exception handling

  dashboard   -> operational summary metrics

  duplicate   -> duplicate ticket detection

  ticket      -> ticket entity, repository, service, and API

  triage      -> AI triage result storage and workflow

```

## AI Safety Design

OpsPilot AI uses AI as decision support, not autonomous action.

AI can:

- summarize a ticket

- classify category

- classify severity

- suggest probable cause

- recommend next action

- provide a confidence score

AI cannot directly apply its own recommendation to a ticket.

A human reviewer must approve or reject the recommendation. Approval updates the ticket category, severity, and status. Rejection preserves the original ticket category and severity. Both actions are recorded in the audit log.

## Main API Endpoints

### Tickets

```http

POST /api/tickets

GET /api/tickets

GET /api/tickets/{id}

```

Example create ticket request:

```json

{

  "title": "Map layer loading slowly",

  "description": "Several users report high latency when loading radar map layers during morning operations.",

  "sourceSystem": "Service Desk",

  "affectedService": "NinJo"

}

```

### AI Triage

```http

POST /api/tickets/{ticketId}/triage

GET /api/tickets/{ticketId}/triage

```

Example triage response:

```json

{

  "id": 1,

  "ticketId": 1,

  "summary": "The ticket reports slow map layer loading.",

  "category": "PERFORMANCE",

  "severity": "MEDIUM",

  "probableCause": "Possible backend latency or map service performance issue.",

  "recommendedAction": "Check logs, service health, and recent changes affecting map layers.",

  "confidenceScore": 0.7,

  "requiresHumanReview": false,

  "modelName": "fake-ai-v1",

  "createdAt": "2026-05-20T00:00:00Z"

}

```

### Human Approval

```http

POST /api/tickets/{ticketId}/triage/{triageId}/approve

POST /api/tickets/{ticketId}/triage/{triageId}/reject

```

Example approval request:

```json

{

  "reviewer": "David",

  "reviewNote": "AI recommendation looks correct."

}

```

### Audit Logs

```http

GET /api/tickets/{ticketId}/audit-logs

```

### Duplicate Detection

```http

GET /api/tickets/{ticketId}/duplicates

```

### Dashboard

```http

GET /api/dashboard

```

Example dashboard response:

```json

{

  "totalTickets": 2,

  "openTickets": 1,

  "pendingReviewTickets": 0,

  "approvedTickets": 1,

  "rejectedTickets": 0,

  "criticalTickets": 0,

  "averageConfidenceScore": 0.7,

  "ticketsBySeverity": {

    "UNTRIAGED": 1,

    "LOW": 0,

    "MEDIUM": 0,

    "HIGH": 1,

    "CRITICAL": 0

  },

  "ticketsByCategory": {

    "PERFORMANCE": 1,

    "UNCLASSIFIED": 1

  }

}

```

## Local Setup

Start PostgreSQL:

```bash

docker compose up -d

```

Run the backend:

```bash

cd backend

./mvnw spring-boot:run

```

The backend runs on:

```text

http://localhost:8080

```

## Configuration

The application uses application.yaml.

For fake AI mode:

```yaml

opspilot:

  ai:

    provider: fake

```

For Gemini mode:

```yaml

opspilot:

  ai:

    provider: gemini

    gemini:

      api-key: ${GEMINI_API_KEY:}

      model: gemini-2.5-flash

```

Set the Gemini key as an environment variable:

```bash

export GEMINI_API_KEY="your-key-here"

```

Do not commit API keys.

## Running Tests

```bash

cd backend

./mvnw test

```

Current test status:

```text

Tests run: 11

Failures: 0

Errors: 0

```

Current coverage includes:

- TicketServiceTest

- AiTriageServiceTest

- ApprovalServiceTest

- DuplicateDetectionServiceTest

- DashboardServiceTest

## Example Manual Test Flow

Create a ticket:

```bash

curl -X POST http://localhost:8080/api/tickets \

  -H "Content-Type: application/json" \

  -d '{

    "title": "Map layer loading slowly",

    "description": "Several users report high latency when loading radar map layers during morning operations.",

    "sourceSystem": "Service Desk",

    "affectedService": "NinJo"

  }'

```

Run triage:

```bash

curl -X POST http://localhost:8080/api/tickets/1/triage

```

Approve triage:

```bash

curl -X POST http://localhost:8080/api/tickets/1/triage/1/approve \

  -H "Content-Type: application/json" \

  -d '{

    "reviewer": "David",

    "reviewNote": "AI recommendation looks correct."

  }'

```

View audit logs:

```bash

curl http://localhost:8080/api/tickets/1/audit-logs

```

View dashboard:

```bash

curl http://localhost:8080/api/dashboard

```

## Portfolio Value

This project demonstrates:

- backend API design

- Spring Boot service-layer architecture

- PostgreSQL persistence

- AI integration behind an interface

- human-in-the-loop approval design

- auditability and traceability

- operational workflow modeling

- unit testing with JUnit and Mockito

## Future Improvements

- React frontend

- Swagger/OpenAPI documentation

- GitHub Actions CI

- Role-based access control

- Better duplicate detection using embeddings or pgvector

- CSV ticket import

- Deployment to a cloud platform

- Integration with Jira-style ticket systems