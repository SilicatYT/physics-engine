# Reset scores
scoreboard players reset #Physics.Init

scoreboard players reset #Physics.NewSettingValue
scoreboard players reset #Physics.SettingValueChanged

    # Datapack Settings
    scoreboard players reset #Physics.Settings.ShowReloadMessage
    scoreboard players reset #Physics.Settings.DeltaTimeDenominator

    # Simulation settings
    #scoreboard players reset #Physics.Settings.LinearDampingPerTick
    #scoreboard players reset #Physics.Settings.AngularDampingPerTick
    #scoreboard players reset #Physics.Settings.GravityPerTick

    # Helper scores

# Remove scoreboard objectives
scoreboard objectives remove Physics

# Delete data storages
data remove storage physics:zprivate settings
data remove storage physics:zprivate temp
