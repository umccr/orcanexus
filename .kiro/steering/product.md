---
inclusion: always
---
# OrcaNexus — Product Overview

OrcaNexus is the **Warehouse Data API** for UMCCR: a backend service that exposes
curated data-warehouse content (genomics LIMS metadata, sequencing artifacts, and
workflow records) over a versioned REST API.

- Consumers query read-oriented endpoints under `/api/v1/...` (e.g. `/api/v1/lims`,
  `/api/v1/system/info`).
- OpenAPI / Swagger UI is served at `/swagger-ui/index.html` and `/v3/api-docs`.
- Data originates from an Aurora PostgreSQL data warehouse; the warehouse "mart"
  schema surfaces entities such as `lims`, `fastq`, `bam`, `vcf`, `workflow`,
  `workflow_output`, and `workflow_cost_report`.

Project docs: https://github.com/umccr/orcahouse-doc
Issues: https://github.com/umccr/orcahouse-project/issues

## Scope & intent

- Primarily a **read / reporting API** over warehouse data. Favor query clarity and
  correctness over write/transactional concerns.
- Treat this as a data-serving tier, not the system of record. The upstream
  warehouse owns data production; OrcaNexus exposes it.
