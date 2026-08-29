$data modify storage physics:zprivate settings.new set value {\
    delta_time_denominator:$(delta_time_denominator),\
    gravity:$(gravity),\
    linear_damping:$(linear_damping),\
    angular_damping:$(angular_damping)\
}

# Setting: Delta Time
execute store result score #Physics.NewSettingValue Physics run data get storage physics:zprivate settings.new.delta_time_denominator
execute unless score #Physics.NewSettingValue Physics = #Physics.Settings.DeltaTimeDenominator Physics run data modify storage physics:zprivate settings.value set from storage physics:zprivate settings.new.delta_time_denominator
execute unless score #Physics.NewSettingValue Physics = #Physics.Settings.DeltaTimeDenominator Physics run function physics:settings/simulation/set_delta_time_denominator with storage physics:zprivate settings

# Setting: Gravity
execute store success score #Physics.SettingValueChanged Physics run data modify storage physics:zprivate settings.simulation.gravity set from storage physics:zprivate settings.new.gravity
execute if score #Physics.SettingValueChanged Physics matches 1 run data modify storage physics:zprivate settings.value set from storage physics:zprivate settings.new.gravity
execute if score #Physics.SettingValueChanged Physics matches 1 run function physics:settings/simulation/set_gravity with storage physics:zprivate settings

# Setting: Linear Damping
execute store success score #Physics.SettingValueChanged Physics run data modify storage physics:zprivate settings.simulation.linear_damping set from storage physics:zprivate settings.new.linear_damping
execute if score #Physics.SettingValueChanged Physics matches 1 run data modify storage physics:zprivate settings.value set from storage physics:zprivate settings.new.linear_damping
execute if score #Physics.SettingValueChanged Physics matches 1 run function physics:settings/simulation/set_linear_damping with storage physics:zprivate settings

# Setting: Angular Damping
execute store success score #Physics.SettingValueChanged Physics run data modify storage physics:zprivate settings.simulation.angular_damping set from storage physics:zprivate settings.new.angular_damping
execute if score #Physics.SettingValueChanged Physics matches 1 run data modify storage physics:zprivate settings.value set from storage physics:zprivate settings.new.angular_damping
execute if score #Physics.SettingValueChanged Physics matches 1 run function physics:settings/simulation/set_angular_damping with storage physics:zprivate settings

# Tellraw
tellraw @s ["",{text:"Physics Engine >> ",color:"#12D9D6"},{text:"Saved configuration!",color:"green"}]
execute at @s run playsound minecraft:ui.cartography_table.take_result ui @s


# (TODO): Decide whether dampingPerTick and gravityPerTick should be stored as scores or in a storage
# (TODO): Add setting explanations (to the dialog?) and reset buttons (or show the default value)
