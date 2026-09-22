tag @s add Physics.Object
execute unless items entity @s contents * run item replace entity @s contents with minecraft:stone
data merge entity @s {teleport_duration:1, interpolation_duration:1}
execute store result score @s Physics.Object.Id store result score @s Physics.Object.Id.Mod200 run scoreboard players add #Physics Physics.Object.Id 1
scoreboard players operation @s Physics.Object.Id.Mod200 %= #Physics.Constant.200 Physics

# Setup pointer system
# (Note): It's used for island creation, and to provide an efficient reference during resolution.
# (Note): The AEC that references the armor stand is spawned in collision detection main.
    # Make an armor stand ride the physics object
    execute at @s run summon minecraft:armor_stand ~ ~ ~ {Marker:1b,Invisible:1b,Tags:["Physics.Temp"]}
    execute at @s run ride @e[type=minecraft:armor_stand,tag=Physics.Temp,distance=..0.001,limit=1] mount @s
    execute on passengers run tag @s remove Physics.Temp
    execute on passengers run data modify storage physics:zprivate temp.aec_data.Owner set from entity @s UUID

# Default values
function physics:object/set_inverse_mass with storage physics:object default
function physics:object/set_scale with storage physics:object default
function physics:object/set_orientation with storage physics:object default
