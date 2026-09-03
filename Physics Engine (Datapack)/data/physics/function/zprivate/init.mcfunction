scoreboard players set #Physics.Init Physics 1

# Tellraw
tellraw @a ["",{text:"Physics Engine >> ",color:"#12D9D6"},"Installed Physics Engine (v0.0.1)"]

# Gamerules
gamerule max_command_sequence_length 2147483647

# Add scoreboard objectives
  # Object
  scoreboard objectives add Physics.Object.BlockPos.x dummy
  scoreboard objectives add Physics.Object.BlockPos.y dummy
  scoreboard objectives add Physics.Object.BlockPos.z dummy

  scoreboard objectives add Physics.Object.PosWithinBlock.x dummy
  scoreboard objectives add Physics.Object.PosWithinBlock.y dummy
  scoreboard objectives add Physics.Object.PosWithinBlock.z dummy

  scoreboard objectives add Physics.Object.LinearVelocity.x dummy
  scoreboard objectives add Physics.Object.LinearVelocity.y dummy
  scoreboard objectives add Physics.Object.LinearVelocity.z dummy

  scoreboard objectives add Physics.Object.AngularVelocity.x dummy
  scoreboard objectives add Physics.Object.AngularVelocity.y dummy
  scoreboard objectives add Physics.Object.AngularVelocity.z dummy

  scoreboard objectives add Physics.Object.Scale.x dummy
  scoreboard objectives add Physics.Object.Scale.y dummy
  scoreboard objectives add Physics.Object.Scale.z dummy

  scoreboard objectives add Physics.Object.InverseMass dummy

  scoreboard objectives add Physics.Object.Orientation.x dummy
  scoreboard objectives add Physics.Object.Orientation.y dummy
  scoreboard objectives add Physics.Object.Orientation.z dummy
  scoreboard objectives add Physics.Object.Orientation.a dummy

  # Object (Derived)
  scoreboard objectives add Physics.Object.RotationMatrix.xx dummy
  scoreboard objectives add Physics.Object.RotationMatrix.xy dummy
  scoreboard objectives add Physics.Object.RotationMatrix.xz dummy
  scoreboard objectives add Physics.Object.RotationMatrix.yx dummy
  scoreboard objectives add Physics.Object.RotationMatrix.yy dummy
  scoreboard objectives add Physics.Object.RotationMatrix.yz dummy
  scoreboard objectives add Physics.Object.RotationMatrix.zx dummy
  scoreboard objectives add Physics.Object.RotationMatrix.zy dummy
  scoreboard objectives add Physics.Object.RotationMatrix.zz dummy

  scoreboard objectives add Physics.Object.SpecificInverseInertiaLocal.x dummy
  scoreboard objectives add Physics.Object.SpecificInverseInertiaLocal.y dummy
  scoreboard objectives add Physics.Object.SpecificInverseInertiaLocal.z dummy

  scoreboard objectives add Physics.Object.SpecificInverseInertiaLocal.ReferenceAxis.Index dummy
  scoreboard objectives add Physics.Object.SpecificInverseInertiaLocal.ReferenceAxis.Value dummy
  scoreboard objectives add Physics.Object.SpecificInverseInertiaLocal.TangentDifference.x dummy
  scoreboard objectives add Physics.Object.SpecificInverseInertiaLocal.TangentDifference.y dummy
  scoreboard objectives add Physics.Object.SpecificInverseInertiaLocal.TangentDifference.z dummy

  scoreboard objectives add Physics.Object.SpecificInverseInertiaWorld.xx dummy
  scoreboard objectives add Physics.Object.SpecificInverseInertiaWorld.xy dummy
  scoreboard objectives add Physics.Object.SpecificInverseInertiaWorld.xz dummy
  scoreboard objectives add Physics.Object.SpecificInverseInertiaWorld.yx dummy
  scoreboard objectives add Physics.Object.SpecificInverseInertiaWorld.yy dummy
  scoreboard objectives add Physics.Object.SpecificInverseInertiaWorld.yz dummy
  scoreboard objectives add Physics.Object.SpecificInverseInertiaWorld.zx dummy
  scoreboard objectives add Physics.Object.SpecificInverseInertiaWorld.zy dummy
  scoreboard objectives add Physics.Object.SpecificInverseInertiaWorld.zz dummy

  # Object (Other, transient)
  scoreboard objectives add Physics.Object.AccumulatedForce.x dummy
  scoreboard objectives add Physics.Object.AccumulatedForce.y dummy
  scoreboard objectives add Physics.Object.AccumulatedForce.z dummy

  scoreboard objectives add Physics.Object.AccumulatedTorque.x dummy
  scoreboard objectives add Physics.Object.AccumulatedTorque.y dummy
  scoreboard objectives add Physics.Object.AccumulatedTorque.z dummy

  scoreboard objectives add Physics.Object.LinearVelocityFromAcceleration.x dummy
  scoreboard objectives add Physics.Object.LinearVelocityFromAcceleration.y dummy
  scoreboard objectives add Physics.Object.LinearVelocityFromAcceleration.z dummy

  scoreboard objectives add Physics.Object.AngularVelocityFromTorque.x dummy
  scoreboard objectives add Physics.Object.AngularVelocityFromTorque.y dummy
  scoreboard objectives add Physics.Object.AngularVelocityFromTorque.z dummy

  # Player

# Set initial scores

# Set data storages
data modify storage physics:object default set value {\
  scale: [1f, 1f, 1f],\
  orientation: [0f, 0f, 0f, 1f],\
  inverse_mass: 0.001f\
}
data modify storage physics:object set set from storage physics:object default
data modify storage physics:zprivate fallback_default set from storage physics:object default

data modify storage physics:zprivate constants set value {\
  quaternion_min_squared_length: 0.000001f,\
  min_scale: 0.0625f,\
  max_scale: 10\
}

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
