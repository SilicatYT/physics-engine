# Setup scores
# (Note): This also sets IsSetupComplete to 1 for performance reasons. It's faster than having a separate command that sets it.
execute store success score #Physics.IsSetupComplete Physics run scoreboard players operation #Physics.ObjectA Physics.Object.Id = @s Physics.Object.Id

scoreboard players operation #Physics.ObjectA Physics.Object.BlockPos.x = @s Physics.Object.BlockPos.x
scoreboard players operation #Physics.ObjectA Physics.Object.BlockPos.y = @s Physics.Object.BlockPos.y
scoreboard players operation #Physics.ObjectA Physics.Object.BlockPos.z = @s Physics.Object.BlockPos.z

scoreboard players operation #Physics.ObjectA Physics.Object.PosWithinBlock.x = @s Physics.Object.PosWithinBlock.x
scoreboard players operation #Physics.ObjectA Physics.Object.PosWithinBlock.y = @s Physics.Object.PosWithinBlock.y
scoreboard players operation #Physics.ObjectA Physics.Object.PosWithinBlock.z = @s Physics.Object.PosWithinBlock.z

scoreboard players operation #Physics.ObjectA Physics.Object.RotationMatrix.xx = @s Physics.Object.RotationMatrix.xx
scoreboard players operation #Physics.ObjectA Physics.Object.RotationMatrix.xy = @s Physics.Object.RotationMatrix.xy
scoreboard players operation #Physics.ObjectA Physics.Object.RotationMatrix.xz = @s Physics.Object.RotationMatrix.xz
scoreboard players operation #Physics.ObjectA Physics.Object.RotationMatrix.yx = @s Physics.Object.RotationMatrix.yx
scoreboard players operation #Physics.ObjectA Physics.Object.RotationMatrix.yy = @s Physics.Object.RotationMatrix.yy
scoreboard players operation #Physics.ObjectA Physics.Object.RotationMatrix.yz = @s Physics.Object.RotationMatrix.yz
scoreboard players operation #Physics.ObjectA Physics.Object.RotationMatrix.zx = @s Physics.Object.RotationMatrix.zx
scoreboard players operation #Physics.ObjectA Physics.Object.RotationMatrix.zy = @s Physics.Object.RotationMatrix.zy
scoreboard players operation #Physics.ObjectA Physics.Object.RotationMatrix.zz = @s Physics.Object.RotationMatrix.zz

scoreboard players operation #Physics.ObjectA Physics.Object.Scale.x = @s Physics.Object.Scale.x
scoreboard players operation #Physics.ObjectA Physics.Object.Scale.y = @s Physics.Object.Scale.y
scoreboard players operation #Physics.ObjectA Physics.Object.Scale.z = @s Physics.Object.Scale.z

# Setup cheap manifold and AEC access
# (Note): This is necessary so I can efficiently access the previous manifold for that object pair during the SAT, as well as cheaply access the AEC for making the manifold or objectB's AEC ride it.
# (Note): The previous tick's manifolds are automatically teleported to y=8216, and the current tick's manifolds are at y=8232.
execute on passengers if entity @s[tag=Physics.ObjectArmorStand] on passengers on origin on vehicle in physics:void run tp @s 8.0 8200.0 8.0
