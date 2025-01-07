## Setup
### Formatting
- Install ktlint plugin
- For automated formatting, Install Ktlint plugin to IDEA then you can go to `Settings -> Tools -> KtLint -> Distract free`.
- For manual formatting you can use `Command+Option+Enter`.

### Wildcard imports
For disabling wildcards go to `Settings -> Editor -> Code style -> Kotlin -> Imports`
Choose `Use single name import` for all cases and uncheck all checkboxes below.

## Ktlint formatting
Check: `./gradlew ktlintCheck`

Format: `./gradlew ktlintFormat`

## Detekt check
Check: `./gradlew detektCheck`

## Dependency soring check
Sort: `./gradlew sortDependencies`

Check: `./gradlew checkSortDependencies`