# Set orientation
$data modify storage physics:zprivate temp.orientation set value $(orientation)
execute store result score #Physics.IsTrue Physics run compute default integer physics:other/set_orientation/is_valid
execute if score #Physics.IsTrue Physics matches 0 run data modify storage physics:zprivate temp.orientation set from storage physics:zprivate fallback_default.orientation

data modify storage physics:zprivate temp.inverse_length set compute default float physics:other/set_orientation/inverse_length
data modify storage physics:zprivate temp.orientation[0] set compute default float physics:other/set_orientation/x
data modify storage physics:zprivate temp.orientation[1] set compute default float physics:other/set_orientation/y
data modify storage physics:zprivate temp.orientation[2] set compute default float physics:other/set_orientation/z
data modify storage physics:zprivate temp.orientation[3] set compute default float physics:other/set_orientation/a
data modify entity @s transformation.left_rotation set from storage physics:zprivate temp.orientation

execute store result score @s Physics.Object.Orientation.x run data get storage physics:zprivate temp.orientation[0] 16777216
execute store result score @s Physics.Object.Orientation.y run data get storage physics:zprivate temp.orientation[1] 16777216
execute store result score @s Physics.Object.Orientation.z run data get storage physics:zprivate temp.orientation[2] 16777216
execute store result score @s Physics.Object.Orientation.a run data get storage physics:zprivate temp.orientation[3] 16777216

# Update derived data
function physics:zprivate/update_derived_data/rotation_matrix
function physics:zprivate/update_derived_data/specific_inverse_world_inertia
function physics:zprivate/update_derived_data/half_extent_axis_projections
function physics:zprivate/update_derived_data/aabb
