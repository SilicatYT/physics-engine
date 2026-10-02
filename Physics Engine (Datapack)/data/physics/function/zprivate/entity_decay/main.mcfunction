# Kill AEC if the corresponding entity isn't loaded anymore
# (Note): As of 26.3, "on origin" may still target the entity even if it's unloaded. But in "after_unloading", I explicitly kill the previously existing AEC, so this isn't a problem. No duplicates to worry about.
# (TODO): As soon as that bug is fixed (MC-312207), remove the "if loaded" check.
scoreboard players set #Physics.IsTrue Physics 0
execute on origin at @s if loaded ~ ~ ~ run scoreboard players set #Physics.IsTrue Physics 1
execute if score #Physics.IsTrue Physics matches 0 run return run function physics:zprivate/entity_decay/kill_aec

# Kill the object's passengers if it (the item display) was manually killed
# (TODO): Does this break if the entity unloads in the same tick it's killed, leaving the passengers there permanently? Because if the object unloads, the AEC (this) is killed, meaning the checks never get run for this stack again.
execute on origin unless predicate physics:has_vehicle run return run function physics:zprivate/entity_decay/kill_all_related_entities

# TODO: Add decay timers for manifold entities

