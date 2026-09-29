# Setup (3)
# (Note): See the stack information from "turn_into_physics_object".
scoreboard players operation @s Physics.Object.Id = #Physics Physics.Object.Id
data modify entity @s {} merge from storage physics:zprivate temp.aec_data

# Summon (4)
summon minecraft:armor_stand 8.1 40.0 8.1 {Marker:1b}
ride @e[y=39.9,dy=0,limit=1] mount @s

# Make (2) point at (4)
execute on passengers run data modify storage physics:zprivate temp.aec_data.Owner set from entity @s UUID
execute on origin on passengers run data modify entity @s Owner set from storage physics:zprivate temp.aec_data.Owner
