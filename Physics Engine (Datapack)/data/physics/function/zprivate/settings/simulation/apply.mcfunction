$data modify storage physics:zprivate settings.new set value {\
    delta_time_denominator:$(delta_time_denominator),\
    gravity:$(gravity),\
    linear_damping:$(linear_damping),\
    angular_damping:$(angular_damping)\
}

# (Note): All setting values are updated every time, because adding guards doesn't matter for one-time calls' performance, and some values depend on others.

# Setting: Delta Time
data modify storage physics:zprivate settings.value set from storage physics:zprivate settings.new.delta_time_denominator
function physics:settings/simulation/set_delta_time_denominator with storage physics:zprivate settings

# Setting: Gravity
data modify storage physics:zprivate settings.value set from storage physics:zprivate settings.new.gravity
function physics:settings/simulation/set_gravity with storage physics:zprivate settings

# Setting: Linear Damping
data modify storage physics:zprivate settings.value set from storage physics:zprivate settings.new.linear_damping
function physics:settings/simulation/set_linear_damping with storage physics:zprivate settings

# Setting: Angular Damping
data modify storage physics:zprivate settings.value set from storage physics:zprivate settings.new.angular_damping
function physics:settings/simulation/set_angular_damping with storage physics:zprivate settings

# Tellraw
tellraw @s ["",{text:"Physics Engine >> ",color:"#12D9D6"},{text:"Saved configuration!",color:"green"}]
execute at @s run playsound minecraft:ui.cartography_table.take_result ui @s
