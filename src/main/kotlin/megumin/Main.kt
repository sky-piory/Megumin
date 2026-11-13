package me.megumin

import me.megumin.config.PlayerDataManager
import me.megumin.gui.MenuListener
import me.megumin.gui.MeguminMenu
import me.megumin.gui.StatusMenu
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class Main : JavaPlugin() {
    private lateinit var dataManager: PlayerDataManager
    private lateinit var statusMenu: StatusMenu
    private lateinit var explosionSkill: ExplosionSkill

    override fun onEnable() {
        dataManager = PlayerDataManager(this)
        statusMenu = StatusMenu(dataManager)
        explosionSkill = ExplosionSkill(this, dataManager)

        val meguminItems = MeguminItems(this)
        val meguminMenu = MeguminMenu(meguminItems)

        server.pluginManager.registerEvents(explosionSkill, this)
        server.pluginManager.registerEvents(MenuListener(dataManager, statusMenu), this)
        server.pluginManager.registerEvents(meguminMenu, this)
        server.pluginManager.registerEvents(
            MeguminItemListener(meguminItems, meguminMenu, explosionSkill, statusMenu),
            this
        )

        getCommand("explosion")?.setExecutor { sender, _, _, args ->
            if (sender !is Player) {
                sender.sendMessage("플레이어만 사용할 수 있습니다.")
                return@setExecutor true
            }
            if (args.firstOrNull()?.equals("menu", true) == true) {
                statusMenu.open(sender)
            } else {
                explosionSkill.cast(sender)
            }
            true
        }

        getCommand("explosion")?.setTabCompleter { _, _, _, args ->
            if (args.size == 1) {
                listOf("menu").filter { it.startsWith(args[0], ignoreCase = true) }
            } else {
                emptyList()
            }
        }

        // /megumin 및 /메구밍: 기존처럼 마법서만 지급한다.
        getCommand("megumin")?.setExecutor { sender, _, _, _ ->
            if (sender !is Player) {
                sender.sendMessage("플레이어만 사용할 수 있습니다.")
                return@setExecutor true
            }
            val spellbook = meguminItems.createSpellbook()
            val leftover = sender.inventory.addItem(spellbook)
            leftover.values.forEach { sender.world.dropItemNaturally(sender.location, it) }
            sender.sendMessage("§d메구밍의 마법서를 지급받았습니다!")
            true
        }

        logger.info("MeguminExplosion enabled!")
    }
}
