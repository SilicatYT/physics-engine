# Check if the player's line-of-sight intersects with the object's AABB

# Get the ray's origin relative to the object's origin & the ray's direction
# (Note): Because it's relative to the block's origin, the max value for RelativePos is EntityInteractionRange + sqrt(3*MaxSize^2)/2, so it can be scaled up a lot. I chose 2^16. It has to be the same as the relative AABB scaling factor, so I can do comparisons faster.
execute if score #Physics.GotRay Physics matches 0 run function physics:zprivate/punchable_hitbox/get_ray

execute store result score #Physics.Ray.RelativePos.x Physics run compute default float physics:punchable_hitbox/ray/relative_pos/x 65536
execute store result score #Physics.Ray.RelativePos.y Physics run compute default float physics:punchable_hitbox/ray/relative_pos/y 65536
execute store result score #Physics.Ray.RelativePos.z Physics run compute default float physics:punchable_hitbox/ray/relative_pos/z 65536

# AABB intersection checks
# (Note): If the ray hits the AABB (& the distance is less than the current MinDistance, in case another object is in front of that), it runs the OBB check.
# (Note): Intersection is checked via the "Slab method" because it can be implemented without function calls and with minimal command count, making it very fast for failing checks.
# (Note): I benchmarked it, and re-calculating the t values in the predicate check is much faster than scoring them individually first and re-using them in the predicate and t_close.
# (TODO): Check if adding an early exit for "if RelativePos.lengthSquared > MinDistance + OBBRadius" would be worth it.
execute store result score #Physics.Math.t Physics run compute default integer physics:punchable_hitbox/aabb_intersection/t_close
execute if score #Physics.Math.t Physics >= #Physics.MinDistance Physics run return 0
execute if score #Physics.Math.t Physics > #Physics.EntityInteractionRange Physics run return 0
execute if predicate physics:punchable_hitbox/aabb_intersection/t_close_is_valid run function physics:zprivate/punchable_hitbox/obb_check
