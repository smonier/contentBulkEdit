# Content Bulk Edit

Jahia DX 8 UI extension module for bulk editing content from a query-driven result set.

This module adds a `Content bulk edit` entry under the `Content Tools` accordion and provides a React 18 + Moonstone interface to:

- search content with multiple criteria
- choose which properties to display
- select matching rows
- prepare bulk values in a dedicated side panel
- apply updates for standard properties, tags, and categories

## Main Features

- Query form with:
  - text
  - path picker
  - content type
  - publication status
  - author
  - language
  - publication date range
  - creation date range
  - last modification date range
- Dynamic property picker based on the selected content type
- Forced support for `j:tagList` and `j:defaultCategory`
- Result table with checkbox selection
- Expandable metadata block for path, status, dates, and author
- Right-side bulk edit panel for:
  - selected property values
  - tags
  - category multi-selection using Moonstone tree dropdown
- Backend GraphQL API for content search, category retrieval, and bulk execution
- Automatic mixin handling for tags and categories:
  - `jmix:tagged`
  - `jmix:categorized`

## Module Layout

```text
src/
  main/
    java/org/jahia/se/modules/contentbulkedit/graphql/
      ContentBulkEditOperations.java
      model/
    resources/
      javascript/locales/
  javascript/
    AdminPanel/
    BulkEdit/
      components/
      BulkEdit.gql-queries.js
      BulkEdit.jsx
```

## Technical Stack

- Jahia DX 8.2
- Java / OSGi bundle
- React 18
- Moonstone
- Apollo Client
- GraphQL Java Annotations

## Requirements

- Jahia 8.2.x
- Node `v20.18.0`
- Yarn `v1.22.10`
- Maven

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
mvn -q -DskipTests compile
```

## Installation

1. Build the module:

```bash
mvn clean package
```

2. Deploy the generated bundle to Jahia.
3. Install the module on the target site.
4. Open `jContent` and go to `Content Tools > Content bulk edit`.

## User Flow

1. Select a content type.
2. Fill in any search criteria.
3. Run the search.
4. Select the properties to display and edit.
5. Select result rows.
6. Enter bulk values in the right panel.
7. Confirm and execute.

## GraphQL API

The module exposes a dedicated GraphQL extension under `contentBulkEdit`.

### Queries

- `searchContent(...)`
- `getCategories(siteKey, language)`

### Mutation

- `bulkEditContent(...)`

Bulk property updates are sent as parallel arrays:

- `propertyNames`
- `propertyValues`
- `propertyInternationalized`

This avoids unreliable binding on custom input lists and keeps the mutation payload explicit.

## Category Handling

Categories are resolved from the Jahia category tree rooted at:

```text
/sites/systemsite/categories
```

The backend walks the tree recursively and returns only `jnt:category` nodes while preserving their category-to-category parent relationships for the Moonstone tree dropdown.

## Notes on Property Updates

- Internationalized properties are updated with the selected language.
- Tags and categories add their required mixins automatically if missing.
- The module searches properties across the relevant content subtree instead of assuming all values live on the top node.

## Localization

Current UI labels are provided in:

- `src/main/resources/javascript/locales/en.json`
- `src/main/resources/javascript/locales/fr.json`

## Troubleshooting

### Categories not visible

Check:

- the module is redeployed after backend changes
- the GraphQL field `contentBulkEdit.getCategories` is available
- categories exist under `/sites/systemsite/categories`

### Property values not updated

Check:

- the property belongs to the selected content type
- the selected language is correct for internationalized fields
- the backend bundle was redeployed after GraphQL or mutation changes

### Frontend build fails on JSX parsing

The module expects:

- `babel.config.js` to include `@babel/preset-react`
- `.eslintrc.json` to use the Jahia ESLint config with Babel parser options

## References

This module was designed against the following Jahia module patterns:

- `search-and-replace`
- `exportContentToCsv`
- `importContentFromJson`
- `contentReportReact`
