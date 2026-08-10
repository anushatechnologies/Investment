# Anusha Bazaar Backend

Spring Boot + MySQL backend implementing the admin-panel and investor-facing APIs described in `AnushaBazaar_Implementation_Plan.docx`.

## Stack

- Java 21
- Spring Boot 3.3
- Spring Security with JWT access tokens
- Spring Data JPA
- Spring Boot Actuator
- Springdoc OpenAPI / Swagger UI
- MySQL 8
- Multipart file upload to local `uploads/` storage

## Seeded Users

- `superadmin@anushabazaar.com` / `Admin@123`
- `admin@anushabazaar.com` / `Admin@123`

## Main API Groups

- `POST /api/auth/register`
- `GET /api/auth/verify-email`
- `POST /api/auth/login`
- `POST /api/auth/refresh-token`
- `POST /api/auth/logout`
- `POST /api/auth/forgot-password`
- `POST /api/auth/reset-password`
- `POST /api/auth/change-password`

- `POST /api/kyc/submit`
- `GET /api/kyc/status`
- `GET /api/admin/kyc/pending`
- `POST /api/admin/kyc/{id}/approve`
- `POST /api/admin/kyc/{id}/reject`
- `GET /api/admin/kyc/{id}/documents`

- `GET /api/plans`
- `GET /api/admin/plans`
- `POST /api/admin/plans`
- `PUT /api/admin/plans/{id}`
- `POST /api/admin/plans/{id}/deactivate`

- `POST /api/investments/apply`
- `POST /api/investments/{id}/upload-receipt`
- `GET /api/investments`
- `GET /api/investments/{id}`
- `POST /api/investments/{id}/cancel`
- `GET /api/admin/investments/pending`
- `POST /api/admin/investments/{id}/verify-receipt`
- `POST /api/admin/investments/{id}/activate`
- `GET /api/admin/investments`

- `GET /api/wallet`
- `GET /api/wallet/transactions`
- `POST /api/withdrawals/request`
- `GET /api/withdrawals`
- `GET /api/admin/withdrawals/pending`
- `POST /api/admin/withdrawals/{id}/approve`
- `POST /api/admin/withdrawals/{id}/process`
- `POST /api/admin/withdrawals/{id}/reject`

- `GET /api/referrals/tree`
- `GET /api/referrals/commissions`
- `GET /api/admin/interest/rates`
- `PUT /api/admin/interest/rates?planId={planId}`
- `POST /api/admin/interest/trigger`
- `GET /api/admin/dashboard`
- `GET /api/admin/users`
- `POST /api/admin/users/{id}/suspend`
- `GET /api/admin/fraud-alerts`
- `POST /api/admin/fraud-alerts/{id}/resolve`
- `GET /api/admin/audit-logs`
- `GET /api/admin/reports/monthly`
- `GET /api/notifications`
- `POST /api/notifications/{id}/read`
- `GET /api/dashboard`

## MySQL Setup

Default connection is configured in [application.yml](src/main/resources/application.yml):

- URL: `jdbc:mysql://localhost:3306/anushabazaar`
- Username: `root`
- Password: `2395`

Update these values before production use.

## Docs And Health

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Health: `http://localhost:8080/actuator/health`

## Run

This workspace currently does not have Maven installed, so the project has been scaffolded but not built here.

When Maven is available:

```bash
mvn spring-boot:run
```

## Notes

- File uploads are stored locally under `uploads/`.
- JPA is configured with `ddl-auto: update` for fast setup.
- JWT access tokens are enabled; refresh tokens are stored in MySQL.
- Admin approval flows, wallet ledger, referral commissions, fraud alerts, notifications, and audit logging are implemented in the service layer.
- Logging defaults are tuned down to reduce noisy framework and SQL output.
- API errors now return a consistent JSON structure for validation, forbidden, bad-request, and server errors.
