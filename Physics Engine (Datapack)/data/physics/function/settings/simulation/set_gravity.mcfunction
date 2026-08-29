# CAUTION: There is NO input validation. Only manually call these functions (or its individual commands) if you know what you're doing.
$data modify storage physics:zprivate settings.simulation.gravity set value $(value)
data modify storage physics:zprivate settings.dialog.dialogs[1].inputs[1].initial set from storage physics:zprivate settings.simulation.gravity
# (TODO): Calculate gravity_per_tick
