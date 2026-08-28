# Setting: Show Reload Message
$execute store result storage physics:zprivate settings.dialog.dialogs[0].inputs[0].initial byte 1 run scoreboard players set #Physics.Settings.ShowReloadMessage Physics $(show_reload_message)

# Tellraw
tellraw @s ["",{text:"Physics Engine >> ",color:"#12D9D6"},{text:"Saved configuration!",color:"green"}]
execute at @s run playsound minecraft:ui.cartography_table.take_result ui @s
