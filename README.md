## Setup
### Formatting
- Install ktlint plugin
- For automated formatting, Install Ktlint plugin to IDEA then you can go to `Settings -> Tools -> KtLint -> Distract free`.
- For manual formatting you can use `Command+Option+Enter`.

### Wildcard imports
For disabling wildcards go to `Settings -> Editor -> Code style -> Kotlin -> Imports`
Choose `Use single name import` for all cases and uncheck all checkboxes below.

### Install conan

```bash
pip install conan==2.9.2 numpy
```

Create profile, get your conan home path

```bash
conan config home
```

Go to the following path and open the profiles folder.
For example, on my system, the path is:
`~/.conan2/profiles`

Then, create a file named `android-studio` with the following content:

```bash
include(default)

[settings]
os=Android
os.api_level=21
compiler=clang
compiler.version=12
compiler.libcxx=c++_static
compiler.cppstd=14

[tool_requires]
*: android-ndk/r26d
```

## Ktlint formatting
Check: `./gradlew ktlintCheck`

Format: `./gradlew ktlintFormat`

## Detekt check
Check: `./gradlew detektCheck`

## Dependency soring check
Sort: `./gradlew sortDependencies`

Check: `./gradlew checkSortDependencies`


