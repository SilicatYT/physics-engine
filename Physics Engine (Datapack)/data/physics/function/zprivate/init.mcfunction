scoreboard players set #Physics.Init Physics 1

# Tellraw
tellraw @a ["",{text:"Physics Engine >> ",color:"#12D9D6"},"Installed Physics Engine (v0.0.1)"]

# Gamerules
gamerule max_command_sequence_length 2147483647

# Summon entities
# (Note): The marker is used for getting the position at any given command context. It's hexadecimal UUID is: "575f7af5-d0dc-4c2c-9182-17931969f0ba".
summon minecraft:marker ~ ~ ~ {UUID:[I;1465875189,-790868948,-1853745261,426373306]}

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

  scoreboard objectives add Physics.Object.HalfExtentAxisProjection.xx dummy
  scoreboard objectives add Physics.Object.HalfExtentAxisProjection.xy dummy
  scoreboard objectives add Physics.Object.HalfExtentAxisProjection.xz dummy
  scoreboard objectives add Physics.Object.HalfExtentAxisProjection.yx dummy
  scoreboard objectives add Physics.Object.HalfExtentAxisProjection.yy dummy
  scoreboard objectives add Physics.Object.HalfExtentAxisProjection.yz dummy
  scoreboard objectives add Physics.Object.HalfExtentAxisProjection.zx dummy
  scoreboard objectives add Physics.Object.HalfExtentAxisProjection.zy dummy
  scoreboard objectives add Physics.Object.HalfExtentAxisProjection.zz dummy

  scoreboard objectives add Physics.Object.Aabb.Max.x dummy
  scoreboard objectives add Physics.Object.Aabb.Max.y dummy
  scoreboard objectives add Physics.Object.Aabb.Max.z dummy
  scoreboard objectives add Physics.Object.Aabb.Min.x dummy
  scoreboard objectives add Physics.Object.Aabb.Min.y dummy
  scoreboard objectives add Physics.Object.Aabb.Min.z dummy

  scoreboard objectives add Physics.Object.AabbRelative.Max.x dummy
  scoreboard objectives add Physics.Object.AabbRelative.Max.y dummy
  scoreboard objectives add Physics.Object.AabbRelative.Max.z dummy
  scoreboard objectives add Physics.Object.AabbRelative.Min.x dummy
  scoreboard objectives add Physics.Object.AabbRelative.Min.y dummy
  scoreboard objectives add Physics.Object.AabbRelative.Min.z dummy

  # Object (Other, transient)
  scoreboard objectives add Physics.Object.LinearVelocityFromAcceleration.x dummy
  scoreboard objectives add Physics.Object.LinearVelocityFromAcceleration.y dummy
  scoreboard objectives add Physics.Object.LinearVelocityFromAcceleration.z dummy

  scoreboard objectives add Physics.Object.AngularVelocityFromTorque.x dummy
  scoreboard objectives add Physics.Object.AngularVelocityFromTorque.y dummy
  scoreboard objectives add Physics.Object.AngularVelocityFromTorque.z dummy

  # Player
  scoreboard objectives add Physics.Player.Id dummy
  scoreboard objectives add Physics.Player.LookingAt.Id dummy
  scoreboard objectives add Physics.Player.LookingAt.Direction.x dummy
  scoreboard objectives add Physics.Player.LookingAt.Direction.y dummy
  scoreboard objectives add Physics.Player.LookingAt.Direction.z dummy
  scoreboard objectives add Physics.Player.LookingAt.BlockPos.x dummy
  scoreboard objectives add Physics.Player.LookingAt.BlockPos.y dummy
  scoreboard objectives add Physics.Player.LookingAt.BlockPos.z dummy
  scoreboard objectives add Physics.Player.LookingAt.PosWithinBlock.x dummy
  scoreboard objectives add Physics.Player.LookingAt.PosWithinBlock.y dummy
  scoreboard objectives add Physics.Player.LookingAt.PosWithinBlock.z dummy

  scoreboard objectives add Physics.Player.PunchStrength dummy

  # Punchable Hitbox
  scoreboard objectives add Physics.Hitbox.Gametime dummy

# Set initial scores
scoreboard players set #Physics.Constant.-1 Physics -1

scoreboard players set #Physics.MinDistance Physics 2147483647

# Set data storages
data modify storage physics:zprivate temp.orientation set value [0f, 0f , 0f, 1f]
data modify storage physics:zprivate hitbox_data set value {height: 0.3f, width: 0.3f, Pos: [0f, 0f, 0f]}

data modify storage physics:object default set value {\
  scale: [1f, 1f, 1f],\
  orientation: [0f, 0f, 0f, 1f],\
  inverse_mass: 0.001f\
}
data modify storage physics:object set set from storage physics:object default
data modify storage physics:zprivate fallback_default set from storage physics:object default

data modify storage physics:zprivate constants set value {\
  quaternion_min_length_squared: 0.000001f,\
  min_scale: 0.0625f,\
  max_scale: 10f\
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
            "template": "function physics:zprivate/settings/datapack/apply {show_reload_message:$(show_reload_message)b}"\
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
            "template": "function physics:zprivate/settings/simulation/apply {delta_time_denominator:$(delta_time_denominator),gravity:$(gravity)f,linear_damping:$(linear_damping)f,angular_damping:$(angular_damping)f}"\
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
