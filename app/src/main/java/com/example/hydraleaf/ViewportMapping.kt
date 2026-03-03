package com.example.hydraleaf

import android.graphics.PointF

interface ViewportMapping {
    val scale: Float
    val offsetX: Float
    val offsetY: Float
}

val IdentityViewport: ViewportMapping = object : ViewportMapping {
    override val scale: Float = 1.0f
    override val offsetX: Float = 0.0f
    override val offsetY: Float = 0.0f
}

fun logicalToScreen(logical: PointF, viewportMapping: ViewportMapping): PointF {
    return PointF(
        (logical.x * viewportMapping.scale) + viewportMapping.offsetX,
        (logical.y * viewportMapping.scale) + viewportMapping.offsetY
    )
}
