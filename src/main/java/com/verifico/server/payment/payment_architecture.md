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

#### *This API should only be called when user is purchasing credits.*

#### <u>Explicity Not Supported:</u>
  - User-initiated credit refunds (make this very clean in UI)
  - Granting credits synchronously during payment creation
  - Trusting client-side payment success callbacks


#### *For future reference we can consider anything that allows for credits being given outside the stripe webhook a security bug.*


