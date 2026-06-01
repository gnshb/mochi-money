# 🍡 Mochi Money

A kawaii, offline, open-source Android app that turns the payment SMS already on your
phone into a spending dashboard. Optionally runs a small LLM fully on-device to name
the merchant or person behind each payment — no data ever leaves your phone.

<p align="center">
  <img src="img/dashboard.png" alt="Dashboard" width="240">
  <img src="img/categorize.png" alt="Categories with regex patterns" width="240">
  <img src="img/tidy.png" alt="Categorize empty state" width="240">
</p>

## Install

Download the latest signed APK from the
[Releases page](https://github.com/gnshb/mochi-money/releases).

## Build from source

Requirements: JDK 17, Android SDK with platform 35 and build-tools.

```sh
git clone https://github.com/gnshb/mochi-money.git
cd mochi-money
./gradlew :app:assembleDebug          # debug APK
./gradlew :app:testDebugUnitTest      # unit tests (parser regressions)
./gradlew :app:assembleRelease        # minified release APK (~23 MB, bundles the on-device LLM runtime)
```

## Changelog

### v0.3.0

- Added optional on-device LLM counterparty detection: identify the merchant or person on each payment, entirely offline.
- Download and manage small models in Settings (Qwen 2.5 0.5B/1.5B, SmolLM 135M); uses Gemini Nano via AICore on supported devices.
- Detect from a per-transaction button or run across all transactions, with progress; rename a counterparty manually and have it remembered for similar payments.
- Scoped the transactions list to the current month, added a 3-month spending history, and moved budget editing to the dashboard.

### v0.2.4

- Improved SMS parsing for broader UPI, linked-account, credit-card, merchant, and payer formats.
- Repaired stale SMS imports when parser improvements identify a better direction, counterparty, or category.

### v0.2.3

- Fixed counterparty parsing for ICICI-style credited merchant SMS and TMB linked-account/credit-card UPI messages.

### v0.2.2

- Updated Splitwise dashboard wording from Spend/Received to You owe/Owed.

### v0.2.1

- Fixed category keyword edits so removing a pattern re-evaluates existing transactions and unmatches stale classifications.

### v0.2.0

- Added Splitwise support with group selection and imported owed/owing expenses.
- Improved the dashboard analytics with category net totals sorted by spend and a brief scan-status popup.

### v0.1.0

- Initial release.

## License

GPL v3. See [LICENSE](LICENSE).
