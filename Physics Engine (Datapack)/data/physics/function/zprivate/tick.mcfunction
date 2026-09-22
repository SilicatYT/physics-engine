execute store result score #Physics.Gametime Physics store result score #Physics.Gametime.Mod200 Physics run time query gametime
scoreboard players operation #Physics.Gametime.Mod200 Physics %= #Physics.Constant.200 Physics

# Integration (Phase 1)
execute as @e[type=minecraft:item_display,tag=Physics.Object] at @s run function physics:zprivate/simulation/integration/phase_one/main

# Collision Detection
# (Note): There are several possible approaches:
#            - Perform collision detection and instantly lead into contact generation for that pair. Then, once everything's done, build the islands.
#               - This has the disadvantage of needing to store the generated contacts in a non-per-island storage, and grouping them retroactively (necessary for resolution) takes at least 1 macro call per island merge from what I can tell.
#            - Perform collision detection (incl. the SAT), then build islands, then perform contact generation. This should get rid of the macro calls from the previous method, but I can't re-use the calculated values (AxisDot, OffsetInA & OffsetInB) from the SAT anymore during contact generation. So single objects will get more expensive (only slightly, thanks to compute), but islands should become noticeably cheaper, so I went with this method for now.
# (Note): Because static objects can become huge (128x128x128), I first perform collision detection as all static objects (with a dynamic "distance=.." check), then I perform collision detection for the dynamic objects (they're much smaller, so a distance of at most ~17 is enough).
#         There were several ideas to optimize this further:
#            - Make a tall interaction ride the object (to offset everything to a subchunk with no other entities), then put an OBB-radius-sized interaction ontop of that. Use "dx,dy,dz" and abuse the fact that it accounts for the other entities' hitbox sizes.
#              Then I could merge all sleeping islands together (ride a single root entity, use "translation" to make it appear normal) so only whole islands can unload. Then I don't need to check for unloading sleeping objects (which would wake up the island), so I can remove the per-object AEC and armor stand.
#               - Problem: Despite using the hitbox, dx, dy and dz only pulls from the pool of entities whose ORIGIN is located in the subchunks that are intersected by the check (plus padding of ~2 blocks), breaking the idea fundamentally.
#            - For each object, summon an AEC (pointing toward the armor stand riding on the object) in every chunk its AABB intersects with (with a Y-offset so no other entities are in the subchunks).
#               - Problem: Summoning new entities when intersecting new chunks is expensive (A data modification for several AECs), especially for large static objects. And for larger objects, there are just too many entities (up to 192 for a rotated max-size static object, and AECs aren't that cheap), so it's too much overhead.
#                          But 2,048 AECs (that don't do anything) only cost ~1.5mspt on my machine, so it might still be worth it or at least break even, idk.
#         But due to issues with how they work (dx only considering entities in intersected subchunks, or requiring too many AECs), I wasn't able to get them to work. That's why I went with the approach I went with.
# (Note): I need to offset the x, y and z position of the AEC selector a bit, because for some (floating point) reason, using the exact values fails.
execute as @e[type=minecraft:item_display,tag=Physics.Object] at @s run function physics:zprivate/simulation/collision_detection/main
execute in physics:void as @e[type=minecraft:area_effect_cloud,x=7.9,y=23.9,z=7.9,dy=0,scores={Physics.Object.Id=1..},predicate=!physics:has_vehicle] run function physics:zprivate/simulation/collision_detection/island_building/main

# Collision Resolution

# Integration (Phase 2)
execute as @e[type=minecraft:item_display,tag=Physics.Object] at @s run function physics:zprivate/simulation/integration/phase_two/main

# New Player: Set PlayerID
# (Important): It's not an advancement because the scoreboard objective gets removed when running 'uninstall', which would break it (Players lose their ID). I can't revoke advancements from offline players, and running 'uninstall' doesn't remove the advancement either.
execute as @a unless score @s Physics.Player.Id matches 1.. run function physics:zprivate/new_player

# Object hitboxes
    # Kill
    # (Note): InteractionCount is necessary because interaction entities can unload.
    execute if score #Physics.InteractionCount Physics matches 1.. store result score #Physics.KillCount Physics run kill @e[type=minecraft:interaction,tag=Physics.Hitbox]
    execute if score #Physics.InteractionCount Physics matches 1.. run scoreboard players operation #Physics.InteractionCount Physics -= #Physics.KillCount Physics

    # Spawn
    execute as @a[gamemode=!spectator] at @s anchored eyes positioned ^ ^ ^ run function physics:zprivate/punchable_hitbox/main

# Schedule next tick
schedule function physics:zprivate/tick 1t
