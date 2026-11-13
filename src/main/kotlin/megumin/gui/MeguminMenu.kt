package me.megumin.gui

import me.megumin.MeguminItems
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.Listener
import org.bukkit.event.EventHandler
import net.kyori.adventure.text.Component
import java.util.UUID

class MeguminMenuHolder(val owner: UUID) : InventoryHolder {
    lateinit var menu: Inventory
    override fun getInventory(): Inventory = menu
}

class MeguminMenu(private val items: MeguminItems) : Listener {
    fun open(player: Player) {
        val holder = MeguminMenuHolder(player.uniqueId)
        val inv = Bukkit.createInventory(holder, 27, Component.text("✦ MEGUMIN MAGIC ✦"))
        holder.menu = inv

        for (slot in 0 until 27) {
            inv.setItem(slot, createItem(Material.PURPLE_STAINED_GLASS_PANE, " "))
        }

        inv.setItem(
            13,
            createItem(
                Material.FIRE_CHARGE,
                "§c폭렬마법",
                listOf("§7메구밍의 궁극 마법", "§e클릭하여 지팡이와 스테이터스 카드 획득")
            )
        )
        player.openInventory(inv)
    }

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val holder = event.view.topInventory.holder as? MeguminMenuHolder ?: return
        event.isCancelled = true
        val player = event.whoClicked as? Player ?: return
        if (player.uniqueId != holder.owner) return
        if (event.clickedInventory != event.view.topInventory) return
        if (event.rawSlot != 13) return

        player.closeInventory()
        val contents = player.inventory.contents
        for (slot in contents.indices) {
            if (items.isSpellbook(contents[slot])) {
                player.inventory.setItem(slot, items.createStaff())
                val leftover = player.inventory.addItem(items.createStatusCard())
                leftover.values.forEach { player.world.dropItemNaturally(player.location, it) }
                player.sendMessage("§c폭렬마법을 선택했다!")
                player.sendMessage("§7메구밍의 지팡이와 스테이터스 카드를 획득했다.")
                return
            }
        }
        player.sendMessage("§c마법서를 찾을 수 없습니다.")
    }

    private fun createItem(
        material: Material,
        name: String,
        lore: List<String> = emptyList()
    ) = org.bukkit.inventory.ItemStack(material).apply {
        val meta = itemMeta
        meta.setDisplayName(name)
        meta.lore = lore
        itemMeta = meta
    }
}
