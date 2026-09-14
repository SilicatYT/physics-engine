# Calculate t
# (Formula): (AabbRelative.<Min/Max>.y - Ray.RelativePos.y) / Ray.Direction.y
# (Note): 'Min' Aabb for the face that points toward negative y, 'Max' for the other.
# (Note): Scaled up by 2^10, same as EntityInteractionRange and MinDistance for easy comparison.
execute store result score #Physics.Math.t Physics run compute default integer physics:punchable_hitbox/aabb_intersection/y/neg_t

# If t is too large, stop. This means no collision with this face is happening (or too late).
# (Note): If "IsExitingFace", I reject a t that's larger than the longest possible diagonal (sqrt(3) * 2 * Max(AABB.x, AABB.y, AABB.z)) because the ray has to originate inside the AABB. So it's an early out and a protection against too large t's that would cause overflows in the intersection calculation later.
execute if score #Physics.IsExitingFace Physics matches 0 if score #Physics.Math.t Physics > #Physics.EntityInteractionRange Physics run return 0
execute if score #Physics.IsExitingFace Physics matches 0 if score #Physics.Math.t Physics >= #Physics.MinDistance Physics run return 0
execute if score #Physics.IsExitingFace Physics matches 1 if predicate physics:punchable_hitbox/exiting_t_too_large run return 0

# Calculate the intersection point and check if it's in-bounds for the other two axes
# (Formula): Ray.RelativePos + t * Ray.Direction
# (Note): Scaled up by 2^16, same as the relative AABB for easy comparison.
# (TODO): Benchmark whether multiplying by the scaling factor in a float provider is faster than dividing in an integer provider.
# (TODO): Benchmark whether merging the 3 lines per axis into a single predicate would be faster.
execute store result score #Physics.Ray.IntersectionPoint.x Physics run compute default integer physics:punchable_hitbox/aabb_intersection/intersection_x
execute if score #Physics.Ray.IntersectionPoint.x Physics > @s Physics.Object.AabbRelative.Max.x run return 0
execute if score #Physics.Ray.IntersectionPoint.x Physics < @s Physics.Object.AabbRelative.Min.x run return 0

execute store result score #Physics.Ray.IntersectionPoint.z Physics run compute default integer physics:punchable_hitbox/aabb_intersection/intersection_z
execute if score #Physics.Ray.IntersectionPoint.z Physics > @s Physics.Object.AabbRelative.Max.z run return 0
execute if score #Physics.Ray.IntersectionPoint.z Physics < @s Physics.Object.AabbRelative.Min.z run return 0

return 1
