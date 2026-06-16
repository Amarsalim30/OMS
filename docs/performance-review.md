# Performance Review

Date: 2026-03-07

## Summary
- The repo is not in immediate performance crisis, but there are several scale-sensitive paths where broad queries and oversized composition surfaces will become more expensive as bakery history grows.
- This pass took one concrete, low-risk improvement in the payment-history move-target path.

## Findings
| ID | Severity | Affected files | Production impact | Recommendation | Implement now vs later | Change risk | Status |
|---|---|---|---|---|---|---|---|
| PERF-1 | Medium | `accounting/ui/PaymentIntakeHistoryViewModel.kt`, `order/data/OrderDao.kt` | Move-target sheet was loading all customer orders and filtering/sorting in Kotlin. | Keep open-order filtering and ordering in SQL. | Implemented now. | Low | Fixed |
| PERF-2 | High | `order/data/OrderDao.kt`, `order/ui/OrderViewModel.kt`, `core/widget/WidgetUpdater.kt` | Open-order queries still depend on broad scans and large limits in multiple hotspots. | Revisit indexes and query shape for open-order heavy features. | Later. | Medium | Open |
| PERF-3 | Medium | `MainAppContent.kt` | Large root composable still collects and reacts to too much unrelated state. | Split route-scoped collection and state derivation. | Later. | Medium-High | Open |
| PERF-4 | Medium | `core/notifications/NotificationScheduler.kt`, `ReminderWorker.kt`, `MainAppContent.kt` | `enqueueNow` and hourly reminder scheduling can create unnecessary churn during heavy use. | Add throttling/coalescing for immediate reminder refreshes. | Later. | Low-Medium | Open |
| PERF-5 | Medium | `customer/data/CustomerDao.kt` | `%query%` contains-search patterns will not scale well on larger customer lists. | Move to prefix search or FTS when customer volume warrants it. | Later. | Medium | Open |

## Verification
- `:app:testDebugUnitTest` passed after the SQL-side move-target change.
- `:app:lintDebug` passed.
- `:app:assembleRelease` passed.
