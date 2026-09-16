advancement revoke @s only physics:punch_hitbox

# Apply impulse to targeted entity
# (TODO): Maybe add a dynamic distance check here, like in "../punchable_hitbox/main".
execute if score @s Physics.Player.PunchStrength matches 0 run return 0
execute if score @s Physics.Player.PunchStrength matches 1.. run scoreboard players operation #Physics.ApplyImpulse.Strength Physics = @s Physics.Player.PunchStrength
execute unless score @s Physics.Player.PunchStrength matches 1.. run scoreboard players operation #Physics.ApplyImpulse.Strength Physics = #Physics.Settings.DefaultPunchStrength Physics
execute store result storage physics:zprivate temp.x float 0.0000152587890625 run scoreboard players operation #Physics.ApplyImpulse.RelativePos.x Physics = @s Physics.Player.LookingAt.RelativePos.x
execute store result storage physics:zprivate temp.y float 0.0000152587890625 run scoreboard players operation #Physics.ApplyImpulse.RelativePos.y Physics = @s Physics.Player.LookingAt.RelativePos.y
execute store result storage physics:zprivate temp.z float 0.0000152587890625 run scoreboard players operation #Physics.ApplyImpulse.RelativePos.z Physics = @s Physics.Player.LookingAt.RelativePos.z
scoreboard players operation #Physics.ApplyImpulse.Direction.x Physics = @s Physics.Player.LookingAt.Direction.x
scoreboard players operation #Physics.ApplyImpulse.Direction.y Physics = @s Physics.Player.LookingAt.Direction.y
scoreboard players operation #Physics.ApplyImpulse.Direction.z Physics = @s Physics.Player.LookingAt.Direction.z

scoreboard players operation #Physics Physics.Object.Id = @s Physics.Player.LookingAt.Id
execute as @e[type=minecraft:item_display,x=0,predicate=physics:same_object_id,limit=1] at @s run function physics:zprivate/punch/apply with storage physics:zprivate temp
