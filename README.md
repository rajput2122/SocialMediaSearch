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
GET /api/v1/search?q={query}&type={type}&size={size}&cursor={cursor}
```

**Authentication:** HTTP Basic Auth required for all requests.

| Parameter | Required | Values |
|---|---|---|
| `q` | Yes | Any non-blank string |
| `type` | Yes | `USER`, `POST`, `PAGE`, `TAG`, `LOCATION` |
| `size` | No | Page size (default `10`, max `100`) |
| `cursor` | No | Opaque cursor returned as `nextCursor` from a previous page |

**Pagination:** uses Elasticsearch `search_after` with a `(_score desc, _id asc)` sort. Pages are stepped forward via the `cursor` param — no random page access. Sidesteps ES `from`/`size` deep-page memory cost and the default `index.max_result_window` of 10,000.

**Response envelope**

```json
{
  "data": {
    "content": [...],
    "size": 10,
    "totalHits": 1234,
    "nextCursor": "WzEuNSwiMSJd"
  },
  "error": null,
  "meta": { "timestamp": "2026-04-15T10:00:00Z" }
}
```

`nextCursor` is `null` on the last page.

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

# Step to next page using the cursor returned by the previous response
curl -u atulk:password123 "http://localhost:8080/api/v1/search?q=spring&type=POST&size=10&cursor=WzEuNSwiMSJd"

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
