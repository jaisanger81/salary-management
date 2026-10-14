# Interview Walkthrough: Salary Management

This guide is a companion to the implementation. Explain the reasoning, then point to the relevant code and tests in the repository.

## 60-second overview

“I built a small modular monolith for one HR team and 10,000 employees. React provides a searchable, paginated salary directory; Quarkus exposes a versioned REST API; application services enforce salary rules; Camel routes provide the requested integration boundary; PostgreSQL stores employee records and salary history. Salary changes and audit events are atomic. Summary values are grouped by currency because combining currencies without an exchange-rate policy would be incorrect. The dataset is synthetic and deterministic. Authentication and deployment are explicitly out of scope, so this is a demo, not production payroll software.”

## How a salary update works

1. HR chooses an employee and enters an annual salary and currency.
2. The UI validates required fields for usability and sends a `PUT` request; backend validation is authoritative.
3. The REST resource maps the request into a typed command and sends it through the Camel route.
4. The application service checks the employee exists, rejects negative amounts or invalid currency codes, and skips a no-op change.
5. A single database transaction updates the employee and inserts a before/after history row.
6. The API returns the updated record; the UI refreshes the employee and summary views or displays a useful error.

The database transaction prevents a partial result where the salary changes but the audit history does not (or vice versa).

## Why this design?

- **Why not microservices?** One team, one bounded workflow, and 10,000 rows do not justify service discovery, network failure modes, or distributed transactions.
- **Why Camel if this app only has PostgreSQL?** Camel is a required technology and centralizes routing at the integration boundary. A direct route keeps the demo synchronous and understandable. When an external HRIS, payroll system, or asynchronous import is added, adapters can be introduced without putting integration code into the React UI.
- **Why PostgreSQL?** Relational constraints, decimal money, transactions, aggregation, and indexing are a natural fit for employee and audit records.
- **Why group summary by currency?** A total of USD plus EUR is not meaningful. Conversion would need an agreed FX provider, rate timestamp, and policy; none was given.
- **Why seed deterministic synthetic users?** Reproducibility makes the 10,000-row acceptance criterion, tests, and demo repeatable without exposing personal data.
- **Why pagination?** It bounds response and browser memory and pushes filtering/aggregation to the database.

## Key invariants to explain

- Annual base salary is decimal (`NUMERIC`, Java `BigDecimal`), not a floating-point value.
- Employee number is unique; an employee belongs to one currency for the current salary record.
- A salary change records old value, new value, currency, and time.
- A salary edit is all-or-nothing; a failed request never creates a partial history entry.
- Search parameters are bounded and parameterized; UI checks do not replace backend validation.
- Seeded employees are clearly fictional; no real compensation data belongs in this repository.

## Important limitations and next steps

1. Add authentication and authorization before any real employee data is used; derive audit actor from verified identity.
2. Add approval/dual-control if the organization requires approval for salary changes.
3. Confirm salary conventions (annual/monthly, gross/net, effective date, currency policy) with HR and model effective-dated compensation if history requires it.
4. Add secure import/export only after defining validation, permissions, error reporting, and retention.
5. Add integration/e2e testing and measure realistic query plans before optimizing further.
6. Configure production TLS, secrets, backups, monitoring, logging policy, retention, and deployment after selecting hosting.

## Demo checklist

- Start PostgreSQL, backend, and frontend following the root README.
- Run seed and show the expected 10,000 employees.
- Search by a name/employee number, filter by country/department, and move to another page.
- Explain the separate currency cards/totals.
- Change one employee’s salary; refresh and show that it persisted and the salary history changed.
- Try a negative amount and explain that the backend rejects it.
- Walk through one service test, one REST test, and one UI test; explain what each proves.

## AI-assisted engineering note

The assessment expects intentional AI use. Keep a truthful record of prompts, generated suggestions, and your own review. Verify generated code against requirements, run checks after edits, inspect edge cases, and do not claim tests or deployment that you did not perform. Add your actual tool/model and prompts in `docs/AI_WORKFLOW.md` as you work; do not present a fictional transcript.
