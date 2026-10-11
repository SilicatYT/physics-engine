# Check if the entity just loaded after unloading
# (Note): I have to re-summon the area effect cloud if that's the case. I delete it for unloaded objects for performance reasons.
scoreboard players add @s Physics.Object.Gametime 1
execute unless score @s Physics.Object.Gametime = #Physics.Gametime Physics run function physics:zprivate/simulation/after_unloading

# Object-object collisions
    # Island management
    # (Note): This also sets IsSetupComplete to 1 for performance reasons. It's faster than having a separate command that sets it.
    #execute if score #Physics.IslandId Physics matches 1.. run say RESOLVE THE PREVIOUS ISLAND
    #scoreboard players add #Physics.IslandId Physics 1

    # TODO: ^ MAKE SURE THIS DOESN'T RUN IF THE PREVIOUS COLLISION DETECTION OBJECT DIDNT FIND ANY CONTACTS (-> no island exists). Either set a global score to 0 at the start of the non-recursive main function (this one) and set it to 1 on successful SATs (would set the score multiple times per island), or check if a new manifold has been created (would only need to be done once per island, in this function right here). Or maybe set the score to 1 in the "setup" function, so it only happens once per object, not once per pair. Maybe there's an even better solution, idk. Or I do a single @e[limit=1] check, would that be faster than the score stuff? Probably not.
    # TODO: And who resolves the last island? Will need to be done in the tick function, probably. A ticking check that checks if the score ("the current island has an entity") is 1, and then resolves that island.

    # Setup
    # (Note): I don't want to run all the score copies just for there to not be any AABB intersections anyway. A lot of objects won't intersect with anything, so I made a "setup" function that's called in the SAT. It's worth it, even though it costs an additional function call for colliding objects.
    scoreboard players set #Physics.IsSetupComplete Physics 0

    scoreboard players operation #Physics.ObjectA Physics.Object.Aabb.Min.x = @s Physics.Object.Aabb.Min.x
    scoreboard players operation #Physics.ObjectA Physics.Object.Aabb.Max.x = @s Physics.Object.Aabb.Max.x
    scoreboard players operation #Physics.ObjectA Physics.Object.Aabb.Min.y = @s Physics.Object.Aabb.Min.y
    scoreboard players operation #Physics.ObjectA Physics.Object.Aabb.Max.y = @s Physics.Object.Aabb.Max.y
    scoreboard players operation #Physics.ObjectA Physics.Object.Aabb.Min.z = @s Physics.Object.Aabb.Min.z
    scoreboard players operation #Physics.ObjectA Physics.Object.Aabb.Max.z = @s Physics.Object.Aabb.Max.z

    # AABB & OBB check
    # (Note): I chose a tag check because it's very fast if it's outside the predicate (faster than a gametime check in the predicate).
    # (Note): AABB checks between static-dynamic have already been performed earlier, so I only check dynamic-dynamic here.
    # (Note): The max distance is hardcoded for the other object's max scale of 10: sqrt(3)*10/2 is added to the OBB Radius for cubes of size 1, 4, 7 or 10 respectively.
    # (TODO): Check if adding an obb radius check in the selector (score check) would help performance.
    tag @s add Physics.Checked
    execute if score @s Physics.Object.ObbRadius matches ..113512 as @e[type=minecraft:item_display,tag=!Physics.Checked,distance=..9.52628231048583984375,predicate=physics:collision_detection/overlaps_aabb] run function physics:zprivate/simulation/collision_detection/object/obb
    execute if score @s Physics.Object.ObbRadius matches 113513..454048 as @e[type=minecraft:item_display,tag=!Physics.Checked,distance=..12.12436580657958984375,predicate=physics:collision_detection/overlaps_aabb] run function physics:zprivate/simulation/collision_detection/object/obb
    execute if score @s Physics.Object.ObbRadius matches 454049..794584 as @e[type=minecraft:item_display,tag=!Physics.Checked,distance=..14.72244930267333984375,predicate=physics:collision_detection/overlaps_aabb] run function physics:zprivate/simulation/collision_detection/object/obb
    execute if score @s Physics.Object.ObbRadius matches 794585.. as @e[type=minecraft:item_display,tag=!Physics.Checked,distance=..17.32050895690917968755,predicate=physics:collision_detection/overlaps_aabb] run function physics:zprivate/simulation/collision_detection/object/obb

    # Teleport AEC back
    execute if score #Physics.IsSetupComplete Physics matches 1 on passengers if entity @s[tag=Physics.ObjectArmorStand] on passengers on origin on vehicle in physics:void run tp @s 8.0 1032.0 8.0


# Terrain collisions

