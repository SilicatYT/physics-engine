# Teleport the interaction entity to the targeted position
execute on vehicle run function physics:zprivate/macro/relative_tp with storage physics:zprivate temp
scoreboard players operation @s Physics.Hitbox.Gametime = #Physics.Gametime Physics
scoreboard players add #Physics.SuccessfulTeleportCount Physics 1

# (Note): To make sure it's always the max int limit by the end of the tick.
scoreboard players set #Physics.MinDistance Physics 2147483647
