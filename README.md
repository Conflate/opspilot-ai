# OpsPilot AI

OpsPilot AI is an AI-assisted operational ticket triage platform built with Java Spring Boot, PostgreSQL, React, and Gemini.

The system helps operations teams summarize tickets, classify severity and category, recommend next steps, detect duplicates, and preserve human approval through audit logs.

## Core Design

AI recommendations are not applied automatically. A human reviewer must approve, reject, or edit the recommendation before it affects ticket state.

## Tech Stack

- Java 17
- Spring Boot 3
- PostgreSQL
- React
- Gemini API
- Docker
- JUnit