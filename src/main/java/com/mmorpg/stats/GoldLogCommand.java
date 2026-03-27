package com.mmorpg.stats;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Player command /goldlog - view your recent gold transactions.
 * Shows the last 15 entries from the gold ledger.
 */
public class GoldLogCommand extends AbstractAsyncCommand {

    private static final int MAX_ENTRIES = 15;
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("MM/dd HH:mm").withZone(ZoneId.systemDefault());

    public GoldLogCommand() {
        super("goldlog", "View your recent gold transactions");
    }

    @Override
    protected boolean canGeneratePermission() { return false; }

    @Nonnull
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
        Ref<EntityStore> ref = ctx.senderAsPlayerRef();
        if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);

        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();

        return CompletableFuture.runAsync(() -> {
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef == null) return;

            String name = playerRef.getUsername();
            List<String> entries = GoldLedger.getRecentTransactions(name, MAX_ENTRIES);

            if (entries.isEmpty()) {
                ctx.sendMessage(Message.raw("No gold transactions found."));
                return;
            }

            ctx.sendMessage(Message.raw("=== Gold Log (last " + entries.size() + ") ==="));

            for (String entry : entries) {
                // Format: timestamp|playerName|TYPE|±amount|bal:N|detail
                String[] parts = entry.split("\\|");
                if (parts.length < 6) continue;

                String when;
                try {
                    when = FMT.format(Instant.parse(parts[0]));
                } catch (Exception e) {
                    when = "??";
                }

                String type = parts[2];
                String amount = parts[3];
                String balance = parts[4].replace("bal:", "");
                String detail = parts[5];

                ctx.sendMessage(Message.raw("  " + when + " | " + type
                        + " " + amount + " | bal:" + balance + " | " + detail));
            }

            ctx.sendMessage(Message.raw("==============================="));
        }, world);
    }
}
