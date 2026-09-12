# (Note): You need to set the storage `physics:object apply_force` (as a float list, e.g. [0.5f,0f,0f]) before calling this function as the physics object.

# (Formula): LinearVelocityFromAcceleration = LinearVelocityFromAcceleration + (Force * InverseMass * DeltaTime)
# (Note): I round for extra precision.
# (TODO): Maybe accumulate the linear acceleration instead of directly incrementing the LinearVelocityFromAcceleration. Basically get rid of the delta time multiplication here and move it to integration. Would decrease precision while increasing range.
execute store result score @s Physics.Object.LinearVelocityFromAcceleration.x run compute default float physics:object/apply_force/x
execute store result score @s Physics.Object.LinearVelocityFromAcceleration.y run compute default float physics:object/apply_force/y
execute store result score @s Physics.Object.LinearVelocityFromAcceleration.z run compute default float physics:object/apply_force/z
