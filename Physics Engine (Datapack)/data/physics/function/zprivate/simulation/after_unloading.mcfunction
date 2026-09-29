scoreboard players operation @s Physics.Object.Gametime = #Physics.Gametime Physics

# Kill the previous (3) and all its passengers (manifolds, contact points etc)
# (Note): This is necessary because "execute on" can currently (as of 26.4-snapshot-2) sometimes target unloaded entities, which can cause duplicates.
execute on passengers on passengers on origin in physics:void run tp @s 8.0 40.0 8.0
execute in physics:void positioned 8.0 40.0 8.0 run kill @e[distance=..1]

# Summon (3) and (4)
# (Note): See the stack information from "turn_into_physics_object".
execute on passengers run data modify storage physics:zprivate temp.aec_data.Owner set from entity @s UUID
execute in physics:void positioned 8.0 24.0 8.0 summon minecraft:area_effect_cloud run function physics:zprivate/turn_into_physics_object/new_aec
