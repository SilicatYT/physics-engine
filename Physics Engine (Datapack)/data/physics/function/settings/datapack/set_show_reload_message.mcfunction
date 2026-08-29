# CAUTION: There is NO input validation. Only manually call these functions (or its individual commands) if you know what you're doing.
$scoreboard players set #Physics.Settings.ShowReloadMessage Physics $(value)
execute store result storage physics:zprivate settings.dialog.dialogs[0].inputs[0].initial byte 1 run scoreboard players get #Physics.Settings.ShowReloadMessage Physics
