# Integration (Phase 1)
execute as @e[type=minecraft:item_display,tag=Physics.Object] run function physics:zprivate/simulation/integration/phase_one/main

# Collision Detection
# (Note): Leads into Contact Generation

# Collision Resolution

# Integration (Phase 2)
execute as @e[type=minecraft:item_display,tag=Physics.Object] run function physics:zprivate/simulation/integration/phase_two/main

# Schedule next tick
schedule function physics:zprivate/tick 1t
