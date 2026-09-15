# Summon hitbox
scoreboard players add #Physics.InteractionCount Physics 1
scoreboard players operation @s Physics.Player.Id = #Physics Physics.Player.Id
scoreboard players operation @s Physics.Hitbox.Gametime = #Physics.Gametime Physics

execute on vehicle run function physics:zprivate/macro/relative_tp with storage physics:zprivate temp
tag @s remove Physics.Temp
