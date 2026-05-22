package com.gyvex.ezafk.integration.ezcountdown;

import com.gyvex.ezafk.bootstrap.Registry;
import com.gyvex.ezafk.integration.Integration;
import com.skyblockexp.ezcountdown.api.EzCountdownApi;
import com.skyblockexp.ezcountdown.api.model.Notification;
import com.skyblockexp.ezcountdown.display.DisplayType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Optional integration with the <a href="https://www.spigotmc.org/resources/ezcountdown.XXXXX/">EzCountdown</a>
 * plugin.
 *
 * <h3>What it does</h3>
 * <p>When EzCountdown is present and enabled, EzAfk delegates kick-warning
 * countdowns to EzCountdown's display system instead of driving BossBar, title,
 * and action-bar rendering through Bukkit reflection directly. This has several
 * advantages:
 * <ul>
 *   <li>EzCountdown handles cross-version display compatibility internally.</li>
 *   <li>The countdown ticks live (the remaining time decrements every second),
 *       whereas the native fallback shows a static snapshot.</li>
 *   <li>Two additional display types become available:
 *       {@code SCOREBOARD} and {@code DIALOG}, which are only supported through
 *       EzCountdown.</li>
 * </ul>
 *
 * <h3>Fallback</h3>
 * <p>If EzCountdown is not installed, or if the integration is explicitly
 * disabled, EzAfk falls back to its own Bukkit-based display code
 * ({@link com.gyvex.ezafk.compatibility.player.PlayerDisplayCompat}).
 * All configured display types (CHAT, TITLE, ACTION_BAR, BOSS_BAR) continue
 * to work without EzCountdown.
 *
 * <h3>Configuration</h3>
 * <pre>{@code
 * # config.yml
 * integration:
 *   ezcountdown: auto   # true | false | auto (default)
 * }</pre>
 * <ul>
 *   <li>{@code auto} – enabled automatically when EzCountdown is detected.</li>
 *   <li>{@code true} – always try to enable; logs a warning if not found.</li>
 *   <li>{@code false} – disabled unconditionally; native display is used.</li>
 * </ul>
 *
 * <h3>Display types</h3>
 * <p>When EzCountdown is active the following display types are supported in
 * {@code kick.warnings.displays}:
 * <pre>
 *   CHAT        – always handled by EzAfk's MessageManager (not EzCountdown)
 *   TITLE       – delegated to EzCountdown
 *   ACTION_BAR  – delegated to EzCountdown
 *   BOSS_BAR    – delegated to EzCountdown
 *   SCOREBOARD  – EzCountdown only; ignored without EzCountdown
 *   DIALOG      – EzCountdown only; ignored without EzCountdown
 * </pre>
 *
 * <h3>Obtaining the instance</h3>
 * <pre>{@code
 *   EzCountdownIntegration ez =
 *       (EzCountdownIntegration) IntegrationManager.getIntegration("ezcountdown");
 * }</pre>
 */
public class EzCountdownIntegration extends Integration {

    private EzCountdownApi api;

    /** Maps player UUID → active kick-warning countdown name. */
    private final Map<UUID, String> activeWarnings = new HashMap<>();

    /**
     * Returns the raw {@link EzCountdownApi} instance, or {@code null} if
     * EzCountdown is not available or the integration has not been loaded.
     */
    public EzCountdownApi getApi() {
        return api;
    }

    @Override
    public void load() {
        try {
            api = Bukkit.getServicesManager().load(EzCountdownApi.class);
            isSetup = api != null;
            if (isSetup) {
                Registry.get().getLogger().info("EzCountdown integration enabled.");
            } else {
                Registry.get().getLogger()
                        .warning("EzCountdown plugin found but its API service is not registered. "
                                + "EzCountdown integration disabled.");
            }
        } catch (NoClassDefFoundError | Exception ex) {
            api = null;
            isSetup = false;
            Registry.get().getLogger()
                    .warning("EzCountdown integration failed to load: " + ex.getMessage());
        }
    }

    @Override
    public void unload() {
        for (UUID uuid : new ArrayList<>(activeWarnings.keySet())) {
            removeKickWarning(uuid);
        }
        api = null;
        isSetup = false;
    }

    // -------------------------------------------------------------------------
    // Per-player kick-warning API
    // -------------------------------------------------------------------------

    /**
     * Sends a per-player kick-warning countdown through EzCountdown's display
     * system for all non-CHAT display types in {@code displays}.
     *
     * <p>The countdown targets only {@code player} via
     * {@link com.skyblockexp.ezcountdown.api.model.Notification}'s
     * {@code players} field, so other online players are not affected.
     *
     * <p>If a warning countdown is already active for this player it is stopped
     * before the new one is created.
     *
     * <p>The {@code message} string may contain {@code %seconds%}; this is
     * automatically translated to EzCountdown's live {@code {seconds}} placeholder
     * so the display decrements in real time.
     *
     * @param player   The player to warn.
     * @param message  Warning message template (supports {@code %seconds%} and
     *                 EzCountdown placeholders such as {@code {seconds}},
     *                 {@code {formatted}}).
     * @param seconds  Warning duration in seconds (also the initial countdown value).
     * @param displays EzAfk display type strings (TITLE, ACTION_BAR, BOSS_BAR,
     *                 SCOREBOARD, DIALOG).  {@code CHAT} entries are ignored here
     *                 and must be sent separately via {@code MessageManager}.
     * @return {@code true} if the notification was sent successfully.
     */
    public boolean sendKickWarning(Player player, String message, int seconds, List<String> displays) {
        if (!isSetup || api == null || player == null || displays.isEmpty()) return false;

        UUID uuid = player.getUniqueId();
        removeKickWarning(uuid); // stop any prior warning first

        EnumSet<DisplayType> displayTypes = mapDisplayTypes(displays);
        if (displayTypes.isEmpty()) return false;

        // Translate EzAfk's %seconds% placeholder to EzCountdown's live {seconds}.
        String formatMsg = message.replace("%seconds%", "{seconds}");

        Notification notification = Notification.builder()
                .duration(seconds)
                .displays(displayTypes)
                .message(formatMsg)
                .players(List.of(player))
                .build();

        Optional<String> result = api.sendNotification(notification);
        result.ifPresent(name -> activeWarnings.put(uuid, name));
        return result.isPresent();
    }

    /**
     * Stops and removes the active EzCountdown kick-warning for the given
     * player, if any.  Safe to call even when no warning is active.
     *
     * @param playerId UUID of the player whose warning should be removed.
     */
    public void removeKickWarning(UUID playerId) {
        String name = activeWarnings.remove(playerId);
        if (name != null) {
            api.stopCountdown(name);
        }
    }

    /**
     * Maps EzAfk display type strings to EzCountdown {@link DisplayType} values.
     *
     * <p>{@code CHAT} entries are excluded here — CHAT warnings are always sent
     * through EzAfk's {@code MessageManager}, not EzCountdown.  Unknown strings
     * are silently ignored.
     */
    private static EnumSet<DisplayType> mapDisplayTypes(List<String> displays) {
        EnumSet<DisplayType> result = EnumSet.noneOf(DisplayType.class);
        for (String d : displays) {
            try {
                result.add(DisplayType.valueOf(d.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {}
        }
        result.remove(DisplayType.CHAT);
        return result;
    }
}
