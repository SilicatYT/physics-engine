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

execute store result score @s Physics.Object.SpecificInverseInertiaLocal.x run compute default float physics:other/specific_inverse_inertia_local/x
execute store result score @s Physics.Object.SpecificInverseInertiaLocal.y run compute default float physics:other/specific_inverse_inertia_local/y
execute store result score @s Physics.Object.SpecificInverseInertiaLocal.z run compute default float physics:other/specific_inverse_inertia_local/z

# Perform pre-calculations for specific inverse inertia (world)
# (Note): If a scale is repeated twice, I can skip some calculations.
scoreboard players reset @s Physics.Object.SpecificInverseInertiaLocal.ReferenceAxis.Index
scoreboard players reset @s Physics.Object.SpecificInverseInertiaLocal.ReferenceAxis.Value
scoreboard players reset @s Physics.Object.SpecificInverseInertiaLocal.TangentDifference.x
scoreboard players reset @s Physics.Object.SpecificInverseInertiaLocal.TangentDifference.y
scoreboard players reset @s Physics.Object.SpecificInverseInertiaLocal.TangentDifference.z

tag @s remove Physics.IsIsotropic
execute if score @s Physics.Object.SpecificInverseInertiaLocal.x = @s Physics.Object.SpecificInverseInertiaLocal.y if score @s Physics.Object.SpecificInverseInertiaLocal.y = @s Physics.Object.SpecificInverseInertiaLocal.z run return run tag @s add Physics.IsIsotropic

execute if score @s Physics.Object.SpecificInverseInertiaLocal.x = @s Physics.Object.SpecificInverseInertiaLocal.y run scoreboard players set @s Physics.Object.SpecificInverseInertiaLocal.ReferenceAxis.Index 0
execute if score @s Physics.Object.SpecificInverseInertiaLocal.x = @s Physics.Object.SpecificInverseInertiaLocal.y run scoreboard players operation @s Physics.Object.SpecificInverseInertiaLocal.ReferenceAxis.Value = @s Physics.Object.SpecificInverseInertiaLocal.x
execute if score @s Physics.Object.SpecificInverseInertiaLocal.x = @s Physics.Object.SpecificInverseInertiaLocal.y store result score @s Physics.Object.SpecificInverseInertiaLocal.TangentDifference.z run return run compute default integer physics:other/specific_inverse_inertia_local/tangent_difference_z

execute if score @s Physics.Object.SpecificInverseInertiaLocal.x = @s Physics.Object.SpecificInverseInertiaLocal.z run scoreboard players set @s Physics.Object.SpecificInverseInertiaLocal.ReferenceAxis.Index 0
execute if score @s Physics.Object.SpecificInverseInertiaLocal.x = @s Physics.Object.SpecificInverseInertiaLocal.z run scoreboard players operation @s Physics.Object.SpecificInverseInertiaLocal.ReferenceAxis.Value = @s Physics.Object.SpecificInverseInertiaLocal.x
execute if score @s Physics.Object.SpecificInverseInertiaLocal.x = @s Physics.Object.SpecificInverseInertiaLocal.z store result score @s Physics.Object.SpecificInverseInertiaLocal.TangentDifference.y run return run compute default integer physics:other/specific_inverse_inertia_local/tangent_difference_y

execute if score @s Physics.Object.SpecificInverseInertiaLocal.y = @s Physics.Object.SpecificInverseInertiaLocal.z run scoreboard players set @s Physics.Object.SpecificInverseInertiaLocal.ReferenceAxis.Index 1
execute if score @s Physics.Object.SpecificInverseInertiaLocal.y = @s Physics.Object.SpecificInverseInertiaLocal.z run scoreboard players operation @s Physics.Object.SpecificInverseInertiaLocal.ReferenceAxis.Value = @s Physics.Object.SpecificInverseInertiaLocal.y
execute if score @s Physics.Object.SpecificInverseInertiaLocal.y = @s Physics.Object.SpecificInverseInertiaLocal.z store result score @s Physics.Object.SpecificInverseInertiaLocal.TangentDifference.x run return run compute default integer physics:other/specific_inverse_inertia_local/tangent_difference_x

# (Note): If all axes differ, choosing the middle axis as the reference produces the smallest value differences and therefore the smallest floating point imprecisions when calculating the inertia (world).
execute store success score #Physics.Check1 Physics if score @s Physics.Object.SpecificInverseInertiaLocal.x < @s Physics.Object.SpecificInverseInertiaLocal.y
execute store success score #Physics.Check2 Physics if score @s Physics.Object.SpecificInverseInertiaLocal.x < @s Physics.Object.SpecificInverseInertiaLocal.z
execute store success score #Physics.Check3 Physics if score @s Physics.Object.SpecificInverseInertiaLocal.y < @s Physics.Object.SpecificInverseInertiaLocal.z

    # x is the median
    execute unless score #Physics.Check1 Physics = #Physics.Check2 Physics run scoreboard players set @s Physics.Object.SpecificInverseInertiaLocal.ReferenceAxis.Index 0
    execute if score @s Physics.Object.SpecificInverseInertiaLocal.ReferenceAxis.Index matches 0 run scoreboard players operation @s Physics.Object.SpecificInverseInertiaLocal.ReferenceAxis.Value = @s Physics.Object.SpecificInverseInertiaLocal.x

    # y is the median
    execute if score #Physics.Check1 Physics = #Physics.Check2 Physics if score #Physics.Check1 Physics = #Physics.Check3 Physics run scoreboard players set @s Physics.Object.SpecificInverseInertiaLocal.ReferenceAxis.Index 1
    execute if score @s Physics.Object.SpecificInverseInertiaLocal.ReferenceAxis.Index matches 1 run scoreboard players operation @s Physics.Object.SpecificInverseInertiaLocal.ReferenceAxis.Value = @s Physics.Object.SpecificInverseInertiaLocal.y

    # z is the median
    execute if score #Physics.Check1 Physics = #Physics.Check2 Physics unless score #Physics.Check1 Physics = #Physics.Check3 Physics run scoreboard players set @s Physics.Object.SpecificInverseInertiaLocal.ReferenceAxis.Index 2
    execute if score @s Physics.Object.SpecificInverseInertiaLocal.ReferenceAxis.Index matches 2 run scoreboard players operation @s Physics.Object.SpecificInverseInertiaLocal.ReferenceAxis.Value = @s Physics.Object.SpecificInverseInertiaLocal.z

execute unless score @s Physics.Object.SpecificInverseInertiaLocal.ReferenceAxis.Index matches 0 store result score @s Physics.Object.SpecificInverseInertiaLocal.TangentDifference.x run compute default integer physics:other/specific_inverse_inertia_local/tangent_difference_x
execute unless score @s Physics.Object.SpecificInverseInertiaLocal.ReferenceAxis.Index matches 1 store result score @s Physics.Object.SpecificInverseInertiaLocal.TangentDifference.y run compute default integer physics:other/specific_inverse_inertia_local/tangent_difference_y
execute unless score @s Physics.Object.SpecificInverseInertiaLocal.ReferenceAxis.Index matches 2 store result score @s Physics.Object.SpecificInverseInertiaLocal.TangentDifference.z run compute default integer physics:other/specific_inverse_inertia_local/tangent_difference_z
