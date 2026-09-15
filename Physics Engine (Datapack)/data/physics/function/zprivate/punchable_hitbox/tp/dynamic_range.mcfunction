$execute as @e[type=minecraft:interaction,predicate=physics:same_player_id,distance=..$(distance_alt),limit=1] positioned ~ ~-0.15 ~ run function physics:zprivate/punchable_hitbox/tp/main
