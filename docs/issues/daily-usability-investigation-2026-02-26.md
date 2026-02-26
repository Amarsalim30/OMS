# Daily Usability Investigation (2026-02-26)

## Why this exists
Client feedback indicates the app still feels hard to use in daily operations. This document defines a concrete investigation plan to identify where daily friction happens, measure it, and convert findings into prioritized fixes.

## Direct client feedback (2026-02-26)
- Current app has too many steps compared to the paper-book method ("mark paid" quickly in one place).
- Back navigation feels confusing and inconsistent.
- Add-order form feels too large/heavy and intimidating during fast entry.
- List views need better scan speed and clearer actions for daily use.

## Investigation goals
- Identify the top usability blockers in the client's daily workflow.
- Quantify baseline usability for core tasks (time, errors, retries, help needed).
- Produce a prioritized, evidence-backed fix list (P0/P1/P2).
- Re-test after fixes to confirm measurable improvement.

## Scope
In scope:
- High-frequency daily flows in `Orders`, `Customers`, and `Money`.
- Navigation clarity, search/findability, form entry friction, and confidence/feedback signals.

Out of scope:
- Rare admin-only workflows.
- Cosmetic-only tweaks with no measurable impact on task success/speed.

## Participants and session setup
- Primary participant: client (daily power user).
- Optional participants: 1-2 staff members who also execute daily tasks.
- Session type: moderated think-aloud on real device.
- Duration: 60-90 minutes.
- Environment: normal working context and realistic sample data.

## Priority workflows to test
1. Create a new order for an existing customer.
2. Create a new walk-in order.
3. Find an unpaid order and mark as fully paid in the fewest steps possible.
4. Find a customer and verify current balance quickly.
5. Edit an order amount/notes and confirm totals update correctly.
6. Switch between `Orders`, `Customers`, and `Money` without losing context.
7. Use back navigation repeatedly across screens and return to expected previous context.

## Task script
Use neutral prompts and do not guide clicks unless the participant is fully blocked.

1. "Add today's order for customer X with total KES Y and pickup time Z."
Success: order saved with correct customer, amount, and date.

2. "Show me all unpaid items and mark one as paid as fast as possible."
Success: payment posted correctly with no confusion and minimal taps.

3. "Find customer X and tell me what they currently owe."
Success: user can state current balance confidently within 30 seconds.

4. "Update this order's amount/notes, then check if totals are correct."
Success: edits persist and downstream balances reflect the change.

5. "From where you are now, jump to another module and return to continue."
Success: user returns to task state with no confusion or rework.

6. "Use back to return to where you started from this workflow."
Success: back behavior matches user expectation without accidental exits or tab confusion.

## Baseline metrics to capture
| Metric | Definition | Target |
|---|---|---|
| Task completion rate | % tasks finished without moderator takeover | >= 90% |
| Time on task | Median time to complete each task | -25% from current baseline |
| Critical errors | Errors causing wrong data/state or failed completion | 0 per session |
| Non-critical errors | Mis-taps, backtracks, retries | Downward trend by task |
| Help requests | Times user asks "what do I do now?" | <= 1 per session |
| Confidence rating | 1-5 self-rating after each task | >= 4 average |
| Taps to complete payment | Number of taps from unpaid list to completed payment | Closest possible to paper workflow baseline |
| Back navigation failures | Unexpected exits/wrong destination when using back | 0 per session |

## Observation log template
| ID | Workflow | Screen/Route | User behavior | Evidence | Impact | Severity | Hypothesis | Candidate fix |
|---|---|---|---|---|---|---|---|---|
| F-01 |  |  |  | timestamp/screenshot | time, error, confusion | P0/P1/P2 | why this happened | first fix idea |

## Severity rubric
- P0: Task cannot be completed or causes incorrect financial data.
- P1: Task completes with major delay/confusion or repeated error risk.
- P2: Minor friction, cosmetic confusion, or low-frequency annoyance.

## Analysis method
1. Cluster findings by root cause:
   - Findability
   - Terminology/copy clarity
   - Form and input friction
   - Navigation/state loss
   - Feedback/confirmation gaps
   - Procedure overhead vs paper workflow
2. Rank by severity x frequency x business impact.
3. Convert each finding into an issue with:
   - Reproduction steps
   - Evidence link (timestamp/screenshot)
   - Proposed fix
   - Owner and due date

## Focus areas derived from client input
### A. Procedure overhead vs paper book (P0 candidate)
- Validation question: how many steps/taps are required to "mark paid" from unpaid list?
- Candidate direction:
  - Introduce a direct `Mark Paid` quick action from list rows.
  - Defer advanced allocation choices behind secondary actions.
  - Default to smartest safe behavior for common full-payment scenario.

### B. Back navigation complexity (P0 candidate)
- Validation question: does back always return to the user's last logical context?
- Candidate direction:
  - Standardize back-stack behavior across tabs and detail screens.
  - Preserve list state (search/filter/scroll) when returning from detail/editor.
  - Reduce dead-end screens and unexpected app exits.

### C. Add-order form feels too big/heavy (P1 candidate)
- Validation question: can users enter routine orders quickly without scrolling fatigue?
- Candidate direction:
  - Progressive disclosure (show only essential fields first).
  - Compact layout for high-frequency fields.
  - Keep optional fields collapsed by default.

### D. Listing UX needs better daily scan speed (P1 candidate)
- Validation question: can users identify priority rows (due today/overdue/paid) in under 3 seconds?
- Candidate direction:
  - Stronger visual hierarchy for amount/status/date.
  - Clear primary row action and reduced secondary clutter.
  - Consistent row patterns across Orders and Customers lists.

## Deliverables
- Updated issue docs in `docs/issues` for each validated finding.
- A prioritized action list for the next implementation cycle (P0 first).
- A short before/after metric comparison after fixes are deployed.

## Proposed timeline
- Day 0: prepare script, test data, device recording.
- Day 1: run baseline usability session.
- Day 2: synthesize findings and create issue docs.
- Day 3-5: implement P0/P1 fixes.
- Day 6-7: re-test the same tasks and compare metrics.

## Exit criteria
Investigation is complete when:
- Top daily pain points are documented with direct evidence.
- Every P0/P1 finding has an owner and planned fix.
- Follow-up session shows improved completion rate and reduced task time/errors.
