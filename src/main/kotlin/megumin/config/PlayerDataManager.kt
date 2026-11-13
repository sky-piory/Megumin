
package me.megumin.config

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.UUID

data class PlayerStats(
    var castSeconds: Int = 15,
    var damage: Double = 20.0,
    var radius: Double = 12.0,
    var cooldownSeconds: Int = 60,
    var exhaustionSeconds: Int = 8,
    var breakBlocks: Boolean = true,
    var sound: Boolean = true,
    var particles: Boolean = true
)

class PlayerDataManager(
    private val plugin: JavaPlugin
) {
    private val file = File(plugin.dataFolder, "players.yml")
    private val yaml = YamlConfiguration()
    private val cache = mutableMapOf<UUID, PlayerStats>()

    init {
        plugin.dataFolder.mkdirs()

        if (file.exists()) {
            yaml.load(file)
        } else {
            file.createNewFile()
        }
    }

    fun get(uuid: UUID): PlayerStats {
        return cache.getOrPut(uuid) {
            val path = "players.$uuid"

            PlayerStats(
                castSeconds = yaml.getInt("$path.cast", 15).coerceIn(3, 60),
                damage = yaml.getDouble("$path.damage", 20.0).coerceIn(1.0, 200.0),
                radius = yaml.getDouble("$path.radius", 12.0).coerceIn(1.0, 32.0),
                cooldownSeconds = yaml.getInt("$path.cooldown", 60).coerceIn(5, 600),
                exhaustionSeconds = yaml.getInt("$path.exhaustion", 8).coerceIn(1, 60),
                breakBlocks = yaml.getBoolean("$path.breakBlocks", true),
                sound = yaml.getBoolean("$path.sound", true),
                particles = yaml.getBoolean("$path.particles", true)
            )
        }
    }

    fun update(uuid: UUID, action: (PlayerStats) -> Unit) {
        val stats = get(uuid)
        action(stats)

        stats.castSeconds = stats.castSeconds.coerceIn(3, 60)
        stats.damage = stats.damage.coerceIn(1.0, 200.0)
        stats.radius = stats.radius.coerceIn(1.0, 32.0)
        stats.cooldownSeconds = stats.cooldownSeconds.coerceIn(5, 600)
        stats.exhaustionSeconds = stats.exhaustionSeconds.coerceIn(1, 60)

        val path = "players.$uuid"

        yaml.set("$path.cast", stats.castSeconds)
        yaml.set("$path.damage", stats.damage)
        yaml.set("$path.radius", stats.radius)
        yaml.set("$path.cooldown", stats.cooldownSeconds)
        yaml.set("$path.exhaustion", stats.exhaustionSeconds)
        yaml.set("$path.breakBlocks", stats.breakBlocks)
        yaml.set("$path.sound", stats.sound)
        yaml.set("$path.particles", stats.particles)

        yaml.save(file)
    }
}
