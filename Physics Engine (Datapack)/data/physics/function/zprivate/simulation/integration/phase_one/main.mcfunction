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
execute store result score @s Physics.Object.PosWithinBlock.x run data get storage physics:zprivate entity_data.Pos[0] 131072
execute store result score @s Physics.Object.PosWithinBlock.y run data get storage physics:zprivate entity_data.Pos[1] 131072
execute store result score @s Physics.Object.PosWithinBlock.z run data get storage physics:zprivate entity_data.Pos[2] 131072

# Update linear velocity
    # Velocity from acceleration (AccumulatedForce + gravity) (Constant, affected by deltatime)
    # (Formula): (AccumulatedForce * InverseMass + Gravity) * DeltaTime
    # (TODO): Check if a division by the DeltaTimeDenominator score is faster than a multiplication with the data storage
    # (TODO): Right now it re-calculates the scaled down inverseMass 3x. Check if calculating it once and storing in in a data storage is faster.
    execute unless score @s Physics.Object.InverseMass matches 0 store result score @s Physics.Object.LinearVelocityFromAcceleration.x Physics run compute default float physics:integration/linear_velocity_from_acceleration_x 16384
    execute unless score @s Physics.Object.InverseMass matches 0 store result score @s Physics.Object.LinearVelocityFromAcceleration.y Physics run compute default float physics:integration/linear_velocity_from_acceleration_y 16384
    execute unless score @s Physics.Object.InverseMass matches 0 store result score @s Physics.Object.LinearVelocityFromAcceleration.z Physics run compute default float physics:integration/linear_velocity_from_acceleration_z 16384

    # Apply damping, then acceleration
    # (Formula): LinearVelocity * LinearDampingPerTick + LinearVelocityFromAcceleration
    # (Note): The values are not scaled down during the calculation, so no compute scaling factor necessary here.
    execute store result score @s Physics.Object.LinearVelocity.x run compute default float physics:integration/damped_linear_velocity_plus_acceleration_x
    execute store result score @s Physics.Object.LinearVelocity.y run compute default float physics:integration/damped_linear_velocity_plus_acceleration_y
    execute store result score @s Physics.Object.LinearVelocity.z run compute default float physics:integration/damped_linear_velocity_plus_acceleration_z

# Update angular velocity
    # Apply torque (Constant, affected by deltatime)
    # (Formula): InverseInertiaTensorWorld * AccumulatedTorque
    # TODO: FIGURE OUT SCALING OF TENSOR FIRST


# TODO: Check if I need to add guards to make sure velocity doesn't get stuck at 1 or -1 forever
# TODO: InverseInertiaTensorWorld is symmetric, so I only need 6 components
# TODO: Check if I need to scale up torque or angularVelocity in case large objects won't move
