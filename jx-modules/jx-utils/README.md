# JX Utils

The foundational utility library of JSK — provides static utility classes, functional interfaces, collections, async primitives, and data structures used by every other module.

## What It Solves

- 10 static utility classes with short names: `Cc` (collections), `St` (strings), `Ex` (exceptions), `Fu` (functions), `Ti` (time), `Ma` (math), `Io` (I/O), `Ar` (arrays), `Im` (images), `Re` (reflection)
- `O<T>` — custom serializable Optional with extra combinators, replaces `java.util.Optional` everywhere
- `OneOf<L,R>` — standard error handling pattern (used instead of exceptions in many APIs)
- 30+ functional interfaces (`F0`-`F3`, `C1`-`C3`, `P1`-`P2`) — all support checked exceptions
- Tuples (`X1`-`X7`), async primitives (`ForeverThreadWithFinish`, `JLock`, `CancelToken`), collections (`DequeWithLimit`, `MultiBiMap`)

## Key Details

- Ultra-short 2-letter naming is a deliberate style choice
- `Cc.java` is ~1000 lines and the most critical utility class
- `O<T>` and `OneOf<L,R>` are pervasive — understanding them is prerequisite to reading any JSK code
