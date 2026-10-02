scoreboard players operation @s Physics.Object.Gametime = #Physics.Gametime Physics

# Summon (3) and (4)
# (Note): See the stack information from "turn_into_physics_object".
scoreboard players operation #Physics Physics.Object.Id = @s Physics.Object.Id
execute on passengers run data modify storage physics:zprivate temp.aec_data_with_pos.Owner set from entity @s UUID
data modify storage physics:zprivate temp.aec_data_with_pos.Pos[1] set compute default integer physics:turn_into_physics_object/aec_y_position
execute in physics:void positioned 8.0 24.0 8.0 summon minecraft:area_effect_cloud run function physics:zprivate/turn_into_physics_object/new_aec
