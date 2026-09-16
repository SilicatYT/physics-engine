
# (Note): I assume that the entity cannot unload (and become inaccessible for the remaining physics objects of the tick) when it only teleports near a physics object or 0,0. If that's not how it works, I'll have to change this approach.
# (TODO): ^ verify
execute as @e[type=minecraft:item_display,tag=Physics.Object] at @s run function physics:zprivate/simulation/integration/phase_one/main
tp @s ~ ~ ~
