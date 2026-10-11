# CAUTION: There is NO input validation. Only manually call these functions (or its individual commands) if you know what you're doing.
$data modify storage physics:zprivate settings.simulation.penetration_slop set value $(value)f
data modify storage physics:zprivate settings.dialog.dialogs[1].inputs[4].initial set from storage physics:zprivate settings.simulation.penetration_slop
execute store result score #Physics.Settings.Derived.PenetrationSlop Physics run data get storage physics:zprivate settings.simulation.penetration_slop 65536
