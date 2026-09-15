# Looked away from object
scoreboard players set @s Physics.Player.LookingAt.Id 0
scoreboard players operation #Physics Physics.Player.Id = @s Physics.Player.Id

    # Kill the hitbox
    # (Note): If the hitbox is outside that range, which is unlikely, it gets killed via the other method (Gametime). It's good for performance to specify a distance here.
    # (Note): The hitbox thresholds are the same as in "main", but slightly inflated (by 0.2 blocks) because the player could have moved.
    execute if score #Physics.EntityInteractionRange Physics matches 0..3072 store success score #Physics.HitboxKillSuccess Physics run kill @e[type=minecraft:interaction,predicate=physics:same_player_id,distance=..11.8602540378,limit=1]
    execute if score #Physics.EntityInteractionRange Physics matches 3073..5120 store success score #Physics.HitboxKillSuccess Physics run kill @e[type=minecraft:interaction,predicate=physics:same_player_id,distance=..13.8602540378,limit=1]

    execute if score #Physics.EntityInteractionRange Physics matches 5121.. run data modify storage physics:zprivate temp.distance_alt set compute default float physics:punchable_hitbox/max_entity_distance_alt
    execute if score #Physics.EntityInteractionRange Physics matches 5121.. run function physics:zprivate/punchable_hitbox/kill/kill_dynamic_range with storage physics:zprivate temp

    execute if score #Physics.HitboxKillSuccess Physics matches 1 run scoreboard players remove #Physics.InteractionCount Physics 1
