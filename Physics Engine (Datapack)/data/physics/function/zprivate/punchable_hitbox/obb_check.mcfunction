# Transform the relative ray origin to the object's local coordinate system (and compress into a unit cube)
# (Formula): (RotationMatrixTranspose * Ray.RelativePos) / Scale
# (Note): The compression makes following calculations a bit simpler and faster, as I can work with hardcoded bounds.
# (Note): Scaled up by 2^16.
execute store result score #Physics.Ray.Local.RelativePos.x Physics run compute default float physics:punchable_hitbox/ray/local_scaled_relative_pos/x
execute store result score #Physics.Ray.Local.RelativePos.y Physics run compute default float physics:punchable_hitbox/ray/local_scaled_relative_pos/y
execute store result score #Physics.Ray.Local.RelativePos.z Physics run compute default float physics:punchable_hitbox/ray/local_scaled_relative_pos/z

# Transform the ray direction to the object's local coordinate system (and compress into a unit cube)
# (Formula): (RotationMatrixTranspose * Ray.Direction) / Scale
# (Note): Scaled up by 2^14.
execute store result score #Physics.Ray.Local.Direction.x Physics run compute default float physics:punchable_hitbox/ray/local_scaled_direction/x
execute store result score #Physics.Ray.Local.Direction.y Physics run compute default float physics:punchable_hitbox/ray/local_scaled_direction/y
execute store result score #Physics.Ray.Local.Direction.z Physics run compute default float physics:punchable_hitbox/ray/local_scaled_direction/z

# (Note): I add 1 to the ray direction because the "Slab method" for intersection checks breaks for a direction component of 0. I could also add guards in the slab method, but that would get executed for every single object, not just once total.
execute if score #Physics.Ray.Local.Direction.x Physics matches 0 run scoreboard players add #Physics.Ray.Local.Direction.x Physics 1
execute if score #Physics.Ray.Local.Direction.y Physics matches 0 run scoreboard players add #Physics.Ray.Local.Direction.y Physics 1
execute if score #Physics.Ray.Local.Direction.z Physics matches 0 run scoreboard players add #Physics.Ray.Local.Direction.z Physics 1

# Check if / where the ray hits the object
execute store result score #Physics.Math.t Physics run compute default integer physics:punchable_hitbox/obb_intersection/t_close
execute if score #Physics.Math.t Physics >= #Physics.MinDistance Physics run return 0
execute unless predicate physics:punchable_hitbox/obb_intersection/t_close_is_valid run return 0

# Valid intersection found
scoreboard players operation #Physics.MinDistance Physics = #Physics.Math.t Physics
scoreboard players operation #Physics Physics.Player.LookingAt.Id = @s Physics.Object.Id
scoreboard players operation #Physics.Ray.WinnerRelativePos.x Physics = #Physics.Ray.RelativePos.x Physics
scoreboard players operation #Physics.Ray.WinnerRelativePos.y Physics = #Physics.Ray.RelativePos.y Physics
scoreboard players operation #Physics.Ray.WinnerRelativePos.z Physics = #Physics.Ray.RelativePos.z Physics
