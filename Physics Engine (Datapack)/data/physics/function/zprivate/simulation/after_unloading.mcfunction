scoreboard players operation @s Physics.Object.Gametime = #Physics.Gametime Physics

# Summon the stack in physics:void
# (Note): See the stack information from "turn_into_physics_object".
scoreboard players operation #Physics Physics.Object.Id = @s Physics.Object.Id
execute on passengers if entity @s[type=minecraft:armor_stand,tag=Physics.ObjectArmorStand] run data modify storage physics:zprivate temp.aec_data.Owner set from entity @s UUID
execute in physics:void positioned 8.0 1032.0 8.0 summon minecraft:area_effect_cloud run function physics:zprivate/turn_into_physics_object/new_aec
