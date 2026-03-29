# Facebook Messenger

Facebook Messenger bot integration — sending rich messages (text, images, videos, quick replies, button templates) and receiving webhook events.

## What It Solves

- `MgcGeneralFbApi` implements `OutMessengerApi` for Messenger: text, images (with reusable asset caching), videos, quick replies (up to 13 options), button templates (up to 3 buttons)
- Auto-selects between ButtonTemplate (≤3 options, short text) and QuickReply (>3 or long text)
- Splits long messages into 1997-char chunks, attaches quick replies only to the last chunk

## Key Details

- Uses `messenger4j` v1.1.0
- Caffeine cache for temporary binary asset storage (30s TTL)
