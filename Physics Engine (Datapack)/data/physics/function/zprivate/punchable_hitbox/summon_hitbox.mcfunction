# Summon hitbox
scoreboard players add #Physics.InteractionCount Physics 1
scoreboard players operation @s Physics.Player.Id = #Physics Physics.Player.Id
scoreboard players operation @s Physics.Hitbox.Gametime = #Physics.Gametime Physics

function physics:zprivate/macro/relative_tp with storage physics:zprivate temp
data merge entity @s {height: 0.3f, width: 0.3f, response: 1b}

tag @s add Physics.Hitbox
