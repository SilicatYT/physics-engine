# Update AABB
# (Note): In world coordinates.
# (Formula): AabbHalfSize.x = abs(HalfExtentAxisProjection.xx) + abs(HalfExtentAxisProjection.yx) + abs(HalfExtentAxisProjection.zx)
#            (Same for y, z)
#            Aabb.Min.x = Pos.x - AabbHalfSize.x
#            Aabb.Max.x = Pos.x + AabbHalfSize.x
#            (Same for y, z)
# (Note): There's a lot of ceil(), floor() and scaling shenanigans to make sure the whole object is covered by the AABB.
# (TODO): Check if inlining the AabbHalfSize instead of pre-calculating it is faster.
# (TODO): Check if pre-calculating the merged pos (BlockPos & PosWithinBlock) instead of inlining it is faster.
# (TODO): Maybe store it in @s Aabb.Min instead of Physics.Math.?, so I can calculate it in-place to save some calculations.
execute store result score #Physics.Math.0 Physics run compute default integer physics:other/aabb/half_size/x
execute store result score #Physics.Math.1 Physics run compute default integer physics:other/aabb/half_size/y
execute store result score #Physics.Math.2 Physics run compute default integer physics:other/aabb/half_size/z

execute store result score @s Physics.Object.Aabb.Min.x run compute default integer physics:other/aabb/min/x
execute store result score @s Physics.Object.Aabb.Min.y run compute default integer physics:other/aabb/min/y
execute store result score @s Physics.Object.Aabb.Min.z run compute default integer physics:other/aabb/min/z

execute store result score @s Physics.Object.Aabb.Max.x run compute default integer physics:other/aabb/max/x
execute store result score @s Physics.Object.Aabb.Max.y run compute default integer physics:other/aabb/max/y
execute store result score @s Physics.Object.Aabb.Max.z run compute default integer physics:other/aabb/max/z
