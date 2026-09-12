# Update specific inverse inertia (world)
# (Note): If the entity is isotropic (all scales are the same, its world inertia tensor is already fully pre-calculated when setting the scale.
# (Formula): W_ij = SUM<k 1 to 3>(R_ik * R_jk * L_k) with W being the global tensor, R the rotation matrix, and L the local tensor
# (Note): I optimized the formula by taking advantage of the reference axis: Use the TangentDifference instead of L, leave out the reference axis from the sum, and add the reference axis tensor if i == j. And I divide the resulting number so it has the correct scaling factor again.
# (Note): I round the sum before adding the reference axis tensor to it, because the inertia tensors aren't scaled that high, so a tiny bit more accuracy won't hurt (even though it costs an extra operation).
# (Note): Because the inertia tensor is symmetrical, I don't need to set yx, zx and zy. But it's barely any overhead, and it makes working with it much easier.
# (TODO): Maybe move the "k == referenceIndex" checks from the number provider to a cached macro. It currently runs 6*3=18 checks just for that.
execute if entity @s[tag=Physics.IsIsotropic] run return 0

execute store result score @s Physics.Object.SpecificInverseInertiaWorld.xx run compute default float physics:other/specific_inverse_inertia_world/xx
execute store result score @s Physics.Object.SpecificInverseInertiaWorld.xy store result score @s Physics.Object.SpecificInverseInertiaWorld.yx run compute default float physics:other/specific_inverse_inertia_world/xy
execute store result score @s Physics.Object.SpecificInverseInertiaWorld.xz store result score @s Physics.Object.SpecificInverseInertiaWorld.zx run compute default float physics:other/specific_inverse_inertia_world/xz
execute store result score @s Physics.Object.SpecificInverseInertiaWorld.yy run compute default float physics:other/specific_inverse_inertia_world/yy
execute store result score @s Physics.Object.SpecificInverseInertiaWorld.yz store result score @s Physics.Object.SpecificInverseInertiaWorld.zy run compute default float physics:other/specific_inverse_inertia_world/yz
execute store result score @s Physics.Object.SpecificInverseInertiaWorld.zz run compute default float physics:other/specific_inverse_inertia_world/zz
