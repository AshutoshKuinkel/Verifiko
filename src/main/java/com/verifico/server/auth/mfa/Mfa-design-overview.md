# MFA Domain (Internal)

## Purpose

`auth.mfa` is an internal security domain responsible for enforcing email-based multi-factor authentication for sensitive actions.
It is not exposed via its own controller.
MFA is invoked from existing authentication and account flows (e.g. login, password change) and acts as a security gate before the action completes.

## High-Level Architecture

```
AuthService / UserService
        ↓
     MfaService
        ↓
     MfaRepository
        ↓
         Mfa
```

* Business services call `MfaService`
* `MfaService` owns all MFA logic
* `Mfa` persists challenge state
* No MFA logic should exist outside this domain

## v1 Model – Email OTP

### Mechanism

* 6-digit alphanumeric code
* Sent via email
* Valid for 10 minutes
* Single-use
* Attempt-limited
* Stored hashed in the database

### Security Guarantee

* The raw MFA code is generated server-side.
* It is sent to the user via email.
* The raw code is never persisted.
* Only a hash of the code is stored in the `Mfa` entity.
* The only party that ever sees the raw code is the user who initiated the action.

Validation is performed by hashing the submitted code and comparing it against the stored hash.
This ensures that database exposure does not reveal usable MFA codes.

### Entity Model

Each `Mfa` row represents a single issued challenge.

Core properties:

* `user`
* `mfaCode` → stores hash, not plaintext
* `expiresAt`
* `used`
* `attempts`

Each row = one issued challenge.

## Flow Integration

### 1. Generation (Step-Up Required)

Called after primary verification succeeds (e.g. password match).

```
verifyPassword();
mfaService.generate(user);
return TWO_FACTOR_REQUIRED;
```

At this stage:

* No JWT issued
* No password updated
* Action is paused pending MFA verification

### 2. Validation (Action Completion)

```
mfaService.validate(user, submittedCode);
performSensitiveAction();
```

Validation enforces:

* Not expired
* Not already used
* Attempts below threshold
* Hash(submittedCode) matches stored hash

On success:

* Mark challenge as used
* Allow action to proceed

## Design Characteristics

* MFA is an internal cross-cutting security boundary
* Stateful (v1) due to email challenge model
* Centralized in `MfaService`
* Designed to allow migration to TOTP without breaking callers

## Evolution Path (v2 – TOTP)

v2 will replace stored challenges with a per-user shared secret:

* Store `totpSecret` on the user
* Compute expected code at verification time
* No per-challenge database rows required

`MfaService` remains the abstraction layer, allowing the underlying mechanism to change without affecting `AuthService` or `UserService`.