# Telegram Modules

Telegram bot integration — sending messages (text, photos, videos, stickers, inline keyboards) with rate limiting, long-polling, and message editing.

## What It Solves

- `MgcGeneralTelegramApi` implements `OutMessengerApi` for Telegram: text, photos (URL or bytes), videos, stickers, documents, inline/reply keyboards
- Long-polling with KV-store-backed offset persistence for crash-resilient restart
- Global + per-user rate limiting (Caffeine-cached, 1-minute expiry, 100K user capacity)

## Key Details

- Uses `telegrambots-client` v9.2.1 (OkHttp-based)
- Keyboard is attached to the *last* message in multi-message sends
- Photo captions truncated to 990 chars; rate limiters are optional (null-safe)
