# Init
# (Note): LookingAtId is set to 0 because I run an "if 0" check. If it's not initialized, it'll stay unset forever. I could use "unless 1..", but that's slightly slower.
scoreboard players set @s Physics.Player.LookingAtId 0
execute store result score @s Physics.Player.Id run scoreboard players add #Physics Physics.Player.Id 1
