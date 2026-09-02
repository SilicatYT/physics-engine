# Reset scores
scoreboard players reset #Physics.Init

    # Datapack Settings
    scoreboard players reset #Physics.Settings.ShowReloadMessage
    scoreboard players reset #Physics.Settings.DeltaTimeDenominator

    # Simulation settings

    # Helper scores

# Remove scoreboard objectives
scoreboard objectives remove Physics

# Delete data storages
data remove storage physics:zprivate settings
data remove storage physics:zprivate temp
