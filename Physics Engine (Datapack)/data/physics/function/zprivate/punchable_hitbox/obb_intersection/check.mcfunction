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

# Check if / where the ray hits the object
# (Note): The "exiting faces" checks explicitly allow for punching the object while facing the backside of a surface.
# (Note): If any "entering face" check succeeds, all remaining checks can be skipped. If any "exiting face" check succeeds, all remaining "exiting" checks can be skipped too.
# (TODO): Check if it would be worth it to utilize the fact that if it collides with one entering face and the distance checks fail, all other checks will automatically fail too. Right now, I exit early before I calculate the tangential intersection points, meaning I lose this information.

    # Entering faces
        # x axis
        execute if score #Physics.Ray.Local.RelativePos.x Physics matches ..-32768 if score #Physics.Ray.Local.Direction.x Physics matches 1.. if function physics:zprivate/punchable_hitbox/obb_intersection/neg_x run return 0
        execute if score #Physics.Ray.Local.RelativePos.x Physics matches 32768.. if score #Physics.Ray.Local.Direction.x Physics matches ..-1 if function physics:zprivate/punchable_hitbox/obb_intersection/pos_x run return 0

        # y axis
        execute if score #Physics.Ray.Local.RelativePos.y Physics matches ..-32768 if score #Physics.Ray.Local.Direction.y Physics matches 1.. if function physics:zprivate/punchable_hitbox/obb_intersection/neg_y run return 0
        execute if score #Physics.Ray.Local.RelativePos.y Physics matches 32768.. if score #Physics.Ray.Local.Direction.y Physics matches ..-1 if function physics:zprivate/punchable_hitbox/obb_intersection/pos_y run return 0

        # z axis
        execute if score #Physics.Ray.Local.RelativePos.z Physics matches ..-32768 if score #Physics.Ray.Local.Direction.z Physics matches 1.. if function physics:zprivate/punchable_hitbox/obb_intersection/neg_z run return 0
        execute if score #Physics.Ray.Local.RelativePos.z Physics matches 32768.. if score #Physics.Ray.Local.Direction.z Physics matches ..-1 if function physics:zprivate/punchable_hitbox/obb_intersection/pos_z run return 0

    # Exiting faces
    # (Note): I explicitly check "Only run exiting checks if the ray originates inside the AABB", because if the entering checks fail because it's out of range, it would still try the exiting faces. The checks would fail, but it would be overhead.
    execute if score #Physics.Ray.Local.RelativePos.x Physics matches ..-32768 run return 0
    execute if score #Physics.Ray.Local.RelativePos.x Physics matches 32768.. run return 0
    execute if score #Physics.Ray.Local.RelativePos.y Physics matches ..-32768 run return 0
    execute if score #Physics.Ray.Local.RelativePos.y Physics matches 32768.. run return 0
    execute if score #Physics.Ray.Local.RelativePos.z Physics matches ..-32768 run return 0
    execute if score #Physics.Ray.Local.RelativePos.z Physics matches 32768.. run return 0

        # x axis
        execute if score #Physics.Ray.Local.RelativePos.x Physics matches -32768.. if score #Physics.Ray.Local.Direction.x Physics matches ..-1 if function physics:zprivate/punchable_hitbox/obb_intersection/neg_x run return 0
        execute if score #Physics.Ray.Local.RelativePos.x Physics matches ..32768 if score #Physics.Ray.Local.Direction.x Physics matches 1.. if function physics:zprivate/punchable_hitbox/obb_intersection/pos_x run return 0

        # y axis
        execute if score #Physics.Ray.Local.RelativePos.y Physics matches -32768.. if score #Physics.Ray.Local.Direction.y Physics matches ..-1 if function physics:zprivate/punchable_hitbox/obb_intersection/neg_y run return 0
        execute if score #Physics.Ray.Local.RelativePos.y Physics matches ..32768 if score #Physics.Ray.Local.Direction.y Physics matches 1.. if function physics:zprivate/punchable_hitbox/obb_intersection/pos_y run return 0

        # z axis
        execute if score #Physics.Ray.Local.RelativePos.z Physics matches -32768.. if score #Physics.Ray.Local.Direction.z Physics matches ..-1 if function physics:zprivate/punchable_hitbox/obb_intersection/neg_z run return 0
        execute if score #Physics.Ray.Local.RelativePos.z Physics matches ..32768 if score #Physics.Ray.Local.Direction.z Physics matches 1.. run function physics:zprivate/punchable_hitbox/obb_intersection/pos_z
