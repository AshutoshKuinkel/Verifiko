# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [0.1.0] - 2026-03-03

### Added
- Initial release — core functionality in progress, not yet production-ready
- CI/CD pipelines with SonarCloud, Trivy security scanning, and Docker Hub publishing
- Basic health and root endpoints with Swagger/OpenAPI documentation
- Docker, docker-compose, and K8s reference manifests

---

## [0.2.0] - 2026-03-07

### Added
- **MFA (multi-factor authentication)** — email-based one-time codes for sensitive actions
  - New `auth.mfa` domain: `Mfa` entity, `MfaService`, `MfaRepository`; no dedicated controller (MFA invoked from existing auth/user flows)
  - 6-character alphanumeric codes sent via email, valid 10 minutes, single-use, attempt-limited (max 5), stored hashed in DB
  - Login: two-step flow — request code (202) then submit code with same credentials to complete login and receive tokens
  - Change password (`POST /api/users/me/change-password`): MFA required before applying new password; request code then submit code + new password
  - Profile update (`PATCH /api/users/me`): MFA required before updating profile (e.g. email/username)
  - Scheduled cleanup job to delete expired MFA codes hourly

---

### [Unreleased]

- Integration tests (Testcontainers)
- Spring Boot Actuator with readiness/liveness probes
- Feed algorithm
- Video Support
- Validity Scoring for posts
- Audit logging
- OAUTH
- Rate-limiting
- Structured JSON logging
- Production deployment to Railway
- UptimeRobot + Sentry monitoring setup
- Semantic release automation
- Full core functionality completion

---