package me.megumin

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType

class MeguminItems(private val plugin: Main) {
    private val itemKey = NamespacedKey(plugin, "megumin_item")

    fun createSpellbook(): ItemStack = createItem(
        Material.ENCHANTED_BOOK,
        "§d메구밍의 마법서",
        "spellbook",
        listOf("§7우클릭하여 사용할 마법을 선택한다.", "§8✦ Megumin Magic ✦")
    )

    fun createStaff(): ItemStack = createItem(
        Material.BLAZE_ROD,
        "§c메구밍의 지팡이",
        "staff",
        listOf("§7우클릭하여 폭렬마법을 시전한다.", "§cExplosion!")
    )

    // Material.MAP은 빈 지도이며 FILLED_MAP과 다르다.
    fun createStatusCard(): ItemStack = createItem(
        Material.MAP,
        "§d메구밍의 스테이터스 카드",
        "status_card",
        listOf("§7우클릭하여 개인 능력치를 설정한다.", "§8✦ Mage Status ✦")
    )

    fun isSpellbook(item: ItemStack?): Boolean = hasType(item, "spellbook")
    fun isStaff(item: ItemStack?): Boolean = hasType(item, "staff")
    fun isStatusCard(item: ItemStack?): Boolean = hasType(item, "status_card")

    private fun hasType(item: ItemStack?, type: String): Boolean {
        if (item == null || item.type == Material.AIR) return false
        val meta = item.itemMeta ?: return false
        return meta.persistentDataContainer.get(itemKey, PersistentDataType.STRING) == type
    }

    private fun createItem(material: Material, name: String, type: String, lore: List<String>): ItemStack {
        val item = ItemStack(material)
        val meta: ItemMeta = item.itemMeta
        meta.setDisplayName(name)
        meta.lore = lore
        meta.persistentDataContainer.set(itemKey, PersistentDataType.STRING, type)
        item.itemMeta = meta
        return item
    }
}
