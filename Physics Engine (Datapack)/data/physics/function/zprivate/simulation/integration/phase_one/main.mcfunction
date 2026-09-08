# Get entity data
data modify storage physics:zprivate entity_data set from entity @s

# Set internal pos to entity pos
# (Explanation): If the entity gets teleported, it should automatically update the internal pos values rather than teleport back to its original position. That's why I update the pos scores every tick.
execute store result score @s Physics.Object.BlockPos.x store result storage physics:zprivate temp.x int -1 run data get storage physics:zprivate entity_data.Pos[0]
execute store result score @s Physics.Object.BlockPos.y store result storage physics:zprivate temp.y int -1 run data get storage physics:zprivate entity_data.Pos[1]
execute store result score @s Physics.Object.BlockPos.z store result storage physics:zprivate temp.z int -1 run data get storage physics:zprivate entity_data.Pos[2]

# (Note): I need the PosWithinBlock at high precision. Number providers use floats, so it would be much less precise to use one at large coordinates.
function physics:zprivate/simulation/integration/phase_one/get_pos_within_block with storage physics:zprivate temp
data modify storage physics:zprivate entity_data.Pos set from entity @s Pos
tp @s ~ ~ ~
execute store result score @s Physics.Object.PosWithinBlock.x run data get storage physics:zprivate entity_data.Pos[0] 16777216
execute store result score @s Physics.Object.PosWithinBlock.y run data get storage physics:zprivate entity_data.Pos[1] 16777216
execute store result score @s Physics.Object.PosWithinBlock.z run data get storage physics:zprivate entity_data.Pos[2] 16777216

# Update linear velocity
# (Note): I use round() in all these calculations because damping rounding keeps negative values at a minimum -390 without rounding, or -195 with rounding. And gravity causes linearVelocityFromAcceleration to be at least -1 unless gravity is exactly 0, increasing the minimum linear velocity without rounding to -780.
# (TODO): Check if this is acceptable, or if I should implement a real fix that gets to 0.

    # Velocity from acceleration (AccumulatedForce + gravity) (Constant, affected by deltatime)
    # (Formula): (AccumulatedForce * InverseMass + Gravity) * DeltaTime
    # (TODO): Check if a division by the DeltaTimeDenominator score is faster than a multiplication with the data storage.
    # (TODO): Check if I can reasonably store gravity as a score (for 1 less storage access).
    # (TODO): Check if I can pre-calculate inverseMass * deltaTime and store it as a score, to remove 1 multiplication from each component.
    execute unless score @s Physics.Object.InverseMass matches 0 store result score @s Physics.Object.LinearVelocityFromAcceleration.x run compute default float physics:integration/linear_velocity_from_acceleration/x
    execute unless score @s Physics.Object.InverseMass matches 0 store result score @s Physics.Object.LinearVelocityFromAcceleration.y run compute default float physics:integration/linear_velocity_from_acceleration/y
    execute unless score @s Physics.Object.InverseMass matches 0 store result score @s Physics.Object.LinearVelocityFromAcceleration.z run compute default float physics:integration/linear_velocity_from_acceleration/z

    # Apply damping, then acceleration
    # (Formula): LinearVelocity * LinearDampingPerTick + LinearVelocityFromAcceleration
    execute store result score @s Physics.Object.LinearVelocity.x run compute default float physics:integration/damped_linear_velocity_plus_acceleration/x
    execute store result score @s Physics.Object.LinearVelocity.y run compute default float physics:integration/damped_linear_velocity_plus_acceleration/y
    execute store result score @s Physics.Object.LinearVelocity.z run compute default float physics:integration/damped_linear_velocity_plus_acceleration/z

# Update angular velocity
# (Note): I use round() in all these calculations because damping rounding keeps negative values at a minimum -390 without rounding, or -195 with rounding.
# (TODO): Check if this is acceptable, or if I should implement a real fix that gets to 0.

    # Apply torque (Constant, affected by deltatime)
    # (Formula): InverseInertiaTensorWorld * AccumulatedTorque => Each entry is a dot product: angularVelocityFromTorque[0] = <first row of inertia tensor> * AccumulatedTorque[0]
    # (Note): Because inverseMass isn't included in the inertia I store (for scaling reasons: not enough bits), I additionally multiply each entry by inverseMass here.
    execute unless score @s Physics.Object.InverseMass matches 0 store result score @s Physics.Object.AngularVelocityFromTorque.x run compute default float physics:integration/angular_velocity_from_torque/x
    execute unless score @s Physics.Object.InverseMass matches 0 store result score @s Physics.Object.AngularVelocityFromTorque.y run compute default float physics:integration/angular_velocity_from_torque/y
    execute unless score @s Physics.Object.InverseMass matches 0 store result score @s Physics.Object.AngularVelocityFromTorque.z run compute default float physics:integration/angular_velocity_from_torque/z

    # Apply damping, then torque
    # (Formula): AngularVelocity * AngularDampingPerTick + AngularVelocityFromTorque
    execute store result score @s Physics.Object.AngularVelocity.x run compute default float physics:integration/damped_angular_velocity_plus_torque/x
    execute store result score @s Physics.Object.AngularVelocity.y run compute default float physics:integration/damped_angular_velocity_plus_torque/y
    execute store result score @s Physics.Object.AngularVelocity.z run compute default float physics:integration/damped_angular_velocity_plus_torque/z

# TODO: InverseInertiaTensorWorld is symmetrical, so I only need 6 components
# TODO: Check if performing the int addition of damped_linear_velocity_plus_acceleration in an integer number provider is faster or more precise
