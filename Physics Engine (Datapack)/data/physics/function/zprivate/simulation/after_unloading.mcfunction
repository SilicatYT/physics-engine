scoreboard players operation @s Physics.Object.Gametime = #Physics.Gametime Physics

# Summon an area effect cloud referencing the armor stand
# (Note): The armor stand middleman is necessary because AECs can only reference living entities.
execute in physics:void positioned 8.0 24.0 8.0 summon minecraft:area_effect_cloud run function physics:zprivate/turn_into_physics_object/new_aec
