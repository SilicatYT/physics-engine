# Summon hitbox
scoreboard players add #Physics.InteractionCount Physics 1
scoreboard players operation @s Physics.Player.Id = #Physics Physics.Player.Id
scoreboard players operation @s Physics.Hitbox.Gametime = #Physics.Gametime Physics

data modify entity @s {} merge from storage physics:zprivate hitbox_data

tag @s add Physics.Hitbox
