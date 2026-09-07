package me.ellieis.bingo.game.phases;


import me.ellieis.bingo.Bingo;
import me.ellieis.bingo.BingoCardCommand;
import me.ellieis.bingo.ItemCraftEvent;
import me.ellieis.bingo.game.config.BingoConfig;
import net.minecraft.SharedConstants;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.dialog.*;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;
import net.minecraft.world.item.AirItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelData;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameCloseReason;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.common.team.*;
import xyz.nucleoid.plasmid.api.game.common.widget.SidebarWidget;
import xyz.nucleoid.plasmid.api.game.config.GameConfig;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinIntent;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.plasmid.api.util.PlayerRef;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.item.ItemPickupEvent;
import xyz.nucleoid.stimuli.event.player.PlayerC2SPacketEvent;
import xyz.nucleoid.stimuli.event.player.PlayerChatEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;
import xyz.nucleoid.stimuli.event.world.EndPortalOpenEvent;
import xyz.nucleoid.stimuli.event.world.NetherPortalOpenEvent;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class BingoActive {
    public GameSpace gameSpace;
    final long startTime;
    boolean gameWon = false;
    long gameWinTime = 0;
    long timeoutTime;
    GameActivity activity;
    BingoConfig config;
    ServerLevel level;
    BlockPos spawnPos;
    GlobalWidgets widgets;
    SidebarWidget sidebar;
    Optional<TeamSelectionLobby> teamSelection;
    Optional<TeamManager> teamManager;
    HashMap<PlayerRef, GameTeamKey> playerTeams;
    HashMap<ServerPlayer, Long> playersRespawning = new HashMap<>();
    HashMap<PlayerRef, PlayerPos> lastPlayerPos = new HashMap<>();
    ArrayList<ServerPlayer> playersToRemove = new ArrayList<>();
    HolderSet<Item> items;
    List<Item> hardItems = List.of(Items.CREEPER_HEAD, Items.DRAGON_HEAD, Items.PIGLIN_HEAD, Items.ZOMBIE_HEAD, Items.ELYTRA, Items.DRAGON_BREATH, Items.BEACON, Items.NETHER_STAR, Items.WITHER_SKELETON_SKULL, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_BOOTS, Items.CHAINMAIL_HELMET, Items.CHAINMAIL_LEGGINGS, Items.NETHERITE_INGOT, Items.NETHERITE_AXE, Items.NETHERITE_BLOCK, Items.NETHERITE_BOOTS, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_HOE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_HELMET, Items.NETHERITE_PICKAXE, Items.NETHERITE_SWORD, Items.NETHERITE_SHOVEL, Items.PITCHER_POD, Items.PITCHER_PLANT, Items.TORCHFLOWER, Items.TORCHFLOWER_SEEDS, Items.POPPED_CHORUS_FRUIT, Items.CHORUS_FLOWER, Items.CHORUS_FRUIT);
    final List<List<BingoSlot>> universalCard;
    public HashMap<GameTeamKey, List<List<BingoSlot>>> teamBingoCards = new HashMap<>();
    public HashMap<PlayerRef, List<List<BingoSlot>>> bingoCards = new HashMap<>();
    public static EquipmentSlot slotToEquipmentSlot(int index) {
        return switch (index) {
            case 98 -> EquipmentSlot.MAINHAND;
            case 99 -> EquipmentSlot.OFFHAND;
            case 100 -> EquipmentSlot.FEET;
            case 101 -> EquipmentSlot.LEGS;
            case 102 -> EquipmentSlot.CHEST;
            case 103 -> EquipmentSlot.HEAD;
            default -> null;
        };
    }
    public BingoActive(GameSpace gameSpace, GameActivity activity, BingoConfig config, ServerLevel level, BlockPos spawnPos, Optional<TeamSelectionLobby> teamSelection, Optional<TeamManager> teamManager) {
        this.gameSpace = gameSpace;
        this.activity = activity;
        this.config = config;
        this.level = level;
        this.spawnPos = spawnPos;
        this.teamSelection = teamSelection;
        this.teamManager = teamManager;
        this.widgets = GlobalWidgets.addTo(activity);
        this.sidebar = widgets.addSidebar();
        this.startTime = level.getGameTime();
        this.timeoutTime = config.timeLimit() + this.startTime;
        this.playerTeams = new HashMap<>();
        level.setRespawnData(LevelData.RespawnData.of(level.dimension(), spawnPos, 0, 0));
        Bingo.activeGames.add(this);
        BingoActive.rules(activity);
        teamSelection.ifPresent(action -> {
            action.allocate(gameSpace.getPlayers().participants(), (key, plr) -> {
                playerTeams.put(new PlayerRef(plr.getUUID()), key);
                teamManager.get().addPlayerTo(plr, key);
            });
        });
        this.items = HolderSet.direct(level.registryAccess()
                .lookupOrThrow(Registries.ITEM)
                .listElements()
                .filter(this::isItemEnabled)
                .toList());
        activity.listen(GameActivityEvents.DESTROY, (_reason) -> {
            Bingo.activeGames.remove(this);
        });
        activity.listen(GamePlayerEvents.ACCEPT, acceptor -> {
            return acceptor.teleport((gameProfile -> {
                if (acceptor.intent() == JoinIntent.PLAY ) {
                    UUID id = gameProfile.id();
                    AtomicReference<Vec3> playerPos = new AtomicReference<>();
                    AtomicReference<ServerLevel> playerlevel = new AtomicReference<>();
                    PlayerRef ref = new PlayerRef(id);
                    teamManager.ifPresent((manager) -> {
                        if (playerTeams.containsKey(ref)) {
                            manager.addPlayerTo(ref, playerTeams.get(ref));
                        } else {
                            manager.addPlayerTo(ref, manager.getSmallestTeam());
                        }
                    });
                    if (lastPlayerPos.containsKey(ref)) {
                        PlayerPos obj = lastPlayerPos.get(ref);
                        playerlevel.set(obj.level());
                        playerPos.set(obj.pos());
                    }
                    if (playerPos.get() == null) {
                        return new xyz.nucleoid.plasmid.api.util.PlayerPos(level, Vec3.atCenterOf(spawnPos), 0, 0);
                    } else {
                        return new xyz.nucleoid.plasmid.api.util.PlayerPos(playerlevel.get(), playerPos.get(), 0, 0);

                    }
                } else {
                    return new xyz.nucleoid.plasmid.api.util.PlayerPos(level, Vec3.atCenterOf(spawnPos), 0, 0);
                }
            }));
        });
        activity.listen(GamePlayerEvents.OFFER, JoinOffer::accept);
        activity.listen(GamePlayerEvents.JOIN, (plr) -> {
            sidebar.addPlayer(plr);
            if (gameSpace.getPlayers().participants().contains(plr)) {
                plr.setGameMode(GameType.SURVIVAL);
                PlayerRef ref = new PlayerRef(plr.getUUID());
                if (lastPlayerPos.containsKey(ref)) {
                    PlayerPos obj = lastPlayerPos.get(ref);
                    Inventory currentInventory = plr.getInventory();
                    obj.inventory().forEach(currentInventory::setItem);
                    for (int i = 98; i <= 103; i++) {
                        ItemStack stack = obj.inventory().get(i);
                        plr.setItemSlot(slotToEquipmentSlot(i), stack);
                    }
                }

                generateCardForPlayer(plr);
            } else {
                plr.setGameMode(GameType.SPECTATOR);
            }
        });
        activity.listen(GamePlayerEvents.LEAVE, (plr) -> {
            Inventory playerInventory = plr.getInventory();
            HashMap<Integer, ItemStack> inventory = new HashMap<>();
            // inventory slots
            for (int i = 0; i <= 35; i++) {
                inventory.put(i, playerInventory.getItem(i));
            }
            // armor slots
            for (int i = 98; i <= 103; i++) {
                inventory.put(i, plr.getItemBySlot(slotToEquipmentSlot(i)));
            }
            lastPlayerPos.put(new PlayerRef(plr.getUUID()), new PlayerPos(plr.position(), plr.level(), plr, inventory));
            playersRespawning.remove(plr);
        });
        activity.listen(GameActivityEvents.TICK, this::onTick);
        activity.listen(PlayerDeathEvent.EVENT, this::onDeath);
        activity.listen(ItemPickupEvent.EVENT, this::onItemPickup);
        activity.listen(ItemCraftEvent.EVENT, this::onCraft);
        activity.listen(PlayerChatEvent.EVENT, (plr, message, params) -> {
            String content = message.decoratedContent().getString();
            String[] args = content.split(" ");
            if (args.length > 0) {
                if (args[0].equals("!bingocard")) {
                    if (args.length > 1) {
                        ServerPlayer otherPlr = gameSpace.getServer().getPlayerList().getPlayerByName(args[1]);
                        if (otherPlr != null) {
                            if (BingoCardCommand.showGui(plr, otherPlr)) {
                                return EventResult.DENY;
                            }
                        }
                    }
                    BingoCardCommand.showGui(plr, plr);
                    return EventResult.DENY;
                }
            }

            return EventResult.PASS;
        });
        activity.listen(PlayerC2SPacketEvent.EVENT, this::onPacketSend);
        activity.listen(NetherPortalOpenEvent.EVENT, (_level, _pos) -> config.hasNether() ? EventResult.ALLOW : EventResult.DENY);
        activity.listen(EndPortalOpenEvent.EVENT, (_context, _result) -> config.hasEnd() ? EventResult.ALLOW : EventResult.DENY);
        sidebar.setTitle(GameConfig.shortName(activity.getGameSpace().getMetadata().sourceConfig()).copy().withStyle(ChatFormatting.GOLD));
        updateSidebar();

        if (!config.separate()) {
            universalCard = generateBingoCard();
        } else {
            universalCard = null;
        }

        gameSpace.getPlayers().participants().forEach(plr -> {
            plr.setGameMode(GameType.SURVIVAL);
            generateCardForPlayer(plr);
            plr.awardRecipes(level.recipeAccess().getRecipes());
            MinecraftServer server = level.getServer();
            server.getCommands().sendCommands(plr);
            sidebar.addPlayer(plr);
            BingoCardCommand.showGui(plr, plr);
            plr.sendSystemMessage(Component.translatable("bingo.config.enabled_options"));
            if (config.separate()) {
                String key = "bingo.config.separate";
                plr.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.GOLD).withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(Component.translatable(key + ".desc")))));
            }
            if (config.lockout()) {
                String key = "bingo.config.lockout";
                plr.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.GOLD).withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(Component.translatable(key + ".desc")))));
            }
            if (config.hardMode()) {
                String key = "bingo.config.hard_mode";
                plr.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.GOLD).withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(Component.translatable(key + ".desc")))));
            }
            if (config.genericArmorTrimDrops()) {
                String key = "bingo.config.generic_armor_trim_drops";
                plr.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.GOLD).withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(Component.translatable(key + ".desc")))));
            }
            if (config.genericSherdDrops()) {
                String key = "bingo.config.generic_sherd_drops";
                plr.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.GOLD).withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(Component.translatable(key + ".desc")))));
            }
            if (config.genericMusicDiscDrops()) {
                String key = "bingo.config.generic_music_disc_drops";
                plr.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.GOLD).withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(Component.translatable(key + ".desc")))));
            }
        });
    }

    private EventResult onPacketSend(ServerPlayer player, Packet<?> packet) {
        if (packet instanceof ServerboundSeenAdvancementsPacket) {
            player.closeContainer();
            BingoCardCommand.showGui(player, player);
        }
        return EventResult.PASS;
    }

    private void deepCopyCard(List<List<BingoSlot>> original, List<List<BingoSlot>> copy) {
        // deep copy is necessary so that the underlying object references don't get shared
        for (int colIndex = 0; colIndex < 5; colIndex++) {
            List<BingoSlot> row = new ArrayList<>();
            for (int rowIndex = 0; rowIndex < 5; rowIndex++) {
                row.add(rowIndex, new BingoSlot(original.get(colIndex).get(rowIndex)));
            }
            copy.add(colIndex, row);
        }
    }

    private void generateCardForPlayer(ServerPlayer plr) {
        PlayerRef ref = new PlayerRef(plr.getUUID());
        if (bingoCards.containsKey(ref)) {
            return;
        }
        if (!config.separate()) {
            List<List<BingoSlot>> card = new ArrayList<>();
            if (playerTeams.containsKey(ref)) {
                GameTeamKey key = playerTeams.get(ref);
                if (teamBingoCards.containsKey(key)) {
                    card = teamBingoCards.get(key);
                } else {
                    deepCopyCard(universalCard, card);
                    teamBingoCards.put(key, card);
                }
            } else {
                deepCopyCard(universalCard, card);
            }
            bingoCards.put(ref, card);
        } else {
            if (playerTeams.containsKey(ref)) {
                GameTeamKey key = playerTeams.get(ref);
                bingoCards.put(ref, teamBingoCards.computeIfAbsent(key, (_key) -> generateBingoCard()));
            } else {
                bingoCards.put(ref, generateBingoCard());
            }
        }
    }
    private void updateSidebar() {
        sidebar.set(content -> {
            content.add(CommonComponents.EMPTY);
            content.add(Component.translatable("bingo.sidebar"));
            content.add(Component.translatable("bingo.sidebar.desc", Component.literal("!bingocard").withStyle(ChatFormatting.GOLD)));
            content.add(Component.translatable("bingo.sidebar.desc2", Component.keybind("key.advancements").withStyle(ChatFormatting.GOLD)));
            content.add(Component.translatable("bingo.sidebar.desc3"));
            if (config.timeLimit() != 0) {
                long timeLeft = (long) Math.abs(Math.floor((level.getGameTime() / SharedConstants.TICKS_PER_SECOND) - (startTime / SharedConstants.TICKS_PER_SECOND)) - config.timeLimit());
                long minutes = timeLeft / 60;
                String seconds;
                if (timeLeft % 60 > 10) {
                    seconds = Long.toString(timeLeft % 60);
                } else {
                    seconds = "0" + timeLeft % 60;
                }
                content.add(Component.translatable("bingo.sidebar.time_left", minutes, seconds));
            }
            content.add(CommonComponents.EMPTY);
        });
    }
    private boolean checkForWin(ServerPlayer plr) {
        List<List<BingoSlot>> bingoCard = bingoCards.get(new PlayerRef(plr.getUUID()));
        if (config.lockout()) {
            int claimedSlotCount = 0;
            for (List<BingoSlot> col : bingoCard) {
                for (BingoSlot slot : col) {
                    if (slot.marked()) {
                        claimedSlotCount++;
                    }
                }
            }
            int threshold;
            if (teamManager.isPresent()) {
                int teamCount = 0;
                for (GameTeam _team : teamManager.get()) {
                    teamCount++;
                }
                threshold = (int) Math.ceil((double) 25 / teamCount);
            } else {
                threshold = (int) Math.ceil((double) 25 / gameSpace.getPlayers().participants().size());
            }
            return claimedSlotCount >= threshold;
        }
        boolean horizontalWin = true;
        for (List<BingoSlot> col : bingoCard) {
            horizontalWin = true;
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
            verticalWin = true;
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
                if (!slot.marked()) {
                    diagonalWin = false;
                    break;
                }
            }
        }
        return horizontalWin || verticalWin || diagonalWin;
    }
    private boolean isSameItem(Item item, Item otherItem) {

        return (item.equals(otherItem) ||
                config.genericSherdDrops() && item.getDefaultInstance().typeHolder().getRegisteredName().contains("sherd") && otherItem.getDefaultInstance().typeHolder().getRegisteredName().contains("sherd") ||
                config.genericMusicDiscDrops() && item.getDefaultInstance().typeHolder().getRegisteredName().contains("disc") && otherItem.getDefaultInstance().typeHolder().getRegisteredName().contains("disc") ||
                config.genericArmorTrimDrops() && item instanceof SmithingTemplateItem && otherItem instanceof SmithingTemplateItem);
    }
    private void checkForScore(ServerPlayer plr, ItemStack stack) {
        List<List<BingoSlot>> bingoCard = bingoCards.get(new PlayerRef(plr.getUUID()));
        int colIndex = 99;
        int rowIndex = 99;
        for (List<BingoSlot> col : bingoCard) {
            for (BingoSlot slot: col.stream().filter(slot -> !slot.marked() && !slot.locked()).toList()) {
                if (isSameItem(slot.item(), stack.getItem())) {
                    rowIndex = col.indexOf(slot);
                    colIndex = bingoCard.indexOf(col);
                    break;
                }
            }
        }
        if (colIndex != 99 ) {
            ServerLevel plrlevel = plr.level();
            // there doesn't need to be specific code for updating team member's bingo cards as they all share the same underlying references
            bingoCard.get(colIndex).set(rowIndex, new BingoSlot(stack.getItem(), true, config.lockout()));
            plrlevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, plr.getX(), plr.getY(), plr.getZ(), 32, 1, 1, 1, 1);
            plrlevel.playSound(null, plr.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 1, 1);
            Component plrName;
            if (plr.getDisplayName() != null) {
                plrName = plr.getDisplayName();
            } else {
                plrName = plr.getName();
            }

            if (config.lockout()) {
                bingoCards.forEach((_plr, card) -> {
                    int colIndex2 = 99;
                    int rowIndex2 = 99;
                    for (List<BingoSlot> col : card) {
                        for (BingoSlot slot : col) {
                            if (!slot.locked() && !slot.marked() && isSameItem(slot.item(), stack.getItem())) {
                                rowIndex2 = col.indexOf(slot);
                                colIndex2 = card.indexOf(col);
                                break;
                            }
                        }
                    }
                    if (colIndex2 != 99) {
                        card.get(colIndex2).set(rowIndex2, new BingoSlot(stack.getItem(), false, true));
                    }
                });
            }

            gameSpace.getPlayers().sendMessage(Component.translatable("bingo.itempickup", plrName, stack.getHoverName().copy().withStyle(ChatFormatting.GOLD)).withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(Component.translatable("bingo.itempickup.hover", plr.getName()))).withClickEvent(new ClickEvent.RunCommand("/bingocard " + plr.getName().getString()))));
            if (checkForWin(plr)) {
                End(plr);
            }
        }
    }

    private EventResult onCraft(ServerPlayer plr, ItemStack stack) {
        checkForScore(plr, stack);
        return EventResult.PASS;
    }

    private EventResult onItemPickup(ServerPlayer plr, ItemEntity item, ItemStack stack) {
        checkForScore(plr, stack);
        return EventResult.PASS;
    }
    private Item generateItem(List<List<BingoSlot>> bingoCard) {
        Item item = items.getRandomElement(RandomSource.create()).orElseThrow().value();
        for (List<BingoSlot> col : bingoCard) {
            for (BingoSlot slot : col) {
                if (isSameItem(item, slot.item())) {
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
                row.add(new BingoSlot(item, false, false));
            }
            bingoCard.add(row);
        }
        return bingoCard;
    }

    private boolean isItemEnabled(Holder.Reference<Item> entry) {
        if (entry.unwrapKey().isPresent() && !entry.unwrapKey().get().identifier().getNamespace().equals(Identifier.DEFAULT_NAMESPACE)) {
            return false;
        }

        for (Holder<Item> allowedItem : config.itemFilter().allowedItems()) {
            if (entry.is(allowedItem)) {
                return true;
            }
        }
        for (TagKey<Item> allowedItemTag : config.itemFilter().allowedItemTags()) {
            if (entry.is(allowedItemTag)) {
                return true;
            }
        }

        if (config.itemFilter().isWhiteList()) {
            // returning now because all the whitelisted items have already been checked
            // so if it reaches this far then the item is not whitelisted
            return false;
        }

        for (Holder<Item> disallowedItem : config.itemFilter().disallowedItems()) {
            if (entry.is(disallowedItem)) {
                return false;
            }
        }
        for (TagKey<Item> disallowedItemTag : config.itemFilter().disallowedItemTags()) {
            if (entry.is(disallowedItemTag)) {
                return false;
            }
        }

        Item item = entry.value();
        if (!config.hardMode() && hardItems.contains(item)) {
            return false;
        }

        return !(item instanceof GameMasterBlockItem) && !(item instanceof AirItem) && !(item instanceof SpawnEggItem) && item.isEnabled(level.enabledFeatures());
    }

    public static void rules(GameActivity activity) {
        activity.allow(GameRuleType.PVP);
        activity.allow(GameRuleType.FALL_DAMAGE);
        activity.allow(GameRuleType.INTERACTION);
        activity.allow(GameRuleType.CRAFTING);
        activity.allow(GameRuleType.BREAK_BLOCKS);
        activity.deny(GameRuleType.HUNGER);
        activity.deny(GameRuleType.SATURATED_REGENERATION);
    }
    public static void Open(GameSpace gameSpace, BingoConfig config, ServerLevel level, BlockPos spawnPos, Optional<TeamSelectionLobby> teamSelection) {
        gameSpace.setActivity(activity -> {
            Optional<TeamManager> maybeTeamManager = config.teams().map(teams -> {
                TeamManager teamManager = TeamManager.addTo(activity);
                TeamChat.addTo(activity, teamManager);

                for (GameTeam team : config.teams().get()) {
                    GameTeamConfig teamConfig = GameTeamConfig.builder(team.config())
                            .setFriendlyFire(false)
                            .build();

                    teamManager.addTeam(team.key(), teamConfig);
                }
                return teamManager;
            });
            new BingoActive(gameSpace, activity, config, level, spawnPos, teamSelection, maybeTeamManager);
        });
    }

    private EventResult onDeath(ServerPlayer plr, DamageSource source) {
        Component deathMessage = plr.getCombatTracker().getDeathMessage();
        gameSpace.getPlayers().forEach(plr1 -> plr1.sendSystemMessage(deathMessage));
        playersRespawning.put(plr, gameSpace.getTime() + SharedConstants.TICKS_PER_SECOND * 5);
        plr.getInventory().dropAll();
        plr.setGameMode(GameType.SPECTATOR);
        // forgive mobs on death
        AABB box = new AABB(plr.blockPosition()).inflate(32.0, 10.0, 32.0);
        plr.level()
                .getEntitiesOfClass(Mob.class, box, EntitySelector.NO_SPECTATORS)
                .stream()
                .filter(entity -> entity instanceof NeutralMob)
                .forEach(entity -> ((NeutralMob)entity).playerDied(plr.level(), plr));
        return EventResult.DENY;
    }

    private void onTick() {
        long time = gameSpace.getTime();
        playersRespawning.forEach((plr, respawnTime) -> {
            long timeLeft = respawnTime - time;
            if (timeLeft <= 0) {

                plr.setGameMode(GameType.SURVIVAL);
                plr.getFoodData().setFoodLevel(20);
                plr.getFoodData().setSaturation(20);
                plr.setHealth(plr.getMaxHealth());
                plr.teleportTo(level, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), Set.of(), 0, 0, false);
                plr.sendSystemMessage(Component.literal(""), true);
                playersToRemove.add(plr);
            } else {
                plr.sendSystemMessage(Component.translatable("bingo.respawning", (timeLeft + 20) / 20), true);
            }
        });
        playersToRemove.forEach(plr -> playersRespawning.remove(plr));
        playersToRemove.clear();
        if (gameWon) {
            if (time - gameWinTime > SharedConstants.TICKS_PER_SECOND * 10) {
                gameSpace.close(GameCloseReason.FINISHED);
            }
        } else {
            if (config.timeLimit() != 0 && time % SharedConstants.TICKS_PER_SECOND == 0) {
                if (time - timeoutTime >= 0) {
                    End();
                }
                updateSidebar();
            }
        }
    }

    private void End() {
        // no player won
        gameWon = true;
        gameWinTime = gameSpace.getTime();
        gameSpace.getPlayers().sendMessage(Component.translatable("bingo.win_message.timeout"));
    }
    private void End(ServerPlayer winner) {
        int claimedSlotCount = 0;
        PlayerRef ref = new PlayerRef(winner.getUUID());
        for (List<BingoSlot> col : bingoCards.get(ref)) {
            for (BingoSlot slot : col) {
                if (slot.marked()) {
                    claimedSlotCount++;
                }
            }
        }
        if (playerTeams.containsKey(ref)) {
            GameTeamKey winningTeam = playerTeams.get(ref);
            String teamPlayers = "";
            for (PlayerRef plrRef : teamManager.get().allPlayersIn(winningTeam)) {
                String plrName;
                ServerPlayer plr = plrRef.getEntity(gameSpace);
                if (plr == null) {
                    continue;
                }
                if (plr.getDisplayName() != null) {
                    plrName = plr.getDisplayName().getString();
                } else {
                    plrName = plr.getName().getString();
                }
                plrName += ", ";
                teamPlayers += plrName;
            }
            teamPlayers = teamPlayers.substring(0, teamPlayers.length() - 2);
            String finalTeamPlayers = teamPlayers;
            gameSpace.getPlayers().sendMessage(Component.translatable("bingo.win_message", teamManager.get().getTeamConfig(winningTeam).name(), claimedSlotCount).withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(Component.literal(finalTeamPlayers)))));
        } else {
            String plrName;
            if (winner.getDisplayName() != null) {
                plrName = winner.getDisplayName().getString();
            } else {
                plrName = winner.getName().getString();
            }
            gameSpace.getPlayers().sendMessage(Component.translatable("bingo.win_message", Component.literal(plrName).withStyle(ChatFormatting.GOLD), claimedSlotCount));
        }

        gameWon = true;
        gameWinTime = gameSpace.getTime();
    }

    record PlayerPos(Vec3 pos, ServerLevel level, PlayerRef plr, HashMap<Integer, ItemStack> inventory) {
        public PlayerPos(Vec3 pos, ServerLevel level, ServerPlayer plr, HashMap<Integer, ItemStack> inventory) {
            this(pos, level, new PlayerRef(plr.getUUID()), inventory);
        }
    }
}
