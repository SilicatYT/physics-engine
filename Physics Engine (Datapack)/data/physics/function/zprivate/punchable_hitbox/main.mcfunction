# Get which object has the closest line-of-sight intersection point
# (Note): The distance selector is hardcoded for the currently largest possible object size of [10f, 10f, 10f]: MaxSupportedEntityInteractionRange + sqrt(3)*10/2. The calculations shouldn't overflow at an object size of 32, either.
# (Note): Entity Interaction Range is capped by the game at 64.
# (Note): The dynamic distance is has 8 possible values, so the macro is always cached.
# (TODO): Check if all these "dynamic distance" checks & function calls are actually worth it for performance, or if it's just overhead.
scoreboard players set #Physics.GotRay Physics 0
execute store result score #Physics.EntityInteractionRange Physics run attribute @s minecraft:entity_interaction_range get 1024
execute if score #Physics.EntityInteractionRange Physics matches 1..3072 at @s anchored eyes positioned ^ ^ ^ as @e[type=minecraft:item_display,tag=Physics.Object,distance=..11.6602540378,sort=nearest] run function physics:zprivate/punchable_hitbox/aabb_intersection/check
execute if score #Physics.EntityInteractionRange Physics matches 3073..5120 at @s anchored eyes positioned ^ ^ ^ as @e[type=minecraft:item_display,tag=Physics.Object,distance=..13.6602540378,sort=nearest] run function physics:zprivate/punchable_hitbox/aabb_intersection/check
execute if score #Physics.EntityInteractionRange Physics matches 5121.. run data modify storage physics:zprivate temp.distance set compute default float physics:punchable_hitbox/max_entity_distance
execute if score #Physics.EntityInteractionRange Physics matches 5121.. run function physics:zprivate/punchable_hitbox/use_dynamic_interaction_range with storage physics:zprivate temp



return 0


# No intersection happened
execute if score #Physics.MinDistance Physics matches 2147483647 run return run execute if score @s Physics.Player.LookingAt.Id matches 1.. at @s run function physics:zprivate/punchable_hitbox/kill_hitbox

# An intersection happened
    # Store the Ray Direction for later when punching
    scoreboard players operation @s Physics.Player.LookingAt.Direction.x = #Physics.Ray.Direction.x Physics
    scoreboard players operation @s Physics.Player.LookingAt.Direction.y = #Physics.Ray.Direction.y Physics
    scoreboard players operation @s Physics.Player.LookingAt.Direction.z = #Physics.Ray.Direction.z Physics

    # Calculate the intersection position
    # (Note): Relative to the player.





    # REWORK v
    #scoreboard players operation #Physics.RayDirectionOriginal.x Physics *= #Physics.MinDistance Physics
    #scoreboard players operation #Physics.RayDirectionOriginal.x Physics /= #Physics.Constants.1000 Physics
    #execute store result storage physics:zprivate temp.pos[0] double 0.001 store result score @s Physics.Player.LookingAt.Pos.x run scoreboard players operation #Physics.RayPosOriginal.x Physics += #Physics.RayDirectionOriginal.x Physics

    #scoreboard players operation #Physics.RayDirectionOriginal.y Physics *= #Physics.MinDistance Physics
    #scoreboard players operation #Physics.RayDirectionOriginal.y Physics /= #Physics.Constants.1000 Physics
    #scoreboard players operation #Physics.RayPosOriginal.y Physics += #Physics.RayDirectionOriginal.y Physics
    #execute store result storage physics:zprivate data.pos[1] double 0.001 store result score @s Physics.Player.LookingAt.Pos.y run scoreboard players remove #Physics.RayPosOriginal.y Physics 175

    #scoreboard players operation #Physics.RayDirectionOriginal.z Physics *= #Physics.MinDistance Physics
    #scoreboard players operation #Physics.RayDirectionOriginal.z Physics /= #Physics.Constants.1000 Physics
    #execute store result storage physics:zprivate data.pos[2] double 0.001 store result score @s Physics.Player.LookingAt.Pos.z run scoreboard players operation #Physics.RayPosOriginal.z Physics += #Physics.RayDirectionOriginal.z Physics
    # REWORK ^






    # Try to teleport the interaction entity
    # (Note): Same distance as before, but inflated by 0.2 blocks (same as in kill_hitbox).
    # (Note): If the distance check fails (rare), it summons a new interaction entity instead and kills the old one in the next tick.
    scoreboard players operation #Physics Physics.Player.Id = @s Physics.Player.Id
    execute if score @s Physics.Player.LookingAt.Id matches 1.. run scoreboard players set #Physics.MinDistance Physics -1
    scoreboard players operation @s Physics.Player.LookingAt.Id = #Physics Physics.Player.LookingAt.Id

    execute if score #Physics.MinDistance Physics matches -1 if score #Physics.EntityInteractionRange Physics matches 1..3072 at @s as @e[type=minecraft:interaction,predicate=physics:same_player_id,distance=..11.8602540378,limit=1] run return run function physics:zprivate/punchable_hitbox/tp_hitbox
    execute if score #Physics.MinDistance Physics matches -1 if score #Physics.EntityInteractionRange Physics matches 3073..5120 at @s as @e[type=minecraft:interaction,predicate=physics:same_player_id,distance=..13.8602540378,limit=1] run return run function physics:zprivate/punchable_hitbox/tp_hitbox
    execute if score #Physics.MinDistance Physics matches -1 if score #Physics.EntityInteractionRange Physics matches 5121.. run data modify storage physics:zprivate temp.distance_alt set compute default float physics:punchable_hitbox/max_entity_distance_alt
    execute if score #Physics.MinDistance Physics matches -1 if score #Physics.EntityInteractionRange Physics matches 5121.. run return run function physics:zprivate/punchable_hitbox/tp_hitbox_dynamic_range with storage physics:zprivate temp

    #scoreboard players set #Physics.MinDistance Physics 2147483647

    # Summon a new interaction entity
    execute summon minecraft:interaction run function physics:zprivate/punchable_hitbox/summon_hitbox
