package com.a307.linkcare.common.util.transformation

import android.graphics.Bitmap
import android.graphics.Point
import androidx.core.graphics.alpha
import androidx.core.graphics.get
import coil.size.Size
import coil.transform.Transformation
import kotlin.math.max
import kotlin.math.min

class CropTransparentTransformation : Transformation {
    override val cacheKey: String = "crop_transparent"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        return input.cropTransparentBorder()
    }
}

/**
 * 비트맵의 투명한 테두리를 자동으로 잘라내는 확장 함수
 */
fun Bitmap.cropTransparentBorder(): Bitmap {
    var min = Point(width, height)
    var max = Point(-1, -1)

    for (y in 0 until height) {
        for (x in 0 until width) {
            if (this[x, y].alpha != 0) {
                min.x = min(x, min.x)
                min.y = min(y, min.y)
                max.x = max(x, max.x)
                max.y = max(y, max.y)
            }
        }
    }

    // 모든 픽셀이 투명한 경우 원본 반환
    if (max.x == -1 || max.y == -1) {
        return this
    }

    val newWidth = max.x - min.x + 1
    val newHeight = max.y - min.y + 1

    return Bitmap.createBitmap(this, min.x, min.y, newWidth, newHeight)
}
