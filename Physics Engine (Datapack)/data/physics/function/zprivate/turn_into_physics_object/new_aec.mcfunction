# Setup (3)
# (Note): See the stack information from "turn_into_physics_object".
# (Note): The AECs are distributed evenly across the Y-axis, because armor stands introduce overhead per entity they share the subchunk with. My benchmarks (that didn't include any manifold entities) with even distribution showed that 128 subchunks produced the best results, and 256 was almost identical. So I chose 256 to be safer against clusters.
scoreboard players operation @s Physics.Object.Id = #Physics Physics.Object.Id
data modify entity @s {} merge from storage physics:zprivate temp.aec_data

# Summon (4) and (5)
# (Note): Because of the /ride, positions are delayed by 1 tick, so I need the tag checks. There could also be other entities from somewhere other than "new_aec" because of this.
summon minecraft:interaction 8.1 24.1 8.1 {width:0f,Tags:["Physics.Temp","Physics.ArmorStand"]}
ride @e[tag=Physics.Temp,y=24,dy=0,limit=1] mount @s
execute on passengers run data modify entity @s height set compute default integer physics:turn_into_physics_object/interaction_entity_height
execute on passengers run tag @s remove Physics.Temp

summon minecraft:armor_stand 8.1 24.1 8.1 {Marker:1b,Tags:["Physics.Temp"]}
execute on passengers run ride @e[tag=Physics.Temp,y=24,dy=0,limit=1] mount @s
execute on passengers on passengers run tag @s remove Physics.Temp

# Make (2) point at (5)
execute on passengers on passengers run data modify storage physics:zprivate temp.aec_data.Owner set from entity @s UUID
execute on origin on passengers run data modify entity @s Owner set from storage physics:zprivate temp.aec_data.Owner

# Summon (6)
summon minecraft:interaction 8.1 24.1 8.1 {height:16f,width:0f,Tags:["Physics.Temp","Physics.OldManifolds"]}
ride @e[tag=Physics.Temp,y=24,dy=0,limit=1] mount @s
execute on passengers run tag @s remove Physics.Temp

# Summon (7)
summon minecraft:interaction 8.1 24.1 8.1 {height:32f,width:0f,Tags:["Physics.Temp","Physics.NewManifolds"]}
ride @e[tag=Physics.Temp,y=24,dy=0,limit=1] mount @s
execute on passengers run tag @s remove Physics.Temp
