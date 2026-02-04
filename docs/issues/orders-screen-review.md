# Orders Screen Review Issues (Refined)

Scope: `app/src/main/java/com/zeynbakers/order_management_system/order/ui/UnpaidOrdersScreen.kt`

## Business/Client-Focused UI/UX Findings
1. Screen does not optimize for “fast triage” of money at risk.
   The list emphasizes customer identity and notes over outstanding amount, which is the primary business concern in an unpaid queue. The “Due” amount should be the most prominent element in each card to speed up daily cash collection decisions. References: `app/src/main/java/com/zeynbakers/order_management_system/order/ui/UnpaidOrdersScreen.kt:259-329`.

2. Card height is too large for high-volume days.
   Each card consumes 5–6 rows of vertical space (name, notes, progress, paid/due, button). In a real operations flow, this reduces throughput and increases time-to-find specific orders. Consolidate to 2 rows max. Suggested structure:
   Row 1: Customer + compact notes (single-line) + Due amount right aligned.
   Row 2: Small status pill (Unpaid/Partial) + Paid/Total ratio + a single icon action. References: `app/src/main/java/com/zeynbakers/order_management_system/order/ui/UnpaidOrdersScreen.kt:238-349`.

3. The “Record Payment” button on every row creates action fatigue.
   A large button per row suggests the primary task is to tap every row, which is unrealistic when scanning. Replace with a small trailing icon or show the button only on selection. This reduces cognitive load and increases scan speed. References: `app/src/main/java/com/zeynbakers/order_management_system/order/ui/UnpaidOrdersScreen.kt:335-348`.

4. Error red is overused for a default list state.
   Unpaid orders are expected here, so red reads as exception and creates visual noise. Use a stable business accent (e.g., tertiary or a brand amber) for “Due,” reserving error red for overdue, flagged, or failed states. References: `app/src/main/java/com/zeynbakers/order_management_system/order/ui/UnpaidOrdersScreen.kt:322-329`.

5. Notes are too verbose for client-facing scanning.
   Multi-line notes push financial info down. For business workflows, notes should be secondary: collapse to a single-line subtitle, with full notes on tap or long-press. References: `app/src/main/java/com/zeynbakers/order_management_system/order/ui/UnpaidOrdersScreen.kt:279-289`.

6. Progress bar is not actionable and adds visual weight.
   The bar adds a full row even when most entries are unpaid (0%). Replace with a compact “Paid/Total” ratio or show the bar only for partials. This improves density and relevance. References: `app/src/main/java/com/zeynbakers/order_management_system/order/ui/UnpaidOrdersScreen.kt:298-306`.

7. Summary card is visually dominant vs. the list.
   The summary card uses large typography and a thick accent strip, which pulls attention away from the actual unpaid queue. Business users need the list first. Use a slimmer summary bar or smaller typography. References: `app/src/main/java/com/zeynbakers/order_management_system/order/ui/UnpaidOrdersScreen.kt:155-197`.

8. Sticky headers lack strong separation for rapid scanning.
   The header background matches the list, so date groups blur together when scrolling quickly. Use a subtle surface variant or divider line to anchor sections. References: `app/src/main/java/com/zeynbakers/order_management_system/order/ui/UnpaidOrdersScreen.kt:205-216`.

9. Customer name is overly emphasized compared to payment risk.
   The name is styled with `primary`, competing with the due amount. In an unpaid view, payment data should be first; customer name should be neutral. References: `app/src/main/java/com/zeynbakers/order_management_system/order/ui/UnpaidOrdersScreen.kt:265-270`.

10. Walk-in entries are not distinctive enough.
   A small icon alone is easy to miss, making it harder to triage anonymous orders. Add a “Walk‑in” badge or lighter background tint to distinguish them without increasing height. References: `app/src/main/java/com/zeynbakers/order_management_system/order/ui/UnpaidOrdersScreen.kt:249-263`.

11. No explicit “Partial” or “Overpaid” signal in list rows.
   The list does not visually differentiate partial vs. fully unpaid, forcing the user to infer from the progress bar. Add a small pill/tag (“Partial”, “Credit”) or a ratio label to speed up decision making. References: `app/src/main/java/com/zeynbakers/order_management_system/order/ui/UnpaidOrdersScreen.kt:309-329`.

12. Date header format lacks business context for upcoming orders.
   Future-dated unpaid orders appear alongside today without a clear “Upcoming” or “Future” cue, risking missed prep for future deliveries. Add a subtle “Upcoming” tag or include the weekday more prominently. References: `app/src/main/java/com/zeynbakers/order_management_system/order/ui/UnpaidOrdersScreen.kt:374-383`.

13. Missing quick filters for high-priority action.
   Business users typically need “Largest due,” “Oldest unpaid,” or “Past due” filters. Without these, the queue is less actionable. Consider adding a compact filter row beneath the summary. References: `app/src/main/java/com/zeynbakers/order_management_system/order/ui/UnpaidOrdersScreen.kt:116-149`.

14. No visual indicator for overdue vs. upcoming.
   All dates look similar. A small badge (e.g., “Overdue”) on past dates or a thin red marker on overdue rows improves clarity for collections. References: `app/src/main/java/com/zeynbakers/order_management_system/order/ui/UnpaidOrdersScreen.kt:129-146`, `app/src/main/java/com/zeynbakers/order_management_system/order/ui/UnpaidOrdersScreen.kt:374-383`.
