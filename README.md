# Animatables

Reusable Jetpack Compose components for readable state changes and Material 3 Expressive motion. Components inherit the host Material theme and keep state and events in the caller.

## Usage

```kotlin
var expanded by rememberSaveable { mutableStateOf(false) }
ExpandableCard(
    expanded = expanded,
    onExpandedChange = { expanded = it },
    title = "Delivery details",
) {
    Text("Arriving tomorrow")
}
```

See the [integration guide](skills/animatables-compose/references/integration.md) and [Animatables Compose skill](skills/animatables-compose/SKILL.md) for API selection and setup.

## Build

Use JDK 17 or later and configure the Android SDK through `local.properties` or the normal SDK environment.

```sh
./gradlew :catalog:assembleDebug
```

## License

[Apache-2.0](LICENSE).
