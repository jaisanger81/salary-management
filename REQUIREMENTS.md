# Salary Management — Product Requirements

**Status:** Scope baseline · **Primary user:** ACME HR Manager · **Scale:** 10,000 employees across multiple countries

## Goal

Replace spreadsheet-based salary administration with a secure, understandable web application that lets an HR manager find employee records, review salary information in its original currency, and answer basic workforce-pay questions without losing the context of country or currency.

## In scope (first usable release)

- A responsive HR dashboard with total headcount, employee search, country and department filters, and clear paginated results.
- A salary overview grouped by currency. Never add amounts in different currencies as if they were comparable; no exchange rates are supplied by the assignment.
- Employee records with identity, job/department, country, currency, and annual base salary.
- An HR workflow to update an employee's annual base salary, with input validation, confirmation, and an auditable before/after history.
- A reproducible seed command that creates exactly 10,000 fictional employees; repeated runs do not duplicate records.
- A documented JSON REST API, relational PostgreSQL schema, focused automated tests, and local development instructions.
- A modular-monolith architecture using Java, Quarkus, Apache Camel, PostgreSQL, and React.

## Deliberately out of scope

- Authentication, authorization, approval workflows, and production deployment: no identity provider, organization-specific access policy, or hosting target was provided. The demo is not safe for real payroll data until these are implemented.
- Payroll calculation, tax/benefits, payslips, compensation recommendations, and salary-band policy: these require jurisdictional and business rules absent from the prompt.
- Currency conversion or a cross-currency total: no trusted rate source, rate timestamp, or conversion policy was supplied.
- Bulk spreadsheet import/export, employee onboarding, and deletion: not needed to demonstrate the core workflow and require file validation/data-retention decisions.
- Multi-tenant organizations and global high availability: the stated 10,000-employee workload is served by one organization and one relational database.

## Success criteria

1. A clean local setup can start PostgreSQL, the API, and the UI using the documented commands.
2. A seed operation creates 10,000 distinct, deterministic employees and is safe to repeat.
3. The HR user can search/filter/page employees and view salary totals segmented by currency.
4. A valid salary change persists and records the previous and new values; invalid values are rejected without changing data.
5. Core rules and API behavior have repeatable automated tests.

## Assumptions and safeguards

- Salary means **annual base salary**, stored as `NUMERIC(14,2)` plus an ISO-style currency code; each amount is displayed with its own currency.
- Employee data is synthetic. Never load real employee compensation into this demo.
- The seed dataset is fictional and stable so screenshots, tests, and demonstrations are reproducible.
- API paging has a bounded page size. Search is intended for 10,000 rows and is backed by database indexes where useful.
