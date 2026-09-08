# Set inverse mass
$data modify storage physics:zprivate temp.inverse_mass set value $(inverse_mass)
execute store result score #Physics.IsTrue Physics run compute default integer physics:other/set_inverse_mass/is_valid
execute if score #Physics.IsTrue Physics matches 0 run data modify storage physics:zprivate temp.inverse_mass set from storage physics:zprivate fallback_default.inverse_mass

execute store result score @s Physics.Object.InverseMass run data get storage physics:zprivate temp.inverse_mass 268435456

# Reset values that need to be 0 if object has infinite mass
execute if score @s Physics.Object.InverseMass matches 1 run return 0

scoreboard players reset @s Physics.Object.LinearVelocityFromAcceleration.x
scoreboard players reset @s Physics.Object.LinearVelocityFromAcceleration.y
scoreboard players reset @s Physics.Object.LinearVelocityFromAcceleration.z

scoreboard players reset @s Physics.Object.AngularVelocityFromTorque.x
scoreboard players reset @s Physics.Object.AngularVelocityFromTorque.y
scoreboard players reset @s Physics.Object.AngularVelocityFromTorque.z
