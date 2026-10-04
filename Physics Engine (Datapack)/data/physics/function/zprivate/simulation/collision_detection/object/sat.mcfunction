# (Note): The initial object is A, whereas @s is B.

# Setup
    # Calculate relative pos (A-B)
    # (Note): Scaled up by 2^16.
    execute store result score #Physics.Offset.x Physics run compute default float physics:collision_detection/relative_pos/x
    execute store result score #Physics.Offset.y Physics run compute default float physics:collision_detection/relative_pos/y
    execute store result score #Physics.Offset.z Physics run compute default float physics:collision_detection/relative_pos/z

# OBB-check (Separating Axes Theorem, using the Gottschalk-Erikson approach)
# (Formula): OffsetInA.x = AxisA.x * Offset
#            OffsetInB.x = OffsetInA * AxisDot.?x
# (Note): I always calculate abs(axisDot) inline in the number providers, because the 9 scoreboard and compute commands would likely be more overhead than what it's worth to store and re-use it.
# (Note): AxisDot is scaled up by 2^24.
# (Note): OffsetInA/B is scaled up by 2^16 (Same as RelativePos).
# (TODO): Check whether it's faster to run the entire SAT as a single predicate check with a large number provider, and to re-compute the values if it succeeds. Currently, (almost) all values that can be re-used later are stored, which increases the command count.
# (TODO): In general, check whether it's worth it to run certain calculations twice (like axisDot, offsetIn, signedDistanceAlongAxis for edge-edge, ...), so I could bundle more into a single number provider to reduce the command count.
    # ObjectA's axes
        # A: x
            # Setup
            execute store result score #Physics.AxisDot.xx Physics run compute default float physics:collision_detection/axis_dot/xx
            execute store result score #Physics.AxisDot.xy Physics run compute default float physics:collision_detection/axis_dot/xy
            execute store result score #Physics.AxisDot.xz Physics run compute default float physics:collision_detection/axis_dot/xz
            execute store result score #Physics.OffsetInA.x Physics run compute default float physics:collision_detection/offset_in_a/x

            # Overlap calculation
            # (Formula): overlap = radiusA + radiusB - distanceAlongAxis
            #            radiusA = halfExtentA.x
            #            radiusB = halfExtentB.x * abs(axisDot.xx) + halfExtentB.y * abs(axisDot.xy) + halfExtentB.z * abs(axisDot.xz)
            #            distanceAlongAxis = abs(offsetInA.x)
            # (Note): Overlap is scaled up by 2^16.
            execute store result score #Physics.Overlap.A.x Physics run compute default float physics:collision_detection/overlap/axis_a/x
            execute if score #Physics.Overlap.A.x Physics matches ..0 run return 0

        # A: y
            # Setup
            execute store result score #Physics.AxisDot.yx Physics run compute default float physics:collision_detection/axis_dot/yx
            execute store result score #Physics.AxisDot.yy Physics run compute default float physics:collision_detection/axis_dot/yy
            execute store result score #Physics.AxisDot.yz Physics run compute default float physics:collision_detection/axis_dot/yz
            execute store result score #Physics.OffsetInA.y Physics run compute default float physics:collision_detection/offset_in_a/y

            # Overlap calculation
            execute store result score #Physics.Overlap.A.y Physics run compute default float physics:collision_detection/overlap/axis_a/y
            execute if score #Physics.Overlap.A.y Physics matches ..0 run return 0

        # A: z
            # Setup
            execute store result score #Physics.AxisDot.zx Physics run compute default float physics:collision_detection/axis_dot/zx
            execute store result score #Physics.AxisDot.zy Physics run compute default float physics:collision_detection/axis_dot/zy
            execute store result score #Physics.AxisDot.zz Physics run compute default float physics:collision_detection/axis_dot/zz
            execute store result score #Physics.OffsetInA.z Physics run compute default float physics:collision_detection/offset_in_a/z

            # Overlap calculation
            execute store result score #Physics.Overlap.A.z Physics run compute default float physics:collision_detection/overlap/axis_a/z
            execute if score #Physics.Overlap.A.z Physics matches ..0 run return 0

    # ObjectB's axes
        # B: x
            # Setup
            execute store result score #Physics.OffsetInB.x Physics run compute default float physics:collision_detection/offset_in_b/x

            # Overlap calculation
            # (Formula): radiusA = halfExtentA.x * abs(axisDot.xx) + halfExtentA.y * abs(axisDot.yx) + halfExtentA.z * abs(axisDot.zx)
            #            radiusB = halfExtentB.x
            #            distanceAlongAxis = abs(offsetInB.x)
            execute store result score #Physics.Overlap.B.x Physics run compute default float physics:collision_detection/overlap/axis_b/x
            execute if score #Physics.Overlap.B.x Physics matches ..0 run return 0

        # B: y
            # Setup
            execute store result score #Physics.OffsetInB.y Physics run compute default float physics:collision_detection/offset_in_b/y

            # Overlap calculation
            execute store result score #Physics.Overlap.B.y Physics run compute default float physics:collision_detection/overlap/axis_b/y
            execute if score #Physics.Overlap.B.y Physics matches ..0 run return 0

        # B: z
            # Setup
            execute store result score #Physics.OffsetInB.z Physics run compute default float physics:collision_detection/offset_in_b/z

            # Overlap calculation
            execute store result score #Physics.Overlap.B.z Physics run compute default float physics:collision_detection/overlap/axis_b/z
            execute if score #Physics.Overlap.B.z Physics matches ..0 run return 0

    # Cross-product axes
        # A: x, B: x
        # (Note): AxisLengthSquared is scaled up by 2^24.
        # (Note): 1024 is a magic number here, should maybe be put in a variable (but then it costs more). It's the CROSS_PRODUCT_EPSILON_SQUARED. If AxisLengthSquared is smaller than that, the cross product axis is degenerate and worth ignoring.
        # (Formula): axisLengthSquared.xx = 1.0 - axisDot.xx^2
        #            overlapSquared.ij = (radiusA.ij + radiusB.ij - distanceAlongAxis.ij)^2
        #            radiusA.ij = halfExtentA.<i_tangent_1> * abs(axisDot.<i_tangent_2><j>) + halfExtentA.<i_tangent_2> * abs(axisDot.<i_tangent_1><j>)
        #            radiusB.ij = halfExtentB.<j_tangent_1> * abs(axisDot.<i><j_tangent_2>) + halfExtentB.<j_tangent_2> * abs(axisDot.<i><j_tangent_1>)
        #            distanceAlongAxis.ij = abs(signedDistanceAlongAxis.ij)
        #            signedDistanceAlongAxis.ij = offsetInA.<i_tangent_1> * axisDot.<i_tangent_2><j> - offsetInA<i_tangent_2> * axisDot<i_tangent_1><j>
        # (Note): In the number providers, I inverted signedDistanceAlongAxis so I can add the terms together instead of needing an additional "sub" operation.

        execute store result score #Physics.AxisLengthSquared.xx Physics run compute default float physics:collision_detection/axis_length_squared/xx
        execute if score #Physics.AxisLengthSquared.xx Physics matches 1024.. store result score #Physics.Overlap.Unnormalized.xx Physics run compute default float physics:collision_detection/overlap/unnormalized/xx
        execute if score #Physics.AxisLengthSquared.xx Physics matches 1024.. if score #Physics.Overlap.Unnormalized.xx Physics matches ..0 run return 0

        # A: x, b: y
        execute store result score #Physics.AxisLengthSquared.xy Physics run compute default float physics:collision_detection/axis_length_squared/xy
        execute if score #Physics.AxisLengthSquared.xy Physics matches 1024.. store result score #Physics.Overlap.Unnormalized.xy Physics run compute default float physics:collision_detection/overlap/unnormalized/xy
        execute if score #Physics.AxisLengthSquared.xy Physics matches 1024.. if score #Physics.Overlap.Unnormalized.xy Physics matches ..0 run return 0

        # A: x, b: z
        execute store result score #Physics.AxisLengthSquared.xz Physics run compute default float physics:collision_detection/axis_length_squared/xz
        execute if score #Physics.AxisLengthSquared.xz Physics matches 1024.. store result score #Physics.Overlap.Unnormalized.xz Physics run compute default float physics:collision_detection/overlap/unnormalized/xz
        execute if score #Physics.AxisLengthSquared.xz Physics matches 1024.. if score #Physics.Overlap.Unnormalized.xz Physics matches ..0 run return 0

        # A: y, b: x
        execute store result score #Physics.AxisLengthSquared.yx Physics run compute default float physics:collision_detection/axis_length_squared/yx
        execute if score #Physics.AxisLengthSquared.yx Physics matches 1024.. store result score #Physics.Overlap.Unnormalized.yx Physics run compute default float physics:collision_detection/overlap/unnormalized/yx
        execute if score #Physics.AxisLengthSquared.yx Physics matches 1024.. if score #Physics.Overlap.Unnormalized.yx Physics matches ..0 run return 0

        # A: y, b: y
        execute store result score #Physics.AxisLengthSquared.yy Physics run compute default float physics:collision_detection/axis_length_squared/yy
        execute if score #Physics.AxisLengthSquared.yy Physics matches 1024.. store result score #Physics.Overlap.Unnormalized.yy Physics run compute default float physics:collision_detection/overlap/unnormalized/yy
        execute if score #Physics.AxisLengthSquared.yy Physics matches 1024.. if score #Physics.Overlap.Unnormalized.yy Physics matches ..0 run return 0

        # A: y, b: z
        execute store result score #Physics.AxisLengthSquared.yz Physics run compute default float physics:collision_detection/axis_length_squared/yz
        execute if score #Physics.AxisLengthSquared.yz Physics matches 1024.. store result score #Physics.Overlap.Unnormalized.yz Physics run compute default float physics:collision_detection/overlap/unnormalized/yz
        execute if score #Physics.AxisLengthSquared.yz Physics matches 1024.. if score #Physics.Overlap.Unnormalized.yz Physics matches ..0 run return 0

        # A: z, b: x
        execute store result score #Physics.AxisLengthSquared.zx Physics run compute default float physics:collision_detection/axis_length_squared/zx
        execute if score #Physics.AxisLengthSquared.zx Physics matches 1024.. store result score #Physics.Overlap.Unnormalized.zx Physics run compute default float physics:collision_detection/overlap/unnormalized/zx
        execute if score #Physics.AxisLengthSquared.zx Physics matches 1024.. if score #Physics.Overlap.Unnormalized.zx Physics matches ..0 run return 0

        # A: z, b: y
        execute store result score #Physics.AxisLengthSquared.zy Physics run compute default float physics:collision_detection/axis_length_squared/zy
        execute if score #Physics.AxisLengthSquared.zy Physics matches 1024.. store result score #Physics.Overlap.Unnormalized.zy Physics run compute default float physics:collision_detection/overlap/unnormalized/zy
        execute if score #Physics.AxisLengthSquared.zy Physics matches 1024.. if score #Physics.Overlap.Unnormalized.zy Physics matches ..0 run return 0

        # A: z, b: z
        execute store result score #Physics.AxisLengthSquared.zz Physics run compute default float physics:collision_detection/axis_length_squared/zz
        execute if score #Physics.AxisLengthSquared.zz Physics matches 1024.. store result score #Physics.Overlap.Unnormalized.zz Physics run compute default float physics:collision_detection/overlap/unnormalized/zz
        execute if score #Physics.AxisLengthSquared.zz Physics matches 1024.. if score #Physics.Overlap.Unnormalized.zz Physics matches ..0 run return 0

# Passed: OBBs are intersecting

# Get previous tick's manifold for this object pair for carrying over the warm-start impulse
# (Note): See the stack information from "turn_into_physics_object".
# (Note): Due to the recursion in collision detection, objectA and objectB can swap. Therefore, I need a stable identity for storing the manifold data. The manifold entity rides the object with the smaller ObjectId, and has the larger Id as its Owner.
scoreboard players set #Physics.PersistedAxis.Index Physics -1

scoreboard players operation #Physics.ObjectB Physics.Object.Id = @s Physics.Object.Id
execute if score #Physics.ObjectA Physics.Object.Id < @s Physics.Object.Id in physics:void as @e[type=minecraft:area_effect_cloud,tag=Physics.Manifold,x=7.9,y=55.9,z=7.9,predicate=physics:collision_detection/manifold/same_object_b_id,dy=0,limit=1] run function physics:zprivate/simulation/collision_detection/get_previous_manifold_data
execute if score #Physics.ObjectA Physics.Object.Id > @s Physics.Object.Id in physics:void on passengers if entity @s[type=minecraft:armor_stand,tag=Physics.ObjectArmorStand] on passengers on origin on vehicle on vehicle on passengers if entity @s[tag=Physics.OldManifolds] on passengers if predicate physics:collision_detection/manifold/same_object_a_id run function physics:zprivate/simulation/collision_detection/get_previous_manifold_data

# Choose an axis and collect its data
# (Note): I choose the axis of the minimum overlap, but I apply a bias toward pointFace axes, as those are more stable. I also perform hysteresis by comparing the overlap with the previously chosen axis (from the old manifold), with a bias toward the persisted axis, again for stability reasons.
# (TODO): I hardcoded the bias in favor of pointFace contacts (axis indices 0-5) in the candidate_axis/index number provider. Could maybe be turned into a score (setting?). Its current value is 1.0 / 0.7.
# (TODO): Check if it's faster to inline the index extraction from physics:collision_detection/axis_data/min_overlap_squared_index/edge_edge and remove the command that stores the score. 1 less command, but up to 8 floor_mod more... hmm... BUT: It would only need to run in case an edge-edge collision occurs, which is rare.
# (TODO): In general, see if this can be optimized by getting a better balance of command count and duplicate calculations in number providers.
    # Candidate axis
    # (Note): To avoid having to run the "overlapSquared" calculation twice for every cross product axis (one pass to get MinOverlapSquared, one pass to get the index), I pack the index directly into the overlapSquared's bottom 4 bits, then recalculate it for one axis in case it's an edge-edge collision (in physics:collision_detection/axis_data/candidate_axis/overlap_squared).
    # (Note): The packing process scales up MinOverlapSquared.EdgeEdge by an additional factor of 2x, so I multiply it by 0.5 in physics:collision_detection/axis_data/candidate_axis/index. This is currently merged with the bias of (1.0 / 0.7)^2.
    execute store result score #Physics.MinOverlap.PointFace Physics run compute default integer physics:collision_detection/axis_data/min_overlap/point_face
    execute store result score #Physics.MinOverlapSquared.EdgeEdge Physics run compute default integer physics:collision_detection/axis_data/min_overlap_squared/edge_edge
    execute store result score #Physics.MinOverlapIndex.EdgeEdge Physics run compute default integer physics:collision_detection/axis_data/min_overlap_index/edge_edge

    execute store result score #Physics.CandidateAxis.Index Physics run compute default integer physics:collision_detection/axis_data/candidate_axis/index
    execute store result score #Physics.CandidateAxis.OverlapSquared Physics run compute default integer physics:collision_detection/axis_data/candidate_axis/overlap_squared

    # Persisted axis
    # (Note): As an optimization, I check if both indices are equal. If yes, simply copy over CandidateAxis.OverlapSquared.
    # (TODO): Maybe add an additional "conditional" layer to check between "is point face" vs "is edge edge", so that in case an edge-edge contact occurs, it doesn't have to check the 6 faces first? At the cost of an additional score access if it's pointFace, though, so maybe it's not worth it.
    # (TODO): Maybe use binary or ternary search to find the respective face or edge axis faster?
    execute unless score #Physics.PersistedAxis.Index Physics matches -1 store result score #Physics.PersistedAxis.OverlapSquared Physics run compute default float physics:collision_detection/axis_data/persisted_axis/overlap_squared

    # Choose between candidate and persisted
    # (Note): If PersistedAxis.Index is -1, it chooses the CandidateAxis.
    # (TODO): Maybe unhardcode the bias. Currently it's 0.9^2 (squared because it's applied to overlapSquared), so 0.81.
    # (TODO): Is it faster if I additionally add an early exit for "both indices are equal"?
    execute store result score #Physics.ChosenAxis.Index Physics run compute default integer physics:collision_detection/axis_data/chosen_axis/index

    # Chosen axis
    execute store result score #Physics.ChosenAxis.OverlapSquared Physics run compute default integer physics:collision_detection/axis_data/chosen_axis/overlap_squared
    execute store result score #Physics.ChosenAxis.SignedDistanceAlongAxis Physics run compute default integer physics:collision_detection/axis_data/chosen_axis/signed_distance_along_axis

