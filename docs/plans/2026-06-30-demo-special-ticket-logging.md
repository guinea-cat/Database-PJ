# Demo Special Ticket And Logging Implementation Plan

> **For Codex:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add a demo-safe special ticket mechanism, backend audit logs, and clear database/privacy demonstration steps for the airline ticketing course project.

**Architecture:** Keep the ER-first structure and existing Controller-Service-Repository layering. Add the special-ticket flag to `FlightSegment`, because segment is the smallest sellable unit; calculate displayed and ordered prices in backend service/DTO code so passenger UI, admin UI, and ticket settlement stay consistent.

**Tech Stack:** Java 17, Spring Boot 2.7, Spring Data JPA, MySQL, React/Vite/TypeScript.

---

### Task 1: Backend Special Ticket Tests

**Files:**
- Modify: `backend/src/test/java/com/example/airticket/service/TicketServiceTest.java`
- Modify: `backend/src/test/java/com/example/airticket/service/AdminServiceTest.java`
- Create: `backend/src/test/java/com/example/airticket/dto/FlightSearchItemResponseTest.java`

**Steps:**
1. Add a failing test that a special `FlightSegment` economy ticket stores `PriceAmount = 500.00` and `PaymentAmount = 500.00` for a normal member when original economy price is `1000.00`.
2. Add a failing test that a VIP special ticket stores `PriceAmount = 500.00` and `PaymentAmount = 450.00`.
3. Add a failing test that `AdminService.saveSegment` copies `request.isSpecialOffer` into `segment.isSpecialOffer`.
4. Add a failing DTO test that `FlightSearchItemResponse.from(segment)` exposes `isSpecialOffer = true`, `originalEconomyPrice = 1000.00`, and `economyPrice = 500.00`.
5. Run targeted Maven tests and confirm they fail because the field/logic is missing.

### Task 2: Backend Special Ticket Implementation

**Files:**
- Modify: `database/schema.sql`
- Modify: `database/seed_data.sql`
- Modify: `backend/src/main/java/com/example/airticket/entity/FlightSegment.java`
- Modify: `backend/src/main/java/com/example/airticket/dto/request/SegmentSaveRequest.java`
- Modify: `backend/src/main/java/com/example/airticket/dto/response/FlightSearchItemResponse.java`
- Modify: `backend/src/main/java/com/example/airticket/dto/response/TicketResponse.java`
- Modify: `backend/src/main/java/com/example/airticket/service/AdminService.java`
- Modify: `backend/src/main/java/com/example/airticket/service/TicketService.java`

**Steps:**
1. Add `IsSpecialOffer BOOLEAN NOT NULL DEFAULT FALSE` to `FlightSegment`.
2. Seed at least one visible route as special for demo.
3. Add `isSpecialOffer` to entity/request/response DTOs.
4. Add `originalFirstClassPrice` and `originalEconomyPrice` to search response so the UI can show original price and discounted price.
5. Calculate special price as original price times `0.50`, then calculate VIP settlement as special price times `0.90`.
6. Include `isSpecialOffer` on ticket responses for order display.
7. Run targeted Maven tests and confirm they pass.

### Task 3: Backend Logging

**Files:**
- Modify: `backend/src/main/resources/application.properties`
- Modify: `backend/src/main/java/com/example/airticket/service/AuthService.java`
- Modify: `backend/src/main/java/com/example/airticket/service/TicketService.java`
- Modify: `backend/src/main/java/com/example/airticket/service/AdminService.java`
- Modify: `backend/src/main/java/com/example/airticket/service/ExpiredOrderService.java`

**Steps:**
1. Configure file logging to `logs/airticket-backend.log`.
2. Add `Logger` fields in services.
3. Log key state changes: register, login success/failure, create order, pay, refund, change, flight/segment save, disable/enable flight, expire pending orders.
4. Do not log plaintext passwords or plaintext ID numbers.

### Task 4: Frontend Special Ticket UI

**Files:**
- Modify: `frontend/src/types.ts`
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/styles.css`
- Modify: `frontend/src/App.source.test.ts`

**Steps:**
1. Add `isSpecialOffer`, `originalEconomyPrice`, `originalFirstClassPrice` to `FlightSearchItem`.
2. Add `isSpecialOffer` to `FlightSegment` and `Ticket`.
3. Add an admin segment checkbox for special tickets.
4. Add special-ticket badge and original-vs-discounted price display in passenger search cards.
5. Add special marker in order/ticket rows.
6. Run frontend tests and build.

### Task 5: Demo Instructions

**Files:**
- Create: `docs/demo-database-privacy-and-logs.md`

**Steps:**
1. Document MySQL Workbench and command-line ways to show the `User`, `FlightSegment`, and `TicketSale` tables.
2. Include exact SQL for showing encrypted `PasswordHash` and SHA-256 `IdNumberDigest`.
3. Include exact SQL for showing `IsSpecialOffer` and the resulting ticket settlement fields.
4. Include how to tail/open `backend/logs/airticket-backend.log`.

### Task 6: Verification

**Commands:**
- `cd backend && mvn test`
- `cd frontend && npm test`
- `cd frontend && npm run build`

**Expected:** All tests pass and frontend build completes without TypeScript errors.
