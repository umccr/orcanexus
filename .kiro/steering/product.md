---
inclusion: always
---
# OrcaNexus — Product Overview

OrcaNexus is the **Warehouse Data API** for UMCCR: a backend service that exposes
curated data-warehouse content (genomics LIMS metadata, sequencing artifacts, and
workflow records) over a versioned REST API.

- Consumers query read-oriented endpoints under `/api/v1/...`. OpenAPI / Swagger UI
  (`/swagger-ui/index.html`, `/v3/api-docs`) is the source of truth for endpoints.
- Data originates from an Aurora PostgreSQL data warehouse ("mart" schema). Overview:
  `docs/architecture/overview.md`. Shared API behaviour: `docs/api/conventions.md`.

Project docs: https://github.com/umccr/orcahouse-doc
Issues: https://github.com/umccr/orcahouse-project/issues

## Scope & intent

- Primarily a **read / reporting API** over warehouse data. Favor query clarity and
  correctness over write/transactional concerns.
- Treat this as a data-serving tier, not the system of record. The upstream
  warehouse owns data production; OrcaNexus exposes it.
