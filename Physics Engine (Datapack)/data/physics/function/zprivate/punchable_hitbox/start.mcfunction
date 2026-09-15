# (Note): The marker has to be teleported to the initial position by the end to avoid unloading it. The starting dimension or world origin cannot be assumed.
# (Note): Setting the rotation to 0 0 is currently necessary because relative teleports can accumulate rotation past the clamped bounds. This would cause overflows in the following calculations.
execute as @a at @s anchored eyes positioned ^ ^ ^ run function physics:zprivate/punchable_hitbox/main
tp @s ~ ~ ~ 0 0
