# AGENTS.md

This repository contains WorldNamePacket, a server-side companion for Minecraft
mapping mods. Keep this file short and put detailed notes under `docs/`.

## Working Rules

- Prefer small, targeted changes that preserve the current multi-platform jar.
- Keep user-facing README content concise. Put implementation details, protocol
  notes, compatibility notes, and troubleshooting material in `docs/`.
- Use `./gradlew build` as the default verification command after code changes.
- Do not change plugin channel names casually. They are compatibility contracts
  with client-side map mods.
- Keep Java source compatible with the configured toolchain and Minecraft/Fabric
  versions in `gradle.properties`.

## Project Layout

- `src/main/java/pl/kosma/worldnamepacket/WorldNamePacket.java`: shared packet
  formatting and channel constants.
- `src/main/java/pl/kosma/worldnamepacket/FabricMod.java`: Fabric entrypoint and
  Fabric networking registration.
- `src/main/java/pl/kosma/worldnamepacket/SpigotPlugin.java`: Spigot/Bukkit
  plugin entrypoint.
- `src/main/java/pl/kosma/worldnamepacket/VelocityPlugin.java`: Velocity proxy
  plugin entrypoint.
- `docs/`: detailed development, protocol, and compatibility notes.

## Documentation Policy

When adding information that is useful but too detailed for the README or this
file, add it to `docs/` and link it from a nearby document when appropriate.
