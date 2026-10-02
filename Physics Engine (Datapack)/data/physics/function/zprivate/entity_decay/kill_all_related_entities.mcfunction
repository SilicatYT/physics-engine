# Kill base area effect cloud (in physics:void) with all its passengers
execute on passengers on origin on vehicle run function physics:zprivate/entity_decay/kill_aec

# Kill area effect cloud
execute on passengers run kill @s

# Kill self
kill @s
