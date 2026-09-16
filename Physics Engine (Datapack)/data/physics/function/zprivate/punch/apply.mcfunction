# Display crit particles at the target location
$particle minecraft:crit ~$(x) ~$(y) ~$(z) 0.1 0.1 0.1 0 3

# Apply the impulse
function physics:object/impulse/apply_from_scores
