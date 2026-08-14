package net.silicatyt.physicsengine.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.util.List;
import java.util.function.Function;

import static net.silicatyt.physicsengine.entity.PhysicsObject.MIN_SCALE;

public final class PhysicsObjectCodecs {
    private static <T> Codec<T> validatedDoubleList(
        int size,
        Function<List<Double>, DataResult<T>> decoder,
        Function<T, List<Double>> encoder
    ) {
        return Codec.DOUBLE.listOf()
            .comapFlatMap(
                values -> {
                    if (values.size() != size) return DataResult.error(() -> "Expected " + size + " components, got " + values.size());

                    for (double value : values) {
                        if (!Double.isFinite(value)) return DataResult.error(() -> "Components must be finite");
                    }

                    return decoder.apply(values);
                },
                encoder
            );
    }

    public static final Codec<Vector3d> VECTOR3D_CODEC = validatedDoubleList(
        3,
        values -> DataResult.success(new Vector3d(values.get(0), values.get(1), values.get(2))),
        vector -> List.of(vector.x, vector.y, vector.z)
    );

    public static final Codec<Vector3d> SCALE_CODEC = validatedDoubleList(
        3,
        values -> {
            double x = values.get(0);
            double y = values.get(1);
            double z = values.get(2);

            if (x < MIN_SCALE || y < MIN_SCALE || z < MIN_SCALE) return DataResult.error(() -> "Scale components must be >= " + MIN_SCALE);

            return DataResult.success(new Vector3d(x, y, z));
        },
        vector -> List.of(vector.x, vector.y, vector.z)
    );

    public static final Codec<Quaterniond> QUATERNIOND_CODEC = validatedDoubleList(
        4,
        values -> {
            double x = values.get(0);
            double y = values.get(1);
            double z = values.get(2);
            double w = values.get(3);

            double lengthSquared = x*x + y*y + z*z + w*w;

            if (!Double.isFinite(lengthSquared)) return DataResult.error(() -> "Quaternion length must be finite");

            if (lengthSquared < 1e-12) return DataResult.error(() -> "Quaternion must not be degenerate");

            Quaterniond q = new Quaterniond(x, y, z, w);
            q.normalize();

            return DataResult.success(q);
        },
        quaternion -> List.of(quaternion.x, quaternion.y, quaternion.z, quaternion.w)
    );
}
