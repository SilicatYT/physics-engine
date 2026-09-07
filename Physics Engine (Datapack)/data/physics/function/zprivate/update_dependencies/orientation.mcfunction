# Update rotation matrix
# (Note): As with most other values that need to be recomputed constantly, I avoid the round() at the end to save performance. Reducing the error by 0.5 when the scaling factor is so high is not important.
# (Note): I benchmarked it, and blindly calculating each provider separately without pre-calculating the shared products (which would need to be stored in storages, which are slower to access than scores) is much faster in this case.
# (Formula): xx = 2*(a^2 + x^2) - 1
#            xy = 2*(x*y - a*z)
#            xz = 2*(x*z + a*y)
#            yx = 2*(x*y + a*z)
#            yy = 2*(a^2 + y^2) - 1
#            yz = 2*(y*z - a*x)
#            zx = 2*(x*z - a*y)
#            zy = 2*(y*z + a*x)
#            zz = 2*(a^2 + z^2) - 1
execute store result score @s Physics.Object.RotationMatrix.xx run compute default float physics:integration/rotation_matrix/xx
execute store result score @s Physics.Object.RotationMatrix.xy run compute default float physics:integration/rotation_matrix/xy
execute store result score @s Physics.Object.RotationMatrix.xz run compute default float physics:integration/rotation_matrix/xz
execute store result score @s Physics.Object.RotationMatrix.yx run compute default float physics:integration/rotation_matrix/yx
execute store result score @s Physics.Object.RotationMatrix.yy run compute default float physics:integration/rotation_matrix/yy
execute store result score @s Physics.Object.RotationMatrix.yz run compute default float physics:integration/rotation_matrix/yz
execute store result score @s Physics.Object.RotationMatrix.zx run compute default float physics:integration/rotation_matrix/zx
execute store result score @s Physics.Object.RotationMatrix.zy run compute default float physics:integration/rotation_matrix/zy
execute store result score @s Physics.Object.RotationMatrix.zz run compute default float physics:integration/rotation_matrix/zz

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
