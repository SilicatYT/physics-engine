# Exponential Map Integration
# (TODO): Check whether pre-calculating 'length' or running the sqrt inline is faster (assuming I DON'T already calculate it for the "is length (squared?) enough?" check.
# (TODO): Check whether using the Taylor series approximation for small angles would be worth it, or if the overhead from checking storage values is too large.
# (TODO): Check whether pre-calculating 'inverse_length' would be worth it, or if I should divide by 'length' three times. I'd only save 2 divisions, but I'd need an extra command with 2 storage accesses, and 3 extra multiplications.
# (TODO): Check whether pre-calculating 'sin_half_angle' would be worth it, or if I should get the sin three times. I'd only save 2 sins, but I'd need an extra command with 2 storage accesses.
# (TODO): => Overall, re-consider every variable and compare to the Java code. I store some variables explicitly while inlining others (like half_angle). There's a balance to be met to maximize performance.
# (Note): Currently, I don't apply the Taylor series approximation for small angles.
# (Note): The rotation change is mathematically guaranteed to be normalized, so I can store it in a score with the same scaling factor as orientation without worrying about overflows. But it can drift over time if it's not re-normalized periodically.
# (Note): I round here so I don't have to re-normalize as often.
data modify storage physics:zprivate temp.sin_half_angle set compute default float physics:integration/update_orientation/sin_half_angle
execute store result score #Physics.Math.0 Physics run compute default float physics:integration/update_orientation/rotation/x
execute store result score #Physics.Math.1 Physics run compute default float physics:integration/update_orientation/rotation/y
execute store result score #Physics.Math.2 Physics run compute default float physics:integration/update_orientation/rotation/z
execute store result score #Physics.Math.3 Physics run compute default float physics:integration/update_orientation/rotation/a

# Apply rotation to orientation
# (Formula): orientation = rotation * orientation
# (Note): I round here so I don't have to re-normalize as often.
# (Note): I have to calculate the whole quaternion before I apply it, otherwise the remaining calculations already use some of the new values.
# (Note): I dismount the entities riding the physics object so the NBT (de)serialization isn't as expensive.
# (Note): The vertical offset is used so no other entities compete in the same subchunk, improving performance. Do note that the armor stand's position won't update after /ride until the end of the tick. This isn't important here, though.
execute store result score #Physics Physics.Object.Orientation.x store result storage physics:zprivate temp.orientation[0] float 0.000000059604644775390625 run compute default float physics:integration/update_orientation/x
execute store result score #Physics Physics.Object.Orientation.y store result storage physics:zprivate temp.orientation[1] float 0.000000059604644775390625 run compute default float physics:integration/update_orientation/y
execute store result score #Physics Physics.Object.Orientation.z store result storage physics:zprivate temp.orientation[2] float 0.000000059604644775390625 run compute default float physics:integration/update_orientation/z
execute store result score #Physics Physics.Object.Orientation.a store result storage physics:zprivate temp.orientation[3] float 0.000000059604644775390625 run compute default float physics:integration/update_orientation/a

scoreboard players operation @s Physics.Object.Orientation.x = #Physics Physics.Object.Orientation.x
scoreboard players operation @s Physics.Object.Orientation.y = #Physics Physics.Object.Orientation.y
scoreboard players operation @s Physics.Object.Orientation.z = #Physics Physics.Object.Orientation.z
scoreboard players operation @s Physics.Object.Orientation.a = #Physics Physics.Object.Orientation.a

execute on passengers run tag @s add Physics.Temp
execute on passengers run tp @s ~ ~512 ~
data modify entity @s transformation.left_rotation set from storage physics:zprivate temp.orientation
execute positioned ~ ~512 ~ run ride @e[type=minecraft:armor_stand,tag=Physics.Temp,distance=..0.001,limit=1] mount @s
execute on passengers run tag @s remove Physics.Temp

# Normalize
# (Note): Only done once every 200 ticks, spread across all objects. Doing it more often wouldn't yield any visible benefit.
# (Note): Technically it could happen that an object loads in, rotates for a few ticks, then unloads, repeatedly without ever normalizing. This is unbelievably unlikely though.
# (TODO): Check whether storing the length is worth it, or if it should be inline.
execute unless score @s Physics.Object.Id.Mod200 = #Physics.Gametime.Mod200 Physics run return 0
data modify storage physics:zprivate temp.inverse_length set compute default float physics:integration/update_orientation/normalize/inverse_length
execute store result score @s Physics.Object.Orientation.x run compute default float physics:integration/update_orientation/normalize/x
execute store result score @s Physics.Object.Orientation.y run compute default float physics:integration/update_orientation/normalize/y
execute store result score @s Physics.Object.Orientation.z run compute default float physics:integration/update_orientation/normalize/z
execute store result score @s Physics.Object.Orientation.a run compute default float physics:integration/update_orientation/normalize/a
