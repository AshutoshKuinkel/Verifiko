# Payments Architecture

### A breif overview on the the flow/invariants of the payment system used to purchase 'credits', the currency for verifiko.

#### Primary goals:

- Prevent double charges (idempotency)
- Make stripe our source of truth for payment outcomes
- Ensure on purchase, credits are added to balance only once.

### <u>High Level Flow</u>

![alt text](./assets/high-level-flow-phase1.png)
![alt text](./assets/high-level-flow-phase2.png)

#### <u>Invariants (Hard Rules):</u>

- Credits are ONLY granted by the Stripe webhook handler
- Client responses are never trusted for payment success
- Webhook processing MUST be idempotent
- Payments start as PENDING and transition once

#### <u>Payment States</u>

PENDING (initial state), then either:

- SUCCEEDED (payment_intent.succeeded)
- FAILED (payment_intent.payment_failed)

#### _This API should only be called when user is purchasing credits._

#### <u>Explicity Not Supported:</u>

- User-initiated credit refunds (make this very clean in UI)
- Granting credits synchronously during payment creation
- Trusting client-side payment success callbacks

#### _For future reference we can consider anything that allows for credits being given outside the stripe webhook a security bug._

For @v2, evaluate refactoring toward a DB-first (database as source of truth)
payment model similar to the reference below, if operational or audit
requirements increase:
https://medium.com/@bharathdayals/building-a-spring-boot-stripe-checkout-redis-idempotency-system-complete-guide-58f063dbb244

<b>_Consideration for improving this at somewhat of a larger scale:_</b>

- Synchronous processing is risky at bursts
  Right now: webhook → verify sig → deserialize → DB lookup + update + creditService call → all inside the HTTP request.
  If DB is slow (contention, indexing missing), or creditService.addPurchasedCredits does heavy work (e.g., recalculates balances, sends emails), or you get 100 events in 10 seconds → endpoint can timeout / hang → Stripe retries → backlog grows.
  Fix (strongly recommended): Switch to queue-based async processing.
  In processWebhook: after verification + basic deserialization check → enqueue the event ID (or minimal payload) to Redis list / Spring Kafka / RabbitMQ / AWS SQS / whatever you already use.
  Return 200 immediately.
  Have a background worker (e.g., @Scheduled + Redis polling, or Spring Cloud Stream, or dedicated consumer) that dequeues and calls handleSuccessPayment / handleFailedPayment.
  This decouples ingestion from work → absorbs spikes → easier to scale workers horizontally.
