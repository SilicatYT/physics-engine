# CAUTION: There is NO input validation. Only manually call these functions (or its individual commands) if you know what you're doing.
$scoreboard players set #Physics.Settings.DeltaTimeDenominator Physics $(value)
execute store result storage physics:zprivate settings.dialog.dialogs[1].inputs[0].initial byte 1 run scoreboard players get #Physics.Settings.DeltaTimeDenominator Physics
data modify storage physics:zprivate settings.simulation.derived.delta_time set compute default float physics:settings/delta_time
