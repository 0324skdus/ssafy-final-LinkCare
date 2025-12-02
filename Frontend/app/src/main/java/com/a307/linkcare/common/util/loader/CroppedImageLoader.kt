package com.a307.linkcare.common.util.loader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import com.a307.linkcare.common.util.transformation.cropTransparentBorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// 전역 캐시
private val croppedCache = mutableMapOf<Int, BitmapPainter>()

// 전역 사용 함수
@Composable
fun painterResourceCropped(@DrawableRes resId: Int): BitmapPainter {
    val context = LocalContext.current
    return remember(resId) {
        croppedCache[resId] ?: run {
            val bmp = BitmapFactory.decodeResource(context.resources, resId)
            val cropped = bmp.cropTransparentBorder()
            val painter = BitmapPainter(cropped.asImageBitmap())
            croppedCache[resId] = painter
            painter
        }
    }
}

// suspend 버전 (IO 스레드에서 호출 가능)
suspend fun loadCroppedPainterIO(context: Context, @DrawableRes resId: Int): BitmapPainter =
    withContext(Dispatchers.IO) {
        croppedCache[resId] ?: run {
            val bmp = BitmapFactory.decodeResource(context.resources, resId)
            val cropped = bmp.cropTransparentBorder()
            val painter = BitmapPainter(cropped.asImageBitmap())
            croppedCache[resId] = painter
            painter
        }
    }

// 공용 크롭 함수
fun Bitmap.cropTransparentBorder(alphaThreshold: Int = 0): Bitmap {
    val w = width
    val h = height
    val pixels = IntArray(w * h)
    getPixels(pixels, 0, w, 0, 0, w, h)

    var minX = w
    var minY = h
    var maxX = -1
    var maxY = -1

    for (y in 0 until h) {
        val rowStart = y * w
        for (x in 0 until w) {
            val a = (pixels[rowStart + x] ushr 24) and 0xFF
            if (a > alphaThreshold) {
                if (x < minX) minX = x
                if (y < minY) minY = y
                if (x > maxX) maxX = x
                if (y > maxY) maxY = y
            }
        }
    }
    if (maxX < minX || maxY < minY) return this
    return Bitmap.createBitmap(this, minX, minY, (maxX - minX + 1), (maxY - minY + 1))
}
