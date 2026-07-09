# Content Bulk Edit

Jahia DX 8 UI extension module for bulk editing content from a query-driven result set.

This module adds a `Content bulk edit` entry under the `Content Tools` accordion and provides a React 18 + Moonstone interface to:

- search content with multiple criteria (index-backed full-text and date filters)
- choose which properties to display and edit, grouped like Content Editor
- select matching rows in a sortable result table with live publication status
- prepare typed bulk values in a dedicated side panel
- apply updates, clears, tags, and categories with per-field replace/append modes
- bulk publish the selection (with references) in the working language

## Main Features

### Search

- Query form with:
  - full-text search (pushed into the JCR-SQL2 query, `CONTAINS`)
  - path picker
  - content type
  - publication status (published / modified / never published / unpublished)
  - author (exact username match)
  - language
  - publication / creation / last modification date ranges (pushed into the query as indexed range conditions)
- Criteria panels auto-collapse into a one-line summary bar after a search ("Edit criteria" restores them)

### Property selection

- Property definitions are served by the module's own GraphQL endpoint with full type metadata: required type, selector type, selector options, constraints, default values, i18n, cardinality, and declaring node type
- Properties are ordered and grouped like Content Editor: the type's own fields first (in CND declaration order), then inherited fields, then one column card per declaring mixin
- Content Editor fieldset overrides (`META-INF/jahia-content-editor-forms/fieldsets/*.json` in any deployed bundle) are merged in, so a `ChoiceTree` override with a `rootPath` scopes a category field exactly as in Content Editor

### Typed bulk edit fields

The side panel renders each field according to its definition:

| Definition | Widget |
|---|---|
| `weakreference` / `reference` + picker | jContent picker (`CE_API.openPicker`), picker type from the CND (`picker[type='image']` opens the image picker) |
| `category[...]` selector or `ChoiceTree` override | Searchable category tree, scoped to the override's `rootPath` when present |
| choicelist with static constraints | Dropdown |
| `boolean` | Yes / No dropdown |
| `date` | Date / datetime input |
| `long` / `double` / `decimal` | Number input |
| rich text / text area | Textarea |
| anything else | Text input |

- Multi-valued properties get a **replace / append** mode selector; append merges into existing values and drops duplicates
- Every field has a **"Clear this property"** checkbox to bulk-remove the value (clearing a mandatory property fails per node with a constraint message)
- Tags and categories have the same replace / append modes
- The panel footer shows a live recap (rows selected, changes ready) with its own Apply button; when disabled, a tooltip explains what is missing

### Results table

- Sticky header inside a scrollable container, ellipsis + hover tooltips on long values
- **Publication status column** driven by Jahia's publication engine (same source as jContent's badges), with five states: published (green), modified (orange), never published (dark), unpublished (gray), marked for deletion (red). Non-i18n property changes and translation changes are both detected.
- **Sortable columns** — click any header (name, status, property columns, tags, categories) to sort ascending/descending; third click restores the original order. Locale-aware, numeric-aware, client-side.
- Reference property values are rendered as the target node's display name, not a raw UUID
- Expandable metadata block for path, status, dates, and author

### Bulk publish

- "Publish selection" header action publishes every selected node (including its references) from default to live in the working language, after an explicit confirmation
- Each node is gated on the `publish` permission and reported individually; one failure never blocks the rest
- Statuses refresh automatically after publication — a typical flow is: sort by status, select all "modified" rows, publish

### Security model

- All operations run with the **calling user's JCR session** — ACLs apply naturally to search results and writes
- Guest access is rejected; every edited node also requires `jcr:write`
- Bulk execution saves **per node** with a session rollback on failure, so a failed node is left untouched
- Full-text input is escaped/neutralized before being embedded in the JCR-SQL2 statement

## Module Layout

```text
src/
  main/
    java/org/jahia/se/modules/contentbulkedit/graphql/
      ContentBulkEditOperations.java
      ContentBulkEditQueryExtension.java
      ContentBulkEditMutationExtension.java
      model/
    resources/
      javascript/locales/          # en.json, fr.json
  javascript/
    AdminPanel/
    BulkEdit/
      components/
        BulkEditPanel.jsx          # side panel: fields, modes, clear, footer
        BulkFieldInput.jsx         # typed field dispatcher (picker, tree, dropdown, ...)
        CategoryTreeInput.jsx
        ConfirmDialog.jsx
        PropertySelector.jsx       # grouped property chips
        ResultsTable.jsx
        SearchFilters.jsx
        SearchSummaryBar.jsx       # collapsed criteria summary
      BulkEdit.gql-queries.js
      BulkEdit.jsx
```

## Technical Stack

- Jahia DX 8.2
- Java / OSGi bundle
- React 18
- Moonstone (styling uses Moonstone design tokens with static fallbacks)
- Apollo Client
- GraphQL Java Annotations

## Requirements

- Jahia 8.2.x
- Node `v20.18.0`
- Yarn `v1.22.10`
- Maven + Java 17

The Maven build already installs the expected Node and Yarn versions through `frontend-maven-plugin`.

## Build

### Frontend only

```bash
./node/yarn/dist/bin/yarn install
./node/yarn/dist/bin/yarn lint:fix
./node/yarn/dist/bin/yarn build:production
```

### Full module build

```bash
mvn clean package
```

### Fast backend compile check

```bash
mvn -q compile -Dskip.yarn=true -Dskip.installnodenpm=true -Dskip.installyarn=true
```

## Installation

1. Build the module:

```bash
mvn clean package
```

2. Deploy the generated bundle to Jahia (e.g. `curl -u root:*** -F bundle=@target/contentBulkEdit-<version>.jar -F start=true http://localhost:8080/modules/api/bundles`).
3. Install the module on the target site.
4. Open `jContent` and go to `Content Tools > Content bulk edit`.

## User Flow

1. Select a content type and any search criteria, then run the search (criteria collapse into a summary bar).
2. Toggle the property chips you want to display and edit — grouped by declaring type, like Content Editor.
3. Select result rows.
4. Enter bulk values in the right panel: typed widgets, replace/append for multi-valued fields, clear checkboxes, tags and categories.
5. Review the recap and confirm — the dialog lists every change including modes and cleared properties.

## GraphQL API

The module exposes a dedicated GraphQL extension under `contentBulkEdit`.

### Queries

- `searchContent(siteKey, language, text, path, contentType, publicationStatus, dates..., author, properties, limit)`
- `getCategories(siteKey, language, rootPath)` — `rootPath` optionally scopes the tree to a subtree of `/sites/systemsite/categories`
- `getPropertyDefinitions(nodeType, language)` — editable property definitions with full type metadata, ordered own type → inherited → mixin groups, with Content Editor fieldset overrides merged in

### Mutations

- `bulkEditContent(siteKey, language, nodeUuids, propertyNames, propertyValues, propertyModes, clearPropertyNames, tagValues, tagMode, categoryIdentifiers, categoryMode)`
- `publishContent(siteKey, language, nodeUuids)` — publishes each node (with references) to live in the given language; requires the `publish` permission per node, failures reported per node

Bulk property updates are sent as parallel arrays (`propertyNames` / `propertyValues` / `propertyModes`). Values are always strings; the server resolves the applicable property definition and coerces them to the required JCR type (references accept a UUID or an absolute path; multi-values are comma-separated). Internationalization and cardinality come from the definition — never from the client.

Modes (`propertyModes`, `tagMode`, `categoryMode`) accept `replace` (default) or `append`; anything else fails the call before any node is touched.

## Category Handling

Categories are resolved from the Jahia category tree rooted at `/sites/systemsite/categories` (or the requested `rootPath` beneath it). The backend walks the tree recursively and returns only `jnt:category` nodes while preserving parent relationships for the Moonstone tree dropdown. `jmix:tagged` / `jmix:categorized` are added automatically when missing.

## Localization

UI labels are provided in:

- `src/main/resources/javascript/locales/en.json`
- `src/main/resources/javascript/locales/fr.json`

Property, type, and fieldset labels come from the content modules' own resource bundles via the definitions.

## Troubleshooting

### Categories not visible

- the module is redeployed after backend changes
- the GraphQL field `contentBulkEdit.getCategories` is available
- categories exist under `/sites/systemsite/categories` (or the field's `rootPath`)

### Property values not updated

- the property belongs to the selected content type (mixin fields appear only when the mixin is a declared supertype)
- the selected language is correct for internationalized fields
- the acting user has `jcr:write` on the node — errors are reported per node in the execution result

### A weakreference field shows a text input instead of a picker

- the CND declares a selector (`picker[type='...']`, `category[...]`) or a Content Editor fieldset override provides one — without any selector metadata the field falls back to a raw UUID/path input

### Frontend build fails on JSX parsing

- `babel.config.js` must include `@babel/preset-react`
- `.eslintrc.json` must use the Jahia ESLint config with Babel parser options

## References

This module was designed against the following Jahia module patterns:

- `search-and-replace`
- `exportContentToCsv`
- `importContentFromJson`
- `contentReportReact`
