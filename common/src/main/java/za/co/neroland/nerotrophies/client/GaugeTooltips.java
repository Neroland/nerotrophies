package za.co.neroland.nerotrophies.client;

import java.util.List;

import net.minecraft.network.chat.Component;

/**
 * The gauge tooltips for the Trophy Book's bars: each bar says what it counts, how much (earned of total,
 * with the percent) and what is left. Trophy progress moves per deed rather than over time, so no bar has a
 * per-second rate. Builders append lines to a list the screen already shows. Numbers come from
 * {@link GaugeMath}; every word is an English source lang key. Only the reader's own record, already on
 * screen — no other player, no gamertag, no positions.
 */
public final class GaugeTooltips {

    private GaugeTooltips() {
    }

    private static Component tr(String key, Object... args) {
        return Component.translatable("nerotrophies.gui.gauge." + key, args);
    }

    /** A count bar (trophies in a tab, collections, trophies in a collection): percent and what is left. */
    public static void count(List<Component> out, long earned, long total) {
        if (total <= 0) {
            out.add(tr("count.none"));
            return;
        }
        out.add(tr("count.percent", GaugeMath.percent(earned, total)));
        long left = Math.max(0L, total - earned);
        out.add(left == 0 ? tr("count.done") : tr("count.left", GaugeMath.grouped(left)));
    }

    /** One trophy's progress bar: amount / target, percent, and how much more earns it. */
    public static void progress(List<Component> out, long progress, long target) {
        if (target <= 0) {
            return;
        }
        long shown = Math.max(0L, Math.min(progress, target));
        out.add(tr("progress.amount", GaugeMath.grouped(shown), GaugeMath.grouped(target),
                GaugeMath.percent(shown, target)));
        long left = target - shown;
        if (left > 0) {
            out.add(tr("progress.left", GaugeMath.grouped(left)));
        }
    }
}
