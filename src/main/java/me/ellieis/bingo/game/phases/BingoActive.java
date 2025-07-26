package me.ellieis.bingo.game.phases;

import it.unimi.dsi.fastutil.ints.IntList;
import me.ellieis.bingo.Bingo;
import me.ellieis.bingo.game.config.BingoConfig;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.*;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.common.widget.SidebarWidget;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.item.ItemPickupEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;
import xyz.nucleoid.stimuli.event.player.PlayerInventoryActionEvent;
import xyz.nucleoid.stimuli.event.world.EndPortalOpenEvent;
import xyz.nucleoid.stimuli.event.world.NetherPortalOpenEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class BingoActive {
    public GameSpace gameSpace;
    GameActivity activity;
    BingoConfig config;
    ServerWorld world;
    BlockPos spawnPos;
    GlobalWidgets widgets;
    SidebarWidget sidebar;
    HashMap<ServerPlayerEntity, Long> playersRespawning = new HashMap<>();
    ArrayList<ServerPlayerEntity> playersToRemove = new ArrayList<>();
    RegistryEntryList<Item> items;
    List<Item> disallowedItems = List.of(Items.KNOWLEDGE_BOOK, Items.BEDROCK, Items.VAULT, Items.PLAYER_HEAD, Items.CREEPER_HEAD, Items.DRAGON_HEAD, Items.PIGLIN_HEAD, Items.ZOMBIE_HEAD, Items.INFESTED_COBBLESTONE, Items.INFESTED_DEEPSLATE, Items.INFESTED_STONE, Items.INFESTED_CHISELED_STONE_BRICKS, Items.INFESTED_CRACKED_STONE_BRICKS, Items.INFESTED_MOSSY_STONE_BRICKS, Items.INFESTED_STONE_BRICKS, Items.SPAWNER, Items.TRIAL_SPAWNER, Items.END_PORTAL_FRAME, Items.BARRIER, Items.STRUCTURE_BLOCK, Items.STRUCTURE_VOID, Items.SUSPICIOUS_GRAVEL, Items.SUSPICIOUS_SAND);
    List<Item> hardItems = List.of(Items.ELYTRA, Items.DRAGON_BREATH, Items.BEACON, Items.NETHER_STAR, Items.WITHER_SKELETON_SKULL, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_BOOTS, Items.CHAINMAIL_HELMET, Items.CHAINMAIL_LEGGINGS, Items.NETHERITE_INGOT, Items.NETHERITE_AXE, Items.NETHERITE_BLOCK, Items.NETHERITE_BOOTS, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_HOE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_HELMET, Items.NETHERITE_PICKAXE, Items.NETHERITE_SWORD, Items.NETHERITE_SHOVEL, Items.PITCHER_POD, Items.PITCHER_PLANT, Items.TORCHFLOWER, Items.TORCHFLOWER_SEEDS, Items.POPPED_CHORUS_FRUIT, Items.CHORUS_FLOWER, Items.CHORUS_FRUIT, Items.CHORUS_PLANT);
    public HashMap<ServerPlayerEntity, List<List<BingoSlot>>> bingoCards = new HashMap<>();

    public BingoActive(GameSpace gameSpace, GameActivity activity, BingoConfig config, ServerWorld world, BlockPos spawnPos) {
        this.gameSpace = gameSpace;
        this.activity = activity;
        this.config = config;
        this.world = world;
        this.spawnPos = spawnPos;
        this.widgets = GlobalWidgets.addTo(activity);
        this.sidebar = widgets.addSidebar();

        world.setSpawnPos(spawnPos, 0);
        Bingo.activeGames.add(this);
        BingoActive.rules(activity);

        this.items = RegistryEntryList.of(world.getRegistryManager()
                .getOrThrow(RegistryKeys.ITEM)
                .streamEntries()
                .filter(this::isItemEnabled)
                .toList());
        activity.listen(GameActivityEvents.DESTROY, (_reason) -> {
            Bingo.activeGames.remove(this);
        });
        activity.listen(GamePlayerEvents.ACCEPT, acceptor ->
            acceptor.teleport(world, spawnPos.toCenterPos()).thenRunForEach(plr -> plr.changeGameMode(GameMode.SPECTATOR))
        );
        activity.listen(GamePlayerEvents.OFFER, JoinOffer::acceptSpectators);
        activity.listen(GamePlayerEvents.JOIN, (plr) -> {
            sidebar.addPlayer(plr);
            plr.changeGameMode(GameMode.SPECTATOR);
        });
        activity.listen(GameActivityEvents.TICK, this::onTick);
        activity.listen(PlayerDeathEvent.EVENT, this::onDeath);
        activity.listen(ItemPickupEvent.EVENT, this::onItemPickup);
        activity.listen(PlayerInventoryActionEvent.EVENT, this::onInventoryAction);
        //activity.listen(ItemCraftEvent.EVENT, this::onCraft);
        activity.listen(NetherPortalOpenEvent.EVENT, (_world, _pos) -> config.hasNether() ? EventResult.ALLOW : EventResult.DENY);
        activity.listen(EndPortalOpenEvent.EVENT, (_context, _result) -> config.hasEnd() ? EventResult.ALLOW : EventResult.DENY);
        gameSpace.getPlayers().forEach(plr -> {
            plr.changeGameMode(GameMode.SURVIVAL);
            bingoCards.put(plr, generateBingoCard());
            world.getServer().getCommandManager().sendCommandTree(plr);
        });
    }

    private boolean checkForWin(ServerPlayerEntity plr) {
        List<List<BingoSlot>> bingoCard = bingoCards.get(plr);

        boolean horizontalWin = true;
        for (List<BingoSlot> col : bingoCard) {
            for (BingoSlot slot : col) {
                if (!slot.marked()) {
                    horizontalWin = false;
                    break;
                }
            }
            if (horizontalWin) {
                break;
            }
        }

        boolean verticalWin = true;
        for (int rowIndex = 0; rowIndex < 5; rowIndex++) {
            List<BingoSlot> row = new ArrayList<>();
            for (int colIndex = 0; colIndex < 5; colIndex ++) {
                row.add(bingoCard.get(colIndex).get(rowIndex));
            }

            for (BingoSlot slot : row) {
                if (!slot.marked()) {
                    verticalWin = false;
                    break;
                }
            }

            if (verticalWin) {
                break;
            }
        }

        boolean diagonalWin = true;
        // upper left to bottom right check
        for (int index = 0; index < 5; index++) {
            if (!bingoCard.get(index).get(index).marked()) {
                diagonalWin = false;
                break;
            }
        }
        // upper right to bottom left check
        if (!diagonalWin) {
            diagonalWin = true;
            for (int index = 4; index >= 0; index--) {
                BingoSlot slot = bingoCard.get(4 - index).get(index);
                System.out.println(slot);
                if (!slot.marked()) {
                    diagonalWin = false;
                    break;
                }
            }
        }
        return horizontalWin || verticalWin || diagonalWin;
    }

    private void checkForScore(ServerPlayerEntity plr, ItemStack stack) {
        List<List<BingoSlot>> bingoCard = bingoCards.get(plr);
        int colIndex = 99;
        int rowIndex = 99;
        for (List<BingoSlot> col : bingoCard) {
            for (BingoSlot slot: col.stream().filter(slot -> !slot.marked()).toList()) {
                if (slot.item().equals(stack.getItem())) {
                    rowIndex = col.indexOf(slot);
                    colIndex = bingoCard.indexOf(col);
                }
            }
        }
        if (colIndex != 99 ) {
            ServerWorld plrWorld = plr.getWorld();
            bingoCard.get(colIndex).set(rowIndex, new BingoSlot(stack.getItem(), true));
            IntList colors = IntList.of(DyeColor.ORANGE.getFireworkColor());
            FireworkExplosionComponent explode = new FireworkExplosionComponent(FireworkExplosionComponent.Type.BURST, colors, IntList.of(), false, false);
            plrWorld.playSound(null, plr.getBlockPos(), SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1, 1);
            checkForWin(plr);
        }
    }
    private EventResult onInventoryAction(ServerPlayerEntity plr, int slot, SlotActionType actionType, int button) {
        if (slot >= 0 && slot <= 36) {
            ItemStack stack = plr.getInventory().getStack(slot);
            if (stack != null) {
                checkForScore(plr, stack);
            }
        }
        return EventResult.PASS;
    }

    private EventResult onItemPickup(ServerPlayerEntity plr, ItemEntity item, ItemStack stack) {
        checkForScore(plr, stack);
        return EventResult.PASS;
    }
    private Item generateItem(List<List<BingoSlot>> bingoCard) {
        Item item = items.getRandom(Random.create()).orElseThrow().value();
        for (List<BingoSlot> col : bingoCard) {
            for (BingoSlot slot : col) {
                if (slot.item().equals(item)) {
                    item = generateItem(bingoCard);
                    break;
                }
            }
        }
        return item;
    }

    private List<List<BingoSlot>> generateBingoCard() {
        List<List<BingoSlot>> bingoCard = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            List<BingoSlot> row = new ArrayList<>();
            for (int j = 0; j < 5; j++) {
                Item item = generateItem(bingoCard);
                row.add(new BingoSlot(item, false));
            }
            bingoCard.add(row);
        }
        return bingoCard;
    }

    private boolean isItemEnabled(RegistryEntry.Reference<Item> entry) {
        if (entry.getKey().isPresent() && !entry.getKey().get().getValue().getNamespace().equals(Identifier.DEFAULT_NAMESPACE)) {
            return false;
        }

        Item item = entry.value();
        if (!config.hardMode() && hardItems.contains(item)) {
            return false;
        }
        // to-do: disallow pottery sherds
        // to-do: disallow armor trims
        return !(item instanceof OperatorOnlyBlockItem) && !(item instanceof AirBlockItem) && !(item instanceof SpawnEggItem) && !disallowedItems.contains(item) && item.isEnabled(world.getEnabledFeatures());
    }

    public static void rules(GameActivity activity) {
        activity.allow(GameRuleType.PVP);
        activity.allow(GameRuleType.FALL_DAMAGE);
        activity.allow(GameRuleType.INTERACTION);
        activity.allow(GameRuleType.CRAFTING);
        activity.allow(GameRuleType.BREAK_BLOCKS);
        activity.allow(GameRuleType.HUNGER);
    }
    public static void Open(GameSpace gameSpace, BingoConfig config, ServerWorld world, BlockPos spawnPos) {
        gameSpace.setActivity(activity -> {
            new BingoActive(gameSpace, activity, config, world, spawnPos);
        });
    }

    private EventResult onDeath(ServerPlayerEntity plr, DamageSource source) {
        Text deathMessage = plr.getDamageTracker().getDeathMessage();
        gameSpace.getPlayers().forEach(plr1 -> plr1.sendMessage(deathMessage));
        playersRespawning.put(plr, gameSpace.getTime() + 100);
        plr.getInventory().dropAll();
        plr.changeGameMode(GameMode.SPECTATOR);
        return EventResult.DENY;
    }

    private void onTick() {
        long time = gameSpace.getTime();
        playersRespawning.forEach((plr, respawnTime) -> {
            long timeLeft = respawnTime - time;
            if (timeLeft <= 0) {
                plr.changeGameMode(GameMode.SURVIVAL);
                plr.getHungerManager().setFoodLevel(20);
                plr.getHungerManager().setSaturationLevel(20);
                plr.setHealth(plr.getMaxHealth());
                plr.teleport(world, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), Set.of(), 0, 0, false);
                plr.sendMessage(Text.literal(""), true);
                playersToRemove.add(plr);
            } else {
                plr.sendMessage(Text.translatable("bingo.respawning", (timeLeft + 20) / 20), true);
            }
        });
        playersToRemove.forEach(plr -> playersRespawning.remove(plr));
        playersToRemove.clear();
    }
}
