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
# TODO
