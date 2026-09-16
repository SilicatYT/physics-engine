# Get which object has the closest line-of-sight intersection point
# (Note): The distance selector is hardcoded for the currently largest possible object size of [10f, 10f, 10f]: MaxSupportedEntityInteractionRange + sqrt(3)*10/2. The calculations shouldn't overflow at an object size of 32, either.
# (Note): Entity Interaction Range is capped by the game at 64.
# (Note): The dynamic distance is has 8 possible values, so the macro is always cached.
# (TODO): Check if all these "dynamic distance" checks & function calls are actually worth it for performance, or if it's just overhead.
scoreboard players set #Physics.GotRay Physics 0
execute store result score #Physics.EntityInteractionRange Physics run attribute @s minecraft:entity_interaction_range get 1024
execute if score #Physics.EntityInteractionRange Physics matches 1..3072 as @e[type=minecraft:item_display,tag=Physics.Punchable,distance=..11.6602540378,sort=nearest] run function physics:zprivate/punchable_hitbox/aabb_check
execute if score #Physics.EntityInteractionRange Physics matches 3073..5120 as @e[type=minecraft:item_display,tag=Physics.Punchable,distance=..13.6602540378,sort=nearest] run function physics:zprivate/punchable_hitbox/aabb_check
execute if score #Physics.EntityInteractionRange Physics matches 5121.. run data modify storage physics:zprivate temp.distance set compute default float physics:punchable_hitbox/max_entity_distance
execute if score #Physics.EntityInteractionRange Physics matches 5121.. run function physics:zprivate/punchable_hitbox/use_dynamic_interaction_range with storage physics:zprivate temp

# Kill the previous tick's hitbox
execute if score @s Physics.Player.LookingAt.Id matches 1.. run function physics:zprivate/punchable_hitbox/kill/main

# No intersection happened
execute if score #Physics Physics.Player.LookingAt.Id matches -1 run return 0

# An intersection happened
    # Store the Ray Direction for later when punching
    scoreboard players operation @s Physics.Player.LookingAt.Direction.x = #Physics.Ray.Direction.x Physics
    scoreboard players operation @s Physics.Player.LookingAt.Direction.y = #Physics.Ray.Direction.y Physics
    scoreboard players operation @s Physics.Player.LookingAt.Direction.z = #Physics.Ray.Direction.z Physics

    # Calculate the intersection position (Relative to the player's eyes)
    # (Formula): RayDirection * t
    # (Note): Scaled up by 2^16.
    execute store result score @s Physics.Player.LookingAt.RelativePos.x store result storage physics:zprivate temp.x float 0.0000152587890625 run compute default integer physics:punchable_hitbox/relative_intersection_pos/scaled_direction/x
    execute store result score @s Physics.Player.LookingAt.RelativePos.y store result storage physics:zprivate temp.y float 0.0000152587890625 run compute default integer physics:punchable_hitbox/relative_intersection_pos/scaled_direction/y
    execute store result score @s Physics.Player.LookingAt.RelativePos.z store result storage physics:zprivate temp.z float 0.0000152587890625 run compute default integer physics:punchable_hitbox/relative_intersection_pos/scaled_direction/z

    # Calculate the intersection position (Relative to the object)
    # (Formula): RayOriginRelative + RayDirection * t
    # (Note): Scaled up by 2^16.
    # (Note): Necessary for the impulse calculation if the player punches later on. I could skip this and WinnerRelativePos, but then I'd need a data call when punching to get the player coordinates, and I find this here to be a cleaner alternative. I could also store the absolute ray origin pos.
    scoreboard players operation @s Physics.Player.LookingAt.RelativePos.x += #Physics.Ray.WinnerRelativePos.x Physics
    scoreboard players operation @s Physics.Player.LookingAt.RelativePos.y += #Physics.Ray.WinnerRelativePos.y Physics
    scoreboard players operation @s Physics.Player.LookingAt.RelativePos.z += #Physics.Ray.WinnerRelativePos.z Physics

    # Summon a new interaction entity
    # (Note): I currently spawn and kill the entity every tick for more responsive movement. If interaction entities ever teleport more than 4x per second, restore the teleportation behaviour from 15.09.2026. I don't use a vehicle because it introduces teleportation delay, which makes people miss their punches.
    # (TODO): Could maybe be optimized by adding a check for "if the entity is already almost at the destination position, teleport it instead".
    scoreboard players operation #Physics Physics.Player.Id = @s Physics.Player.Id
    scoreboard players operation @s Physics.Player.LookingAt.Id = #Physics Physics.Player.LookingAt.Id
    execute positioned ~ ~-0.15 ~ run function physics:zprivate/punchable_hitbox/summon/main with storage physics:zprivate temp
