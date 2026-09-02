# Reset scores
scoreboard players reset #Physics.Init

    # Datapack Settings
    scoreboard players reset #Physics.Settings.ShowReloadMessage
    scoreboard players reset #Physics.Settings.DeltaTimeDenominator

    # Simulation settings

    # Helper scores
    scoreboard players reset #Physics.IsTrue

# Remove scoreboard objectives
scoreboard objectives remove Physics

scoreboard objectives remove Physics.Object.BlockPos.x
scoreboard objectives remove Physics.Object.BlockPos.y
scoreboard objectives remove Physics.Object.BlockPos.z

scoreboard objectives remove Physics.Object.PosWithinBlock.x
scoreboard objectives remove Physics.Object.PosWithinBlock.y
scoreboard objectives remove Physics.Object.PosWithinBlock.z

scoreboard objectives remove Physics.Object.LinearVelocity.x
scoreboard objectives remove Physics.Object.LinearVelocity.y
scoreboard objectives remove Physics.Object.LinearVelocity.z

scoreboard objectives remove Physics.Object.AngularVelocity.x
scoreboard objectives remove Physics.Object.AngularVelocity.y
scoreboard objectives remove Physics.Object.AngularVelocity.z

scoreboard objectives remove Physics.Object.Scale.x
scoreboard objectives remove Physics.Object.Scale.y
scoreboard objectives remove Physics.Object.Scale.z

scoreboard objectives remove Physics.Object.InverseMass

scoreboard objectives remove Physics.Object.Orientation.x
scoreboard objectives remove Physics.Object.Orientation.y
scoreboard objectives remove Physics.Object.Orientation.z
scoreboard objectives remove Physics.Object.Orientation.a

scoreboard objectives remove Physics.Object.RotationMatrix.00
scoreboard objectives remove Physics.Object.RotationMatrix.01
scoreboard objectives remove Physics.Object.RotationMatrix.02
scoreboard objectives remove Physics.Object.RotationMatrix.10
scoreboard objectives remove Physics.Object.RotationMatrix.11
scoreboard objectives remove Physics.Object.RotationMatrix.12
scoreboard objectives remove Physics.Object.RotationMatrix.20
scoreboard objectives remove Physics.Object.RotationMatrix.21
scoreboard objectives remove Physics.Object.RotationMatrix.22

scoreboard objectives remove Physics.Object.SpecificInverseInertiaLocal.x
scoreboard objectives remove Physics.Object.SpecificInverseInertiaLocal.y
scoreboard objectives remove Physics.Object.SpecificInverseInertiaLocal.z

scoreboard objectives remove Physics.Object.SpecificInverseInertiaWorld.00
scoreboard objectives remove Physics.Object.SpecificInverseInertiaWorld.01
scoreboard objectives remove Physics.Object.SpecificInverseInertiaWorld.02
scoreboard objectives remove Physics.Object.SpecificInverseInertiaWorld.10
scoreboard objectives remove Physics.Object.SpecificInverseInertiaWorld.11
scoreboard objectives remove Physics.Object.SpecificInverseInertiaWorld.12
scoreboard objectives remove Physics.Object.SpecificInverseInertiaWorld.20
scoreboard objectives remove Physics.Object.SpecificInverseInertiaWorld.21
scoreboard objectives remove Physics.Object.SpecificInverseInertiaWorld.22

scoreboard objectives remove Physics.Object.AccumulatedForce.x
scoreboard objectives remove Physics.Object.AccumulatedForce.y
scoreboard objectives remove Physics.Object.AccumulatedForce.z

scoreboard objectives remove Physics.Object.AccumulatedTorque.x
scoreboard objectives remove Physics.Object.AccumulatedTorque.y
scoreboard objectives remove Physics.Object.AccumulatedTorque.z

scoreboard objectives remove Physics.Object.LinearVelocityFromAcceleration.x
scoreboard objectives remove Physics.Object.LinearVelocityFromAcceleration.y
scoreboard objectives remove Physics.Object.LinearVelocityFromAcceleration.z

scoreboard objectives remove Physics.Object.AngularVelocityFromTorque.x
scoreboard objectives remove Physics.Object.AngularVelocityFromTorque.y
scoreboard objectives remove Physics.Object.AngularVelocityFromTorque.z

# Delete data storages
data remove storage physics:zprivate settings
data remove storage physics:zprivate entity_data
data remove storage physics:zprivate temp
data remove storage physics:zprivate fallback_default
data remove storage physics:zprivate constants

data remove storage physics:object default
data remove storage physics:object set
