# Kill AEC if the corresponding entity isn't loaded anymore
# (TODO): Check if there's a potential desync bug that creates multiple AECs because the item display is unloaded but still reachable via "on origin" for a little bit (happened to me once in 26.3), creating a 2nd AEC when the object loads back in without the 1st being killed.
scoreboard players set #Physics.IsTrue Physics 0
execute on origin run scoreboard players set #Physics.IsTrue Physics 1
execute if score #Physics.IsTrue Physics matches 0 run return run kill @s

# Build island
    # Check if it's a single object (not an island)

    # Flatten the stack

    # Summon island entity and ride it
