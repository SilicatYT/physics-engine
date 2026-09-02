# CAUTION: There is NO input validation. Only manually call these functions (or its individual commands) if you know what you're doing.
$data modify storage physics:zprivate settings.simulation.angular_damping set value $(value)
data modify storage physics:zprivate settings.dialog.dialogs[1].inputs[3].initial set from storage physics:zprivate settings.simulation.angular_damping
data modify storage physics:zprivate settings.simulation.derived.angular_damping_per_tick set compute default float physics:settings/angular_damping_per_tick
