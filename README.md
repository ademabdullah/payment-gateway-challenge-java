# Payment Gateway

A Spring Boot payment gateway that validates card payments, forwards valid requests to the
provided acquiring-bank simulator, and stores safe payment details in memory for retrieval.

## Requirements

- JDK 17
- Docker (Docker Compose is useful for manual operation but is not required by the test tasks)

## API

Start the bank simulator and application for manual use:

```shell
docker compose up -d
./gradlew bootRun
```

Swagger UI is available at <http://localhost:8090/swagger-ui/index.html>.

### Process a payment

`POST /payments`

```json
{
  "card_number": "2222405343248877",
  "expiry_month": 12,
  "expiry_year": 2099,
  "currency": "GBP",
  "amount": 100,
  "cvv": "123"
}
```

Authorized and Declined payments return `201 Created`, a `Location` header, and a safe payment
representation:

```json
{
  "id": "ea18a4cc-4fb3-4745-bc65-64c86560e743",
  "status": "Authorized",
  "last_four": "8877",
  "expiry_month": 12,
  "expiry_year": 2099,
  "currency": "GBP",
  "amount": 100
}
```

Invalid requests return `400 Bad Request` with status `Rejected` and field-level errors. They are
not sent to the bank or stored.

### Retrieve a payment

`GET /payments/{id}` returns the stored safe representation, or `404 Not Found` when the payment
does not exist.

## Validation and assumptions

- Card numbers must contain 14–19 numeric characters.
- CVVs must contain 3–4 numeric characters.
- The expiry month must be 1–12 and the combined expiry date must be in the future.
- Supported currencies are GBP, USD, and EUR.
- Amounts are integer minor units and must be greater than zero.
- Negative amounts could form part of refund behaviour in a broader payment API. Refunds require
  their own business semantics and are not implemented here.

The full card number and CVV are used only for the acquiring-bank request. They are never stored,
returned, or logged. Models containing those fields deliberately do not override `toString()`.

## Bank integration

The supplied Mountebank simulator listens on `http://localhost:8080/payments`:

- Card numbers ending in an odd digit are Authorized.
- Card numbers ending in 2, 4, 6, or 8 are Declined.
- Card numbers ending in 0 produce a 503 response, which the gateway maps to 502 Bad Gateway.

The bank URL and timeouts can be changed through `bank.base-url`, `bank.connect-timeout`, and
`bank.read-timeout`.

## Testing and formatting

Run formatting checks and all Docker-independent tests with:

```shell
./gradlew check
```

Apply the configured format with:

```shell
./gradlew spotlessApply
```

Run the end-to-end HTTP acceptance suite with:

```shell
./gradlew localAcceptanceTest
```

That task starts the configured Mountebank image with Docker on dynamically allocated localhost
ports, waits for it to become ready, runs the application on a random port, tests real HTTP calls
through the gateway to the simulator, and shuts the container down.

## Design considerations and limitations

- The controller is intentionally thin; the service coordinates the bank client and repository.
- The acquiring-bank client is an interface so application logic remains independently testable.
- The in-memory repository is concurrency-safe but intentionally does not survive restarts.
- Idempotency, retries, authentication, refunds, and a durable database are outside this exercise.
- There is an unavoidable authorization gap: the bank could authorize a payment and the gateway
  could fail before persistence. Without durable storage and idempotency, a retry could create
  another payment. A production gateway must address that failure mode; this assessment documents
  it rather than introducing incomplete guarantees.
