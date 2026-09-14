# Get which object has the closest line-of-sight intersection point
# (Note): The distance selector is hardcoded for the currently largest possible object size of [10f, 10f, 10f]: MaxSupportedEntityInteractionRange + sqrt(3)*10/2. The calculations shouldn't overflow at an object size of 32, either.
# (Note): Entity Interaction Range is capped by the game at 64.
# (Note): The dynamic distance is has 8 possible values, so the macro is always cached.
scoreboard players set #Physics.GotRay Physics 0
execute store result score #Physics.EntityInteractionRange Physics run attribute @s minecraft:entity_interaction_range get 1024
execute if score #Physics.EntityInteractionRange Physics matches 1..3072 at @s anchored eyes positioned ^ ^ ^ as @e[type=minecraft:item_display,tag=Physics.Object,distance=..11.6602540378,sort=nearest] run function physics:zprivate/punchable_hitbox/aabb_intersection/check
execute if score #Physics.EntityInteractionRange Physics matches 3073..5120 at @s anchored eyes positioned ^ ^ ^ as @e[type=minecraft:item_display,tag=Physics.Object,distance=..13.6602540378,sort=nearest] run function physics:zprivate/punchable_hitbox/aabb_intersection/check
execute unless score #Physics.EntityInteractionRange Physics matches 0..3072 unless score #Physics.EntityInteractionRange Physics matches 3073..5120 run data modify storage physics:zprivate temp.distance set compute default float physics:punchable_hitbox/max_entity_distance
execute unless score #Physics.EntityInteractionRange Physics matches 0..3072 unless score #Physics.EntityInteractionRange Physics matches 3073..5120 run function physics:zprivate/punchable_hitbox/use_dynamic_interaction_range with storage physics:zprivate temp

# Check if an intersection happened
#execute if score #Physics.MinDistance Physics matches 2147483647 run return run execute if score @s Physics.Player.LookingAt.Id matches 1.. run function physics:zprivate/punchable_hitbox/kill_hitbox

# An intersection happened
    # Store the Ray Direction for later when punching
    #scoreboard players operation @s Physics.Player.LookingAt.Direction.x = #Physics.Ray.Direction.x Physics
    #scoreboard players operation @s Physics.Player.LookingAt.Direction.y = #Physics.Ray.Direction.y Physics
    #scoreboard players operation @s Physics.Player.LookingAt.Direction.z = #Physics.Ray.Direction.z Physics

    # Calculate the targeted position

    #DOES IT HAVE TO BE ABSOLUTE, OR CAN I MAKE IT RELATIVE TO THE PLAYER? I NEED TO MAKE IT WORK FOR THE FULL POS RANGE (incl. worldborder)

    #TICKING PERFORMANCE HAS PRIORITY OVER MAYBE NEEDING AN EXTRA DATA CALL ON PUNCHES

    #=> In the intersection checks, RayPos is relative to the object. So I can store it in a single scaled up score (no splitting necessary).
    #   Then, in here, where I calculate the raypos: Calculate it relative to the player (still only 1 score).
    #   Then, when punching an object, I can split it into two scores and apply an offset, OR directly make it relative to the object and keep it as a single score?

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

    # Try to teleport the interaction entity
    #execute if score @s Physics.Player.LookingAt.Id matches 1.. run scoreboard players set #Physics.MinDistance Physics -1
    #scoreboard players operation @s Physics.Player.LookingAt.Id = #Physics Physics.Player.LookingAt.Id
    #execute if score #Physics.MinDistance Physics matches -1 at @s as @e[type=minecraft:interaction,predicate=physics:same_player_id,distance=..17.3205080757,limit=1] if function physics:zprivate/punchable_hitbox/tp_hitbox run return run scoreboard players set #Physics.MinDistance Physics 2147483647
    #scoreboard players set #Physics.MinDistance Physics 2147483647

    # Summon a new interaction entity
    #scoreboard players operation #Physics Physics.Player.Id = @s Physics.Player.Id
    #execute summon minecraft:interaction run function physics:zprivate/punchable_hitbox/summon_hitbox
