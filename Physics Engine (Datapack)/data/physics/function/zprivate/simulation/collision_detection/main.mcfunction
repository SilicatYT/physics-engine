# Check if the entity just loaded after unloading
# (Note): I have to re-summon the area effect cloud if that's the case. I delete it for unloaded objects for performance reasons.
scoreboard players add @s Physics.Object.Gametime 1
execute unless score @s Physics.Object.Gametime = #Physics.Gametime Physics run function physics:zprivate/simulation/after_unloading

# Object-object collisions
