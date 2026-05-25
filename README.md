# Payment Reliability Platform

AI-enhanced distributed payment reliability platform built with Spring Boot, Kafka, Redis, MySQL, Resilience4j, Spring AI, Ollama, and Flyway.

## Overview

This project evolves a basic payment backend into a reliability-focused distributed system with an AI operational intelligence layer.

Core platform capabilities:

- asynchronous payment processing with Kafka
- durable payment state in MySQL
- Redis-backed rate limiting and payment caching
- Resilience4j retry and circuit breaker for downstream bank calls
- AI-assisted failure analysis, incident reporting, log summarization, and operational querying

## Architecture

```text
+--------+      +------------------+      +----------------------+      +------------------+
| Client | ---> | Payment API      | ---> | Kafka Topic          | ---> | Payment Consumer |
+--------+      +------------------+      | payment-created-events|      +------------------+
       |                 |                 +----------------------+                 |
       |                 v                                                          v
       |         +------------------+                                      +------------------+
       |         | MySQL Payments   | <----------------------------------- | Bank API Service |
       |         +------------------+                                      +------------------+
       |                 ^                                                          |
       v                 |                                                          v
+------------------+     |                                               +------------------+
| Redis Cache and  | <---+                                               | Spring AI Ops    |
| Redis Rate Limit |                                                     | Intelligence     |
+------------------+                                                     +------------------+
```

## Payment Flow

### Request flow

1. Client sends `POST /payments`
2. Redis rate limit is checked with `rate_limit:{userId}`
3. DB idempotency is checked using `idempotency_key`
4. Payment is stored as `PENDING`
5. Payment snapshot is cached in Redis
6. Kafka event is published after DB commit
7. API returns immediately

### Async processing flow

1. Kafka consumer receives `PaymentCreatedEvent`
2. Consumer fetches payment from MySQL
3. Consumer calls external bank processing through `BankApiService`
4. Resilience4j retry and circuit breaker guard the bank call
5. Payment becomes `SUCCESS` or `FAILED`
6. Updated payment is saved
7. Redis cache is refreshed

## AI Operational Layer

The AI integration is designed as operational tooling, not as a chatbot.

AI capabilities:

- failure analysis for a specific failed payment
- incident report generation for recent failure windows
- raw log summarization into SRE-readable insights
- natural language operational query answering

### AI workflow

```text
Operational API
  -> repository metrics and payment context
  -> prompt factory with sanitized operational input
  -> Spring AI ChatClient
  -> Ollama model
  -> structured operational response
```

### AI endpoints

- `POST /ai/analyze-failure`
- `POST /ai/generate-incident-report`
- `POST /ai/summarize-logs`
- `POST /ai/query`

## Package Structure

```text
src/main/java/com/sourabh/payment_platform
|-- ai
|-- config
|-- controller
|-- payment
|   |-- api
|   |-- cache
|   |-- client
|   |-- consumer
|   |-- domain
|   |-- event
|   |-- producer
|   |-- ratelimit
|   `-- service
`-- shared
    |-- api
    `-- exception
```

## API Examples

### Create payment

`POST /payments`

```json
{
  "userId": "user-123",
  "amount": 249.99,
  "idempotencyKey": "idem-001"
}
```

### Analyze failure

`POST /ai/analyze-failure`

```json
{
  "transactionId": "txn_123"
}
```

### Generate incident report

`POST /ai/generate-incident-report`

```json
{
  "minutes": 30
}
```

### Summarize logs

`POST /ai/summarize-logs`

```json
{
  "logs": "Kafka consumer retrying bank timeout for txn_123 ..."
}
```

### Operational query

`POST /ai/query`

```json
{
  "question": "Why are payments failing?"
}
```

## Kafka

- Topic: `payment-created-events`
- Producer: `PaymentEventProducer`
- Consumer group: `payment-service-consumer-group`
- Event payload: `PaymentCreatedEvent`

## Redis

### Rate limiting

- Key: `rate_limit:{userId}`
- Limit: `5`
- Window: `60 seconds`

### Payment caching

- Key: `payment:{transactionId}`
- TTL: `10 minutes`

## Spring AI Configuration

This project uses the Spring AI Ollama starter for local model execution.

Optional environment variables:

```bash
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_CHAT_MODEL=qwen3:4b
```

Default behavior:

- base URL: `http://localhost:11434`
- model: `qwen3:4b`
- pull strategy: `never`

## Prompt Design

Prompts are written for operational use cases:

- incident reporting
- payment failure analysis
- operational Q&A
- log summarization

Prompt characteristics:

- structured JSON response requirement
- concise SRE-oriented wording
- downstream system awareness
- sanitized input to avoid sending secrets

## Local Setup

### Prerequisites

- JDK 17
- Docker Desktop
- local Ollama instance with the selected model pulled

### Start infrastructure

```bash
docker compose up -d
```

### Start Ollama

Make sure Ollama is running locally and the selected model is available:

```bash
ollama pull qwen3:4b
ollama serve
```

### Run application

```powershell
./mvnw.cmd spring-boot:run
```

## Testing Guide

### Payment flow

1. Create a payment
2. Verify immediate `PENDING` response
3. Poll `GET /payments/{transactionId}`
4. Confirm eventual `SUCCESS` or `FAILED`

### AI flow

1. Create or identify failed payments
2. Call `POST /ai/analyze-failure`
3. Call `POST /ai/generate-incident-report`
4. Call `POST /ai/summarize-logs`
5. Call `POST /ai/query`

### Redis verification

```bash
docker exec -it payment-redis redis-cli
keys payment:*
keys rate_limit:*
```


