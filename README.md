# Smarternow Bulk Messaging App

Bulk SMS Android app that composes messages for specific groups or ALL contacts in the database and sends them through the **Africa's Talking** SMS gateway.

## Features

- **Splash screen with auto-pick validation** — before the dashboard opens, the app picks the oldest PENDING message, validates it (length, spam phrases, unresolved placeholders), marks it SENT or REJECTED, and only then navigates home.
- **Dashboard / home page** — live stats: total contacts, total groups, pending queue, total sent, plus quick cards for Compose, Groups, History and Settings.
- **Groups & contacts** — create/rename/delete groups, add contacts one by one or paste many rows at once. Kenyan numbers are normalised to `+254…` automatically.
- **Compose & send** — target ALL contacts or a specific group. Live 160-char counter and inline validation. Send now (through Africa's Talking) or queue as PENDING for the next splash auto-pick.
- **Message history** — every record with status, target, recipient count and the raw gateway response.
- **Settings** — Africa's Talking username, API key and Sender ID, plus sandbox/production toggle.

## How the auto-pick gate works

1. App launches → splash screen (Material splash + animated logo).
2. A background task loads all `PENDING` SMS rows.
3. The oldest one is *auto-picked* and run through `MessageValidator`.
4. Valid message → status becomes `SENT`; invalid → `REJECTED` with the reason shown.
5. The recipient count is recounted against the target (group or all).
6. After the result is displayed the user is taken to the dashboard.

## Africa's Talking API

`AfricaTalkingService` posts form data to:

- Production: `https://api.africastalking.com/version1/messaging`
- Sandbox: `https://api.sandbox.africastalking.com/version1/messaging`

Headers: `apiKey` + `Accept: application/json`. Fields: `username`, `to` (comma separated international numbers), `message`, optional `from`.

## Tech stack

- Android 13 (API 33+), target SDK 34, min SDK 24
- Java with ViewBinding-style XML layouts (Material Components 3, CardView, RecyclerView)
- Room database (Groups, Contacts, SmsMessages)
- OkHttp for the gateway, Gson for JSON
- Africa's Talking message content validation before send

## Build & run

1. Open the project in Android Studio (or run `gradlew assembleDebug`).
2. Create groups and add contacts.
3. In Settings enter your Africa's Talking **username**, **API key** and optional Sender ID (start in Sandbox mode).
4. Compose a message, target a group or "ALL CONTACTS", and send.

> App icon and splash use the bundled adaptive icon (blue `SMART`-style bubble with a green SMS bolt).