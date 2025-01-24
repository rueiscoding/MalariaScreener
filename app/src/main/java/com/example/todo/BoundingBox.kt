package com.example.todo

/**
 * Describes the bounding box returned by the model.
 */
data class BoundingBox(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val conf: Float,
    val cls: Int
)
