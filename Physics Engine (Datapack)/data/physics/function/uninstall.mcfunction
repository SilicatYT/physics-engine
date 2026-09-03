# Reset scores
scoreboard players reset #Physics.Init

    # Datapack Settings
    scoreboard players reset #Physics.Settings.ShowReloadMessage
    scoreboard players reset #Physics.Settings.DeltaTimeDenominator

    # Simulation settings

    # Helper scores
    scoreboard players reset #Physics.IsTrue
    scoreboard players reset #Physics.Check1
    scoreboard players reset #Physics.Check2
    scoreboard players reset #Physics.Check3

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

scoreboard objectives remove Physics.Object.RotationMatrix.xx
scoreboard objectives remove Physics.Object.RotationMatrix.xy
scoreboard objectives remove Physics.Object.RotationMatrix.xz
scoreboard objectives remove Physics.Object.RotationMatrix.yx
scoreboard objectives remove Physics.Object.RotationMatrix.yy
scoreboard objectives remove Physics.Object.RotationMatrix.yz
scoreboard objectives remove Physics.Object.RotationMatrix.zx
scoreboard objectives remove Physics.Object.RotationMatrix.zy
scoreboard objectives remove Physics.Object.RotationMatrix.zz

scoreboard objectives remove Physics.Object.SpecificInverseInertiaLocal.x
scoreboard objectives remove Physics.Object.SpecificInverseInertiaLocal.y
scoreboard objectives remove Physics.Object.SpecificInverseInertiaLocal.z

scoreboard objectives remove Physics.Object.SpecificInverseInertiaLocal.ReferenceAxis.Index
scoreboard objectives remove Physics.Object.SpecificInverseInertiaLocal.ReferenceAxis.Value
scoreboard objectives remove Physics.Object.SpecificInverseInertiaLocal.TangentDifference.x
scoreboard objectives remove Physics.Object.SpecificInverseInertiaLocal.TangentDifference.y
scoreboard objectives remove Physics.Object.SpecificInverseInertiaLocal.TangentDifference.z

scoreboard objectives remove Physics.Object.SpecificInverseInertiaWorld.xx
scoreboard objectives remove Physics.Object.SpecificInverseInertiaWorld.xy
scoreboard objectives remove Physics.Object.SpecificInverseInertiaWorld.xz
scoreboard objectives remove Physics.Object.SpecificInverseInertiaWorld.yx
scoreboard objectives remove Physics.Object.SpecificInverseInertiaWorld.yy
scoreboard objectives remove Physics.Object.SpecificInverseInertiaWorld.yz
scoreboard objectives remove Physics.Object.SpecificInverseInertiaWorld.zx
scoreboard objectives remove Physics.Object.SpecificInverseInertiaWorld.zy
scoreboard objectives remove Physics.Object.SpecificInverseInertiaWorld.zz

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
