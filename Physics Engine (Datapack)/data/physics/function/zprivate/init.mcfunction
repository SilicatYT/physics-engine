scoreboard players set #Physics.Init Physics 1

# Tellraw
tellraw @a ["",{text:"Physics Engine >> ",color:"#12D9D6"},"Installed Physics Engine (v0.0.1)"]

# Add scoreboard objectives

# Set initial scores
scoreboard players set #Physics.Settings.ShowReloadMessage Physics 1

# Set data storages

# Set initial config dialog
data modify storage physics:zprivate settings.dialog set value \
{\
  "type": "minecraft:dialog_list",\
  "title": "Physics Engine — Configuration",\
  "body": {\
    "type": "minecraft:plain_message",\
    "contents": "Configure the general datapack and finetune the simulation"\
  },\
  "inputs": [],\
  "pause": 0,\
  "dialogs": [\
    {\
      "type": "minecraft:multi_action",\
      "title": "Datapack Settings",\
      "inputs": [\
        {\
          "type": "minecraft:boolean",\
          "key": "show_reload_message",\
          "label": "Show reload message",\
          "initial": 1,\
          "on_true": "1",\
          "on_false": "0"\
        }\
      ],\
      "pause": 0,\
      "actions": [\
        {\
          "label": "Confirm",\
          "action": {\
            "type": "minecraft:dynamic/run_command",\
            "template": "function physics:zprivate/settings/datapack/apply {show_reload_message:$(show_reload_message)}"\
          }\
        },\
        {\
          "label": "Cancel"\
        }\
      ]\
    },\
    {\
      "type": "minecraft:multi_action",\
      "title": "Simulation Settings",\
      "pause": 0,\
      "actions": [\
        {\
          "label": "Confirm"\
        },\
        {\
          "label": "Cancel"\
        }\
      ]\
    }\
  ]\
}
