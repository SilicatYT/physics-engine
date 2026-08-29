scoreboard players set #Physics.Init Physics 1

# Tellraw
tellraw @a ["",{text:"Physics Engine >> ",color:"#12D9D6"},"Installed Physics Engine (v0.0.1)"]

# Add scoreboard objectives
  # Object

  # Object (Derived)

  # Player

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
    "contents": "Configure the general datapack and fine-tune the simulation",\
    "width": 350\
  },\
  "inputs": [],\
  "dialogs": [\
    {\
      "type": "minecraft:multi_action",\
      "title": "Datapack Settings",\
      "body": {\
        "type": "minecraft:plain_message",\
        "contents": "Broad adjustments to the datapack itself",\
        "width": 350\
      },\
      "inputs": [\
        {\
          "type": "minecraft:boolean",\
          "key": "show_reload_message",\
          "label": "Show reload message",\
          "on_true": "1",\
          "on_false": "0"\
        }\
      ],\
      "columns": 3,\
      "actions": [\
        {\
          "label": "Confirm",\
          "tooltip": "Click",\
          "width": 150,\
          "action": {\
            "type": "minecraft:dynamic/run_command",\
            "template": "function physics:zprivate/settings/datapack/apply {show_reload_message:$(show_reload_message)}"\
          }\
        },\
        {\
          "label": "Cancel",\
          "tooltip": "Click",\
          "width": 100\
        },\
        {\
          "label": {\
            "text": "Reset",\
            "color": "red"\
          },\
          "tooltip": "Click",\
          "width": 50,\
          "action": {\
            "type": "minecraft:run_command",\
            "command": "function physics:zprivate/settings/datapack/reset_and_show"\
          }\
        },\
        {\
          "label": "← Previous",\
          "tooltip": "Click to open",\
          "width": 75,\
          "action": {\
            "type": "minecraft:run_command",\
            "command": "function physics:zprivate/settings/show"\
          }\
        }\
      ]\
    },\
    {\
      "type": "minecraft:multi_action",\
      "title": "Simulation Settings",\
      "body": {\
        "type": "minecraft:plain_message",\
        "contents": "Parameters relating to the simulation",\
        "width": 350\
      },\
      "inputs": [\
        {\
          "type": "minecraft:number_range",\
          "key": "delta_time_denominator",\
          "label": "Delta Time",\
          "label_format": "%1$s: 1s/%2$s",\
          "start": 1,\
          "end": 120,\
          "step": 1\
        },\
        {\
          "type": "minecraft:number_range",\
          "key": "gravity",\
          "label": "Gravity",\
          "label_format": "%1$s: %2$sm/s²",\
          "start": -20,\
          "end": 20,\
          "step": 0.01\
        },\
        {\
          "type": "minecraft:number_range",\
          "key": "linear_damping",\
          "label": "Linear Damping",\
          "start": 0,\
          "end": 1,\
          "step": 0.01\
        },\
        {\
          "type": "minecraft:number_range",\
          "key": "angular_damping",\
          "label": "Angular Damping",\
          "start": 0,\
          "end": 1,\
          "step": 0.01\
        }\
      ],\
      "columns": 3,\
      "actions": [\
        {\
          "label": "Confirm",\
          "tooltip": "Click",\
          "width": 150,\
          "action": {\
            "type": "minecraft:dynamic/run_command",\
            "template": "function physics:zprivate/settings/simulation/apply {delta_time_denominator:$(delta_time_denominator),gravity:$(gravity),linear_damping:$(linear_damping),angular_damping:$(angular_damping)}"\
          }\
        },\
        {\
          "label": "Cancel",\
          "tooltip": "Click",\
          "width": 100\
        },\
        {\
          "label": {\
            "text": "Reset",\
            "color": "red"\
          },\
          "tooltip": "Click",\
          "width": 50,\
          "action": {\
            "type": "minecraft:run_command",\
            "command": "function physics:zprivate/settings/simulation/reset_and_show"\
          }\
        },\
        {\
          "label": "← Previous",\
          "tooltip": "Click to open",\
          "width": 75,\
          "action": {\
            "type": "minecraft:run_command",\
            "command": "function physics:zprivate/settings/show"\
          }\
        }\
      ]\
    }\
  ]\
}

# Set initial settings
function physics:zprivate/settings/datapack/reset
function physics:zprivate/settings/simulation/reset
