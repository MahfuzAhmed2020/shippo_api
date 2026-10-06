# Shippo Shipping Label API

A beginner-friendly, enterprise-style Java 17 + Spring Boot application that rate-shops through Shippo, lets the user select a carrier/service, creates a PDF shipping label, and saves the completed shipment in MySQL.

## 1. Project overview

The application has three major flows:

```text
Browser / Swagger / Postman
          |
          v
 ShipmentController
          |
          v
 ShipmentService
     |           |
     |           +------------------> Shippo API
     v
ShipmentRepository
     |
     v
   MySQL
```

The frontend is served by Spring Boot from `src/main/resources/static/index.html`, so browser requests use same-origin URLs such as:

```javascript
fetch('/api/shipments/rates')
```

### Shipping workflow

```text
POST /api/shipments/rates
          |
          v
       Shippo
          |
          v
Return multiple rates + Shippo shipment ID
          |
          v
User selects a rate
          |
          v
POST /api/shipments/label
          |
          v
Shippo transaction purchases label
          |
          v
Save completed shipment in MySQL
          |
          v
Return tracking + label PDF URL
```

The `shippoShipmentId` is intentionally returned by the rates endpoint and sent back with the selected `rateId`. Shippo rates are associated with a particular shipment, so the application does not recreate the shipment during label purchase.

## 2. Technologies

- Java 17
- Spring Boot 3.5.6
- Maven
- Spring Web
- Spring Data JPA
- Hibernate
- MySQL
- Shippo Java client 3.2.0
- Springdoc OpenAPI 2.8.13
- HTML5 / CSS3 / JavaScript
- JUnit 5 / Mockito
- H2 for repository tests

### Why these versions?

Spring Boot 3.5.6 supports Java 17, and the Springdoc dependency used here is the Springdoc 2.x line intended for Spring Boot 3.x applications.

Shippo Java client 3.2.0 explicitly documents Java 7+ support and the `2018-02-08` Shippo API version, so Java 17 is compatible. Shippo's repository also states that the library is no longer actively maintained. This project therefore keeps the requested 3.2.0 SDK but treats it as a legacy integration dependency that should be reviewed before a production deployment.

## 3. Prerequisites

Install:

1. JDK 17
2. Maven 3.9+ recommended
3. MySQL 8.x
4. VS Code
5. A Shippo account and test API key

Verify Java and Maven in PowerShell:

```powershell
java -version
mvn -version
```

The Java version should report Java 17.

## 4. MySQL setup

Open MySQL Workbench or the MySQL command line and execute:

```sql
CREATE DATABASE IF NOT EXISTS shippo_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
```

The application uses:

```properties
spring.jpa.hibernate.ddl-auto=update
```

Therefore Hibernate creates/updates the `shipments` table automatically from the `Shipment` entity.

For this demo, use a MySQL user that can create/modify tables in `shippo_db`.

## 5. Environment variables

Never put the Shippo API key in Java source, HTML, JavaScript, `application.properties`, or Git.

The application imports an optional root `.env` file as properties, and `.env` is ignored by Git. Alternatively, set these variables in the environment of the process that starts Spring Boot.

PowerShell:

```powershell
$env:SHIPPO_API_KEY="YOUR_SHIPPO_TEST_KEY"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="YOUR_MYSQL_PASSWORD"
```

Verify that variables exist without printing their values:

```powershell
if ($env:SHIPPO_API_KEY) { "SHIPPO_API_KEY is configured" }
if ($env:DB_USERNAME) { "DB_USERNAME is configured" }
if ($env:DB_PASSWORD) { "DB_PASSWORD is configured" }
```

These variables apply only to the current PowerShell session. To make them persistent for future sessions, use Windows environment-variable settings or `setx` carefully.

## 6. Build

From the project root:

```powershell
mvn clean install
```

Run tests only:

```powershell
mvn test
```

## 7. Run

```powershell
mvn spring-boot:run
```

Or after building:

```powershell
java -jar target/shippo-api-1.0.0.jar
```

## 8. URLs

Frontend:

```text
http://localhost:8080/
```

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

## 9. REST endpoints

### Get rates

```text
POST /api/shipments/rates
```

Returns available rates. Each rate contains:

- rateId
- shippoShipmentId
- carrier
- service
- amount
- currency
- estimatedDays

### Create label

```text
POST /api/shipments/label
```

Uses the selected `rateId` and the associated `shippoShipmentId` to create the label.

### Get shipment

```text
GET /api/shipments/{id}
```

### Get all shipments

```text
GET /api/shipments
```

### Delete shipment

```text
DELETE /api/shipments/{id}
```

## 10. Postman testing

Base URL:

```text
http://localhost:8080
```

### A. Get rates

Method:

```text
POST
```

URL:

```text
http://localhost:8080/api/shipments/rates
```

Header:

```text
Content-Type: application/json
```

Body -> raw -> JSON:

```json
{
  "fromName": "Shippo Test",
  "fromStreet": "215 Clayton St.",
  "fromCity": "San Francisco",
  "fromState": "CA",
  "fromZip": "94117",
  "fromCountry": "US",
  "fromPhone": "+1 555 341 9393",
  "fromEmail": "test@example.com",
  "toName": "Test Customer",
  "toStreet": "Broadway 1",
  "toCity": "New York",
  "toState": "NY",
  "toZip": "10007",
  "toCountry": "US",
  "toPhone": "+1 555 555 5555",
  "toEmail": "customer@example.com",
  "length": 5,
  "width": 5,
  "height": 5,
  "weight": 2
}
```

Expected response shape:

```json
[
  {
    "rateId": "SHIPPO_RATE_ID",
    "shippoShipmentId": "SHIPPO_SHIPMENT_ID",
    "carrier": "USPS",
    "service": "Priority Mail",
    "amount": "9.54",
    "currency": "USD",
    "estimatedDays": "2"
  }
]
```

Do not assume the first item is cheapest. Compare the returned `amount` values and select the desired service.

### B. Create label

Method:

```text
POST
```

URL:

```text
http://localhost:8080/api/shipments/label
```

Header:

```text
Content-Type: application/json
```

Body:

```json
{
  "fromName": "Shippo Test",
  "fromStreet": "215 Clayton St.",
  "fromCity": "San Francisco",
  "fromState": "CA",
  "fromZip": "94117",
  "fromCountry": "US",
  "toName": "Test Customer",
  "toStreet": "Broadway 1",
  "toCity": "New York",
  "toState": "NY",
  "toZip": "10007",
  "toCountry": "US",
  "length": 5,
  "width": 5,
  "height": 5,
  "weight": 2,
  "shippoShipmentId": "SHIPPO_SHIPMENT_ID_FROM_RATES",
  "rateId": "SHIPPO_RATE_ID_FROM_RATES"
}
```

Expected response shape:

```json
{
  "shipmentId": 1,
  "status": "SUCCESS",
  "trackingNumber": "TRACKING_NUMBER",
  "carrier": "USPS",
  "service": "Priority Mail",
  "amount": "9.54",
  "currency": "USD",
  "labelUrl": "https://..."
}
```

### C. Get by ID

```text
GET http://localhost:8080/api/shipments/1
```

Expected: HTTP 200 with the saved shipment DTO.

### D. Get all

```text
GET http://localhost:8080/api/shipments
```

Expected: HTTP 200 with an array.

### E. Delete

```text
DELETE http://localhost:8080/api/shipments/1
```

Expected: HTTP 204 with no response body.

## 11. HTTP status codes

| Endpoint | Success | Typical errors |
|---|---:|---:|
| POST `/rates` | 200 | 400, 502 |
| POST `/label` | 201 | 400, 502 |
| GET `/{id}` | 200 | 404 |
| GET `/` | 200 | 500 |
| DELETE `/{id}` | 204 | 404 |

## 12. Database design

`Shipment` uses `BigDecimal` for dimensions, weight, and shipping cost because decimal values should not be represented using binary floating-point when exact decimal storage matters. `shippingCost` is stored with two decimal places.

`LocalDateTime` is used for `createdAt` because this is an application-local timestamp. A production multi-region system may prefer `Instant` plus UTC database handling.

The entity also stores Shippo shipment/rate/transaction IDs so an operator can correlate a local database row with the third-party transaction.

## 13. Security

The browser never receives the Shippo API key. JavaScript calls this application's `/api/...` endpoints, and the Spring service calls Shippo server-to-server.

This is important because anything embedded in browser JavaScript is visible to the browser user.

Environment variables provide a basic local-development secret boundary. In production, use a secrets manager such as Azure Key Vault, AWS Secrets Manager, GCP Secret Manager, or another approved enterprise secret store.

Also add secret files to `.gitignore` and never commit API keys to GitHub.

## 14. Shippo test mode

Use a Shippo test API key while developing. Test transactions are intended for testing and should not be treated as production postage. Shippo's legacy Java client documentation notes that test labels are free and that live labels are charged according to the applicable shipping setup.

Before using live credentials, review Shippo's current production requirements and carrier-account configuration.

## 15. Same-origin CORS behavior

No CORS configuration is required for the normal browser flow because Spring Boot serves both the HTML and REST API from the same origin:

```text
http://localhost:8080/
http://localhost:8080/api/shipments/rates
```

Using relative URLs also avoids hardcoding a host/port into JavaScript.

## 16. Testing

### Service unit test

`ShipmentServiceTest` tests validation before Shippo interaction. It does not make a real Shippo request.

Run:

```powershell
mvn -Dtest=ShipmentServiceTest test
```

### Repository test

`ShipmentRepositoryTest` uses H2 and `@DataJpaTest`, so it does not require a running MySQL server or Shippo account.

Run:

```powershell
mvn -Dtest=ShipmentRepositoryTest test
```

For production-level integration tests, add a dedicated test profile and a disposable MySQL instance, such as Testcontainers, and mock the external Shippo integration separately.

## 17. Troubleshooting

### `SHIPPO_API_KEY is not configured`

Set the variable in the same PowerShell window used to start Spring Boot:

```powershell
$env:SHIPPO_API_KEY="YOUR_SHIPPO_TEST_KEY"
```

### MySQL access denied

Check:

```powershell
$env:DB_USERNAME
$env:DB_PASSWORD
```

Then verify that the MySQL user can access `shippo_db`.

### Unknown database `shippo_db`

Run the SQL in `sql/01_create_database.sql`.

### Port 8080 already in use

Find the process:

```powershell
netstat -ano | findstr :8080
```

Then either stop the process or change:

```properties
server.port=8080
```

to another port.

### Shippo returns no rates

Check the addresses, parcel units, test account/carrier availability, and the Shippo response. Some carriers/services may not be available for every origin/destination combination.

### Label purchase says the rate is invalid/expired

Request fresh rates. Shippo documents that a rate must be sufficiently recent to purchase a label. The application deliberately preserves the `shippoShipmentId` from the rate-shopping step so it does not accidentally create a different shipment.

### Label purchase returns `Unable to create a shipping label with Shippo`

That message means the browser received the application's generic Shippo-integration error; the underlying cause is logged by the Spring Boot process. Check the terminal where the app is running for `Shippo API call failed while creating a shipping label` and read the exception immediately below it. Database-save failures are reported separately as server errors because Shippo may already have purchased the label.

Common causes include an expired rate (request rates again and purchase promptly), an invalid or unverified address, a parcel with incorrect dimensions/weight/units, a Shippo API key for the wrong mode, or a carrier account that is not enabled for the selected rate. Set `SHIPPO_API_KEY` in the same PowerShell session used to start the app, and never paste the key into browser code or share it in logs or screenshots.

### Label purchase returns `Shippo could not generate the label. Status: ERROR`

Shippo returned a failed transaction rather than a transport/API exception. The application logs the Shippo transaction ID and any validation messages returned by Shippo; it also includes those messages in the API error response. Restart the updated application, request fresh rates, and retry. Use the returned message to correct the address, parcel, or carrier configuration. Do not retry blindly if Shippo shows that a transaction or label was already created.

### Swagger is not found

Use:

```text
http://localhost:8080/swagger-ui/index.html
```

and confirm the application started successfully.

### SDK problems

Shippo's `shippo-java-client` 3.2.0 is a legacy client and is no longer actively maintained. If Shippo changes or retires the legacy API behavior, migrate the integration to Shippo's current SDK/OpenAPI-generated client or direct HTTP integration.

## 18. Recommended Git workflow

Before committing:

```powershell
git status
git add .
git commit -m "Create Spring Boot Shippo shipping label application"
```

Never commit:

```text
SHIPPO_API_KEY
DB_PASSWORD
.env
```
