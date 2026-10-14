# AI Workflow Record

The assignment asks for AI-assisted development and transparent engineering judgment. Complete this record with the tools and prompts you actually used; do not invent a transcript.

## Tools used

- Assistant/tool: Codebuff coding agent (GPT-6 Luna), terminal and file-editing tools.
- Date/session: 2026-10-14.
- Human review performed: Jai should review generated changes, compare them with the assignment, and fill in additional tools/prompts used before submission. Do not claim review not yet performed.

## Prompt and decision log

| Task | Prompt or intent (summarize accurately) | AI contribution | My verification / decision |
|---|---|---|---|
| Requirements and architecture | Turn the assignment into explicit product scope and a comprehensible architecture using Quarkus, Camel, PostgreSQL, and React. | Drafted requirements, architecture, trade-offs, and interview guide. | Checked these against the attached assessment; marked authentication/deployment as exclusions. Jai should verify and amend. |
| Implementation | Build the end-to-end salary management application using the chosen stack and document it for an interview walkthrough. | Generated initial implementation and documentation drafts. | Backend/frontend checks and live local API/UI calls were run; fixes followed test failures. Jai should review all code and record any additional prompts/tools. |
| Test/design review | Run meaningful deterministic tests and verify the 10,000-person seed and core API/UI flows. | Drafted backend integration tests and UI component tests. | Verified repeatable seeding, search/pagination, salary update/history, invalid salary response, and React build. See README commands; Jai should independently reproduce before submission. |

## Quality safeguards

- Requirements and constraints remain the source of truth, not generated output.
- Review all generated code for correctness, security, maintainability, and consistency.
- Validate salary math with decimal types and currency-aware aggregation.
- Run deterministic tests and inspect failures; never hide a failing check.
- Do not include secrets or real employee data in prompts, code, or seed data.
- Report limitations and unrun checks honestly.
