# Protocol Notes

WorldNamePacket communicates with client-side mapping mods through Minecraft
plugin/custom payload channels.

## Channels

- VoxelMap: `worldinfo:world_id`
- Xaero's Map: `xaeroworldmap:main`

These names are compatibility contracts. Do not rename them unless the client
mods change their expected channels.

## VoxelMap

VoxelMap is request-response based.

Nominal packet shape:

- Request: `00 42 00`
- Response: `00 42 <length> <world name>`

Fabric VoxelMap has historically used a different request-response shape:

- Request: `00 00 00 42`
- Response: `42 <length> <world name>`

`WorldNamePacket.formatResponsePacket` detects the Fabric request shape and
omits the Forge discriminator for that case.

## Xaero's Map

Xaero expects the world ID to be sent proactively on join or world/server
change.

Packet shape:

- byte 0: packet type
- bytes 1-4: world ID as a big-endian int

The current world ID is `worldName.hashCode()`.

## Fabric Custom Payload IDs

On modern Minecraft/Fabric versions, avoid passing a full namespaced channel
string to `CustomPacketPayload.createType`. That helper applies the default
namespace and can turn `worldinfo:world_id` into the invalid path
`minecraft:worldinfo:world_id`.

Use a parsed `Identifier` when constructing payload types:

```java
new CustomPacketPayload.Type<>(Identifier.parse("worldinfo:world_id"))
```

The same rule applies to `xaeroworldmap:main`.
