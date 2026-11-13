
package me.megumin

import me.megumin.config.PlayerDataManager
import me.megumin.config.PlayerStats
import org.bukkit.*
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

class ExplosionSkill(
    private val plugin: Main,
    private val dataManager: PlayerDataManager
) : Listener {

    private val cooldowns = mutableMapOf<UUID, Long>()
    private val exhausted = mutableSetOf<UUID>()
    private val originalSpeeds = mutableMapOf<UUID, Float>()
    private val chargingTasks = mutableMapOf<UUID, BukkitRunnable>()
    private val readyToDetonate = mutableSetOf<UUID>()

    private val visualRadius = 30.0

    private val redDust = Particle.DustOptions(
        Color.fromRGB(230, 0, 35),
        2.0f
    )

    private val purpleDust = Particle.DustOptions(
        Color.fromRGB(130, 0, 220),
        1.8f
    )

    private val brightDust = Particle.DustOptions(
        Color.fromRGB(255, 80, 100),
        3.0f
    )

    fun cast(player: Player) {
        val id = player.uniqueId
        val now = System.currentTimeMillis()
        val stats = dataManager.get(id)

        if (id in exhausted) {
            player.sendMessage("§c아직 움직일 수 없다!")
            return
        }

        // 영창이 끝난 상태에서 다시 우클릭하면 폭발한다.
        if (id in readyToDetonate) {
            readyToDetonate.remove(id)
            cooldowns[id] = now + (stats.cooldownSeconds * 1000L)
            player.sendMessage("§c폭렬마법 발동!")
            detonate(player, stats)
            return
        }

        // 영창 중 중복 입력 방지
        if (id in chargingTasks) {
            player.sendMessage("§e아직 영창 중이다...")
            return
        }

        val cooldown = cooldowns[id] ?: 0L
        if (now < cooldown) {
            val seconds = ((cooldown - now) / 1000) + 1
            player.sendMessage("§c쿨타임: ${seconds}초")
            return
        }

        player.sendTitle(
            "§cEXPLOSION!",
            "§7폭렬마법 영창 시작 (${stats.castSeconds}초)",
            10, 40, 10
        )

        val targetTicks = (stats.castSeconds * 20).coerceAtLeast(0)
        val task = object : BukkitRunnable() {
            private var elapsed = 0

            override fun run() {
                if (!player.isOnline) {
                    chargingTasks.remove(id)
                    cancel()
                    return
                }

                val center = player.location
                val progress = if (targetTicks > 0) {
                    (elapsed.toDouble() / targetTicks).coerceIn(0.0, 1.0)
                } else {
                    1.0
                }

                if (stats.particles && elapsed % 5 == 0) {
                    createMagicCircle(center, elapsed, progress)
                }

                if (elapsed % 10 == 0) {
                    if (stats.particles) {
                        player.world.spawnParticle(
                            Particle.FLAME,
                            center.clone().add(0.0, 1.0, 0.0),
                            7,
                            0.35, 0.35, 0.35,
                            0.015
                        )
                    }

                    if (stats.sound) {
                        player.world.playSound(
                            center,
                            Sound.BLOCK_BEACON_AMBIENT,
                            1f,
                            0.7f + progress.toFloat() * 0.8f
                        )
                    }
                }

                if (elapsed >= targetTicks) {
                    cancel()
                    chargingTasks.remove(id)
                    readyToDetonate.add(id)
                    player.sendTitle(
                        "§c영창 완료!",
                        "§e다시 우클릭하면 폭렬마법 발동",
                        5, 60, 10
                    )
                    player.sendMessage("§e[폭렬마법] 영창 완료! 지팡이를 다시 우클릭하면 발동한다.")
                    return
                }

                if (elapsed > 0 && elapsed % 60 == 0) {
                    player.sendMessage("§c[폭렬마법] §f마력이 집중되고 있다...")
                }
                elapsed++
            }
        }

        chargingTasks[id] = task
        task.runTaskTimer(plugin, 0L, 1L)
    }

    // =====================================
    // 마법진 전체 연출
    // =====================================

    private fun createMagicCircle(
        center: Location,
        elapsed: Int,
        progress: Double
    ) {
        val world = center.world ?: return
        val rotation = elapsed * 1.2

        val ground = center.clone().apply {
            y = blockY.toDouble() + 0.12
        }
        val sky = center.clone().add(0.0, 12.0, 0.0)

        // 바닥에는 간격을 둔 네 겹의 원형 마법진만 표시한다.
        val groundRings = listOf(18.0, 13.0, 8.0, 3.5)
        groundRings.forEachIndexed { index, radius ->
            val dust = if (index % 2 == 0) redDust else purpleDust
            for (angle in 0 until 360 step 12) {
                val rad = Math.toRadians(angle.toDouble() + rotation * (if (index % 2 == 0) 1 else -1))
                val point = ground.clone().add(
                    cos(rad) * radius,
                    0.0,
                    sin(rad) * radius
                )
                world.spawnParticle(Particle.DUST, point, 1, dust)
            }
        }

        // 바닥의 간단한 별 문양과 여덟 개의 방사형 선
        val starPoints = mutableListOf<Location>()
        for (i in 0 until 10) {
            val angle = Math.toRadians(i * 36.0 + rotation)
            val radius = if (i % 2 == 0) 8.0 else 3.4
            starPoints.add(
                ground.clone().add(cos(angle) * radius, 0.025, sin(angle) * radius)
            )
        }

        for (i in starPoints.indices) {
            val from = starPoints[i]
            val to = starPoints[(i + 1) % starPoints.size]
            for (step in 0..8) {
                val t = step / 8.0
                val point = from.clone().add(
                    (to.x - from.x) * t,
                    (to.y - from.y) * t,
                    (to.z - from.z) * t
                )
                world.spawnParticle(Particle.DUST, point, 1, brightDust)
            }
        }

        for (i in 0 until 8) {
            val angle = Math.toRadians(i * 45.0 + rotation * 0.25)
            val inner = ground.clone().add(cos(angle) * 9.0, 0.02, sin(angle) * 9.0)
            val outer = ground.clone().add(cos(angle) * 18.0, 0.02, sin(angle) * 18.0)
            for (step in 0..10) {
                val t = step / 10.0
                val point = inner.clone().add(
                    (outer.x - inner.x) * t,
                    0.0,
                    (outer.z - inner.z) * t
                )
                world.spawnParticle(
                    Particle.DUST,
                    point,
                    1,
                    if (i % 2 == 0) purpleDust else redDust
                )
            }
        }

        // 머리 위에는 얇은 원 두 겹만 두어 시야를 가리지 않게 한다.
        for ((index, radius) in listOf(8.0, 4.5).withIndex()) {
            for (angle in 0 until 360 step 18) {
                val rad = Math.toRadians(angle.toDouble() - rotation * (index + 1) * 0.35)
                val point = sky.clone().add(
                    cos(rad) * radius,
                    0.0,
                    sin(rad) * radius
                )
                world.spawnParticle(
                    Particle.DUST,
                    point,
                    1,
                    if (index == 0) purpleDust else brightDust
                )
            }
        }

        // 영창 후반에는 플레이어 발밑에 작은 에너지만 모은다.
        if (progress > 0.7) {
            val amount = 3 + (progress * 5).toInt()
            world.spawnParticle(
                Particle.ENCHANT,
                center.clone().add(0.0, 0.25, 0.0),
                amount,
                0.45, 0.15, 0.45,
                0.08
            )
        }
    }

    // =====================================
    // 폭발 연출
    // =====================================

    private fun detonate(player: Player, stats: PlayerStats) {

        val world = player.world
        val location = player.location

        player.sendTitle(
            "§4EXPLOSION!!",
            "§c§l爆裂魔法",
            5, 50, 20
        )

        if (stats.sound) {

            world.playSound(
                location,
                Sound.ENTITY_GENERIC_EXPLODE,
                5f,
                0.6f
            )

            world.playSound(
                location,
                Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
                3f,
                0.8f
            )
        }

        if (stats.particles) {

            // 폭발 중심
            world.spawnParticle(
                Particle.FLAME,
                location,
                400,
                4.0, 3.0, 4.0,
                0.2
            )

            // 거대한 연기
            world.spawnParticle(
                Particle.CLOUD,
                location,
                300,
                5.0, 3.0, 5.0,
                0.25
            )

            // 붉은 마력 잔광
            world.spawnParticle(
                Particle.DUST,
                location,
                250,
                5.0, 3.0, 5.0,
                0.0,
                brightDust
            )

            world.spawnParticle(
                Particle.LAVA,
                location,
                100,
                4.0, 2.0, 4.0
            )

            // 하늘로 솟아오르는 불꽃
            world.spawnParticle(
                Particle.FLAME,
                location.clone().add(0.0, 3.0, 0.0),
                150,
                2.0, 5.0, 2.0,
                0.12
            )

            // 충격파 애니메이션
            createShockwave(location)
        }

        player.addPotionEffect(
            PotionEffect(
                PotionEffectType.RESISTANCE,
                40,
                4
            )
        )

        // 실제 폭발 설정은 기존 스테이터스 유지
        val explosionPower =
            (stats.radius / 1.5).toFloat().coerceIn(1.0f, 20.0f)

        world.createExplosion(
            location,
            explosionPower,
            stats.breakBlocks,
            stats.breakBlocks,
            player
        )

        val r = stats.radius

        location.getNearbyEntities(r, r, r).forEach { entity ->

            if (entity is LivingEntity &&
                entity.uniqueId != player.uniqueId
            ) {
                entity.damage(stats.damage, player)
            }
        }

        startExhaustion(
            player,
            stats.exhaustionSeconds * 20L
        )
    }

    // =====================================
    // 바깥으로 퍼지는 시각적 충격파
    // =====================================

    private fun createShockwave(center: Location) {

        val world = center.world ?: return

        object : BukkitRunnable() {

            private var radius = 2.0

            override fun run() {

                if (radius > visualRadius) {
                    cancel()
                    return
                }

                for (angle in 0 until 360 step 10) {

                    val rad = Math.toRadians(
                        angle.toDouble()
                    )

                    val point = center.clone().add(
                        cos(rad) * radius,
                        0.3,
                        sin(rad) * radius
                    )

                    world.spawnParticle(
                        Particle.DUST,
                        point,
                        1,
                        brightDust
                    )

                    if (angle % 30 == 0) {
                        world.spawnParticle(
                            Particle.FLAME,
                            point,
                            1,
                            0.1, 0.1, 0.1,
                            0.01
                        )
                    }
                }

                // 충격파의 연기
                for (angle in 0 until 360 step 30) {

                    val rad = Math.toRadians(
                        angle.toDouble()
                    )

                    val point = center.clone().add(
                        cos(rad) * radius,
                        1.0,
                        sin(rad) * radius
                    )

                    world.spawnParticle(
                        Particle.CLOUD,
                        point,
                        1,
                        0.1, 0.2, 0.1,
                        0.01
                    )
                }

                radius += 2.5
            }

        }.runTaskTimer(plugin, 0L, 2L)
    }

    // =====================================
    // 탈진
    // =====================================

    private fun startExhaustion(
        player: Player,
        durationTicks: Long
    ) {

        val id = player.uniqueId

        exhausted.add(id)
        originalSpeeds[id] = player.walkSpeed
        player.walkSpeed = 0f

        player.addPotionEffect(
            PotionEffect(
                PotionEffectType.SLOWNESS,
                durationTicks.toInt(),
                10,
                false,
                false,
                true
            )
        )

        player.sendTitle(
            "§7...",
            "§8마력이 모두 소진되었다.",
            10, durationTicks.toInt().coerceAtLeast(20), 20
        )

        Bukkit.getScheduler().runTaskLater(
            plugin,
            Runnable {

                if (player.isOnline) {

                    player.walkSpeed =
                        originalSpeeds[id] ?: 0.2f

                    player.removePotionEffect(
                        PotionEffectType.SLOWNESS
                    )

                    player.sendMessage(
                        "§a마력이 조금씩 회복되었다."
                    )
                }

                exhausted.remove(id)
                originalSpeeds.remove(id)

            },
            durationTicks
        )
    }

    @EventHandler
    fun onMove(event: PlayerMoveEvent) {

        if (event.player.uniqueId !in exhausted) return

        val to = event.to ?: return
        val from = event.from

        if (
            from.x != to.x ||
            from.y != to.y ||
            from.z != to.z
        ) {
            event.to = from.clone().apply {
                yaw = to.yaw
                pitch = to.pitch
            }
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {

        val player = event.player
        val id = player.uniqueId

        chargingTasks.remove(id)?.cancel()
        readyToDetonate.remove(id)

        originalSpeeds[id]?.let {
            player.walkSpeed = it
        }

        player.removePotionEffect(
            PotionEffectType.SLOWNESS
        )

        exhausted.remove(id)
        originalSpeeds.remove(id)
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {

        val player = event.player

        if (player.walkSpeed <= 0f) {
            player.walkSpeed = 0.2f
            player.removePotionEffect(
                PotionEffectType.SLOWNESS
            )
        }
    }
}


