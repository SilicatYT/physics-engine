tag @s add Physics.Object
execute unless items entity @s contents * run item replace entity @s contents with minecraft:stone
data merge entity @s {teleport_duration:1, interpolation_duration:1}
execute store result score @s Physics.Object.Id store result score @s Physics.Object.Id.Mod200 run scoreboard players add #Physics Physics.Object.Id 1
scoreboard players operation @s Physics.Object.Id.Mod200 %= #Physics.Constant.200 Physics

# Setup pointer system
# (TODO): Benchmark whether having an additional interaction entity as a vehicle of the current tick's manifolds (instead of using the armor stand as the vehicle) would be faster. It would mean less entities near the armorstand, but more (cheap) entities overall. This also lightly affects the single @e call's performance, but I doubt that matters much. Currently I chose to have the extra interaction entity.
# (Note): Stacks:
#           - Per Object:
#               - 1: Armor Stand riding the physics object
#                   - So (3) can directly reference its object.
#               - 2: AEC riding (1), pointing toward (5)
#                   - So the object can directly reference its (3).
#               - Stack in physics:void:
#                   - 3: AEC for the physics object, pointing toward (1)
#                       - Used as a base. The object can find its related manifolds and other important data. It's also used for unload detection.
#                   - 4: Interaction riding (3)
#                       - Height: dynamic, so the armor stands will be spread across 256 different subchunks.
#                   - 5: Armor Stand riding (4)
#                       - So (3) can be directly referenced by the object.
#                       - Also used as a vehicle for the current tick's manifolds.
#                   - 6: Interaction riding (3)
#                       - So the previous tick's manifolds can be stored as its passengers, without colliding with the current tick's. Any rideable entity type works, but interactions are the fastest (even faster than block displays).
#                       - Height:16, so the manifolds aren't in the same subchunk as (5).
#                   - 7: Interaction riding (3)
#                       - So the current tick's manifolds can be stored as its passengers. I could remove this entity and store the manifolds directly on (5), but that would increase the number of entities in the armor stands' subchunks.
#                       - Height:32, so I can target the current tick's contact points or manifolds in a cheap @e call (during resolution) without having to dismount the previous tick's manifolds first.
#           - Per Manifold:
#               - 8: AEC for the manifold, riding objectB's (7, for current tick) or (6, for previous tick)
#               - 9: 1-4 Markers for the contact points (if point-face), riding (8).
# (Note): It's used for island creation, cheap manifold assignment, and to provide an efficient reference during resolution.
    # Summon (1)
    execute at @s run summon minecraft:armor_stand ~ 504 ~ {Marker:1b,Invisible:1b,Tags:["Physics.Temp"]}
    execute at @s run ride @e[type=minecraft:armor_stand,y=504,tag=Physics.Temp,distance=..0.001,limit=1] mount @s
    execute on passengers run tag @s remove Physics.Temp

    # Summon (2)
    # (Note): The entities in physics:void are spawned later.
    # (Note): "Age" is necessary so the particles don't show.
    execute at @s run summon minecraft:area_effect_cloud ~ 504 ~ {Radius:0f,Age:20,Tags:["Physics.Temp"]}
    execute at @s on passengers run ride @e[type=minecraft:area_effect_cloud,y=504,tag=Physics.Temp,distance=..0.001,limit=1] mount @s
    execute on passengers on passengers run tag @s remove Physics.Temp

# Default values
function physics:object/set_inverse_mass with storage physics:object default
function physics:object/set_scale with storage physics:object default
function physics:object/set_orientation with storage physics:object default
