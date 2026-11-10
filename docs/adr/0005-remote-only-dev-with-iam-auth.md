# 5. Remote-only dev with IAM database auth

- Status: Accepted
- Date: 2026-11-10 (recorded retroactively)

> The setup is stated in the former `README_DEV.md` "Concepts" section. The reasoning is
> inferred.

## Context

The data lives in Aurora PostgreSQL in a private network. A local copy of the warehouse is
impractical and would add data-handling risk.

## Decision

Develop against the remote dev database only; there is no local database.

- Reach it through an SSM tunnel to a small EC2 instance in a private subnet. AWS VPN may
  follow at around 10 developers.
- Authenticate with IAM database auth (15-minute tokens) as a read-only user, through the AWS
  Advanced JDBC Wrapper.
- Use no static passwords.

## Consequences

- Developers need an AWS session and the tunnel to run the app.
- No credentials to leak or rotate by hand.
- Offline development is not possible.
