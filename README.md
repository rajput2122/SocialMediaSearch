# Social Media Search

A Spring Boot REST API that provides full-text search across social media entities — users, posts, pages, tags, and locations — backed by Elasticsearch.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.3.5 |
| Primary store | H2 (in-memory, JPA) |
| Search index | Elasticsearch 8.13.0 |
| Auth | HTTP Basic Auth |
| Build | Maven (mvnw wrapper) |

---

## Prerequisites

- Java 17+
- Docker (for Elasticsearch)
- Maven wrapper is included — no local Maven install needed

---

## Setup

### 1. Start Elasticsearch

```bash
docker compose up -d
```

Verify it's healthy:

```bash
curl http://localhost:9200/_cluster/health
# expect "status":"green" or "yellow"
```

### 2. Start the application

```bash
./mvnw spring-boot:run
```

On startup the app seeds sample data automatically (2 users, 2 posts, 2 pages, 3 tags, 2 locations).

**Test credentials**

| Username | Password |
|---|---|
| `atulk` | `password123` |
| `priyas` | `password123` |

---

## API

### Search

```
GET /api/v1/search?q={query}&type={type}&page={page}&size={size}
```

**Authentication:** HTTP Basic Auth required for all requests.

| Parameter | Required | Values |
|---|---|---|
| `q` | Yes | Any non-blank string |
| `type` | Yes | `USER`, `POST`, `PAGE`, `TAG`, `LOCATION` |
| `page` | No | 0-based page index (default `0`) |
| `size` | No | Page size (default `10`) |

**Response envelope**

```json
{
  "data": {
    "content": [...],
    "totalElements": 2,
    "totalPages": 1,
    "size": 10,
    "number": 0
  },
  "error": null,
  "meta": { "timestamp": "2026-04-15T10:00:00Z" }
}
```

**Examples**

```bash
# Search users by name or username
curl -u atulk:password123 "http://localhost:8080/api/v1/search?q=atul&type=USER"

# Search posts by caption or tag
curl -u atulk:password123 "http://localhost:8080/api/v1/search?q=spring&type=POST"

# Search pages
curl -u atulk:password123 "http://localhost:8080/api/v1/search?q=java&type=PAGE"

# Search tags
curl -u atulk:password123 "http://localhost:8080/api/v1/search?q=microservices&type=TAG"

# Search locations
curl -u atulk:password123 "http://localhost:8080/api/v1/search?q=bengaluru&type=LOCATION"

# No credentials → 401
curl "http://localhost:8080/api/v1/search?q=atul&type=USER"

# Wrong password → 401
curl -u atulk:wrongpass "http://localhost:8080/api/v1/search?q=atul&type=USER"

# Blank query → 400
curl -u atulk:password123 "http://localhost:8080/api/v1/search?q=%20&type=USER"

# Invalid type → 400
curl -u atulk:password123 "http://localhost:8080/api/v1/search?q=atul&type=INVALID"
```

---

## Running Tests

```bash
# All tests
./mvnw test

# Single test class
./mvnw test -Dtest=SearchControllerTest

# Single test method
./mvnw test -Dtest=SearchControllerTest#searchShouldReturnWrappedPageResponse
```

---

## E2E Test Script

A self-contained bash script that exercises all search types and auth scenarios:

```bash
chmod +x scripts/e2e-test.sh
./scripts/e2e-test.sh
```

The script requires the app to be running (`./mvnw spring-boot:run`) with Elasticsearch up (`docker compose up -d`).

---

## Architecture

```
src/main/java/com/practice/socialmediasearch/
├── controller/       # REST layer — HTTP in, HTTP out
├── service/          # Business logic (SearchService, IndexingService)
├── repository/
│   ├── jpa/          # Spring Data JPA repositories (H2)
│   └── es/           # Spring Data Elasticsearch repositories
├── model/            # JPA entities (source of truth)
├── document/         # Elasticsearch documents (search index)
├── dto/              # Request/response objects
├── exception/        # Custom exceptions + GlobalExceptionHandler
├── config/           # SecurityConfig, ElasticsearchConfig, DataInitializer
└── common/           # ApiResponse wrapper
```

**Dual-write pattern:** every write goes to H2 (JPA) first, then Elasticsearch via `IndexingService`. H2 is the source of truth; ES is the search index.

**Search path:** all queries go directly to Elasticsearch — never to H2.
