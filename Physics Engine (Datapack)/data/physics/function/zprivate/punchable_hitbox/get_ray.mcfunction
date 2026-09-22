scoreboard players set #Physics.GotRay Physics 1

# Setup for later
scoreboard players operation #Physics.MinDistance Physics = #Physics.EntityInteractionRange Physics
scoreboard players add #Physics.MinDistance Physics 1

# Get ray origin
# (Note): In world coordinates.
# (Note): I use the same method for PosWithinBlock as in integration phase one, for the same reason.
tp @s ~ ~ ~ ~ ~
data modify storage physics:zprivate entity_data set from entity @s

execute store result score #Physics.Ray.BlockPos.x Physics store result storage physics:zprivate temp.x int -1 run data get storage physics:zprivate entity_data.Pos[0]
execute store result score #Physics.Ray.BlockPos.y Physics store result storage physics:zprivate temp.y int -1 run data get storage physics:zprivate entity_data.Pos[1]
execute store result score #Physics.Ray.BlockPos.z Physics store result storage physics:zprivate temp.z int -1 run data get storage physics:zprivate entity_data.Pos[2]

function physics:zprivate/macro/relative_tp with storage physics:zprivate temp
data modify storage physics:zprivate entity_data.Pos set from entity @s Pos

execute store result score #Physics.Ray.PosWithinBlock.x Physics run data get storage physics:zprivate entity_data.Pos[0] 16777216
execute store result score #Physics.Ray.PosWithinBlock.y Physics run data get storage physics:zprivate entity_data.Pos[1] 16777216
execute store result score #Physics.Ray.PosWithinBlock.z Physics run data get storage physics:zprivate entity_data.Pos[2] 16777216

# Reset marker
# (Note): The teleport back is necessary to avoid the entity from unloading, as it's a separate dimension with only a single loaded chunk.
# (Note): Resetting the rotation is necessary because relative rotation changes (like the one in "get_ray") can cause unclamped overflow that messes with calculations.
tp @s 8.0 8.0 8.0 0.0 0.0

# Get ray direction
# (TODO): Check whether "data get" and an additional teleport is faster for getting the normalized direction vector.
# (TODO): Assuming I calculate the direction from pitch & yaw: Reconsider what intermediates I store and which ones I re-calculate inline (I currently don't store cos(pitchRad) explicitly).
# (TODO): Benchmark whether doing the final multiplication in the number provider or in the compute command is faster.
# (TODO): Check if I can re-use the BlockPos data call from above by using "align xyz positioned ~0.5 ~0.5 ~0.5 positioned ^ ^ ^0.499", then subtracting BlockPos+0.5 from it. But it seems to require doubles, so it's not feasible currently.

   # (Formula): pitchRad = pitch * pi / 180
   # (Note): Scaled up by 2^28.
   execute store result score #Physics.Math.PitchRad Physics run data get storage physics:zprivate entity_data.Rotation[1] 4685082.53629

   # (Formula): yawRad = yaw * pi / 180
   # (Note): Scaled up by 2^28.
   execute store result score #Physics.Math.YawRad Physics run data get storage physics:zprivate entity_data.Rotation[0] 4685082.53629

   # (Formula): x = -sin(yawRad) * cos(pitchRad)
   #            y = -sin(pitchRad)
   #            z = cos(yawRad) * cos(pitchRad)
   # (Note): Scaled up by 2^14.
   execute store result score #Physics.Ray.Direction.x Physics run compute default float physics:punchable_hitbox/ray/direction/x 16384
   execute store result score #Physics.Ray.Direction.y Physics run compute default float physics:punchable_hitbox/ray/direction/y 16384
   execute store result score #Physics.Ray.Direction.z Physics run compute default float physics:punchable_hitbox/ray/direction/z 16384

   # (Note): I add 1 to the ray direction because the "Slab method" for intersection checks breaks for a direction component of 0. I could also add guards in the slab method, but that would get executed for every single object, not just once total.
   execute if score #Physics.Ray.Direction.x Physics matches 0 run scoreboard players add #Physics.Ray.Direction.x Physics 1
   execute if score #Physics.Ray.Direction.y Physics matches 0 run scoreboard players add #Physics.Ray.Direction.y Physics 1
   execute if score #Physics.Ray.Direction.z Physics matches 0 run scoreboard players add #Physics.Ray.Direction.z Physics 1
