# Check if the entity just loaded after unloading
# (Note): I have to re-summon the area effect cloud if that's the case. I delete it for unloaded objects for performance reasons.
scoreboard players add @s Physics.Object.Gametime 1
execute unless score @s Physics.Object.Gametime = #Physics.Gametime Physics run function physics:zprivate/simulation/after_unloading

# Object-object collisions
    # Setup scores
    scoreboard players operation #Physics.ObjectA Physics.Object.BlockPos.x = @s Physics.Object.BlockPos.x
    scoreboard players operation #Physics.ObjectA Physics.Object.BlockPos.y = @s Physics.Object.BlockPos.y
    scoreboard players operation #Physics.ObjectA Physics.Object.BlockPos.z = @s Physics.Object.BlockPos.z

    scoreboard players operation #Physics.ObjectA Physics.Object.PosWithinBlock.x = @s Physics.Object.PosWithinBlock.x
    scoreboard players operation #Physics.ObjectA Physics.Object.PosWithinBlock.y = @s Physics.Object.PosWithinBlock.y
    scoreboard players operation #Physics.ObjectA Physics.Object.PosWithinBlock.z = @s Physics.Object.PosWithinBlock.z

    scoreboard players operation #Physics.ObjectA Physics.Object.Aabb.Min.x = @s Physics.Object.Aabb.Min.x
    scoreboard players operation #Physics.ObjectA Physics.Object.Aabb.Max.x = @s Physics.Object.Aabb.Max.x
    scoreboard players operation #Physics.ObjectA Physics.Object.Aabb.Min.y = @s Physics.Object.Aabb.Min.y
    scoreboard players operation #Physics.ObjectA Physics.Object.Aabb.Max.y = @s Physics.Object.Aabb.Max.y
    scoreboard players operation #Physics.ObjectA Physics.Object.Aabb.Min.z = @s Physics.Object.Aabb.Min.z
    scoreboard players operation #Physics.ObjectA Physics.Object.Aabb.Max.z = @s Physics.Object.Aabb.Max.z

    scoreboard players operation #Physics.ObjectA Physics.Object.RotationMatrix.xx = @s Physics.Object.RotationMatrix.xx
    scoreboard players operation #Physics.ObjectA Physics.Object.RotationMatrix.xy = @s Physics.Object.RotationMatrix.xy
    scoreboard players operation #Physics.ObjectA Physics.Object.RotationMatrix.xz = @s Physics.Object.RotationMatrix.xz
    scoreboard players operation #Physics.ObjectA Physics.Object.RotationMatrix.yx = @s Physics.Object.RotationMatrix.yx
    scoreboard players operation #Physics.ObjectA Physics.Object.RotationMatrix.yy = @s Physics.Object.RotationMatrix.yy
    scoreboard players operation #Physics.ObjectA Physics.Object.RotationMatrix.yz = @s Physics.Object.RotationMatrix.yz
    scoreboard players operation #Physics.ObjectA Physics.Object.RotationMatrix.zx = @s Physics.Object.RotationMatrix.zx
    scoreboard players operation #Physics.ObjectA Physics.Object.RotationMatrix.zy = @s Physics.Object.RotationMatrix.zy
    scoreboard players operation #Physics.ObjectA Physics.Object.RotationMatrix.zz = @s Physics.Object.RotationMatrix.zz

    scoreboard players operation #Physics.ObjectA Physics.Object.Scale.x = @s Physics.Object.Scale.x
    scoreboard players operation #Physics.ObjectA Physics.Object.Scale.y = @s Physics.Object.Scale.y
    scoreboard players operation #Physics.ObjectA Physics.Object.Scale.z = @s Physics.Object.Scale.z

    # AABB & OBB check
    # (Note): I chose a tag check because it's very fast if it's outside the predicate (faster than a gametime check in the predicate).
    # (Note): AABB checks between static-dynamic have already been performed earlier, so I only check dynamic-dynamic here.
    # (Note): The max distance is hardcoded for the other object's max scale of 10: sqrt(3)*10/2 is added to the OBB Radius for cubes of size 1, 4, 7 or 10 respectively.
    # (TODO): Check if adding an obb radius check in the selector (score check) would help performance.
    tag @s add Physics.Checked
    execute if score @s Physics.Object.ObbRadius matches ..113512 as @e[type=minecraft:item_display,tag=!Physics.Checked,distance=..9.52628231048583984375,predicate=physics:collision_detection/overlaps_aabb] run function physics:zprivate/simulation/collision_detection/object/sat
    execute if score @s Physics.Object.ObbRadius matches 113513..454048 as @e[type=minecraft:item_display,tag=!Physics.Checked,distance=..12.12436580657958984375,predicate=physics:collision_detection/overlaps_aabb] run function physics:zprivate/simulation/collision_detection/object/sat
    execute if score @s Physics.Object.ObbRadius matches 454049..794584 as @e[type=minecraft:item_display,tag=!Physics.Checked,distance=..14.72244930267333984375,predicate=physics:collision_detection/overlaps_aabb] run function physics:zprivate/simulation/collision_detection/object/sat
    execute if score @s Physics.Object.ObbRadius matches 794585.. as @e[type=minecraft:item_display,tag=!Physics.Checked,distance=..17.32050895690917968755,predicate=physics:collision_detection/overlaps_aabb] run function physics:zprivate/simulation/collision_detection/object/sat

# Terrain collisions

