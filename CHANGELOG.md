# Changelog

All notable changes to this module are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.1] - 2026-07-09

Security hardening from an authorized security review (issue #1).

### Security

- **`getPropertyDefinitions` authorization** (finding 2): the field previously required only
  an authenticated user, letting anyone enumerate the property model of any node type
  (`jnt:user`, `jnt:virtualsite`, …) plus content-editor form overrides from all bundles.
  It now takes a mandatory `siteKey`, gates on the `jContentAccess` permission for that site,
  and only exposes editorial content types. (PRs #2, #3)
- **Exact site path boundary** (finding 3): `resolveSearchPath` and the `matchesFilters` path
  filter used a plain prefix check, so a prefixed sibling site key (e.g. `/sites/luxeX` for
  `luxe`) could satisfy the scope. Both now require an exact match or a `/`-delimited
  descendant. (PRs #2, #4)

### Changed

- `getPropertyDefinitions` now requires a `siteKey` argument (breaking change to the module's
  own GraphQL field; the module UI is updated accordingly).

### Note

- The system-session ACL bypass affecting ≤ 1.0.1 (finding 1) was already fixed in 1.1.0.
  Anyone still on ≤ 1.0.1 should upgrade to ≥ 1.1.0.

## [1.2.0] - 2026-07-07

Publication awareness and table ergonomics.

### Added

- **Publication status column** in the results table, resolved by Jahia's publication
  engine (`ComplexPublicationService`, the same source as jContent's badges) so i18n,
  non-i18n, and reference changes are all detected. Five states with jContent colors:
  published, modified, never published, unpublished, marked for deletion.
- **Bulk publish**: `publishContent(siteKey, language, nodeUuids)` GraphQL mutation and
  a "Publish selection" header action with confirmation dialog. Publishes each selected
  node (including references) to live in the working language; every node is gated on
  the `publish` permission and failures are reported per node. Statuses refresh after
  publication.
- Publication status search filter extended with **Modified** and **Never published**.
- **Sortable result table**: click any column header (name, status, property columns,
  tags, categories) for ascending/descending sort; third click restores search order.
  Locale-aware and numeric-aware, client-side, with `aria-sort`.

### Changed

- The French label for unpublished content is now "Dépublié" (previously "Non publié")
  to disambiguate from "Jamais publié" (never published).

## [1.1.0] - 2026-07-07

Substantial rework of typing, security, and UX, with breaking API changes.

### Added

- `getPropertyDefinitions(nodeType, language)` GraphQL query: editable property definitions
  with full type metadata (required type, selector type, selector options, constraints,
  default values, i18n, cardinality, mandatory, declaring node type + label).
- Content Editor fieldset overrides (`META-INF/jahia-content-editor-forms/fieldsets/*.json`
  scanned across all active bundles) are merged into property definitions — e.g. a
  `ChoiceTree` override with `rootPath` scopes a category field to a subtree.
- Typed bulk edit widgets driven by the definition: jContent picker for references
  (picker type from the CND, e.g. `picker[type='image']`), scoped category tree for
  `category` / `ChoiceTree` fields, dropdown for static choicelists, Yes/No for booleans,
  date/datetime and number inputs, textarea for rich text.
- Typed server-side writes: string values are coerced to the definition's required JCR
  type (references accept UUID or absolute path; dates, booleans, numbers, decimals).
  Internationalization and cardinality are resolved from the definition, never trusted
  from the client.
- Per-property **replace / append** mode for multi-valued properties (`propertyModes`);
  append merges into existing values and drops duplicates.
- **Replace / append** modes for tags (`tagMode`) and categories (`categoryMode`).
- Explicit **clear property** action (`clearPropertyNames`) with a per-field checkbox;
  clearing a mandatory property fails per node with the constraint message.
- `getCategories` accepts an optional `rootPath` (validated to stay under
  `/sites/systemsite/categories`).
- Property list ordered and grouped like Content Editor: own fields first (declaration
  order), then inherited fields, then one group per declaring mixin; rendered as
  side-by-side column cards with the declaring type as caption.
- Search criteria auto-collapse into a one-line summary bar after a search, with an
  "Edit criteria" button to restore the panels.
- Bulk panel footer with a live recap (rows selected / changes ready) and its own Apply
  button; disabled state explains what is missing via tooltip.
- Sticky results-table header inside a scrollable container; ellipsis + hover tooltips
  on long cell values; reference values rendered as the target node's display name.
- Confirm dialog closes on Escape and lists modes and cleared properties.
- EN + FR labels for all new UI strings.

### Changed

- **Security**: all operations now run with the calling user's JCR session (ACLs apply
  to search results and writes); guest access is rejected; each edited node requires
  `jcr:write`. Previously everything ran on an unauthenticated system session.
- Bulk execution saves **per node** and rolls the session back on failure, so a failed
  node is left fully untouched (previously one save at the end could persist partial
  changes from failed nodes).
- Text and date filters are pushed into the JCR-SQL2 query (`CONTAINS` + indexed date
  ranges) instead of an in-memory scan of up to 5000 nodes; text search is now
  word-based full-text matching. Full-text input is escaped/neutralized against
  parser errors and injection.
- Property updates apply to the selected node only. The previous recursive propagation
  to every descendant carrying a same-named property is removed.
- Author filter is a free-text username input (exact match). The unbounded all-users
  query (usernames + emails loaded into the browser) is removed.
- Property selector uses toggle chips grouped by declaring type instead of card
  checkboxes; results density and layout reworked (results visible without scrolling).
- Styling migrated to Moonstone design tokens (colors, radii, spacing, selector borders)
  with static fallbacks — the panel now follows the jContent theme.
- Locale parsing uses `LanguageCodeConverters` (fixes region variants like `en_US`).
- Repository hygiene: `.DS_Store`, `yarn-error.log`, `pom.xml.releaseBackup`,
  `release.properties`, and the `node/` runtime are untracked and ignored.

### Removed (breaking)

- `bulkEditContent` no longer accepts `propertyInternationalized` (i18n is resolved
  server-side from the definition) nor the structured `propertyUpdates` input.
- The GraphQL property metadata previously fetched from the generic
  `jcr.nodeTypes` API is replaced by `contentBulkEdit.getPropertyDefinitions`.

### Fixed

- Weakreference and other non-string properties were edited through a plain text input
  and written as strings, failing or storing garbage — the original issue motivating
  this release.
- `totalCount` semantics and truncation warning kept consistent with the capped result
  window.

## [1.0.1] - 2025

### Added

- i18n support for property updates (`propertyInternationalized` flag).

## [1.0.0] - 2025

### Added

- Initial release: query-driven search (text, path, type, status, author, date ranges),
  property display picker, result table with selection and metadata expansion, bulk
  panel for property values, tags and categories, GraphQL extension
  (`searchContent`, `getCategories`, `bulkEditContent`).
