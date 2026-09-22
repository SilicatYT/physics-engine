# (TODO): Maybe using "execute summon" or directly running the setup commands (2 @e calls) would be faster than running the setup function?
summon minecraft:interaction ~ ~ ~ {width: 0.3f, height: 0.3f, response: 1b, Tags: ["Physics.Hitbox", "Physics.Temp"]}
execute as @e[type=minecraft:interaction,tag=Physics.Temp,distance=..0.01,limit=1] run function physics:zprivate/punchable_hitbox/summon/setup
