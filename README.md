# Payment Gateway

This API is a payment gateway (implemented as a SpringBoot application) that provides two endpoints, a POST /payment
endpoint for sending payment requests and a GET /payment endpoint for getting payment data for a specific payment request.

## Requirements
- Java 17
- Docker

## API

To manually test the application start the bank simulator and the application

```shell
docker compose up -d
./gradlew bootRun
```

When the application is running, the Swagger Doc/UI is available at <http://localhost:8090/swagger-ui/index.html>.

### Process a payment

`POST /payment` The below is a valid example request body 

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

Authorized and Declined payments return `201 Created` and payments details are stored in-memory (the card number is 
sanitised to store the last four digits only). An example of the JSON snippet stored in memory is provided below.
Note: The stored payment `id` can be used with `GET /payment/{id}`.

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

Invalid requests return `400 Bad Request` with the message `Rejected`. They are not sent to the
bank or stored.

### Idempotency
  
The POST endpoint accepts an optional `Idempotency-Key` header:

```shell
curl -X POST http://localhost:8090/payment \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: 67d39dc7-2430-4734-b74b-268bb455c27d' \
  -d '{"card_number":"2222405343248877","expiry_month":12,"expiry_year":2099,"currency":"GBP","amount":100,"cvv":"123"}'
```

Merchants should generate one unique key (typically a UUID) for each payment and
reuse it only when retrying that payment. Repeated requests with the same key return the first stored
authorized or declined response without calling the bank again. 

This is a deliberately simple, in-memory implementation. I've added this as I believe idempotency keys are a key 
part of a payment flow and I saw something similar in the checkout.com API documentation: https://api-reference.checkout.com/tag/Payments/
We can discuss this more during the interview

### Retrieve a payment

`GET /payment/{id}` returns payment details or `404 Not Found` when the payment does not exist.

Use the `id` returned when the payment was created:

```shell
curl -X GET http://localhost:8090/payment/fc328041-18cd-46eb-8b89-eaad6aee7757 \
  -H 'Content-Type: application/json'
```

Example response:

```json
{
  "id": "fc328041-18cd-46eb-8b89-eaad6aee7757",
  "status": "Authorized",
  "currency": "GBP",
  "amount": 100,
  "last_four": "8877",
  "expiry_month": 12,
  "expiry_year": 2099
}
```

## Validation and assumptions

- I've followed the request and response schemas specified in the challenge, I've summarised these below:
  - Card numbers must contain 14–19 numeric characters.
  - CVVs must contain 3–4 numeric characters.
  - The expiry month must be 1–12 and the combined expiry date must be in the future.
  - Supported currencies are GBP, USD, and EUR.
  - Amounts are integers and must be greater than zero.
  - Negative amounts could form part of refund behavior but I've decided this is beyond the scope of this excercise.

The full card number and CVV are used only for the inital request to the acquiring bank. They are not stored,
returned, or logged. For the long card number, only the last 4 digits are stored and returned as part of the /Payment (GET request)

## Bank integration

The supplied Mountebank simulator listens on `http://localhost:8080/payments`:

This was supplied as part of the intial challenge code and I'm reconfirming the behavior below:

- Card numbers ending in an odd digit are Authorized.
- Card numbers ending in 2, 4, 6, or 8 are Declined.
- Card numbers ending in 0 produce a 503 response, which the gateway maps to 502 Bad Gateway.

The bank URL and timeouts can be changed through `bank.base-url`, `bank.connect-timeout` and `bank.read-timeout`.

## Testing and formatting

Apply formatting with the below command:

```shell
./gradlew spotlessApply
```

Run unit-tests with 

```shell
./gradlew test
```

Run the local acceptance suite with (but Docker must be installed locally and running on the user's machine):

```shell
./gradlew localAcceptanceTest
```

## Design Decisions

- I've extended the existing Spring Boot application to create two endpoints - a /payment (POST endpoint) and a /payment/{id} (GET endpoint) 
- The GET endpoint /payment/{id} requires a path variable and the POST /payment endpoint requires a valid request in its request body 
- I've also extended the error handling pattern that was in place - if an exception reaches the controller - this is mapped into an http response via the logic in the commonExceptionHandler class
- The acquiring-bank client is an interface (my thinking here is the SOLID principle - rely on abstractions not concretions) this also aids testability 
- I've decided that retries, authentication, refunds, and saving data to a database or external redis cache are outside the scope of this exercise - but we can discuss this (and how this could be API could be extended/productionized) during the interview
- I have included a minimal implementation of idempotency keys, the implementation as is only works within a single instance. Production-grade idempotency would require more thought/implementation: e.g. shared durable storage, processing states, uniqueness constraints, ect...
