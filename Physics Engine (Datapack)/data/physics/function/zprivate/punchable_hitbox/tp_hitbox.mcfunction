# Teleport the interaction entity to the targeted position
# (TODO): Benchmark whether a macro tp would be faster.
data modify entity @s Pos set from storage physics:zprivate hitbox_data.Pos
scoreboard players operation @s Physics.Hitbox.Gametime = #Physics.Gametime Physics
scoreboard players add #Physics.SuccessfulTeleports Physics 1

# (Note): To make sure it's always the max int limit by the end of the tick.
scoreboard players set #Physics.MinDistance Physics 2147483647
