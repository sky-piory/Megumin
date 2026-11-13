
package me.megumin.gui

import me.megumin.config.PlayerDataManager
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import java.util.UUID

class StatusMenuHolder(
    val owner: UUID
) : InventoryHolder {
    lateinit var menu: Inventory

    override fun getInventory(): Inventory = menu
}

class StatusMenu(
    private val data: PlayerDataManager
) {

    fun open(player: org.bukkit.entity.Player) {
        val stats = data.get(player.uniqueId)
        val holder = StatusMenuHolder(player.uniqueId)

        val inv = Bukkit.createInventory(
            holder,
            54,
            Component.text("✦ MAGE STATUS ✦")
        )

        holder.menu = inv

        val activeSlots = setOf(
            10, 11, 12,
            14, 15, 16,
            19, 20, 21,
            28, 29, 30,
            32, 33, 34,
            37, 38, 39,
            49
        )

        // 배경
        for (slot in 0 until 54) {
            if (slot !in activeSlots) {
                inv.setItem(
                    slot,
                    createItem(
                        Material.PURPLE_STAINED_GLASS_PANE,
                        " ",
                        emptyList()
                    )
                )
            }
        }

        // 영창 시간
        inv.setItem(10, button(Material.REDSTONE, "영창 시간 감소", "1초 감소"))
        inv.setItem(11, info(Material.CLOCK, "영창 시간", "${stats.castSeconds}초"))
        inv.setItem(12, button(Material.GLOWSTONE_DUST, "영창 시간 증가", "1초 증가"))

        // 데미지
        inv.setItem(19, button(Material.REDSTONE, "데미지 감소", "2 감소"))
        inv.setItem(20, info(Material.DIAMOND_SWORD, "마법 데미지", "${stats.damage}"))
        inv.setItem(21, button(Material.GLOWSTONE_DUST, "데미지 증가", "2 증가"))

        // 범위
        inv.setItem(28, button(Material.REDSTONE, "폭발 범위 감소", "1블록 감소"))
        inv.setItem(29, info(Material.FIRE_CHARGE, "폭발 범위", "${stats.radius}블록"))
        inv.setItem(30, button(Material.GLOWSTONE_DUST, "폭발 범위 증가", "1블록 증가"))

        // 쿨타임
        inv.setItem(37, button(Material.REDSTONE, "쿨타임 감소", "5초 감소"))
        inv.setItem(38, info(Material.CLOCK, "쿨타임", "${stats.cooldownSeconds}초"))
        inv.setItem(39, button(Material.GLOWSTONE_DUST, "쿨타임 증가", "5초 증가"))

        // 탈진 시간
        inv.setItem(14, button(Material.REDSTONE, "탈진 시간 감소", "1초 감소"))
        inv.setItem(15, info(Material.SOUL_SAND, "탈진 시간", "${stats.exhaustionSeconds}초"))
        inv.setItem(16, button(Material.GLOWSTONE_DUST, "탈진 시간 증가", "1초 증가"))

        // ON / OFF
        inv.setItem(32, toggleItem("블록 파괴", stats.breakBlocks))
        inv.setItem(33, toggleItem("사운드", stats.sound))
        inv.setItem(34, toggleItem("파티클", stats.particles))

        inv.setItem(
            49,
            createItem(
                Material.BARRIER,
                "닫기",
                listOf("클릭하여 메뉴 닫기")
            )
        )

        player.openInventory(inv)
    }

    private fun button(
        material: Material,
        name: String,
        description: String
    ) = createItem(material, name, listOf(description))

    private fun info(
        material: Material,
        name: String,
        value: String
    ) = createItem(
        material,
        name,
        listOf("현재 설정: $value", "§7좌우 버튼으로 변경")
    )

    private fun toggleItem(
        name: String,
        enabled: Boolean
    ) = createItem(
        if (enabled) Material.LIME_DYE else Material.GRAY_DYE,
        name,
        listOf(if (enabled) "상태: ON" else "상태: OFF")
    )

    private fun createItem(
        material: Material,
        name: String,
        lore: List<String>
    ): ItemStack {
        val item = ItemStack(material)
        val meta: ItemMeta = item.itemMeta

        meta.displayName(
            Component.text(name, NamedTextColor.GOLD)
        )

        meta.lore(
            lore.map { Component.text(it) }
        )

        item.itemMeta = meta
        return item
    }
}
