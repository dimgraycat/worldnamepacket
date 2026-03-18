package pl.kosma.worldnamepacket;

import io.netty.buffer.ByteBuf;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Properties;

public class FabricMod implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger();
    private static final Path CONFIG_PATH = Path.of("config", "worldnamepacket", "config.yml");
    private static final Path LEGACY_CONFIG_PATH = Path.of("worldnamepacket", "config.yml");
    private static final Path SERVER_PROPERTIES_PATH = Path.of("server.properties");
    private static final String CONFIG_KEY_WORLD_NAME = "world-name";
    private static final String DEFAULT_WORLD_NAME = "world";
    private static volatile String configuredWorldName;

    @Override
    public void onInitialize() {
        ensureConfigExists();
        configuredWorldName = loadConfiguredWorldName();

        PayloadTypeRegistry.playC2S().register(VoxelmapPayload.ID, VoxelmapPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(VoxelmapPayload.ID, VoxelmapPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(XaeroPayload.ID, XaeroPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(VoxelmapPayload.ID, (payload, context) -> {
            sendResponse(context.player(), WorldNamePacket.CHANNEL_NAME_VOXELMAP, payload.data());
        });
    }

    /**
     * Xaero's Map requires the world name to be send unprompted upon world join/change.
     */
    public static void onServerWorldInfo(ServerPlayerEntity player) {
        sendResponse(player, WorldNamePacket.CHANNEL_NAME_XAEROMAP, null);
    }

    private static void sendResponse(ServerPlayerEntity player, String channel, @Nullable byte[] requestBytes) {
        String levelName = configuredWorldName;
        if (levelName == null || levelName.isEmpty()) {
            levelName = ((MinecraftDedicatedServer) player.getEntityWorld().getServer()).getLevelName();
        }

        byte[] normalizedRequestBytes = (requestBytes != null) ? requestBytes : new byte[0];
        byte[] responseBytes;
        if (WorldNamePacket.CHANNEL_NAME_XAEROMAP.equals(channel)) {
            responseBytes = WorldNamePacket.formatXaeroResponsePacket(levelName);
        } else {
            responseBytes = WorldNamePacket.formatResponsePacket(normalizedRequestBytes, levelName);
        }

        FabricMod.LOGGER.debug("request: " + WorldNamePacket.byteArrayToHexString(normalizedRequestBytes));
        FabricMod.LOGGER.debug("response: " + WorldNamePacket.byteArrayToHexString(responseBytes));
        FabricMod.LOGGER.info("WorldNamePacket: [" + channel + "] sending levelName: " + levelName);

        if (WorldNamePacket.CHANNEL_NAME_VOXELMAP.equals(channel)) {
            ServerPlayNetworking.send(player, new VoxelmapPayload(responseBytes));
            return;
        }

        ServerPlayNetworking.send(player, new XaeroPayload(responseBytes));
    }

    private static void ensureConfigExists() {
        if (Files.exists(CONFIG_PATH)) {
            return;
        }

        if (Files.exists(LEGACY_CONFIG_PATH)) {
            migrateLegacyConfig();
            return;
        }

        String defaultWorldName = readLevelNameFromServerProperties();
        StringBuilder builder = new StringBuilder();
        builder.append("# WorldNamePacket Fabric config").append(System.lineSeparator());
        builder.append("# This value is used for map-mod world identification.").append(System.lineSeparator());
        builder.append(CONFIG_KEY_WORLD_NAME).append(": ").append(defaultWorldName).append(System.lineSeparator());

        try {
            Path parent = CONFIG_PATH.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(CONFIG_PATH, builder.toString(), StandardCharsets.UTF_8);
            LOGGER.info("WorldNamePacket: generated default Fabric config at {}", CONFIG_PATH);
        } catch (IOException e) {
            LOGGER.error("WorldNamePacket: failed to generate default config at {}", CONFIG_PATH, e);
        }
    }

    private static void migrateLegacyConfig() {
        try {
            Path parent = CONFIG_PATH.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.move(LEGACY_CONFIG_PATH, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("WorldNamePacket: moved legacy config from {} to {}", LEGACY_CONFIG_PATH, CONFIG_PATH);
        } catch (IOException e) {
            LOGGER.error("WorldNamePacket: failed to move legacy config from {} to {}", LEGACY_CONFIG_PATH, CONFIG_PATH, e);
        }
    }

    private static String readLevelNameFromServerProperties() {
        if (!Files.exists(SERVER_PROPERTIES_PATH)) {
            return DEFAULT_WORLD_NAME;
        }

        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(SERVER_PROPERTIES_PATH)) {
            properties.load(inputStream);
            String levelName = properties.getProperty("level-name", DEFAULT_WORLD_NAME).trim();
            return levelName.isEmpty() ? DEFAULT_WORLD_NAME : levelName;
        } catch (IOException e) {
            LOGGER.warn("WorldNamePacket: failed to read {} (using default world name)", SERVER_PROPERTIES_PATH, e);
            return DEFAULT_WORLD_NAME;
        }
    }

    @Nullable
    private static String loadConfiguredWorldName() {
        Path configPath = resolveConfigPathForRead();
        if (configPath == null) {
            return null;
        }

        try {
            List<String> lines = Files.readAllLines(configPath, StandardCharsets.UTF_8);
            for (String rawLine : lines) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (!line.startsWith(CONFIG_KEY_WORLD_NAME + ":")) {
                    continue;
                }

                String value = line.substring((CONFIG_KEY_WORLD_NAME + ":").length()).trim();
                if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                    if (value.length() >= 2) {
                        value = value.substring(1, value.length() - 1);
                    }
                }

                if (value.isEmpty()) {
                    LOGGER.warn("WorldNamePacket: {} is empty in {}", CONFIG_KEY_WORLD_NAME, configPath);
                    return null;
                }

                LOGGER.info("WorldNamePacket: using Fabric world name from {}: {}", configPath, value);
                return value;
            }
        } catch (IOException e) {
            LOGGER.error("WorldNamePacket: failed to read {} (fallback to level-name)", configPath, e);
            return null;
        }

        LOGGER.warn("WorldNamePacket: key '{}' not found in {} (fallback to level-name)", CONFIG_KEY_WORLD_NAME, configPath);
        return null;
    }

    @Nullable
    private static Path resolveConfigPathForRead() {
        if (Files.exists(CONFIG_PATH)) {
            return CONFIG_PATH;
        }

        if (Files.exists(LEGACY_CONFIG_PATH)) {
            LOGGER.warn("WorldNamePacket: using legacy config path {} (recommended: {})", LEGACY_CONFIG_PATH, CONFIG_PATH);
            return LEGACY_CONFIG_PATH;
        }

        LOGGER.info("WorldNamePacket: Fabric config not found at {} (fallback to level-name)", CONFIG_PATH);
        return null;
    }

    private record VoxelmapPayload(byte[] data) implements CustomPayload {
        private static final Id<VoxelmapPayload> ID = new Id<>(Identifier.of(WorldNamePacket.CHANNEL_NAME_VOXELMAP));
        private static final PacketCodec<ByteBuf, VoxelmapPayload> CODEC =
                PacketCodec.ofStatic(
                        (buf, payload) -> writeRemainingBytes(buf, payload.data()),
                        buf -> new VoxelmapPayload(readRemainingBytes(buf))
                );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    private record XaeroPayload(byte[] data) implements CustomPayload {
        private static final Id<XaeroPayload> ID = new Id<>(Identifier.of(WorldNamePacket.CHANNEL_NAME_XAEROMAP));
        private static final PacketCodec<ByteBuf, XaeroPayload> CODEC =
                PacketCodec.ofStatic(
                        (buf, payload) -> writeRemainingBytes(buf, payload.data()),
                        buf -> new XaeroPayload(readRemainingBytes(buf))
                );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    private static byte[] readRemainingBytes(ByteBuf buf) {
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        return data;
    }

    private static void writeRemainingBytes(ByteBuf buf, byte[] data) {
        buf.writeBytes(data);
    }
}
