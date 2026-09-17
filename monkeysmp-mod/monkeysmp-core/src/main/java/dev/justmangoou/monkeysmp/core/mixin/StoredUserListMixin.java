package dev.justmangoou.monkeysmp.core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.players.IpBanList;
import net.minecraft.server.players.StoredUserList;
import net.minecraft.server.players.UserBanList;

@Mixin(StoredUserList.class)
public abstract class StoredUserListMixin {
	@SuppressWarnings("ConstantValue")
	@Inject(method = "save", at = @At("HEAD"), cancellable = true)
	private void monkeySmpCore$disableBanListSave(CallbackInfo ci) {
		if ((Object) this instanceof IpBanList || (Object) this instanceof UserBanList) {
			ci.cancel();
		}
	}

	@SuppressWarnings("ConstantValue")
	@Inject(method = "load", at = @At("HEAD"), cancellable = true)
	private void monkeySmpCore$disableBanListLoad(CallbackInfo ci) {
		if ((Object) this instanceof IpBanList || (Object) this instanceof UserBanList) {
			ci.cancel();
		}
	}
}
