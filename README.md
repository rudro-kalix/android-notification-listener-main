# Automatic Payment Gateway — Android Collector

This repository is the **Android collector part** of a larger payment-verification system.

- ✅ This app collects payment notifications/SMS from Android and syncs normalized transaction data to Firebase.
- ✅ It is designed to run in background, queue unsent items, and push when network/config is available.
- The **other part of the project** (payment gateway / verification side) is here:  
  **https://github.com/rudro-kalix/payment-gateway**

## System Architecture Diagram
![Architecture Diagram](https://raw.githubusercontent.com/rudro-kalix/android-notification-listener-main/main/System%20Architecture%20Diagram.jpg)



## What this app does

This Android app:

1. Listens to transaction notifications via `NotificationListenerService`.
2. Reads trusted payment SMS where applicable( bKash, NAGAD, 16216 ).
3. Extracts important fields (amount, transaction ID, sender/title, app package, timestamp).
4. Stores events in a local queue.
5. Syncs queued events to Firebase (Firestore) using app-provided runtime config.



## Important architecture note

This project is **one part** of the complete system:

- **Part 1 (this repo):** Android data collector/uploader.
- **Part 2:** Payment gateway / verification backend + website (linked above).

Use both parts together for end-to-end automatic verification.


## Features

- Notification capture from selected apps
- SMS transaction capture from trusted senders
- Sender/package filters before upload
- Foreground keep-alive service support
- Local queue + retry sync with WorkManager
- Runtime Firebase configuration inside app settings
- Multilingual strings (EN + ET)


## How to use

## 1) Clone and build

```bash
git clone <your-fork-or-this-repo-url>
cd anl-dev-mode
./gradlew assembleDebug
```

> If Gradle download is blocked in your environment, build from Android Studio with internet access.

## 2) Install app on Android device

- Install debug APK from Android Studio or `app/build/outputs/apk/...`.

## 3) Grant required permissions

On first launch:

- Allow **Notification Access**
- Allow **SMS permissions** (if you use SMS-based parsing)
- Allow **Background execution / battery optimization bypass**
- Allow **Notification permission** (Android 13+)

## 4) Configure Firebase from app settings (required)

In **Settings → Firebase**, enter:

- Firebase API Key
- Firebase App ID
- Firebase Project ID
- (Optional) Storage Bucket
- (Optional) Realtime DB URL
- Firestore collection name
- Source tag

> Current behavior is app-input driven for Firebase runtime usage in sync paths; keep required fields filled before expecting uploads.

## 5) Configure filters

In **Settings → Filter**:

- Package filter (regex)
- Sender filter:
  - Single regex, OR
  - Multiple sender names separated by comma/semicolon/newline

Examples:

- `bKash,NAGAD,16216`
- `(?i)bkash|nagad|16216`

## 6) Connect backend/payment gateway

Use the companion project for verification/business logic:

- https://github.com/rudro-kalix/payment-gateway

Your backend should validate amount/title/transaction ID and enforce one-time transaction use.


## Expected Firebase document fields

Typical notification sync payload includes:

- `source`
- `docId`
- `transactionId`
- `amount`
- `packageName`
- `title`
- `text`
- `time`

SMS transaction payload includes:

- `source`
- `provider`
- `amount`
- `transactionId`
- `text`
- `timestamp`
- `smsApp`


## Security and sharing notes

- Do **not** commit private Firebase credentials into source control.
- Prefer entering runtime Firebase values on-device.
- Use backend rules and verification logic to prevent replay/fraud.
- This app reads only user-visible notifications/SMS with permission.


## Disclaimer

This is an educational/experimental project and is not an official payment gateway provider.
Always comply with local law, platform policy, and payment-provider terms.
