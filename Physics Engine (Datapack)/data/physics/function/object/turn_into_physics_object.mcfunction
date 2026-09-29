tag @s add Physics.Object
execute unless items entity @s contents * run item replace entity @s contents with minecraft:stone
data merge entity @s {teleport_duration:1, interpolation_duration:1}
execute store result score @s Physics.Object.Id store result score @s Physics.Object.Id.Mod200 run scoreboard players add #Physics Physics.Object.Id 1
scoreboard players operation @s Physics.Object.Id.Mod200 %= #Physics.Constant.200 Physics

# Setup pointer system
# (Note): Stacks:
#           - Per Object:
#               - 1: Armor Stand riding the physics object
#               - 2: AEC riding (1), pointing toward (4)
#               - 3: AEC in physics:void for the physics object, pointing toward (1)
#               - 4: Armor Stand riding (3)
#           - Per Manifold:
#               - 5: AEC for the manifold, riding objectB's (4)
#               - 6: 1-4 Markers for the contact points (if point-face), riding (5).
# (Note): It's used for island creation, cheap manifold assignment, and to provide an efficient reference during resolution.
# (Note): The AEC that references the armor stand is spawned in collision detection main.
    # Summon (1)
    execute at @s run summon minecraft:armor_stand ~ 504 ~ {Marker:1b,Invisible:1b,Tags:["Physics.Temp"]}
    execute at @s run ride @e[type=minecraft:armor_stand,y=504,tag=Physics.Temp,distance=..0.001,limit=1] mount @s
    execute on passengers run tag @s remove Physics.Temp

    # Summon (2)
    # (Note): (3) and (4) don't exist yet, so I can't make (2) point toward (4) here.
    # (Note): "Age" is necessary so the particles don't show.
    execute at @s run summon minecraft:area_effect_cloud ~ 504 ~ {Radius:0f,Age:20,Tags:["Physics.Temp"]}
    execute at @s on passengers run ride @e[type=minecraft:area_effect_cloud,y=504,tag=Physics.Temp,distance=..0.001,limit=1] mount @s
    execute on passengers on passengers run tag @s remove Physics.Temp

# Default values
function physics:object/set_inverse_mass with storage physics:object default
function physics:object/set_scale with storage physics:object default
function physics:object/set_orientation with storage physics:object default
