# QuietGuard application architecture

QuietGuard uses a unidirectional, feature-owned architecture in shared Kotlin code.
The Android VPN/database engine remains platform-specific; Android and Wasm expose
those capabilities through small `expect`/`actual` adapters.

## Dependency direction

```text
Compose screen
    ↓ immutable state / user events
Feature ViewModel
    ↓ application operations
Repository or platform bridge
    ↓
Room, DataStore, VPN service, or Wasm implementation
```

Dependencies only point downward. Screens do not load data, register listeners, or
coordinate persistence. Repositories do not know about Compose or screen models.
Platform differences terminate at `platform` adapters and dependency injection.

## State ownership

- A screen renders one immutable state stream from its feature ViewModel.
- Durable application data lives in repositories; transient UI choices live in the
  owning ViewModel or `rememberSaveable` when they are strictly presentational.
- Root-tab ViewModels are created above Navigation 3 entries when their state must
  survive tab replacement. Detail ViewModels remain entry-scoped.
- Composables collect ViewModel state with lifecycle-aware collection.

## Async resources

`UiAsyncState<T>` is the canonical contract for asynchronous data:

- `hasReceived == false`: no authoritative result exists yet.
- `isInitialLoading`: the first result is loading.
- `isRefreshing`: previously received data remains usable while it refreshes.
- `isReady`: the current result is authoritative.
- `hasFailed`: loading failed; feature retry events restart the producer.

An empty collection is an empty state only when the resource is ready. Existing
data remains visible during refreshes. Screens must never infer readiness from
`data.isEmpty()` alone.

## Events and effects

- User actions call named ViewModel methods.
- ViewModels coordinate repository writes and platform side effects.
- Auto-updating database flows do not expose decorative refresh controls.
- A visible refresh or retry control must restart real work.
- Cancellation is rethrown; other failures become explicit resource state.

## Presentation rules

- Initial loading, confirmed empty, filtered empty, content, and failure are distinct.
- Loading does not replace usable content.
- Navigation destinations always render a meaningful state; no blank panes.
- Motion tokens honor the platform reduced-motion preference. Infinite decorative
  motion is disabled when reduced motion is requested.
- Icons inside labeled placeholders are decorative; interactive icons have content
  descriptions and Material-sized touch targets.

## Maintenance boundaries

- Keep orchestration in ViewModels and rendering in screen/component files.
- Extract cohesive components before a screen reaches roughly 700–900 lines.
- Prefer explicit feature models over adding another boolean branch to a screen.
- Add host tests for state transitions and both Android and Wasm compile checks for
  shared architectural changes.
