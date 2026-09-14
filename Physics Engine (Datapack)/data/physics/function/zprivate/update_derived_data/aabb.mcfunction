# Update AABB
# (Note): In world coordinates.
# (Formula): AabbRelative.Max.x = abs(HalfExtentAxisProjection.xx) + abs(HalfExtentAxisProjection.yx) + abs(HalfExtentAxisProjection.zx)
#            (Same for y, z)
#            Aabb.Min.x = Pos.x + AabbRelative.Min.x
#            Aabb.Max.x = Pos.x + AabbRelative.Max.x
#            (Same for y, z)
# (Note): There's a lot of ceil(), floor() and scaling shenanigans to make sure the whole object is covered by the AABB.
# (Note): I store my relative AABB at a different scale from the world AABB.
# (TODO): Check if pre-calculating the merged pos (BlockPos & PosWithinBlock) instead of inlining it is faster.
# (TODO): Check if using float mode to multiply by 1/scaling is faster than using int mode and dividing.
execute store result score @s Physics.Object.AabbRelative.Min.x store result score @s Physics.Object.AabbRelative.Max.x run compute default integer physics:other/aabb/half_size/x
execute store result score @s Physics.Object.AabbRelative.Min.y store result score @s Physics.Object.AabbRelative.Max.y run compute default integer physics:other/aabb/half_size/y
execute store result score @s Physics.Object.AabbRelative.Min.z store result score @s Physics.Object.AabbRelative.Max.z run compute default integer physics:other/aabb/half_size/z

scoreboard players operation @s Physics.Object.AabbRelative.Min.x *= #Physics.Constant.-1 Physics
scoreboard players operation @s Physics.Object.AabbRelative.Min.y *= #Physics.Constant.-1 Physics
scoreboard players operation @s Physics.Object.AabbRelative.Min.z *= #Physics.Constant.-1 Physics

execute store result score @s Physics.Object.Aabb.Min.x run compute default integer physics:other/aabb/min/x
execute store result score @s Physics.Object.Aabb.Min.y run compute default integer physics:other/aabb/min/y
execute store result score @s Physics.Object.Aabb.Min.z run compute default integer physics:other/aabb/min/z

execute store result score @s Physics.Object.Aabb.Max.x run compute default integer physics:other/aabb/max/x
execute store result score @s Physics.Object.Aabb.Max.y run compute default integer physics:other/aabb/max/y
execute store result score @s Physics.Object.Aabb.Max.z run compute default integer physics:other/aabb/max/z
