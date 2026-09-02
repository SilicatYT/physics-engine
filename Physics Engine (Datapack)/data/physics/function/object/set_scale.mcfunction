# Set scale
$data modify storage physics:zprivate temp.scale set value $(scale)
execute store result score #Physics.IsTrue Physics run compute default integer physics:other/set_scale/is_valid
execute if score #Physics.IsTrue Physics matches 0 run data modify storage physics:zprivate temp.scale set from storage physics:zprivate fallback_default.scale

execute store result score @s Physics.Object.Scale.x run data get storage physics:zprivate temp.scale[0] 1024
execute store result score @s Physics.Object.Scale.y run data get storage physics:zprivate temp.scale[1] 1024
execute store result score @s Physics.Object.Scale.z run data get storage physics:zprivate temp.scale[2] 1024
data modify entity @s transformation.scale set from storage physics:zprivate temp.scale

# Update specific inverse inertia (local)
# (Note): Due to the strong scaling necessary for inverse inertia & inverse mass and only being able to store them as integers, I decided to store the inertia without the mass component. I still have to be careful about overflows and precision loss.
# (Formula): X: 12 / (scaleY^2 + scaleZ^2), Y: 12 / (scaleX^2 + scaleZ^2), Z: 12 / (scaleX^2 + scaleY^2)
data modify storage physics:zprivate temp.squared_scale_x set compute default float physics:other/specific_inverse_inertia_local/squared_scale_x
data modify storage physics:zprivate temp.squared_scale_y set compute default float physics:other/specific_inverse_inertia_local/squared_scale_y
data modify storage physics:zprivate temp.squared_scale_z set compute default float physics:other/specific_inverse_inertia_local/squared_scale_z

execute store result score @s Physics.Object.SpecificInverseInertiaLocal.x run compute default float physics:other/specific_inverse_inertia_local/x 16384
execute store result score @s Physics.Object.SpecificInverseInertiaLocal.y run compute default float physics:other/specific_inverse_inertia_local/y 16384
execute store result score @s Physics.Object.SpecificInverseInertiaLocal.z run compute default float physics:other/specific_inverse_inertia_local/z 16384
