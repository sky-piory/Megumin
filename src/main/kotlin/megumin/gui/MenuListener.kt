
package me.megumin.gui

import me.megumin.config.PlayerDataManager
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent

class MenuListener(
    private val data: PlayerDataManager,
    private val menu: StatusMenu
) : Listener {

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val top = event.view.topInventory
        val holder = top.holder as? StatusMenuHolder ?: return

        // GUI 안의 아이템 이동 방지
        event.isCancelled = true

        val player = event.whoClicked as? Player ?: return

        // 다른 사람의 메뉴 조작 방지
        if (player.uniqueId != holder.owner) return

        // 플레이어 인벤토리 클릭 무시
        if (event.clickedInventory != top) return

        val uuid = player.uniqueId

        when (event.rawSlot) {

            // 영창 시간
            10 -> data.update(uuid) {
                it.castSeconds--
            }

            12 -> data.update(uuid) {
                it.castSeconds++
            }

            // 데미지
            19 -> data.update(uuid) {
                it.damage -= 2.0
            }

            21 -> data.update(uuid) {
                it.damage += 2.0
            }

            // 폭발 범위
            28 -> data.update(uuid) {
                it.radius -= 1.0
            }

            30 -> data.update(uuid) {
                it.radius += 1.0
            }

            // 쿨타임
            37 -> data.update(uuid) {
                it.cooldownSeconds -= 5
            }

            39 -> data.update(uuid) {
                it.cooldownSeconds += 5
            }

            // 탈진 시간
            14 -> data.update(uuid) {
                it.exhaustionSeconds--
            }

            16 -> data.update(uuid) {
                it.exhaustionSeconds++
            }

            // 설정 ON/OFF
            32 -> data.update(uuid) {
                it.breakBlocks = !it.breakBlocks
            }

            33 -> data.update(uuid) {
                it.sound = !it.sound
            }

            34 -> data.update(uuid) {
                it.particles = !it.particles
            }

            // 닫기
            49 -> {
                player.closeInventory()
                return
            }

            else -> return
        }

        // 변경된 능력치로 GUI 새로고침
        menu.open(player)
    }

    @EventHandler
    fun onDrag(event: InventoryDragEvent) {
        val holder = event.view.topInventory.holder
                as? StatusMenuHolder ?: return

        if (event.whoClicked.uniqueId == holder.owner) {
            event.isCancelled = true
        }
    }
}
