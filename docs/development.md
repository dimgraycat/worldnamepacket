# Development Notes

## Build

Use the Gradle wrapper:

```sh
./gradlew build
```

The built jar is written to `build/libs/`.

## Version Inputs

Dependency and mod versions are centralized in `gradle.properties`.

Current important inputs:

- `minecraft_version`
- `loader_version`
- `fabric_version`
- `spigot_version`
- `velocity_version`
- `mod_version`

When updating Minecraft or Fabric API versions, verify the networking API usage
in `FabricMod.java`. Minecraft custom payload APIs have changed across recent
versions.

## Multi-Platform Jar

The jar contains entrypoints for Fabric, Spigot/Bukkit, and Velocity. Keep shared
packet behavior in `WorldNamePacket.java` so each platform sends the same wire
format.

Platform-specific behavior:

- Fabric registers `CustomPacketPayload` types and uses Fabric networking.
- Spigot registers plugin messaging channels and responds from Bukkit events.
- Velocity registers Minecraft channel identifiers and responds from proxy
  events.

## Verification Checklist

For normal code changes:

1. Run `./gradlew build`.
2. Check that generated resources still expand `${version}`.
3. For networking changes, verify both VoxelMap and Xaero channel paths.
4. For config changes, verify new installs and legacy config migration paths.

## Known Build Warnings

Gradle may print warnings about native access or deprecated Gradle features.
These are currently warnings, not build failures. Treat new compiler errors or
test failures as blockers.
