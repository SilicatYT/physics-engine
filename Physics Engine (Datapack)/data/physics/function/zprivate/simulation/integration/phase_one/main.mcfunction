# Get entity data
tp 575f7af5-d0dc-4c2c-9182-17931969f0ba ~ ~ ~
data modify storage physics:zprivate pos set from entity 575f7af5-d0dc-4c2c-9182-17931969f0ba Pos

# Set internal pos to entity pos
# (Explanation): If the entity gets teleported, it should automatically update the internal pos values rather than teleport back to its original position. That's why I update the pos scores every tick.
execute store result score @s Physics.Object.BlockPos.x store result storage physics:zprivate temp.x int -1 run data get storage physics:zprivate pos[0]
execute store result score @s Physics.Object.BlockPos.y store result storage physics:zprivate temp.y int -1 run data get storage physics:zprivate pos[1]
execute store result score @s Physics.Object.BlockPos.z store result storage physics:zprivate temp.z int -1 run data get storage physics:zprivate pos[2]

# (Note): I need the PosWithinBlock at high precision. Number providers use floats, so it would be much less precise to use one at large coordinates.
execute as 575f7af5-d0dc-4c2c-9182-17931969f0ba run function physics:zprivate/macro/relative_tp with storage physics:zprivate temp
data modify storage physics:zprivate pos set from entity 575f7af5-d0dc-4c2c-9182-17931969f0ba Pos
execute store result score @s Physics.Object.PosWithinBlock.x run data get storage physics:zprivate pos[0] 16777216
execute store result score @s Physics.Object.PosWithinBlock.y run data get storage physics:zprivate pos[1] 16777216
execute store result score @s Physics.Object.PosWithinBlock.z run data get storage physics:zprivate pos[2] 16777216

# Update linear velocity
    # Gravity
    # (Note): I don't have an "AccumulatedForce" intermediate because it would easily overflow for large objects before any meaningful acceleration is achieved. So when applying force, it's directly converted to LinearVelocityFromAcceleration. So I don't need an additional step calculating that.
    # (Note): I use round() here for extra precision.
    execute unless score @s Physics.Object.InverseMass matches 0 run scoreboard players operation @s Physics.Object.LinearVelocityFromAcceleration.y += #Physics.Settings.Derived.ScaledGravityPerTick Physics

    # Apply damping, then gravity + acceleration
    # (Formula): LinearVelocity * LinearDampingPerTick + LinearVelocityFromAcceleration
    # (Note): The number provider clamps between -128 and +128 blocks per second to avoid overflows. It actually clamps it 1 unit lower than +128, so that the position update in phase_two doesn't overflow (would reach exactly 2^31, which overflows to -2^31).
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
# (TODO): In general, go over everything and check if I can turn it into an integer number provider or move things from storage to score without losing any visible precision.
