package net.silicatyt.physicsengine.data;

import org.joml.Vector3dc;

public record ClipPoint(double tangentProjectionA, double tangentProjectionB, double normalProjection, Vector3dc pos, int id) {}
