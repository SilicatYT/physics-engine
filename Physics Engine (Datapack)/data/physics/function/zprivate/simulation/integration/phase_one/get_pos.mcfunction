# (Note): This function needs to be called in "physics:void" as 575f7af5-d0dc-4c2c-9182-17931969f0ba.
# (Note): A function call for this is unfortunate, but it's cheaper than forceloading and unforceloading the current chunk.
    # Get entity data
    tp @s ~ ~ ~
    data modify storage physics:zprivate pos set from entity @s Pos

    # Set internal pos to entity pos
    # (Explanation): If the entity gets teleported, it should automatically update the internal pos values rather than teleport back to its original position. That's why I update the pos scores every tick.
    execute store result score #Physics Physics.Object.BlockPos.x store result storage physics:zprivate temp.x int -1 run data get storage physics:zprivate pos[0]
    execute store result score #Physics Physics.Object.BlockPos.y store result storage physics:zprivate temp.y int -1 run data get storage physics:zprivate pos[1]
    execute store result score #Physics Physics.Object.BlockPos.z store result storage physics:zprivate temp.z int -1 run data get storage physics:zprivate pos[2]

    # (Note): I need the PosWithinBlock at high precision. Number providers use floats, so it would be much less precise to use one at large coordinates.
    function physics:zprivate/macro/relative_tp with storage physics:zprivate temp
    data modify storage physics:zprivate pos set from entity @s Pos
    execute store result score #Physics Physics.Object.PosWithinBlock.x run data get storage physics:zprivate pos[0] 16777216
    execute store result score #Physics Physics.Object.PosWithinBlock.y run data get storage physics:zprivate pos[1] 16777216
    execute store result score #Physics Physics.Object.PosWithinBlock.z run data get storage physics:zprivate pos[2] 16777216

# Reset
# (Note): The teleport back is necessary to avoid the entity from unloading, as it's a separate dimension with only a single loaded chunk.
# (Note): Resetting the rotation is necessary because relative rotation changes (like the one in "get_ray") can cause unclamped overflow that messes with calculations, but I don't change rotation here, so I can assume it's already 0.
tp @s 8.0 8.0 8.0
