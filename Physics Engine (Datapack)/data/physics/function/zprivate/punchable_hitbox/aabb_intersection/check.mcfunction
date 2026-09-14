# Check if the player's line-of-sight intersects with the object's AABB

# Get the ray's origin relative to the object's origin & the ray's direction
# (Note): Because it's relative to the block's origin, the max value for RelativePos is EntityInteractionRange + sqrt(3*MaxSize^2)/2, so it can be scaled up a lot. I chose 2^16. It has to be the same as the relative AABB scaling factor, so I can do comparisons faster.
execute if score #Physics.GotRay Physics matches 0 run function physics:zprivate/punchable_hitbox/get_ray

execute store result score #Physics.Ray.RelativePos.x Physics run compute default float physics:punchable_hitbox/ray/relative_pos/x 65536
execute store result score #Physics.Ray.RelativePos.y Physics run compute default float physics:punchable_hitbox/ray/relative_pos/y 65536
execute store result score #Physics.Ray.RelativePos.z Physics run compute default float physics:punchable_hitbox/ray/relative_pos/z 65536

# AABB intersection checks
# (Note): If the ray hits the AABB (& the distance is less than the current MinDistance, in case another object is in front of that), it runs the detailed check.
# (TODO): Check if adding "if outside the AABB and looking away from it -> Don't check any entering face" would be worth it.
    # Entering faces
        # x axis
        execute if score #Physics.Ray.RelativePos.x Physics < @s Physics.Object.AabbRelative.Min.x if score #Physics.Ray.Direction.x Physics matches 1.. if function physics:zprivate/punchable_hitbox/aabb_intersection/neg_x run return run function physics:zprivate/punchable_hitbox/obb_intersection/check
        execute if score #Physics.Ray.RelativePos.x Physics > @s Physics.Object.AabbRelative.Max.x if score #Physics.Ray.Direction.x Physics matches ..-1 if function physics:zprivate/punchable_hitbox/aabb_intersection/pos_x run return run function physics:zprivate/punchable_hitbox/obb_intersection/check

        # y axis
        execute if score #Physics.Ray.RelativePos.y Physics < @s Physics.Object.AabbRelative.Min.y if score #Physics.Ray.Direction.y Physics matches 1.. if function physics:zprivate/punchable_hitbox/aabb_intersection/neg_y run return run function physics:zprivate/punchable_hitbox/obb_intersection/check
        execute if score #Physics.Ray.RelativePos.y Physics > @s Physics.Object.AabbRelative.Max.y if score #Physics.Ray.Direction.y Physics matches ..-1 if function physics:zprivate/punchable_hitbox/aabb_intersection/pos_y run return run function physics:zprivate/punchable_hitbox/obb_intersection/check

        # z axis
        execute if score #Physics.Ray.RelativePos.z Physics < @s Physics.Object.AabbRelative.Min.z if score #Physics.Ray.Direction.z Physics matches 1.. if function physics:zprivate/punchable_hitbox/aabb_intersection/neg_z run return run function physics:zprivate/punchable_hitbox/obb_intersection/check
        execute if score #Physics.Ray.RelativePos.z Physics > @s Physics.Object.AabbRelative.Max.z if score #Physics.Ray.Direction.z Physics matches ..-1 if function physics:zprivate/punchable_hitbox/aabb_intersection/pos_z run return run function physics:zprivate/punchable_hitbox/obb_intersection/check

    # Exiting faces
    # (Note): For AABB, when exiting faces, I ignore the EntityInteractionRange and MinDistance. That's because the distance to exit the AABB can be vastly greater than the distance to intersect with the model itself. Because of that, any ray that starts inside the AABB will trigger a "hit", meaning I can remove the checks for the AABB. I don't need to know the distance here.
    # (Note): Careful about overflows in the intersection point calculations here, as t can become very large on exiting faces.
    execute if score #Physics.Ray.RelativePos.x Physics >= @s Physics.Object.AabbRelative.Min.x if score #Physics.Ray.RelativePos.x Physics <= @s Physics.Object.AabbRelative.Max.x if score #Physics.Ray.RelativePos.y Physics >= @s Physics.Object.AabbRelative.Min.y if score #Physics.Ray.RelativePos.y Physics <= @s Physics.Object.AabbRelative.Max.y if score #Physics.Ray.RelativePos.z Physics >= @s Physics.Object.AabbRelative.Min.z if score #Physics.Ray.RelativePos.z Physics <= @s Physics.Object.AabbRelative.Max.z run function physics:zprivate/punchable_hitbox/obb_intersection/check
