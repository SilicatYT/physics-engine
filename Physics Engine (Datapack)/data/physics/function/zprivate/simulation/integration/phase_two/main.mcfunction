# TODO: Add split velocities from resolution

# Apply velocity to position
# (TODO): Check whether it's faster or more precise to use an int number provider (and divide by delta time denominator), or to use float and multiply by delta time.
# (TODO): Check if there's a way to calculate the new Pos without needing a macro or reducing the precision. Currently, I calculate the relative position change.
data modify storage physics:zprivate temp.x set compute default float physics:integration/pos_change/x
data modify storage physics:zprivate temp.y set compute default float physics:integration/pos_change/y
data modify storage physics:zprivate temp.z set compute default float physics:integration/pos_change/z
function physics:zprivate/macro/relative_tp with storage physics:zprivate temp

# Apply velocity to orientation
# (Note): Check if I should add a small epsilon for the squared length as the guard, instead of an exact "is not zero" guard.
data modify storage physics:zprivate temp.length set compute default float physics:integration/update_orientation/velocity_length
execute if predicate physics:integration/update_orientation/length_is_enough run function physics:zprivate/simulation/integration/phase_two/update_orientation/main

# Clear accumulators
execute \
    store result score @s Physics.Object.LinearVelocityFromAcceleration.x \
    store result score @s Physics.Object.LinearVelocityFromAcceleration.y \
    store result score @s Physics.Object.LinearVelocityFromAcceleration.z \
    store result score @s Physics.Object.AngularVelocityFromTorque.x \
    store result score @s Physics.Object.AngularVelocityFromTorque.y \
    run scoreboard players set @s Physics.Object.AngularVelocityFromTorque.z 0

# Update rotation matrix
# (Note): Inlined from 'physics:zprivate/update_derived_data/rotation_matrix'
execute store result score @s Physics.Object.RotationMatrix.xx run compute default float physics:integration/rotation_matrix/xx
execute store result score @s Physics.Object.RotationMatrix.xy run compute default float physics:integration/rotation_matrix/xy
execute store result score @s Physics.Object.RotationMatrix.xz run compute default float physics:integration/rotation_matrix/xz
execute store result score @s Physics.Object.RotationMatrix.yx run compute default float physics:integration/rotation_matrix/yx
execute store result score @s Physics.Object.RotationMatrix.yy run compute default float physics:integration/rotation_matrix/yy
execute store result score @s Physics.Object.RotationMatrix.yz run compute default float physics:integration/rotation_matrix/yz
execute store result score @s Physics.Object.RotationMatrix.zx run compute default float physics:integration/rotation_matrix/zx
execute store result score @s Physics.Object.RotationMatrix.zy run compute default float physics:integration/rotation_matrix/zy
execute store result score @s Physics.Object.RotationMatrix.zz run compute default float physics:integration/rotation_matrix/zz

# Update half extent axis projections
# (Note): Inlined from 'physics:zprivate/update_derived_data/half_extent_axis_projections'
    # Object Axis X
    execute store result score @s Physics.Object.HalfExtentAxisProjection.xx run compute default float physics:other/half_extent_axis_projection/xx
    execute store result score @s Physics.Object.HalfExtentAxisProjection.xy run compute default float physics:other/half_extent_axis_projection/xy
    execute store result score @s Physics.Object.HalfExtentAxisProjection.xz run compute default float physics:other/half_extent_axis_projection/xz

    # Object Axis Y
    execute store result score @s Physics.Object.HalfExtentAxisProjection.yx run compute default float physics:other/half_extent_axis_projection/yx
    execute store result score @s Physics.Object.HalfExtentAxisProjection.yy run compute default float physics:other/half_extent_axis_projection/yy
    execute store result score @s Physics.Object.HalfExtentAxisProjection.yz run compute default float physics:other/half_extent_axis_projection/yz

    # Object Axis Z
    execute store result score @s Physics.Object.HalfExtentAxisProjection.zx run compute default float physics:other/half_extent_axis_projection/zx
    execute store result score @s Physics.Object.HalfExtentAxisProjection.zy run compute default float physics:other/half_extent_axis_projection/zy
    execute store result score @s Physics.Object.HalfExtentAxisProjection.zz run compute default float physics:other/half_extent_axis_projection/zz

# Update AABB
# (Note): Inlined from 'physics:zprivate/update_derived_data/aabb'
execute store result score #Physics.Math.0 Physics run compute default integer physics:other/aabb/half_size/x
execute store result score #Physics.Math.1 Physics run compute default integer physics:other/aabb/half_size/y
execute store result score #Physics.Math.2 Physics run compute default integer physics:other/aabb/half_size/z

execute store result score @s Physics.Object.Aabb.Min.x run compute default integer physics:other/aabb/min/x
execute store result score @s Physics.Object.Aabb.Min.y run compute default integer physics:other/aabb/min/y
execute store result score @s Physics.Object.Aabb.Min.z run compute default integer physics:other/aabb/min/z

execute store result score @s Physics.Object.Aabb.Max.x run compute default integer physics:other/aabb/max/x
execute store result score @s Physics.Object.Aabb.Max.y run compute default integer physics:other/aabb/max/y
execute store result score @s Physics.Object.Aabb.Max.z run compute default integer physics:other/aabb/max/z

# Update world inertia
# (Note): Inlined from 'physics:zprivate/update_derived_data/specific_inverse_world_inertia'
execute if entity @s[tag=Physics.IsIsotropic] run return 0
execute store result score @s Physics.Object.SpecificInverseInertiaWorld.xx run compute default float physics:other/specific_inverse_inertia_world/xx
execute store result score @s Physics.Object.SpecificInverseInertiaWorld.xy store result score @s Physics.Object.SpecificInverseInertiaWorld.yx run compute default float physics:other/specific_inverse_inertia_world/xy
execute store result score @s Physics.Object.SpecificInverseInertiaWorld.xz store result score @s Physics.Object.SpecificInverseInertiaWorld.zx run compute default float physics:other/specific_inverse_inertia_world/xz
execute store result score @s Physics.Object.SpecificInverseInertiaWorld.yy run compute default float physics:other/specific_inverse_inertia_world/yy
execute store result score @s Physics.Object.SpecificInverseInertiaWorld.yz store result score @s Physics.Object.SpecificInverseInertiaWorld.zy run compute default float physics:other/specific_inverse_inertia_world/yz
execute store result score @s Physics.Object.SpecificInverseInertiaWorld.zz run compute default float physics:other/specific_inverse_inertia_world/zz
