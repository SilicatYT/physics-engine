# Integration (Phase 1)
execute as @e[type=minecraft:item_display,tag=Physics.Object] at @s run function physics:zprivate/simulation/integration/phase_one

# Collision Detection
# (Note): Leads into Contact Generation

# Collision Resolution

# Integration (Phase 2)
execute as @e[type=minecraft:item_display,tag=Physics.Object] at @s run function physics:zprivate/simulation/integration/phase_two/main

# New Player: Set PlayerID
# (Important): It's not an advancement because the scoreboard objective gets removed when running 'uninstall', which would break it (Players lose their ID). I can't revoke advancements from offline players, and running 'uninstall' doesn't remove the advancement either.
execute as @a unless score @s Physics.Player.Id matches 1.. run function physics:zprivate/new_player

# Spawn object hitboxes
    # Spawn or teleport hitboxes
    execute store result score #Physics.Gametime Physics run time query gametime
    scoreboard players set #Physics.SuccessfulTeleportCount Physics 0
    execute as 575f7af5-d0dc-4c2c-9182-17931969f0ba at @s run function physics:zprivate/punchable_hitbox/start

    # Kill leftover hitboxes
    # (Note): Because interaction entities can unload, players can leave etc.
    execute if score #Physics.InteractionCount Physics > #Physics.SuccessfulTeleportCount Physics as @e[type=minecraft:interaction,tag=Physics.Hitbox] unless score @s Physics.Hitbox.Gametime = #Physics.Gametime Physics run function physics:zprivate/punchable_hitbox/kill/leftover

# Schedule next tick
schedule function physics:zprivate/tick 1t
