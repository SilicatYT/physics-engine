# (Note): The forceload at world spawn is not automatically removed for compatibility reasons.

# Reset scores
scoreboard players reset #Physics.Init
scoreboard players reset #Physics

    # Datapack Settings
    scoreboard players reset #Physics.Settings.ShowReloadMessage
    scoreboard players reset #Physics.Settings.DefaultPunchStrength

    # Simulation settings
    scoreboard players reset #Physics.Settings.DeltaTimeDenominator
    scoreboard players reset #Physics.Settings.Derived.ScaledGravityPerTick

    # Helper scores
    scoreboard players reset #Physics.IsTrue
    scoreboard players reset #Physics.Check1
    scoreboard players reset #Physics.Check2
    scoreboard players reset #Physics.Check3

    scoreboard players reset #Physics.PosChange.x
    scoreboard players reset #Physics.PosChange.y
    scoreboard players reset #Physics.PosChange.z

    scoreboard players reset #Physics.Gametime
    scoreboard players reset #Physics.Gametime.Mod200

    scoreboard players reset #Physics.InteractionCount
    scoreboard players reset #Physics.KillCount
    scoreboard players reset #Physics.GotRay
    scoreboard players reset #Physics.EntityInteractionRange
    scoreboard players reset #Physics.MinDistance
    scoreboard players reset #Physics.Ray.BlockPos.x
    scoreboard players reset #Physics.Ray.BlockPos.y
    scoreboard players reset #Physics.Ray.BlockPos.z
    scoreboard players reset #Physics.Ray.PosWithinBlock.x
    scoreboard players reset #Physics.Ray.PosWithinBlock.y
    scoreboard players reset #Physics.Ray.PosWithinBlock.z
    scoreboard players reset #Physics.Ray.Direction.x
    scoreboard players reset #Physics.Ray.Direction.y
    scoreboard players reset #Physics.Ray.Direction.z
    scoreboard players reset #Physics.Ray.RelativePos.x
    scoreboard players reset #Physics.Ray.RelativePos.y
    scoreboard players reset #Physics.Ray.RelativePos.z
    scoreboard players reset #Physics.Ray.WinnerRelativePos.x
    scoreboard players reset #Physics.Ray.WinnerRelativePos.y
    scoreboard players reset #Physics.Ray.WinnerRelativePos.z
    scoreboard players reset #Physics.Ray.Local.RelativePos.x
    scoreboard players reset #Physics.Ray.Local.RelativePos.y
    scoreboard players reset #Physics.Ray.Local.RelativePos.z
    scoreboard players reset #Physics.Ray.Local.Direction.x
    scoreboard players reset #Physics.Ray.Local.Direction.y
    scoreboard players reset #Physics.Ray.Local.Direction.z

    scoreboard players reset #Physics.Math.PitchRad
    scoreboard players reset #Physics.Math.YawRad
    scoreboard players reset #Physics.Math.t

    scoreboard players reset #Physics.Math.0
    scoreboard players reset #Physics.Math.1
    scoreboard players reset #Physics.Math.2
    scoreboard players reset #Physics.Math.3

    # Constants
    scoreboard players reset #Physics.Constant.-1

# Kill entities
kill 575f7af5-d0dc-4c2c-9182-17931969f0ba

# Remove scoreboard objectives
scoreboard objectives remove Physics

scoreboard objectives remove Physics.Object.Id
scoreboard objectives remove Physics.Object.Id.Mod200

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

scoreboard objectives remove Physics.Object.HalfExtentAxisProjection.xx
scoreboard objectives remove Physics.Object.HalfExtentAxisProjection.xy
scoreboard objectives remove Physics.Object.HalfExtentAxisProjection.xz
scoreboard objectives remove Physics.Object.HalfExtentAxisProjection.yx
scoreboard objectives remove Physics.Object.HalfExtentAxisProjection.yy
scoreboard objectives remove Physics.Object.HalfExtentAxisProjection.yz
scoreboard objectives remove Physics.Object.HalfExtentAxisProjection.zx
scoreboard objectives remove Physics.Object.HalfExtentAxisProjection.zy
scoreboard objectives remove Physics.Object.HalfExtentAxisProjection.zz

scoreboard objectives remove Physics.Object.Aabb.Max.x
scoreboard objectives remove Physics.Object.Aabb.Max.y
scoreboard objectives remove Physics.Object.Aabb.Max.z
scoreboard objectives remove Physics.Object.Aabb.Min.x
scoreboard objectives remove Physics.Object.Aabb.Min.y
scoreboard objectives remove Physics.Object.Aabb.Min.z

scoreboard objectives remove Physics.Object.AabbRelative.Max.x
scoreboard objectives remove Physics.Object.AabbRelative.Max.y
scoreboard objectives remove Physics.Object.AabbRelative.Max.z
scoreboard objectives remove Physics.Object.AabbRelative.Min.x
scoreboard objectives remove Physics.Object.AabbRelative.Min.y
scoreboard objectives remove Physics.Object.AabbRelative.Min.z

scoreboard objectives remove Physics.Object.LinearVelocityFromAcceleration.x
scoreboard objectives remove Physics.Object.LinearVelocityFromAcceleration.y
scoreboard objectives remove Physics.Object.LinearVelocityFromAcceleration.z

scoreboard objectives remove Physics.Object.AngularVelocityFromTorque.x
scoreboard objectives remove Physics.Object.AngularVelocityFromTorque.y
scoreboard objectives remove Physics.Object.AngularVelocityFromTorque.z

scoreboard objectives remove Physics.Player.Id
scoreboard objectives remove Physics.Player.LookingAt.Id
scoreboard objectives remove Physics.Player.LookingAt.Direction.x
scoreboard objectives remove Physics.Player.LookingAt.Direction.y
scoreboard objectives remove Physics.Player.LookingAt.Direction.z
scoreboard objectives remove Physics.Player.LookingAt.RelativePos.x
scoreboard objectives remove Physics.Player.LookingAt.RelativePos.y
scoreboard objectives remove Physics.Player.LookingAt.RelativePos.z
scoreboard objectives remove Physics.Player.PunchStrength

scoreboard objectives remove Physics.Hitbox.Gametime

# Delete data storages
data remove storage physics:zprivate settings
data remove storage physics:zprivate entity_data
data remove storage physics:zprivate pos
data remove storage physics:zprivate temp
data remove storage physics:zprivate fallback_default
data remove storage physics:zprivate constants

data remove storage physics:object default
data remove storage physics:object set
data remove storage physics:object apply_force
data remove storage physics:object apply_torque
