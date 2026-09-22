# Required scaling:
#    RelativePos: 2^16
#    Direction: 2^14
#    Strength: 1

# Linear component
# (Formula): LinearVelocity += Impulse * InverseMass
# (Note): The impulse is Direction * Strength. I intentionally don't pre-calculate their product because it's faster, and it gives the user more control.
execute store result score @s Physics.Object.LinearVelocity.x run compute default float physics:apply_impulse/linear/x
execute store result score @s Physics.Object.LinearVelocity.y run compute default float physics:apply_impulse/linear/y
execute store result score @s Physics.Object.LinearVelocity.z run compute default float physics:apply_impulse/linear/z

# Angular component
# (Formula): Torque = RelativeContactPos x Impulse
# (Formula): AngularVelocity += InverseInertiaTensorWorld * Torque
# (Note): As stated in integration, I don't store the inverseMass as part of the inverse inertia tensor for scaling reasons. So I multiply by inverseMass here.
# (TODO): Maybe it's worth it to add alternate calculation paths for isotropic objects, or ones that share multiple dimension scales?
# (TODO): Benchmark whether it's faster to do everything in a single '/compute', or to store the torque (but it would need to be downscaled a lot to fit into a score, and storage accesses might not be worth it performance-wise?).
# (TODO): Once resolution is done, store the AngularImpulseFactor as object scores and re-use them here if there was a contact generated for this object in the current tick. Although... that requires the impulse to be in contact space, which probably won't work.
execute store result score @s Physics.Object.AngularVelocity.x run compute default float physics:apply_impulse/angular/x
execute store result score @s Physics.Object.AngularVelocity.y run compute default float physics:apply_impulse/angular/y
execute store result score @s Physics.Object.AngularVelocity.z run compute default float physics:apply_impulse/angular/z
