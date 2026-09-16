# (Note): You need to set the storage `physics:object apply_torque` (as a float list, e.g. [0.5f,0f,0f]) before calling this function as the physics object.

# (Formula): AngularVelocityFromTorque = AngularVelocityFromTorque + InverseInertiaTensorWorld * Torque * DeltaTime
# (Note): Because inverseMass isn't included in the inertia I store (for scaling reasons: not enough bits), I additionally multiply each entry by inverseMass here.
# (Note): I round for extra precision.
# (TODO): Add early outs if specific components of the torque are 0, or if the object is isotropic.
# (TODO): Maybe accumulate the angular acceleration instead of directly incrementing the AngularVelocityFromTorque. Basically get rid of the delta time multiplication here and move it to integration. Would decrease precision while increasing range.
execute store result score @s Physics.Object.AngularVelocityFromTorque.x run compute default float physics:object/apply_torque/x
execute store result score @s Physics.Object.AngularVelocityFromTorque.y run compute default float physics:object/apply_torque/y
execute store result score @s Physics.Object.AngularVelocityFromTorque.z run compute default float physics:object/apply_torque/z
