$execute as @e[type=minecraft:item_display,tag=Physics.Object,distance=..$(distance),sort=nearest] run function physics:zprivate/punchable_hitbox/aabb_intersection/check
