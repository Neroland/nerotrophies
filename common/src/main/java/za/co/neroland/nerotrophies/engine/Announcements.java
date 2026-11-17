package za.co.neroland.nerotrophies.engine;

import java.util.Set;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
//? if >=1.21.11 {
import net.minecraft.world.level.gamerules.GameRules;
//?} else {
/*import net.minecraft.world.level.GameRules;
*///?}

import za.co.neroland.nerolandcore.trophy.AwardSource;
import za.co.neroland.nerolandcore.trophy.TrophyDefinition;
import za.co.neroland.nerolandcore.trophy.TrophyTier;

import za.co.neroland.nerotrophies.config.NeroTrophiesConfig;
import za.co.neroland.nerotrophies.data.TrophyRecord;

/**
 * The chat announcement. Sent to each online player with {@code sendSystemMessage}, never through
 * vanilla's broadcast (which also echoes to the server log). This is the only place NeroTrophies puts a player's
 * name into chat, and this class never logs it.
 */
public final class Announcements {

    private Announcements() {
    }

    /**
     * Whether an award should be announced: tier listed in {@code announceTiers}, a {@code LIVE} or {@code API}
     * source, the earner's {@code announce} and {@code showName} both on, and the vanilla advancement-message rule on.
     */
    public static boolean shouldAnnounce(TrophyTier tier, AwardSource source, TrophyRecord record,
            Set<TrophyTier> tiers, boolean gameRule) {
        return tiers.contains(tier)
                && (source == AwardSource.LIVE || source == AwardSource.API)
                && record != null && record.announce() && record.showName()
                && gameRule;
    }

    /** Sends the announcement for {@code earner} if {@link #shouldAnnounce} holds. */
    public static void maybeAnnounce(MinecraftServer server, ServerPlayer earner, TrophyDefinition definition,
            AwardSource source, TrophyRecord record) {
        if (!shouldAnnounce(definition.tier(), source, record,
                NeroTrophiesConfig.parseTiersConfigured(), gameRuleOn(server))) {
            return;
        }
        Component message = Component.translatable("nerotrophies.announce." + definition.tier().getSerializedName(),
                earner.getDisplayName(), title(definition));
        for (ServerPlayer recipient : server.getPlayerList().getPlayers()) {
            recipient.sendSystemMessage(message);
        }
    }

    /** The trophy title with its description on hover. No coordinates, no dimension. */
    public static MutableComponent title(TrophyDefinition definition) {
        Component hover = Component.translatable(definition.description());
        return Component.translatable(definition.title()).withStyle(style -> style.withHoverEvent(
                //? if >=1.21.5 {
                new HoverEvent.ShowText(hover)));
                //?} else {
                /*new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));
                *///?}
    }

    private static boolean gameRuleOn(MinecraftServer server) {
        //? if >=1.21.11 {
        return server.overworld().getGameRules().get(GameRules.SHOW_ADVANCEMENT_MESSAGES);
        //?} else {
        /*return server.overworld().getGameRules().getBoolean(GameRules.RULE_ANNOUNCE_ADVANCEMENTS);
        *///?}
    }
}
