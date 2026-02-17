package fr.swordcraft.scoin;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Collection;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ScoinMod implements ModInitializer {

    public static final String MOD_ID = "scoin";

    // Item ID: scoin:s_coin (nom visible via lang: "S coin")
    public static final Item S_COIN = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "s_coin"),
            new Item(new Item.Settings())
    );

    public static final ItemGroup SCOIN_GROUP = Registry.register(
            Registries.ITEM_GROUP,
            new Identifier(MOD_ID, "group"),
            FabricItemGroup.builder()
                    .displayName(Text.translatable("itemGroup.scoin"))
                    .icon(() -> new ItemStack(S_COIN))
                    .entries((ctx, entries) -> entries.add(S_COIN))
                    .build()
    );

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            dispatcher.register(
                    literal("scoin")
                            .requires(src -> src.hasPermissionLevel(2))

                            .then(literal("give")
                                    .then(argument("targets", EntityArgumentType.players())
                                            .then(argument("amount", IntegerArgumentType.integer(1, 64000))
                                                    .executes(ctx -> {
                                                        Collection<ServerPlayerEntity> targets =
                                                                EntityArgumentType.getPlayers(ctx, "targets");
                                                        int amount = IntegerArgumentType.getInteger(ctx, "amount");

                                                        int total = 0;
                                                        for (ServerPlayerEntity p : targets) {
                                                            total += give(p, amount);
                                                        }

                                                        final int totalFinal = total; // ✅ important pour la lambda
                                                        ctx.getSource().sendFeedback(
                                                                () -> Text.literal("✅ Donné " + totalFinal + " S coin(s)."),
                                                                true
                                                        );
                                                        return 1;
                                                    })
                                            )
                                    )
                            )

                            .then(literal("count")
                                    .then(argument("target", EntityArgumentType.player())
                                            .executes(ctx -> {
                                                ServerPlayerEntity p = EntityArgumentType.getPlayer(ctx, "target");
                                                int c = count(p);
                                                final int cFinal = c; // ✅ safe

                                                ctx.getSource().sendFeedback(
                                                        () -> Text.literal("💰 " + p.getName().getString() + " a " + cFinal + " S coin(s)."),
                                                        false
                                                );
                                                return cFinal;
                                            })
                                    )
                            )
            );

        });
    }

    private static int give(ServerPlayerEntity player, int amount) {
        int remaining = amount;
        int given = 0;

        while (remaining > 0) {
            int toGive = Math.min(64, remaining);
            ItemStack stack = new ItemStack(S_COIN, toGive);

            boolean inserted = player.getInventory().insertStack(stack);
            if (!inserted) {
                player.dropItem(stack, false);
            }

            given += toGive;
            remaining -= toGive;
        }

        return given;
    }

    private static int count(ServerPlayerEntity player) {
        int total = 0;

        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack st = player.getInventory().getStack(i);
            if (!st.isEmpty() && st.isOf(S_COIN)) {
                total += st.getCount();
            }
        }

        return total;
    }
}


