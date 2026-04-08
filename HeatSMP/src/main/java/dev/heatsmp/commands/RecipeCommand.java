package dev.heatsmp.commands;

import dev.heatsmp.HeatSMPPlugin;
import dev.heatsmp.rods.RodManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class RecipeCommand implements CommandExecutor, Listener {

    private final HeatSMPPlugin plugin;

    // GUI slot indices for a 54-slot chest
    // Crafting grid uses slots 10,11,12 / 19,20,21 / 28,29,30
    // Arrow at slot 23, Result at slot 25

    public RecipeCommand(HeatSMPPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.").color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            openMainMenu(player);
        } else {
            switch (args[0].toLowerCase()) {
                case "pearl", "cooling_pearl" -> openRecipeGui(player, "cooling_pearl");
                case "ember" -> openRecipeGui(player, "ember");
                case "scorch" -> openRecipeGui(player, "scorch");
                case "inferno" -> openRecipeGui(player, "inferno");
                case "molten" -> openRecipeGui(player, "molten");
                case "assassin" -> openRecipeGui(player, "assassin");
                default -> openMainMenu(player);
            }
        }
        return true;
    }

    // ─── Main Menu ────────────────────────────────────────────────────────────

    private void openMainMenu(Player player) {
        Inventory gui = plugin.getServer().createInventory(null, 54,
                Component.text("📖 HeatSMP Recipes").color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));

        // Fill borders with gray glass
        fillBorder(gui);

        // Cooling Pearl — slot 20
        gui.setItem(20, makeMenuItem(Material.ENDER_PEARL,
                Component.text("❄ Cooling Pearl").color(NamedTextColor.AQUA).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false),
                List.of(Component.text("Click to view recipe").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)),
                "cooling_pearl"));

        // Ember Rod — slot 22
        gui.setItem(22, makeMenuItem(Material.BLAZE_ROD,
                Component.text("🔥 Ember Rod").color(NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false),
                List.of(Component.text("Tier I | Click to view recipe").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)),
                "ember"));

        // Scorch Rod — slot 24
        gui.setItem(24, makeMenuItem(Material.BLAZE_ROD,
                Component.text("💥 Scorch Rod").color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false),
                List.of(Component.text("Tier II | Click to view recipe").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)),
                "scorch"));

        // Inferno Rod — slot 29
        gui.setItem(29, makeMenuItem(Material.BLAZE_ROD,
                Component.text("🌋 Inferno Rod").color(NamedTextColor.RED).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false),
                List.of(Component.text("Tier III | Click to view recipe").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)),
                "inferno"));

        // Molten Rod — slot 31
        gui.setItem(31, makeMenuItem(Material.BLAZE_ROD,
                Component.text("☄ Molten Rod").color(NamedTextColor.DARK_RED).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false),
                List.of(Component.text("Tier IV | Click to view recipe").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)),
                "molten"));

        // Assassin Rod — slot 33
        gui.setItem(33, makeMenuItem(Material.BLAZE_ROD,
                Component.text("🗡 Assassin's Rod").color(NamedTextColor.DARK_PURPLE).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false),
                List.of(Component.text("Tier V | Click to view recipe").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)),
                "assassin"));

        player.openInventory(gui);
    }

    // ─── Recipe GUI ───────────────────────────────────────────────────────────

    private void openRecipeGui(Player player, String recipe) {
        String title = switch (recipe) {
            case "cooling_pearl" -> "❄ Cooling Pearl Recipe";
            case "ember" -> "🔥 Ember Rod Recipe";
            case "scorch" -> "💥 Scorch Rod Recipe";
            case "inferno" -> "🌋 Inferno Rod Recipe";
            case "molten" -> "☄ Molten Rod Recipe";
            case "assassin" -> "🗡 Assassin's Rod Recipe";
            default -> "Recipe";
        };

        Inventory gui = plugin.getServer().createInventory(null, 54,
                Component.text(title).color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));

        fillBorder(gui);

        // Crafting grid positions (3x3 starting at slot 10)
        // Row 1: 10, 11, 12
        // Row 2: 19, 20, 21
        // Row 3: 28, 29, 30
        // Arrow: 23
        // Result: 25

        int[] grid = {10, 11, 12, 19, 20, 21, 28, 29, 30};

        // Fill empty grid slots with light gray glass
        ItemStack empty = makeGlass(Material.LIGHT_GRAY_STAINED_GLASS_PANE, " ");
        for (int slot : grid) gui.setItem(slot, empty);

        // Arrow
        gui.setItem(23, makeArrow());

        switch (recipe) {
            case "cooling_pearl" -> {
                //  _I_
                // IPI
                //  _I_
                gui.setItem(11, ingredient(Material.ICE, "Ice"));
                gui.setItem(19, ingredient(Material.ICE, "Ice"));
                gui.setItem(20, ingredient(Material.ENDER_PEARL, "Ender Pearl"));
                gui.setItem(21, ingredient(Material.ICE, "Ice"));
                gui.setItem(29, ingredient(Material.ICE, "Ice"));
                gui.setItem(25, plugin.getCoolingPearl().createItem());
            }
            case "ember" -> {
                // _B_
                // _S_
                // _S_
                gui.setItem(11, ingredient(Material.BLAZE_ROD, "Blaze Rod"));
                gui.setItem(20, ingredient(Material.STICK, "Stick"));
                gui.setItem(29, ingredient(Material.STICK, "Stick"));
                gui.setItem(25, plugin.getRodManager().createRod(RodManager.RodType.EMBER));
            }
            case "scorch" -> {
                // FBF
                // _B_
                // _B_
                gui.setItem(10, ingredient(Material.FIRE_CHARGE, "Fire Charge"));
                gui.setItem(11, ingredient(Material.BLAZE_ROD, "Blaze Rod"));
                gui.setItem(12, ingredient(Material.FIRE_CHARGE, "Fire Charge"));
                gui.setItem(20, ingredient(Material.BLAZE_ROD, "Blaze Rod"));
                gui.setItem(29, ingredient(Material.BLAZE_ROD, "Blaze Rod"));
                gui.setItem(25, plugin.getRodManager().createRod(RodManager.RodType.SCORCH));
            }
            case "inferno" -> {
                // MBM
                // MBM
                // _B_
                gui.setItem(10, ingredient(Material.MAGMA_BLOCK, "Magma Block"));
                gui.setItem(11, ingredient(Material.BLAZE_ROD, "Blaze Rod"));
                gui.setItem(12, ingredient(Material.MAGMA_BLOCK, "Magma Block"));
                gui.setItem(19, ingredient(Material.MAGMA_BLOCK, "Magma Block"));
                gui.setItem(20, ingredient(Material.BLAZE_ROD, "Blaze Rod"));
                gui.setItem(21, ingredient(Material.MAGMA_BLOCK, "Magma Block"));
                gui.setItem(29, ingredient(Material.BLAZE_ROD, "Blaze Rod"));
                gui.setItem(25, plugin.getRodManager().createRod(RodManager.RodType.INFERNO));
            }
            case "molten" -> {
                // NLN
                // MBM
                // MBM
                gui.setItem(10, ingredient(Material.NETHERITE_INGOT, "Netherite Ingot"));
                gui.setItem(11, ingredient(Material.LAVA_BUCKET, "Lava Bucket"));
                gui.setItem(12, ingredient(Material.NETHERITE_INGOT, "Netherite Ingot"));
                gui.setItem(19, ingredient(Material.MAGMA_BLOCK, "Magma Block"));
                gui.setItem(20, ingredient(Material.BLAZE_ROD, "Blaze Rod"));
                gui.setItem(21, ingredient(Material.MAGMA_BLOCK, "Magma Block"));
                gui.setItem(28, ingredient(Material.MAGMA_BLOCK, "Magma Block"));
                gui.setItem(29, ingredient(Material.BLAZE_ROD, "Blaze Rod"));
                gui.setItem(30, ingredient(Material.MAGMA_BLOCK, "Magma Block"));
                gui.setItem(25, plugin.getRodManager().createRod(RodManager.RodType.MOLTEN));
            }
            case "assassin" -> {
                // WDW
                // RBR
                // ___
                gui.setItem(10, ingredient(Material.GLASS_BOTTLE, "Glass Bottle"));
                gui.setItem(11, ingredient(Material.DIAMOND_BLOCK, "Diamond Block"));
                gui.setItem(12, ingredient(Material.GLASS_BOTTLE, "Glass Bottle"));
                gui.setItem(19, ingredient(Material.BLAZE_ROD, "Blaze Rod"));
                gui.setItem(20, ingredient(Material.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE, "Silence Armor Trim"));
                gui.setItem(21, ingredient(Material.BLAZE_ROD, "Blaze Rod"));
                gui.setItem(25, plugin.getRodManager().createRod(RodManager.RodType.ASSASSIN));
            }
        }

        // Back button
        gui.setItem(49, makeBackButton());

        player.openInventory(gui);
    }

    // ─── Inventory Click Handler ──────────────────────────────────────────────

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        var title = event.getView().title();
        String titleStr = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(title);

        // Cancel all clicks in our GUIs
        if (titleStr.contains("HeatSMP Recipes") || titleStr.contains("Recipe")) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
            ItemStack clicked = event.getCurrentItem();
            ItemMeta meta = clicked.getItemMeta();
            if (meta == null) return;

            // Check for recipe navigation via PDC
            var pdc = meta.getPersistentDataContainer();
            var navKey = new org.bukkit.NamespacedKey(plugin, "recipe_nav");
            String nav = pdc.get(navKey, org.bukkit.persistence.PersistentDataType.STRING);

            if (nav != null) {
                if (nav.equals("back")) {
                    openMainMenu(player);
                } else {
                    openRecipeGui(player, nav);
                }
            }
        }
    }

    // ─── GUI Helpers ──────────────────────────────────────────────────────────

    private void fillBorder(Inventory gui) {
        ItemStack border = makeGlass(Material.GRAY_STAINED_GLASS_PANE, " ");
        int size = gui.getSize();
        for (int i = 0; i < 9; i++) gui.setItem(i, border);
        for (int i = size - 9; i < size; i++) gui.setItem(i, border);
        for (int i = 0; i < size; i += 9) gui.setItem(i, border);
        for (int i = 8; i < size; i += 9) gui.setItem(i, border);
    }

    private ItemStack makeGlass(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeMenuItem(Material mat, Component name, List<Component> lore, String navTarget) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name);
        meta.lore(lore);
        meta.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey(plugin, "recipe_nav"),
            org.bukkit.persistence.PersistentDataType.STRING, navTarget);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeArrow() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("➜").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeBackButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("← Back to all recipes").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        meta.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey(plugin, "recipe_nav"),
            org.bukkit.persistence.PersistentDataType.STRING, "back");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack ingredient(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name).color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }
}
