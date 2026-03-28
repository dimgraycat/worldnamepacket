package pl.kosma.worldnamepacket.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import pl.kosma.worldnamepacket.FabricMod;


@Mixin(PlayerList.class)
public class MixinPlayerManager {
	@Inject(at = @At("HEAD"), method = "sendLevelInfo(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/level/ServerLevel;)V")
	public void onSendWorldInfo(ServerPlayer player, ServerLevel world, CallbackInfo info) {
		FabricMod.onServerWorldInfo(player);
	}
}
