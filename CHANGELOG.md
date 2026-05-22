# Changelog

All notable changes to EzAfk are documented here.

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
Versions follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
Release tags use the `v` prefix (e.g. `v3.0.0`).

---

## [Unreleased]

### Added

- **EzCountdown integration** — EzAfk now optionally integrates with the
  [EzCountdown](https://github.com/ez-plugins/EzCountdown) plugin. Detected
  automatically when present (`integration.ezcountdown: auto`).
- **Configurable kick-warning display types** — `kick.warnings.displays` accepts
  a list of display types: `CHAT`, `TITLE`, `ACTION_BAR`, `BOSS_BAR`. Multiple
  types can be active at once (e.g. `[ACTION_BAR, BOSS_BAR]`). When the list is
  empty the legacy `kick.warnings.mode` value is used for backward compatibility.
- New message keys `kick.warning.action_bar` and `kick.warning.boss_bar` in
  `messages.yml` for the new display types.

### Changed

- `CompatibilityUtil` now handles `sendActionBar`, `showBossBarWarning`, and
  `removeWarningBossBar` via reflection — safe on servers without the BossBar
  API (pre-1.9) or without the action-bar method available.

---

## [3.0.1] - 2026-05-22

[Modrinth](https://modrinth.com/plugin/ezafk/version/3.0.1)

### Added

- Paper 1.20.4 + Java 17 legacy smoke test in CI to verify genuine Java 17 runtime
  compatibility on older server versions.
- Server version compatibility table in `getting-started.md` listing per-feature MC
  version minimums (cherry-leaf animation 1.20+, bubble-column 1.13+, kick cause
  1.19.2+, hide-screen 1.13+).

### Changed

- `afk.sound.enabled` and `unafk.sound.enabled` now default to `false`; Simple Voice
  Chat must be installed and `integration.voicechat` must be enabled for sound to work.
- Documented minimum server requirement as MC 1.19 / Java 17 (was incorrectly shown as
  1.26 / Java 25 in `getting-started.md`).
- Added MC version compatibility notes to `configuration.md` and feature docs.
- [Simple Voice Chat](https://modrinth.com/plugin/simple-voice-chat) API compile
  dependency lowered from 2.6.0 → 2.2.5 so the plugin is compatible with any SVC
  release from 2.2.5 through the latest 2.6.x (MC 1.19–latest). The SVC API is
  additive between 2.x minor versions, so no SVC version in that range will throw
  `NoSuchMethodError`.
- CI Spigot 1.21.4 smoke test now builds the plugin JAR with JDK 25 and runs
  BuildTools + the server with JDK 21. Spigot 1.21.4's BuildTools rejects Java 25
  (`javaVersions: [65, 68]`); `maven.compiler.release=17` still guarantees Java 17
  output bytecode regardless of the build JDK.
- CI Paper and Folia smoke tests always build and run with JDK 25 (paper-api 26.x
  uses Java 25 class file format; `maven.compiler.release=17` still guarantees Java
  17 output bytecode).

### Fixed

- `mvn package` no longer fails when MockBukkit is absent from the local Maven cache.
  MockBukkit is now declared in a `with-mockbukkit` Maven profile that auto-activates
  only when `-Dmockbukkit.version=…` is passed (i.e. in CI); plain `mvn package` is
  unaffected.
- Suppressed `AsyncPlayerChatEvent` deprecation warning in `PlayerActivityListener`
  (`@SuppressWarnings("deprecation")`); the event still fires correctly on all supported
  server versions (1.19–1.21.x).

---

## [3.0.0] - 2026-05-22

### Added

- Initial 3.x release.
