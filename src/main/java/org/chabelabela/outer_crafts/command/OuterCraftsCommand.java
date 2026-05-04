package org.chabelabela.outer_crafts.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.chabelabela.outer_crafts.OuterCrafts;
import org.chabelabela.outer_crafts.physics.CelestialBody;
import org.chabelabela.outer_crafts.registry.CelestialBodyRegistry;
import org.chabelabela.outer_crafts.timeloop.SupernovaManager;
import org.chabelabela.outer_crafts.timeloop.TimeLoopManager;
import org.chabelabela.outer_crafts.timeloop.TimeLoopPhase;
import org.chabelabela.outer_crafts.world.dimension.OuterCraftsDimensions;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * The {@code /outercrafts} (alias {@code /oc}) admin command tree.
 *
 * <p>Permission level 2 required (op-only) for any state-mutating sub-command.
 * Read-only sub-commands (status, body list) are available to everyone.
 *
 * <pre>{@code
 *   /oc status
 *   /oc loop reset                    -- force a new loop now
 *   /oc loop pause                    -- freeze loop progression
 *   /oc loop resume                   -- unfreeze
 *   /oc supernova now                 -- jump straight to supernova
 *   /oc tp <body_id>                  -- teleport caller to a celestial body's surface
 *   /oc body list                     -- list registered bodies + positions
 * }</pre>
 */
@EventBusSubscriber(modid = OuterCrafts.MODID)
public final class OuterCraftsCommand {

    private OuterCraftsCommand() {}

    /** Suggests every registered celestial body ID. */
    private static final SuggestionProvider<CommandSourceStack> BODY_SUGGESTIONS =
            (ctx, builder) -> suggestBodies(builder);

    private static CompletableFuture<Suggestions> suggestBodies(SuggestionsBuilder builder) {
        for (String id : CelestialBodyRegistry.allBodies().keySet()) {
            builder.suggest(id);
        }
        return builder.buildFuture();
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("outercrafts")
                .then(Commands.literal("status").executes(OuterCraftsCommand::statusCmd))
                .then(Commands.literal("loop")
                        .requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        .then(Commands.literal("reset").executes(OuterCraftsCommand::loopResetCmd))
                        .then(Commands.literal("pause").executes(OuterCraftsCommand::loopPauseCmd))
                        .then(Commands.literal("resume").executes(OuterCraftsCommand::loopResumeCmd)))
                .then(Commands.literal("supernova")
                        .requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        .then(Commands.literal("now").executes(OuterCraftsCommand::supernovaNowCmd)))
                .then(Commands.literal("tp")
                        .requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        .then(Commands.argument("body", StringArgumentType.word())
                                .suggests(BODY_SUGGESTIONS)
                                .executes(OuterCraftsCommand::tpCmd)))
                .then(Commands.literal("body")
                        .then(Commands.literal("list").executes(OuterCraftsCommand::bodyListCmd)));

        event.getDispatcher().register(root);
        // Short alias
        event.getDispatcher().register(Commands.literal("oc").redirect(event.getDispatcher().register(root)));
    }

    // ── Sub-command implementations ─────────────────────────────────────────

    private static int statusCmd(CommandContext<CommandSourceStack> ctx) {
        TimeLoopPhase phase = TimeLoopManager.getPhase();
        long tick = TimeLoopManager.getCurrentTick();
        int loop = TimeLoopManager.getLoopCount();
        int remainingSec = TimeLoopManager.getRemainingSeconds();
        boolean active = TimeLoopManager.isActive();

        ctx.getSource().sendSuccess(() -> Component.literal(
                "§b[Outer Crafts] §fLoop §a#%d §fphase §e%s §f(tick %d, %d s remaining, active=%s)"
                        .formatted(loop, phase, tick, remainingSec, active)
        ), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int loopResetCmd(CommandContext<CommandSourceStack> ctx) {
        TimeLoopManager.forceReset();
        ctx.getSource().sendSuccess(
                () -> Component.literal("§b[Outer Crafts] §fLoop reset triggered."), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int loopPauseCmd(CommandContext<CommandSourceStack> ctx) {
        TimeLoopManager.setActive(false);
        ctx.getSource().sendSuccess(
                () -> Component.literal("§b[Outer Crafts] §fLoop §cpaused§f."), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int loopResumeCmd(CommandContext<CommandSourceStack> ctx) {
        TimeLoopManager.setActive(true);
        ctx.getSource().sendSuccess(
                () -> Component.literal("§b[Outer Crafts] §fLoop §aresumed§f."), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int supernovaNowCmd(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        SupernovaManager.beginSupernova(server);
        ctx.getSource().sendSuccess(
                () -> Component.literal("§b[Outer Crafts] §6Supernova §ftriggered."), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int tpCmd(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String bodyId = StringArgumentType.getString(ctx, "body");
        CelestialBody body = CelestialBodyRegistry.get(bodyId);
        if (body == null) {
            ctx.getSource().sendFailure(Component.literal(
                    "§c[Outer Crafts] Unknown celestial body: " + bodyId));
            return 0;
        }
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        long tick = player.level().getGameTime();
        Vec3 center = CelestialBodyRegistry.resolveAllPositions(tick).get(bodyId);
        if (center == null) {
            ctx.getSource().sendFailure(Component.literal(
                    "§c[Outer Crafts] Could not resolve position for: " + bodyId));
            return 0;
        }
        // Place player above the surface on the +Y side of the body.
        double surfaceY = center.y + body.radius() + 4.0;
        ServerLevel solar = ctx.getSource().getServer().getLevel(OuterCraftsDimensions.SOLAR_SYSTEM_LEVEL);
        if (solar == null) {
            ctx.getSource().sendFailure(Component.literal(
                    "§c[Outer Crafts] Solar system dimension not loaded."));
            return 0;
        }
        player.teleportTo(solar, center.x, surfaceY, center.z,
                java.util.Set.<Relative>of(), player.getYRot(), 0f, true);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§b[Outer Crafts] §fTeleported to §a" + bodyId + "§f."), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int bodyListCmd(CommandContext<CommandSourceStack> ctx) {
        long tick = ctx.getSource().getServer().overworld().getGameTime();
        Map<String, Vec3> positions = CelestialBodyRegistry.resolveAllPositions(tick);

        ctx.getSource().sendSuccess(() -> Component.literal(
                "§b[Outer Crafts] §f%d registered bodies:".formatted(positions.size())), false);
        positions.forEach((id, pos) -> {
            CelestialBody body = CelestialBodyRegistry.get(id);
            String parent = body.parentId() == null ? "—" : body.parentId();
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "  §7• §a%s §7(r=%.0f, parent=%s) §faround [%.0f, %.0f, %.0f]"
                            .formatted(id, body.radius(), parent, pos.x, pos.y, pos.z)), false);
        });
        return Command.SINGLE_SUCCESS;
    }
}
