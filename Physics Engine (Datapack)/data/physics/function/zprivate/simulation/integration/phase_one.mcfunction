# Get entity data
data modify storage physics:zprivate entity_data set from entity @s

# Set internal pos to entity pos
# (Explanation): If the entity gets teleported, it should automatically update the internal pos values rather than teleport back to its original position. That's why I update the pos scores every tick.
execute store result score @s Physics.Object.BlockPos.x store result storage physics:zprivate temp.x int -1 run data get storage physics:zprivate entity_data.Pos[0]
execute store result score @s Physics.Object.BlockPos.y store result storage physics:zprivate temp.y int -1 run data get storage physics:zprivate entity_data.Pos[1]
execute store result score @s Physics.Object.BlockPos.z store result storage physics:zprivate temp.z int -1 run data get storage physics:zprivate entity_data.Pos[2]

# (Note): I need the PosWithinBlock at high precision. Number providers use floats, so it would be much less precise to use one at large coordinates.
function physics:zprivate/macro/relative_tp with storage physics:zprivate temp
data modify storage physics:zprivate entity_data.Pos set from entity @s Pos
tp @s ~ ~ ~
execute store result score @s Physics.Object.PosWithinBlock.x run data get storage physics:zprivate entity_data.Pos[0] 16777216
execute store result score @s Physics.Object.PosWithinBlock.y run data get storage physics:zprivate entity_data.Pos[1] 16777216
execute store result score @s Physics.Object.PosWithinBlock.z run data get storage physics:zprivate entity_data.Pos[2] 16777216

# Refresh orientation
# (Note): Only necessary so it stays normalized.
execute store result score @s Physics.Object.Orientation.x run data get storage physics:zprivate entity_data.transformation.left_rotation[0] 16777216
execute store result score @s Physics.Object.Orientation.y run data get storage physics:zprivate entity_data.transformation.left_rotation[1] 16777216
execute store result score @s Physics.Object.Orientation.z run data get storage physics:zprivate entity_data.transformation.left_rotation[2] 16777216
execute store result score @s Physics.Object.Orientation.a run data get storage physics:zprivate entity_data.transformation.left_rotation[3] 16777216

# Update linear velocity
    # Gravity
    # (Note): I don't have an "AccumulatedForce" intermediate because it would easily overflow for large objects before any meaningful acceleration is achieved. So when applying force, it's directly converted to LinearVelocityFromAcceleration. So I don't need an additional step calculating that.
    # (Note): I use round() here for extra precision.
    execute unless score @s Physics.Object.InverseMass matches 0 run scoreboard players operation @s Physics.Object.LinearVelocityFromAcceleration.y += #Physics.Settings.Derived.ScaledGravityPerTick Physics

    # Apply damping, then gravity + acceleration
    # (Formula): LinearVelocity * LinearDampingPerTick + LinearVelocityFromAcceleration
    # (Note): The number provider clamps between -128 and +128 blocks per second to avoid overflows.
    execute store result score @s Physics.Object.LinearVelocity.x run compute default float physics:integration/damped_linear_velocity_plus_acceleration/x
    execute store result score @s Physics.Object.LinearVelocity.y run compute default float physics:integration/damped_linear_velocity_plus_acceleration/y
    execute store result score @s Physics.Object.LinearVelocity.z run compute default float physics:integration/damped_linear_velocity_plus_acceleration/z

# Update angular velocity
    # (Note): I don't have an "AccumulatedTorque" intermediate because it would easily overflow for large objects before any meaningful acceleration is achieved. So when applying torque, it's directly converted to AngularVelocityFromTorque. So I don't need an additional step calculating that.

    # Apply damping, then torque
    # (Formula): AngularVelocity * AngularDampingPerTick + AngularVelocityFromTorque
    # (Note): The number provider clamps between -62 and +62 radians per second to avoid overflow.
    execute store result score @s Physics.Object.AngularVelocity.x run compute default float physics:integration/damped_angular_velocity_plus_torque/x
    execute store result score @s Physics.Object.AngularVelocity.y run compute default float physics:integration/damped_angular_velocity_plus_torque/y
    execute store result score @s Physics.Object.AngularVelocity.z run compute default float physics:integration/damped_angular_velocity_plus_torque/z

# (TODO): Check if performing the int addition of damped_linear_velocity_plus_acceleration in an integer number provider is faster or more precise
# (TODO): Check if a division by the DeltaTimeDenominator score is faster than a multiplication with the data storage.
# (TODO): Check if I can pre-calculate inverseMass * deltaTime and store it as a score, to remove 1 multiplication from each component when calculating the acceleration from a force.
# (TODO): In general, go over everything and check if I can turn it into an integer number provider without losing any visible precision.
