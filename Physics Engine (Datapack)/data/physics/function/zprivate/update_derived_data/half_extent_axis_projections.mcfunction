# Projection of the three half extents onto the three axes
# (Note): Each column of the rotation matrix is an axis.
# (Note): I don't store half extents explicitly. Instead, I just divide by an additional factor of 2.
    # Object Axis X
    execute store result score @s Physics.Object.HalfExtentAxisProjection.xx run compute default float physics:other/half_extent_axis_projection/xx
    execute store result score @s Physics.Object.HalfExtentAxisProjection.xy run compute default float physics:other/half_extent_axis_projection/xy
    execute store result score @s Physics.Object.HalfExtentAxisProjection.xz run compute default float physics:other/half_extent_axis_projection/xz

    # Object Axis Y
    execute store result score @s Physics.Object.HalfExtentAxisProjection.yx run compute default float physics:other/half_extent_axis_projection/yx
    execute store result score @s Physics.Object.HalfExtentAxisProjection.yy run compute default float physics:other/half_extent_axis_projection/yy
    execute store result score @s Physics.Object.HalfExtentAxisProjection.yz run compute default float physics:other/half_extent_axis_projection/yz

    # Object Axis Z
    execute store result score @s Physics.Object.HalfExtentAxisProjection.zx run compute default float physics:other/half_extent_axis_projection/zx
    execute store result score @s Physics.Object.HalfExtentAxisProjection.zy run compute default float physics:other/half_extent_axis_projection/zy
    execute store result score @s Physics.Object.HalfExtentAxisProjection.zz run compute default float physics:other/half_extent_axis_projection/zz
