package me.ellieis.bingo.game.phases;

import me.ellieis.bingo.Bingo;
import me.ellieis.bingo.BingoCardCommand;
import me.ellieis.bingo.ItemCraftEvent;
import me.ellieis.bingo.game.config.BingoConfig;
import net.minecraft.SharedConstants;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.*;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameMode;
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
    ServerWorld world;
    BlockPos spawnPos;
    GlobalWidgets widgets;
    SidebarWidget sidebar;
    Optional<TeamSelectionLobby> teamSelection;
    Optional<TeamManager> teamManager;
    HashMap<PlayerRef, GameTeamKey> playerTeams;
    HashMap<ServerPlayerEntity, Long> playersRespawning = new HashMap<>();
    HashMap<PlayerRef, PlayerPos> lastPlayerPos = new HashMap<>();
    ArrayList<ServerPlayerEntity> playersToRemove = new ArrayList<>();
    RegistryEntryList<Item> items;
    List<Item> disallowedItems = List.of(Items.KNOWLEDGE_BOOK, Items.DEBUG_STICK, Items.LIGHT, Items.BEDROCK, Items.VAULT, Items.PLAYER_HEAD, Items.INFESTED_COBBLESTONE, Items.INFESTED_DEEPSLATE, Items.INFESTED_STONE, Items.INFESTED_CHISELED_STONE_BRICKS, Items.INFESTED_CRACKED_STONE_BRICKS, Items.INFESTED_MOSSY_STONE_BRICKS, Items.INFESTED_STONE_BRICKS, Items.SPAWNER, Items.TRIAL_SPAWNER, Items.END_PORTAL_FRAME, Items.BARRIER, Items.STRUCTURE_BLOCK, Items.STRUCTURE_VOID, Items.SUSPICIOUS_GRAVEL, Items.SUSPICIOUS_SAND, Items.SMALL_AMETHYST_BUD, Items.MEDIUM_AMETHYST_BUD, Items.LARGE_AMETHYST_BUD, Items.PETRIFIED_OAK_SLAB, Items.REINFORCED_DEEPSLATE, Items.BUDDING_AMETHYST, Items.FARMLAND, Items.FROGSPAWN);
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
    public BingoActive(GameSpace gameSpace, GameActivity activity, BingoConfig config, ServerWorld world, BlockPos spawnPos, Optional<TeamSelectionLobby> teamSelection, Optional<TeamManager> teamManager) {
        this.gameSpace = gameSpace;
        this.activity = activity;
        this.config = config;
        this.world = world;
        this.spawnPos = spawnPos;
        this.teamSelection = teamSelection;
        this.teamManager = teamManager;
        this.widgets = GlobalWidgets.addTo(activity);
        this.sidebar = widgets.addSidebar();
        this.startTime = world.getTime();
        this.timeoutTime = config.timeLimit() + this.startTime;
        this.playerTeams = new HashMap<>();
        world.setSpawnPos(spawnPos, 0);
        Bingo.activeGames.add(this);
        BingoActive.rules(activity);
        teamSelection.ifPresent(action -> {
            action.allocate(gameSpace.getPlayers().participants(), (key, plr) -> {
                playerTeams.put(new PlayerRef(plr.getUuid()), key);
                teamManager.get().addPlayerTo(plr, key);
            });
        });
        this.items = RegistryEntryList.of(world.getRegistryManager()
                .getOrThrow(RegistryKeys.ITEM)
                .streamEntries()
                .filter(this::isItemEnabled)
                .toList());
        activity.listen(GameActivityEvents.DESTROY, (_reason) -> {
            Bingo.activeGames.remove(this);
        });
        activity.listen(GamePlayerEvents.ACCEPT, acceptor -> {
            return acceptor.teleport((gameProfile -> {
                if (acceptor.intent() == JoinIntent.PLAY ) {
                    UUID id = gameProfile.getId();
                    AtomicReference<Vec3d> playerPos = new AtomicReference<>();
                    AtomicReference<ServerWorld> playerWorld = new AtomicReference<>();

                    teamManager.ifPresent((manager) -> {
                        PlayerRef ref = new PlayerRef(id);
                        if (playerTeams.containsKey(ref)) {
                            manager.addPlayerTo(ref, playerTeams.get(ref));
                        } else {
                            manager.addPlayerTo(ref, manager.getSmallestTeam());
                        }
                        if (lastPlayerPos.containsKey(ref)) {
                            PlayerPos obj = lastPlayerPos.get(ref);
                            playerWorld.set(obj.world());
                            playerPos.set(obj.pos());
                        }
                    });
                    if (playerPos.get() == null) {
                        return new xyz.nucleoid.plasmid.api.util.PlayerPos(world, spawnPos.toCenterPos(), 0, 0);
                    } else {
                        return new xyz.nucleoid.plasmid.api.util.PlayerPos(playerWorld.get(), playerPos.get(), 0, 0);

                    }
                } else {
                    return new xyz.nucleoid.plasmid.api.util.PlayerPos(world, spawnPos.toCenterPos(), 0, 0);
                }
            }));
        });
        activity.listen(GamePlayerEvents.OFFER, JoinOffer::accept);
        activity.listen(GamePlayerEvents.JOIN, (plr) -> {
            sidebar.addPlayer(plr);
            if (gameSpace.getPlayers().participants().contains(plr)) {
                plr.changeGameMode(GameMode.SURVIVAL);
                PlayerRef ref = new PlayerRef(plr.getUuid());
                if (lastPlayerPos.containsKey(ref)) {
                    PlayerPos obj = lastPlayerPos.get(ref);
                    PlayerInventory currentInventory = plr.getInventory();
                    obj.inventory().forEach(currentInventory::setStack);
                    for (int i = 98; i <= 103; i++) {
                        ItemStack stack = obj.inventory().get(i);
                        plr.equipStack(slotToEquipmentSlot(i), stack);
                    }
                }

                generateCardForPlayer(plr);
            } else {
                plr.changeGameMode(GameMode.SPECTATOR);
            }
        });
        activity.listen(GamePlayerEvents.LEAVE, (plr) -> {
            PlayerInventory playerInventory = plr.getInventory();
            HashMap<Integer, ItemStack> inventory = new HashMap<>();
            // inventory slots
            for (int i = 0; i <= 35; i++) {
                inventory.put(i, playerInventory.getStack(i));
            }
            // armor slots
            for (int i = 98; i <= 103; i++) {
                inventory.put(i, plr.getEquippedStack(slotToEquipmentSlot(i)));
            }
            lastPlayerPos.put(new PlayerRef(plr.getUuid()), new PlayerPos(plr.getPos(), plr.getWorld(), plr, inventory));
        });
        activity.listen(GameActivityEvents.TICK, this::onTick);
        activity.listen(PlayerDeathEvent.EVENT, this::onDeath);
        activity.listen(ItemPickupEvent.EVENT, this::onItemPickup);
        activity.listen(ItemCraftEvent.EVENT, this::onCraft);
        activity.listen(PlayerChatEvent.EVENT, (plr, message, params) -> {
            String content = message.getContent().getString();
            String[] args = content.split(" ");
            if (args.length > 0) {
                if (args[0].equals("!bingocard")) {
                    if (args.length > 1) {
                        ServerPlayerEntity otherPlr = gameSpace.getServer().getPlayerManager().getPlayer(args[1]);
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
        activity.listen(NetherPortalOpenEvent.EVENT, (_world, _pos) -> config.hasNether() ? EventResult.ALLOW : EventResult.DENY);
        activity.listen(EndPortalOpenEvent.EVENT, (_context, _result) -> config.hasEnd() ? EventResult.ALLOW : EventResult.DENY);
        sidebar.setTitle(GameConfig.shortName(activity.getGameSpace().getMetadata().sourceConfig()).copy().formatted(Formatting.GOLD));
        updateSidebar();

        if (!config.separate()) {
            universalCard = generateBingoCard();
        } else {
            universalCard = null;
        }

        gameSpace.getPlayers().participants().forEach(plr -> {
            plr.changeGameMode(GameMode.SURVIVAL);
            generateCardForPlayer(plr);
            plr.unlockRecipes(world.getRecipeManager().values());
            MinecraftServer server = world.getServer();
            server.getCommandManager().sendCommandTree(plr);
            sidebar.addPlayer(plr);
            BingoCardCommand.showGui(plr, plr);
            plr.sendMessage(Text.translatable("bingo.config.enabled_options"));
            if (config.separate()) {
                String key = "bingo.config.separate";
                plr.sendMessage(Text.translatable(key).formatted(Formatting.GOLD).styled(style -> style.withHoverEvent(new HoverEvent.ShowText(Text.translatable(key + ".desc")))));
            }
            if (config.lockout()) {
                String key = "bingo.config.lockout";
                plr.sendMessage(Text.translatable(key).formatted(Formatting.GOLD).styled(style -> style.withHoverEvent(new HoverEvent.ShowText(Text.translatable(key + ".desc")))));
            }
            if (config.hardMode()) {
                String key = "bingo.config.hard_mode";
                plr.sendMessage(Text.translatable(key).formatted(Formatting.GOLD).styled(style -> style.withHoverEvent(new HoverEvent.ShowText(Text.translatable(key + ".desc")))));
            }
            if (config.genericArmorTrimDrops()) {
                String key = "bingo.config.generic_armor_trim_drops";
                plr.sendMessage(Text.translatable(key).formatted(Formatting.GOLD).styled(style -> style.withHoverEvent(new HoverEvent.ShowText(Text.translatable(key + ".desc")))));
            }
            if (config.genericSherdDrops()) {
                String key = "bingo.config.generic_sherd_drops";
                plr.sendMessage(Text.translatable(key).formatted(Formatting.GOLD).styled(style -> style.withHoverEvent(new HoverEvent.ShowText(Text.translatable(key + ".desc")))));
            }
            if (config.genericMusicDiscDrops()) {
                String key = "bingo.config.generic_music_disc_drops";
                plr.sendMessage(Text.translatable(key).formatted(Formatting.GOLD).styled(style -> style.withHoverEvent(new HoverEvent.ShowText(Text.translatable(key + ".desc")))));
            }
        });
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

    private void generateCardForPlayer(ServerPlayerEntity plr) {
        PlayerRef ref = new PlayerRef(plr.getUuid());
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
            content.add(ScreenTexts.EMPTY);
            content.add(Text.translatable("bingo.sidebar"));
            content.add(Text.translatable("bingo.sidebar.desc"));
            content.add(Text.translatable("bingo.sidebar.desc2"));
            if (config.timeLimit() != 0) {
                long timeLeft = (long) Math.abs(Math.floor((world.getTime() / SharedConstants.TICKS_PER_SECOND) - (startTime / SharedConstants.TICKS_PER_SECOND)) - config.timeLimit());
                long minutes = timeLeft / 60;
                String seconds;
                if (timeLeft % 60 > 10) {
                    seconds = Long.toString(timeLeft % 60);
                } else {
                    seconds = "0" + timeLeft % 60;
                }
                content.add(Text.translatable("bingo.sidebar.time_left", minutes, seconds));
            }
            content.add(ScreenTexts.EMPTY);
        });
    }
    private boolean checkForWin(ServerPlayerEntity plr) {
        List<List<BingoSlot>> bingoCard = bingoCards.get(new PlayerRef(plr.getUuid()));
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
                config.genericSherdDrops() && item.getDefaultStack().getRegistryEntry().getIdAsString().contains("sherd") && otherItem.getDefaultStack().getRegistryEntry().getIdAsString().contains("sherd") ||
                config.genericMusicDiscDrops() && item.getDefaultStack().getRegistryEntry().getIdAsString().contains("disc") && otherItem.getDefaultStack().getRegistryEntry().getIdAsString().contains("disc") ||
                config.genericArmorTrimDrops() && item instanceof SmithingTemplateItem && otherItem instanceof SmithingTemplateItem);
    }
    private void checkForScore(ServerPlayerEntity plr, ItemStack stack) {
        List<List<BingoSlot>> bingoCard = bingoCards.get(new PlayerRef(plr.getUuid()));
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
            ServerWorld plrWorld = plr.getWorld();
            // there doesn't need to be specific code for updating team member's bingo cards as they all share the same underlying references
            bingoCard.get(colIndex).set(rowIndex, new BingoSlot(stack.getItem(), true, config.lockout()));
            plrWorld.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, plr.getX(), plr.getY(), plr.getZ(), 32, 1, 1, 1, 1);
            plrWorld.playSound(null, plr.getBlockPos(), SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1, 1);
            Text plrName;
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

            gameSpace.getPlayers().sendMessage(Text.translatable("bingo.itempickup", plrName, stack.getName().copy().formatted(Formatting.GOLD)).styled(style -> style.withHoverEvent(new HoverEvent.ShowText(Text.translatable("bingo.itempickup.hover", plr.getName()))).withClickEvent(new ClickEvent.RunCommand("/bingocard " + plr.getName().getString()))));
            if (checkForWin(plr)) {
                End(plr);
            }
        }
    }

    private EventResult onCraft(ServerPlayerEntity plr, ItemStack stack) {
        checkForScore(plr, stack);
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

    private boolean isItemEnabled(RegistryEntry.Reference<Item> entry) {
        if (entry.getKey().isPresent() && !entry.getKey().get().getValue().getNamespace().equals(Identifier.DEFAULT_NAMESPACE)) {
            return false;
        }

        Item item = entry.value();
        if (!config.hardMode() && hardItems.contains(item)) {
            return false;
        }

        return !(item instanceof OperatorOnlyBlockItem) && !(item instanceof AirBlockItem) && !(item instanceof SpawnEggItem) && !disallowedItems.contains(item) && item.isEnabled(world.getEnabledFeatures());
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
    public static void Open(GameSpace gameSpace, BingoConfig config, ServerWorld world, BlockPos spawnPos, Optional<TeamSelectionLobby> teamSelection) {
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
            new BingoActive(gameSpace, activity, config, world, spawnPos, teamSelection, maybeTeamManager);
        });
    }

    private EventResult onDeath(ServerPlayerEntity plr, DamageSource source) {
        Text deathMessage = plr.getDamageTracker().getDeathMessage();
        gameSpace.getPlayers().forEach(plr1 -> plr1.sendMessage(deathMessage));
        playersRespawning.put(plr, gameSpace.getTime() + SharedConstants.TICKS_PER_SECOND * 5);
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
        gameSpace.getPlayers().sendMessage(Text.translatable("bingo.win_message.timeout"));
    }
    private void End(ServerPlayerEntity winner) {
        int claimedSlotCount = 0;
        PlayerRef ref = new PlayerRef(winner.getUuid());
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
                ServerPlayerEntity plr = plrRef.getEntity(gameSpace);
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
            gameSpace.getPlayers().sendMessage(Text.translatable("bingo.win_message", teamManager.get().getTeamConfig(winningTeam).name(), claimedSlotCount).styled(style -> style.withHoverEvent(new HoverEvent.ShowText(Text.literal(finalTeamPlayers)))));
        } else {
            String plrName;
            if (winner.getDisplayName() != null) {
                plrName = winner.getDisplayName().getString();
            } else {
                plrName = winner.getName().getString();
            }
            gameSpace.getPlayers().sendMessage(Text.translatable("bingo.win_message", Text.literal(plrName).formatted(Formatting.GOLD), claimedSlotCount));
        }

        gameWon = true;
        gameWinTime = gameSpace.getTime();
    }

    record PlayerPos(Vec3d pos, ServerWorld world, PlayerRef plr, HashMap<Integer, ItemStack> inventory) {
        public PlayerPos(Vec3d pos, ServerWorld world, ServerPlayerEntity plr, HashMap<Integer, ItemStack> inventory) {
            this(pos, world, new PlayerRef(plr.getUuid()), inventory);
        }
    }
}
