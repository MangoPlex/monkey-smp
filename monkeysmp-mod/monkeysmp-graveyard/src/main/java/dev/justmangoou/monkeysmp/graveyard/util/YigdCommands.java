package dev.justmangoou.monkeysmp.graveyard.util;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import java.util.*;
import java.util.function.Predicate;

import dev.justmangoou.monkeysmp.graveyard.components.GraveComponent;
import dev.justmangoou.monkeysmp.graveyard.config.MonkeySMPGraveyardConfig;
import dev.justmangoou.monkeysmp.graveyard.data.DeathInfoManager;
import dev.justmangoou.monkeysmp.graveyard.data.GraveStatus;
import dev.justmangoou.monkeysmp.graveyard.data.ListMode;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.item.component.ResolvableProfile;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class YigdCommands {
	private static DeathInfoManager deathInfo(CommandContext<CommandSourceStack> context) {
		return DeathInfoManager.get(context.getSource().getServer());
	}

	public static void register() {
		MonkeySMPGraveyardConfig config = MonkeySMPGraveyardConfig.getConfig();
		MonkeySMPGraveyardConfig.CommandConfig commandConfig = config.commandConfig;

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				literal(commandConfig.mainCommand)
						.requires(requires(commandConfig.basePermissionLevel))
						.then(literal("restore")
								.requires(requires(commandConfig.restorePermissionLevel))
								.executes(YigdCommands::restore)
								.then(argument("player", EntityArgument.player())
										.executes(context -> restore(context, EntityArgument.getPlayer(context, "player")))
										.then(argument("pos", BlockPosArgument.blockPos())
												.executes(context -> restore(
														context,
														EntityArgument.getPlayer(context, "player"),
														BlockPosArgument.getBlockPos(context, "pos"))))))
						.then(literal("rob")
								.requires(requires(commandConfig.robPermissionLevel))
								.then(argument("victim", EntityArgument.player())
										.executes(context -> rob(context, EntityArgument.getPlayer(context, "victim"))))
								.then(argument("grave_id", UuidArgument.uuid())
										.executes(context -> rob(context, UuidArgument.getUuid(context, "grave_id")))))
						.then(literal("whitelist")
								.requires(requires(commandConfig.whitelistPermissionLevel))
								.executes(YigdCommands::showListType)
								.then(literal("add")
										.then(argument("target", EntityArgument.players())
												.executes(context -> addToList(context, EntityArgument.getPlayers(context, "target")))))
								.then(literal("remove")
										.then(argument("target", EntityArgument.players())
												.executes(context -> removeFromList(context, EntityArgument.getPlayers(context, "target")))))
								.then(literal("toggle")
										.executes(YigdCommands::toggleListType))
								.then(literal("list")
										.executes(YigdCommands::showList)))
		));
	}

	private static int restore(CommandContext<CommandSourceStack> context) {
		ServerPlayer player = context.getSource().getPlayer();
		if (player == null) return -1;

		return restore(context, player);
	}
	private static int restore(CommandContext<CommandSourceStack> context, ServerPlayer target) {
		ResolvableProfile profile = ResolvableProfile.createResolved(target.getGameProfile());

		List<GraveComponent> graves = new ArrayList<>(deathInfo(context).getBackupData(profile));
		graves.removeIf(graveComponent -> graveComponent.getStatus() == GraveStatus.CLAIMED);
		int size = graves.size();
		if (size < 1) return -1;

		return restore(context, target, graves.get(size - 1));
	}
	private static int restore(CommandContext<CommandSourceStack> context, ServerPlayer target, BlockPos pos) {
		ResolvableProfile profile = ResolvableProfile.createResolved(target.getGameProfile());

		List<GraveComponent> graves = new ArrayList<>(deathInfo(context).getBackupData(profile));
		graves.removeIf(graveComponent -> graveComponent.getStatus() == GraveStatus.CLAIMED);

		for (GraveComponent grave : graves) {
			if (grave.getPos().equals(pos))
				return restore(context, target, grave);
		}
		return -1;
	}
	private static int restore(CommandContext<CommandSourceStack> context, ServerPlayer target, GraveComponent component) {
		component.applyToPlayer(target, target.level(), target.position(), true);
		component.setStatus(GraveStatus.CLAIMED);

		component.removeGraveBlock();

		context.getSource().sendSystemMessage(Component.translatable("text.monkeysmp.command.restore.success"));
		return 1;
	}

	private static int rob(CommandContext<CommandSourceStack> context, ServerPlayer victim) {
		ResolvableProfile profile = ResolvableProfile.createResolved(victim.getGameProfile());
		List<GraveComponent> graves = new ArrayList<>(deathInfo(context).getBackupData(profile));
		graves.removeIf(graveComponent -> graveComponent.getStatus() != GraveStatus.UNCLAIMED);

		int size = graves.size();
		if (size < 1) return -1;
		GraveComponent component = graves.get(size - 1);

		return rob(context, component);
	}
	private static int rob(CommandContext<CommandSourceStack> context, UUID graveId) {
		Optional<GraveComponent> component = deathInfo(context).getGrave(graveId);
		return component.map(graveComponent -> rob(context, graveComponent)).orElse(-1);
	}
	private static int rob(CommandContext<CommandSourceStack> context, GraveComponent component) {
		ServerPlayer player = context.getSource().getPlayer();
		if (player == null) return -1;

		component.applyToPlayer(player, context.getSource().getLevel(), player.position(), false);
		component.setStatus(GraveStatus.CLAIMED);

		component.removeGraveBlock();

		player.sendSystemMessage(Component.translatable("text.monkeysmp.command.rob.success"));
		return 1;
	}

	private static int showListType(CommandContext<CommandSourceStack> context) {
		context.getSource().sendSystemMessage(Component.translatable("text.monkeysmp.command.whitelist.show_current", deathInfo(context).getGraveListMode().name()));
		return 1;
	}
	private static int addToList(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> players) {
		int i = 0;
		for (ServerPlayer player : players) {
			deathInfo(context).addToList(ResolvableProfile.createResolved(player.getGameProfile()));
			++i;
		}
		context.getSource().sendSystemMessage(Component.translatable("text.monkeysmp.command.whitelist.added_players", i, deathInfo(context).getGraveListMode().name()));
		return i > 0 ? 1 : 0;
	}
	private static int removeFromList(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> players) {
		int i = 0;
		for (ServerPlayer player : players) {
			if (deathInfo(context).removeFromList(ResolvableProfile.createResolved(player.getGameProfile())))
				++i;
		}

		context.getSource().sendSystemMessage(Component.translatable("text.monkeysmp.command.whitelist.removed_players", i, deathInfo(context).getGraveListMode().name()));
		return i > 0 ? 1 : 0;
	}
	private static int toggleListType(CommandContext<CommandSourceStack> context) {
		ListMode listMode = deathInfo(context).getGraveListMode();
		ListMode newMode = listMode == ListMode.WHITELIST ? ListMode.BLACKLIST : ListMode.WHITELIST;
		deathInfo(context).setGraveListMode(newMode);
		context.getSource().sendSystemMessage(Component.translatable("text.monkeysmp.command.whitelist.toggle", newMode.name()));

		return 1;
	}
	private static int showList(CommandContext<CommandSourceStack> context) {
		ListMode listMode = deathInfo(context).getGraveListMode();
		Set<GameProfile> affectedPlayers = deathInfo(context).getAffectedPlayers();

		StringJoiner joiner = new StringJoiner(", ");
		for (GameProfile profile : affectedPlayers) {
			joiner.add(Optional.ofNullable(profile.name()).orElse("PLAYER_NOT_FOUND"));
		}
		context.getSource().sendSystemMessage(Component.literal(listMode.name() + ": %s" + joiner));

		return 1;
	}

	public static Predicate<CommandSourceStack> requires(int level) {
		return source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(level)));
	}
}
