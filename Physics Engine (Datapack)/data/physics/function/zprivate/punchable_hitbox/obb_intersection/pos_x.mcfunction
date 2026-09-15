# Calculate t
# (Formula): (Boundary.<Min/Max>.x - Ray.Local.RelativePos.x) / Ray.Local.Direction.x
# (Note): 'Min' boundary for the face that points toward negative x, 'Max' for the other.
# (Note): Scaled up by 2^10, same as EntityInteractionRange and MinDistance for easy comparison.
execute store result score #Physics.Math.t Physics run compute default integer physics:punchable_hitbox/obb_intersection/x/pos_t

# If t is too large, stop. This means no collision with this face is happening (or too late).
execute if score #Physics.Math.t Physics > #Physics.EntityInteractionRange Physics run return 0
execute if score #Physics.Math.t Physics >= #Physics.MinDistance Physics run return 0

# Calculate the intersection point and check if it's in-bounds for the other two axes
# (Formula): Ray.Local.RelativePos + t * Ray.Local.Direction
# (Note): Scaled up by 2^16.
execute store result score #Physics.Ray.IntersectionPoint.y Physics run compute default integer physics:punchable_hitbox/obb_intersection/intersection_y
execute unless score #Physics.Ray.IntersectionPoint.y Physics matches -32768..32768 run return 0

execute store result score #Physics.Ray.IntersectionPoint.z Physics run compute default integer physics:punchable_hitbox/obb_intersection/intersection_z
execute unless score #Physics.Ray.IntersectionPoint.z Physics matches -32768..32768 run return 0

# Valid intersection found
scoreboard players operation #Physics.MinDistance Physics = #Physics.Math.t Physics
scoreboard players operation #Physics Physics.Player.LookingAt.Id = @s Physics.Object.Id
scoreboard players operation #Physics.Ray.WinnerRelativePos.x Physics = #Physics.Ray.RelativePos.x Physics
scoreboard players operation #Physics.Ray.WinnerRelativePos.y Physics = #Physics.Ray.RelativePos.y Physics
scoreboard players operation #Physics.Ray.WinnerRelativePos.z Physics = #Physics.Ray.RelativePos.z Physics
return 1
