# Downsizing roadmap

## Phase 1 — Local MVP
- [x] Android Kotlin/Compose foundation
- [x] Private SQLite weight, food, movement and routine logs
- [x] Dashboard with recorded weight trend and estimated energy balance
- [x] Read-only Health Connect integration for compatible watches and phones
- [x] Consent-gated Google AdMob banner (test ad unit)
- [x] Optional local daily reminders
- [ ] Pass CI debug build and on-device tests
- [x] First-run profile onboarding, Norway flag, metric inputs, and system light/dark themes
- [ ] Complete permission explanations and device accessibility checks
- [ ] Local data export/delete flows and migration tests
- [ ] Google Play privacy and Health apps declarations

## Phase 2 — Food catalog
- [x] Separate food catalog SQLite schema from private profile and diary data
- [ ] Downloadable food dataset and catalog update flow
- [ ] GitHub release manifest: schema version, SHA-256, signature and compatibility
- [ ] Atomic catalog download and replacement with rollback
- [ ] Product data licensing audit for Norwegian/international sources
- [ ] Barcode scan; per-100-g/ml nutritional breakdown; serving sizes; allergens
- [ ] Recipes and customizable calorie/macro targets

## Phase 3 — Activity and smart modeling
- [ ] Phone step-counter sensor fallback where Health Connect steps unavailable
- [ ] Workout sessions, heart-rate graphs, metrics deduplication
- [ ] Repeating daily/weekly routines and notifications
- [ ] 7/30/90-day weight and energy trends using locally recorded history
- [ ] Confidence-aware weight projections, no medical diagnosis
- [ ] Wear OS companion only if required; optional remote push notifications

## Phase 4 — UtgardNet
- [ ] Pluggable UtgardNet advertising renderer after provider is operational
- [ ] Optional end-to-end encrypted cloud storage independent of advertising
- [ ] Opt-in data synchronization with export and deletion rights
- [ ] Fund product development from revenue, without monetizing health records

**Privacy invariant:** No advertisement module ever reads the personal SQLite tables or Health Connect data.
