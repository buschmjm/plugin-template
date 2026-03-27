package com.mmorpg.stats;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Player command /trade &lt;player&gt; &lt;amount&gt; - propose a gold trade (requires /trade accept).
 *   /trade &lt;player&gt; &lt;amount&gt; - send gold offer
 *   /trade accept              - accept a pending offer
 *   /trade decline             - decline a pending offer
 */
public class TradeCommand extends AbstractAsyncCommand {

    /** Pending trade offers: targetUUID -> PendingTrade */
    private static final ConcurrentHashMap<String, PendingTrade> pendingTrades = new ConcurrentHashMap<>();
    private static final long OFFER_TIMEOUT_MS = 60_000; // 60 seconds

    private record PendingTrade(String senderUuid, String senderName, int amount, long timestamp) {
        boolean isExpired() { return System.currentTimeMillis() - timestamp > OFFER_TIMEOUT_MS; }
    }

    // -- Subcommand: /trade accept --
    private static class AcceptSubcommand extends AbstractAsyncCommand {
        AcceptSubcommand() { super("accept", "Accept a pending trade offer"); }

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
                PlayerRef receiverRef = store.getComponent(ref, PlayerRef.getComponentType());
                if (receiverRef == null) return;
                String receiverUuid = receiverRef.getUuid().toString();

                PendingTrade offer = pendingTrades.remove(receiverUuid);
                if (offer == null) {
                    ctx.sendMessage(Message.raw("You have no pending trade offers."));
                    return;
                }
                if (offer.isExpired()) {
                    ctx.sendMessage(Message.raw("That trade offer has expired."));
                    return;
                }

                // Find sender online
                PlayerRef senderPlayerRef = null;
                for (PlayerRef pr : world.getPlayerRefs()) {
                    if (pr.getUuid().toString().equals(offer.senderUuid)) {
                        senderPlayerRef = pr;
                        break;
                    }
                }
                if (senderPlayerRef == null) {
                    ctx.sendMessage(Message.raw("The sender is no longer online."));
                    return;
                }

                Ref<EntityStore> senderRef = senderPlayerRef.getReference();
                if (senderRef == null || !senderRef.isValid()) {
                    ctx.sendMessage(Message.raw("The sender is not available."));
                    return;
                }

                var goldType = StatsPlugin.getInstance().getPlayerGoldDataType();
                PlayerGoldData senderGold = store.getComponent(senderRef, goldType);
                PlayerGoldData receiverGold = store.getComponent(ref, goldType);

                if (senderGold == null || receiverGold == null) {
                    ctx.sendMessage(Message.raw("Could not access gold data."));
                    return;
                }

                if (!senderGold.canAfford(offer.amount)) {
                    ctx.sendMessage(Message.raw("The sender no longer has enough gold."));
                    senderPlayerRef.sendMessage(Message.raw("Trade failed - you don't have "
                            + offer.amount + " gold anymore."));
                    return;
                }

                // Execute trade
                senderGold.spend(offer.amount);
                receiverGold.addGold(offer.amount);

                // Log transactions
                GoldLedger.log(offer.senderName, "TRADE_SEND", -offer.amount,
                        senderGold.getGold(), "to " + receiverRef.getUsername());
                GoldLedger.log(receiverRef.getUsername(), "TRADE_RECV", offer.amount,
                        receiverGold.getGold(), "from " + offer.senderName);

                ctx.sendMessage(Message.raw("* Trade accepted! Received " + offer.amount
                        + " gold from " + offer.senderName + "."));
                senderPlayerRef.sendMessage(Message.raw("* " + receiverRef.getUsername()
                        + " accepted your trade! Sent " + offer.amount
                        + " gold. Balance: " + senderGold.getGold()));
            }, world);
        }
    }

    // -- Subcommand: /trade decline --
    private static class DeclineSubcommand extends AbstractAsyncCommand {
        DeclineSubcommand() { super("decline", "Decline a pending trade offer"); }

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
                PlayerRef receiverRef = store.getComponent(ref, PlayerRef.getComponentType());
                if (receiverRef == null) return;

                PendingTrade offer = pendingTrades.remove(receiverRef.getUuid().toString());
                if (offer == null) {
                    ctx.sendMessage(Message.raw("You have no pending trade offers."));
                    return;
                }

                ctx.sendMessage(Message.raw("Trade offer from " + offer.senderName + " declined."));

                // Notify sender if online
                for (PlayerRef pr : world.getPlayerRefs()) {
                    if (pr.getUuid().toString().equals(offer.senderUuid)) {
                        pr.sendMessage(Message.raw(receiverRef.getUsername()
                                + " declined your trade offer."));
                        break;
                    }
                }
            }, world);
        }
    }

    private final RequiredArg<String> playerArg;
    private final RequiredArg<Integer> amountArg;

    public TradeCommand() {
        super("trade", "Send gold to another player");
        this.playerArg = withRequiredArg("player", "Player name", ArgTypes.STRING);
        this.amountArg = withRequiredArg("amount", "Amount of gold to send", ArgTypes.INTEGER);
        addSubCommand(new AcceptSubcommand());
        addSubCommand(new DeclineSubcommand());
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
        String targetName = ctx.get(playerArg);
        int amount = ctx.get(amountArg);

        return CompletableFuture.runAsync(() -> {
            if (amount <= 0) {
                ctx.sendMessage(Message.raw("Amount must be positive."));
                return;
            }

            PlayerRef senderRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (senderRef == null) return;

            PlayerRef targetPlayerRef = findPlayer(world, targetName);
            if (targetPlayerRef == null) {
                ctx.sendMessage(Message.raw("Player not found: " + targetName));
                return;
            }

            if (senderRef.getUuid().equals(targetPlayerRef.getUuid())) {
                ctx.sendMessage(Message.raw("You can't trade with yourself!"));
                return;
            }

            var goldType = StatsPlugin.getInstance().getPlayerGoldDataType();
            PlayerGoldData senderGold = store.getComponent(ref, goldType);
            if (senderGold == null || !senderGold.canAfford(amount)) {
                ctx.sendMessage(Message.raw("Not enough gold! You have "
                        + (senderGold != null ? senderGold.getGold() : 0)
                        + " but tried to send " + amount + "."));
                return;
            }

            // Create pending offer
            String targetUuid = targetPlayerRef.getUuid().toString();
            pendingTrades.put(targetUuid, new PendingTrade(
                    senderRef.getUuid().toString(), senderRef.getUsername(),
                    amount, System.currentTimeMillis()));

            ctx.sendMessage(Message.raw("* Trade offer sent to " + targetPlayerRef.getUsername()
                    + " for " + amount + " gold. Expires in 60s."));
            targetPlayerRef.sendMessage(Message.raw("* " + senderRef.getUsername()
                    + " wants to send you " + amount + " gold!"));
            targetPlayerRef.sendMessage(Message.raw("  Type /trade accept or /trade decline"));
        }, world);
    }

    /** Remove expired pending trades. */
    static void cleanupExpired() {
        pendingTrades.entrySet().removeIf(e -> e.getValue().isExpired());
    }

    /** Remove any pending trade involving this player UUID (as sender or target). */
    static void removeFor(String uuid) {
        pendingTrades.remove(uuid);
        pendingTrades.entrySet().removeIf(e -> e.getValue().senderUuid().equals(uuid));
    }

    /** Find a PlayerRef by username prefix in the same world. */
    private static PlayerRef findPlayer(World world, String name) {
        String lower = name.toLowerCase();
        for (PlayerRef pr : world.getPlayerRefs()) {
            if (pr.getUsername().toLowerCase().startsWith(lower)) {
                return pr;
            }
        }
        return null;
    }
}
