# Exponential Map Integration
# (TODO): Check whether pre-calculating 'length' or running the sqrt inline is faster (assuming I DON'T already calculate it for the "is length (squared?) enough?" check.
# (TODO): Check whether using the Taylor series approximation for small angles would be worth it, or if the overhead from checking storage values is too large.
# (TODO): Check whether pre-calculating 'inverse_length' would be worth it, or if I should divide by 'length' three times. I'd only save 2 divisions, but I'd need an extra command with 2 storage accesses, and 3 extra multiplications.
# (TODO): Check whether pre-calculating 'sin_half_angle' would be worth it, or if I should get the sin three times. I'd only save 2 sins, but I'd need an extra command with 2 storage accesses.
# (TODO): => Overall, re-consider every variable and compare to the Java code. I store some variables explicitly while inlining others (like half_angle). There's a balance to be met to maximize performance.
# (Note): Currently, I don't apply the Taylor series approximation for small angles.
# (Note): The rotation change is mathematically guaranteed to be normalized, so I can store it in a score with the same scaling factor as orientation without worrying about overflows.
data modify storage physics:zprivate temp.sin_half_angle set compute default float physics:integration/update_orientation/sin_half_angle
execute store result score #Physics.Math.0 Physics run compute default float physics:integration/update_orientation/rotation/x
execute store result score #Physics.Math.1 Physics run compute default float physics:integration/update_orientation/rotation/y
execute store result score #Physics.Math.2 Physics run compute default float physics:integration/update_orientation/rotation/z
execute store result score #Physics.Math.3 Physics run compute default float physics:integration/update_orientation/rotation/a

# Apply rotation to orientation
# (Formula): orientation = rotation * orientation
execute store result score @s Physics.Object.Orientation.x store result storage physics:zprivate temp.orientation[0] float 0.000000059604644775390625 run compute default float physics:integration/update_orientation/x
execute store result score @s Physics.Object.Orientation.y store result storage physics:zprivate temp.orientation[1] float 0.000000059604644775390625 run compute default float physics:integration/update_orientation/y
execute store result score @s Physics.Object.Orientation.z store result storage physics:zprivate temp.orientation[2] float 0.000000059604644775390625 run compute default float physics:integration/update_orientation/z
execute store result score @s Physics.Object.Orientation.a store result storage physics:zprivate temp.orientation[3] float 0.000000059604644775390625 run compute default float physics:integration/update_orientation/a
data modify entity @s transformation.left_rotation set from storage physics:zprivate temp.orientation
