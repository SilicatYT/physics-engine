# Setup (3)
# (Note): See the stack information from "turn_into_physics_object".
# (Note): The AECs are distributed evenly across the Y-axis, because armor stands introduce overhead per entity they share the subchunk with. My benchmarks (that didn't include any manifold entities) with even distribution showed that 128 subchunks produced the best results, and 256 was almost identical. So I chose 256 to be safer against clusters.
# (TODO): Check whether adding an interaction entity to each stack that the armor stand rides on would improve performance (-> the armor stands would be alone in the subchunk).
tag @s add Physics.BaseAEC
scoreboard players operation @s Physics.Object.Id = #Physics Physics.Object.Id
data modify entity @s {} merge from storage physics:zprivate temp.aec_data_with_pos


# Summon (4)
# (TODO): If possible, fix the issue where the armor stand's position only updates 1 tick delayed, so I need to specify the tag in the interaction ride command... For now, I just reserved this subchunk (24.0) for cases where I need tag checks. Only gets run very rarely anyway.
summon minecraft:armor_stand 8.1 24.1 8.1 {Marker:1b,Tags:["Physics.Temp"]}
ride @e[tag=Physics.Temp,dy=0,limit=1] mount @s
execute on passengers run tag @s remove Physics.Temp

# Make (2) point at (4)
execute on passengers run data modify storage physics:zprivate temp.aec_data.Owner set from entity @s UUID
execute on origin on passengers run data modify entity @s Owner set from storage physics:zprivate temp.aec_data.Owner

# Summon (5)
# (Note): Interactions with 0 height and width use less memory, even if it doesn't matter for this pack's functionality.
summon minecraft:interaction 8.1 24.1 8.1 {height:0f,width:0f,Tags:["Physics.Temp"]}
ride @e[tag=Physics.Temp,dy=0,limit=1] mount @s
execute on passengers run tag @s remove Physics.Temp
