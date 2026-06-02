# OpsPilot AI

OpsPilot AI is a local operations ticketing console with AI-assisted triage, human review, duplicate tracking, and audit history.

I built this as a portfolio project to explore a practical AI workflow: using a model to help with messy operational tickets while keeping final decisions traceable and human-controlled. AI can recommend severity, category, probable cause, and next action, but it does not apply its own recommendation without operator approval.

## What It Does

- Spring Boot backend with PostgreSQL persistence
- React/Vite frontend for the operations console
- Gemini-backed AI triage behind a replaceable `AiClient`
- Fake AI client for local development and tests
- Human approval, rejection, and manual correction flow
- Duplicate detection suggestions plus persisted duplicate links
- Audit trail for ticket creation, triage, review, duplicate activity, and manual edits
- Dashboard metrics for ticket counts, severity, category, and confidence
- Flyway migration files staged in an inactive Maven profile

## Workflow

```text
Create ticket
  -> Run AI triage
  -> Store recommendation
  -> Move ticket to PENDING_REVIEW
  -> Approve or reject recommendation
  -> If rejected, apply manual triage
  -> Check and mark duplicates as needed
  -> Review audit history
```

## Tech Stack

Backend:

- Java 17
- Spring Boot
- Spring Web MVC
- Spring WebFlux `WebClient` for Gemini
- Spring Data JPA
- PostgreSQL
- Maven
- JUnit 5 and Mockito

Frontend:

- React
- Vite
- CSS modules by convention through `App.css` and `index.css`
- Vite dev proxy from `/api` to `http://localhost:8080`

Infrastructure:

- Docker Compose for local PostgreSQL
- Flyway SQL migrations are present but not active by default

## Project Structure

```text
backend/src/main/java/com/opspilot
  ai          AI client interface, fake client, Gemini client
  approval    human approval and rejection workflow
  audit       audit log entity, service, and API
  common      shared exceptions and API error handling
  dashboard   dashboard metrics
  duplicate   duplicate detection and duplicate links
  ticket      ticket entity, repository, service, and API
  triage      triage result storage and workflow

frontend/src
  App.jsx     main operations console
  App.css     application layout and components
  index.css   global design tokens and base styles
```

## Run Locally

Start PostgreSQL:

```bash
docker compose up -d
```

Run the backend:

```bash
cd backend
./mvnw spring-boot:run
```

Run the frontend:

```bash
cd frontend
npm install
npm run dev
```

Open the frontend at:

```text
http://127.0.0.1:5173/
```

The backend runs at:

```text
http://localhost:8080
```

## AI Configuration

Set a Gemini key before running the backend in Gemini mode:

```bash
export GEMINI_API_KEY="your-key-here"
```

The backend also contains a fake AI client for deterministic local development and tests. Do not commit API keys.

## Main API Endpoints

Tickets:

```http
POST /api/tickets
GET /api/tickets
GET /api/tickets/{id}
```

Triage:

```http
POST /api/tickets/{ticketId}/triage
GET /api/tickets/{ticketId}/triage
POST /api/tickets/{ticketId}/triage/manual
```

Review:

```http
POST /api/tickets/{ticketId}/triage/{triageId}/approve
POST /api/tickets/{ticketId}/triage/{triageId}/reject
```

Duplicates:

```http
GET /api/tickets/{ticketId}/duplicates
GET /api/tickets/{ticketId}/duplicates/links
POST /api/tickets/{ticketId}/duplicates/{duplicateOfTicketId}
```

Audit and dashboard:

```http
GET /api/tickets/{ticketId}/audit-logs
GET /api/dashboard
```

## Design Choices

- AI recommendations are advisory until a reviewer approves them.
- Rejection leaves the AI result in history and opens a manual triage path.
- Manual triage creates its own triage result with `modelName` set to `manual`.
- Duplicate detection is advisory. Marking a duplicate creates a persisted duplicate link and audit entries on both tickets.
- The duplicate-mark audit currently uses `DUPLICATE_CHECKED` to remain compatible with the current database constraint. A staged Flyway migration adds `DUPLICATE_MARKED` for a future runtime migration step.
- Flyway dependencies are intentionally isolated in the inactive Maven `flyway` profile. The application still relies on the current Hibernate/PostgreSQL setup unless that profile and runtime migration strategy are enabled later.

## Verification

Backend tests:

```bash
cd backend
./mvnw test
```

Frontend checks:

```bash
cd frontend
npm run lint
npm run build
```

The current backend test suite covers ticket creation, AI triage, Gemini response parsing, approval/rejection, manual triage, duplicate detection, duplicate links, and dashboard metrics.

## What I Would Add Next

The core workflow is in place, so the next improvements would be about making it more production-like:

- Add authentication and reviewer identity instead of free-form reviewer names
- Add unmark/resolve duplicate actions
- Add OpenAPI documentation
- Activate Flyway after creating a deliberate baseline strategy
- Add GitHub Actions for backend tests and frontend lint/build
- Improve duplicate matching with embeddings or PostgreSQL full-text search
- Add deployment documentation for a cloud target
