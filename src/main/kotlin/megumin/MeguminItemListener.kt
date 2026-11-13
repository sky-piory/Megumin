package me.megumin

import me.megumin.gui.MeguminMenu
import me.megumin.gui.StatusMenu
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot

class MeguminItemListener(
    private val items: MeguminItems,
    private val menu: MeguminMenu,
    private val explosionSkill: ExplosionSkill,
    private val statusMenu: StatusMenu
) : Listener {
    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        val action = event.action
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return
        val item = event.item ?: return
        val player = event.player

        when {
            items.isSpellbook(item) -> {
                event.isCancelled = true
                menu.open(player)
            }
            items.isStaff(item) -> {
                event.isCancelled = true
                explosionSkill.cast(player)
            }
            items.isStatusCard(item) -> {
                // 빈 지도의 기본 사용 동작을 취소해 지도 제작/변환을 막는다.
                event.isCancelled = true
                statusMenu.open(player)
            }
        }
    }
}
