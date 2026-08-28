# Initialize
scoreboard objectives add Physics dummy
execute unless score #Physics.Init Physics matches 1 run function physics:zprivate/init

# Tellraw
# (TODO): Modrinth link is currently a placeholder
execute if score #Physics.Settings.ShowReloadMessage Physics matches 0 run return 0
tellraw @a ["",{text:"Physics Engine >> ",color:"#12D9D6"},"By SilicatYT"]
tellraw @a ["",{text:"Physics Engine >> ",color:"#12D9D6"},{text:"Main page (Press ",click_event:{action:"run_command",command:"/dialog show @s physics:main"},hover_event:{action:"show_text",value:[{text:"Click to open"}]}},{keybind:"key.quickActions",click_event:{action:"run_command",command:"/dialog show @s physics:main"},hover_event:{action:"show_text",value:[{text:"Click to open"}]}},{text:")",click_event:{action:"run_command",command:"/dialog show @s physics:main"},hover_event:{action:"show_text",value:[{text:"Click to open"}]}}]
