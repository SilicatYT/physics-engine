$data modify storage physics:zprivate settings.new set value {\
    show_reload_message:$(show_reload_message)\
}

# Setting: Show Reload Message
execute store result score #Physics.NewSettingValue Physics run data get storage physics:zprivate settings.new.show_reload_message
execute unless score #Physics.NewSettingValue Physics = #Physics.Settings.ShowReloadMessage Physics run data modify storage physics:zprivate settings.value set from storage physics:zprivate settings.new.show_reload_message
execute unless score #Physics.NewSettingValue Physics = #Physics.Settings.ShowReloadMessage Physics run function physics:settings/datapack/set_show_reload_message with storage physics:zprivate settings

# Tellraw
tellraw @s ["",{text:"Physics Engine >> ",color:"#12D9D6"},{text:"Saved configuration!",color:"green"}]
execute at @s run playsound minecraft:ui.cartography_table.take_result ui @s
