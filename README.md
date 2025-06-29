## Setup
### Formatting
- Install ktlint plugin
- For automated formatting, Install Ktlint plugin to IDEA then you can go to `Settings -> Tools -> KtLint -> Distract free`.
- For manual formatting you can use `Command+Option+Enter`.

### Wildcard imports
For disabling wildcards go to `Settings -> Editor -> Code style -> Kotlin -> Imports`
Choose `Use single name import` for all cases and uncheck all checkboxes below.


### Submodules

```bash
git submodule update --init --recursive
```

### Install conan

(For Windows, refer to these [instructions](https://github.com/batchar2/fptn/tree/master/deploy/windows) to install all required dependencies.)


```bash
pip install conan numpy
```



```bash
sudo apt install clang
```

Create profile, get your conan home path

```bash
conan config home
```

```bash
conan profile detect -f
```

Go to the following path and open the profiles folder.
For example, on my system, the path is:
`~/.conan2/profiles`

Then, create a file named `android-studio` with the following content:

```bash
include(default)

[settings]
os=Android
os.api_level=28
compiler=clang
compiler.version=20
compiler.libcxx=c++_static
compiler.cppstd=17

[tool_requires]
*: android-ndk/r27c
```
[Hack] If above config doesn't help, install ndk 29.0.13599879 and point it out in 3 places:
  - app/build.gradle.kts in android section, for example
    ```android {
        ...
        ndkVersion = "29.0.13599879"
        ...
    ```
  - local.properties
    ```
        ndk.dir=/Users/<user-name>/Library/Android/sdk/ndk/29.0.13599879
    ```
  - ~/.conan2/profiles/android-studio in conf section
    ```
        ...
        [conf]
        tools.android:ndk_path=/Users/ddeviatilov/Library/Android/sdk/ndk/29.0.13599879
        ...
    ```


## Ktlint formatting
Check: `./gradlew ktlintCheck`

Format: `./gradlew ktlintFormat`

## Detekt check
Check: `./gradlew detektCheck`

## Dependency soring check
Sort: `./gradlew sortDependencies`

Check: `./gradlew checkSortDependencies`