tag @s add Physics.Object
item replace entity @s contents with minecraft:stone
data merge entity @s {teleport_duration:1, interpolation_duration:1}
execute store result score @s Physics.Object.Id run scoreboard players add #Physics Physics.Object.Id 1

# Default values
function physics:object/set_inverse_mass with storage physics:object default
function physics:object/set_scale with storage physics:object default
function physics:object/set_orientation with storage physics:object default
