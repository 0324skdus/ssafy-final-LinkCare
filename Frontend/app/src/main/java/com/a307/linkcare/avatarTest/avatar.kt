package com.a307.linkcare.avatarTest

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

// ---------- 모델 ----------
enum class Species(val id: String) { BEAR("bear"), DUCK("duck") }
enum class AccessoryType(val id: String) { HAT("hat"), GLASSES("glasses") }

data class AvatarSelection(
    val species: Species,
    val variant: Int,
    val hatId: String? = null,
    val glassesId: String? = null,
    val animation: String = "default"
)

// ---------- placements.json ----------
@Serializable
data class OffsetSpec(
    val offset: List<Int> = listOf(0, 0),
    val scale: Float = 1.0f,
    val layer: String? = null
)

@Serializable
data class SpeciesPlacement(
    val hat: Map<String, OffsetSpec> = emptyMap(),
    val glasses: Map<String, OffsetSpec> = emptyMap()
)

@Serializable
data class Placements(
    val zOrder: List<String> = listOf("hat_back", "base", "glasses", "hat_front"),
    val bear: SpeciesPlacement = SpeciesPlacement(),
    val duck: SpeciesPlacement = SpeciesPlacement()
)

object PlacementLoader {
    private val json = Json { ignoreUnknownKeys = true }

    fun load(context: Context, assetPath: String = "placements/placements.json"): Placements {
        val text = context.assets.open(assetPath).bufferedReader(Charsets.UTF_8).use { it.readText() }
        return json.decodeFromString(Placements.serializer(), text)
    }

    fun offsetFor(
        placements: Placements,
        species: Species,
        type: AccessoryType,
        id: String?
    ): Pair<Int, Int>? {
        if (id.isNullOrBlank()) return null
        val spec = when (species) {
            Species.BEAR -> when (type) {
                AccessoryType.HAT -> placements.bear.hat[id] ?: placements.bear.hat["*"]
                AccessoryType.GLASSES -> placements.bear.glasses[id] ?: placements.bear.glasses["*"]
            }
            Species.DUCK -> when (type) {
                AccessoryType.HAT -> placements.duck.hat[id] ?: placements.duck.hat["*"]
                AccessoryType.GLASSES -> placements.duck.glasses[id] ?: placements.duck.glasses["*"]
            }
        } ?: return null
        val x = spec.offset.getOrElse(0) { 0 }
        val y = spec.offset.getOrElse(1) { 0 }
        return x to y
    }

    fun specFor(
        placements: Placements,
        species: Species,
        type: AccessoryType,
        id: String?
    ): OffsetSpec? {
        if (id.isNullOrBlank()) return null
        return when (species) {
            Species.BEAR -> when (type) {
                AccessoryType.HAT     -> placements.bear.hat[id] ?: placements.bear.hat["*"]
                AccessoryType.GLASSES -> placements.bear.glasses[id] ?: placements.bear.glasses["*"]
            }
            Species.DUCK -> when (type) {
                AccessoryType.HAT     -> placements.duck.hat[id] ?: placements.duck.hat["*"]
                AccessoryType.GLASSES -> placements.duck.glasses[id] ?: placements.duck.glasses["*"]
            }
        }
    }
}

// ---------- 파일 존재 확인 ----------
object FileExistsChecker {
    fun exists(context: Context, assetPath: String): Boolean {
        return try {
            context.assets.open(assetPath).close()
            true
        } catch (_: Exception) {
            false
        }
    }
}

// ---------- assets 경로 ----------
object Paths {
    fun baseDir(sel: AvatarSelection) =
        "sprites/${sel.species.id}/v${sel.variant}"

    fun defaultBase(context: Context, sel: AvatarSelection): String {
        val defaultPath = "${baseDir(sel)}/default/base.png"
        val altPath = "${baseDir(sel)}/base.png"
        return if (FileExistsChecker.exists(context, defaultPath)) defaultPath else altPath
    }

    fun walkBaseDir(sel: AvatarSelection) =
        "sprites/${sel.species.id}/v${sel.variant}/walk"

    fun accessoryPath(sel: AvatarSelection, type: AccessoryType, id: String): String =
        "accessories/${sel.species.id}/${type.id}/$id.png"
}

// ---------- 이미지 로드 ----------
private fun loadBitmapsFromAssetsDir(context: Context, dir: String): List<ImageBitmap> {
    val am = context.assets
    val files = (am.list(dir) ?: emptyArray())
        .filter { it.endsWith(".png", ignoreCase = true) }
        .sorted()
    return files.mapNotNull { name ->
        try {
            am.open("$dir/$name").use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
        } catch (_: Exception) {
            null
        }
    }
}

private fun loadBitmapFromAssetsOrNull(context: Context, path: String): ImageBitmap? =
    try {
        context.assets.open(path).use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
    } catch (_: Throwable) {
        null
    }

// ---------- 합성 ----------
private fun composeLayers(base: ImageBitmap, overlays: List<Pair<ImageBitmap, Pair<Int, Int>>>): ImageBitmap {
    val b = base.asAndroidBitmap()
    val out = Bitmap.createBitmap(b.width, b.height, Bitmap.Config.ARGB_8888)
    val cvs = Canvas(out)
    cvs.drawBitmap(b, 0f, 0f, null)
    overlays.forEach { (img, off) ->
        cvs.drawBitmap(img.asAndroidBitmap(), off.first.toFloat(), off.second.toFloat(), null)
    }
    return out.asImageBitmap()
}

// ---------- 프레임 생성 ----------
private fun buildDefault(
    context: Context,
    sel: AvatarSelection,
    placements: Placements
): List<ImageBitmap> {
    val base = loadBitmapFromAssetsOrNull(context, Paths.defaultBase(context, sel)) ?: return emptyList()

    // spec 읽기
    val gSpec = sel.glassesId?.let { PlacementLoader.specFor(placements, sel.species, AccessoryType.GLASSES, it) }
    val hSpec = sel.hatId?.let     { PlacementLoader.specFor(placements, sel.species, AccessoryType.HAT, it) }

    // 비트맵 로드 + 스케일 적용 + 오프셋 준비
    val glasses = sel.glassesId?.let {
        val bmp0 = loadBitmapFromAssetsOrNull(context, Paths.accessoryPath(sel, AccessoryType.GLASSES, it)) ?: return@let null
        val bmp  = scaleBitmapIfNeeded(bmp0, gSpec?.scale ?: 1f)
        val off  = (gSpec?.offset?.getOrElse(0){0} ?: 0) to (gSpec?.offset?.getOrElse(1){0} ?: 0)
        val layerName = resolvedLayerName("glasses", gSpec)   // 레이어 오버라이드 반영
        layerName to (bmp to off)
    }

    val hat = sel.hatId?.let {
        val bmp0 = loadBitmapFromAssetsOrNull(context, Paths.accessoryPath(sel, AccessoryType.HAT, it)) ?: return@let null
        val bmp  = scaleBitmapIfNeeded(bmp0, hSpec?.scale ?: 1f)
        val off  = (hSpec?.offset?.getOrElse(0){0} ?: 0) to (hSpec?.offset?.getOrElse(1){0} ?: 0)
        val layerName = resolvedLayerName("hat", hSpec)       // 이 모자만 다른 레이어로
        layerName to (bmp to off)
    }

    val layerMap = buildMap {
        glasses?.let { put(it.first, it.second) }
        hat?.let { put(it.first, it.second) }
    }

    val composed = drawByZOrder(base, placements.zOrder, layerMap)
    return listOf(composed)
}

private fun buildWalk(
    context: Context,
    sel: AvatarSelection,
    placements: Placements
): List<ImageBitmap> {
    val baseFrames = loadBitmapsFromAssetsDir(context, Paths.walkBaseDir(sel))
    if (baseFrames.isEmpty()) return emptyList()

    // spec
    val gSpec = sel.glassesId?.let { PlacementLoader.specFor(placements, sel.species, AccessoryType.GLASSES, it) }
    val hSpec = sel.hatId?.let     { PlacementLoader.specFor(placements, sel.species, AccessoryType.HAT, it) }

    // 비트맵(프레임 전체에서 재사용) + 스케일
    val glassesBitmap = sel.glassesId?.let {
        loadBitmapFromAssetsOrNull(context, Paths.accessoryPath(sel, AccessoryType.GLASSES, it))
    }?.let { scaleBitmapIfNeeded(it, gSpec?.scale ?: 1f) }

    val hatBitmap = sel.hatId?.let {
        loadBitmapFromAssetsOrNull(context, Paths.accessoryPath(sel, AccessoryType.HAT, it))
    }?.let { scaleBitmapIfNeeded(it, hSpec?.scale ?: 1f) }

    val glassesOff = gSpec?.let { (it.offset.getOrElse(0){0} to it.offset.getOrElse(1){0}) }
    val hatOff     = hSpec?.let { (it.offset.getOrElse(0){0} to it.offset.getOrElse(1){0}) }
    val glassesLayer = resolvedLayerName("glasses", gSpec)
    val hatLayer = resolvedLayerName("hat", hSpec)
    return baseFrames.map { base ->
        val layerMap = buildMap {
            if (glassesBitmap != null && glassesOff != null) put("glasses", glassesBitmap to glassesOff)
            if (hatBitmap != null && hatOff != null)         put("hat",     hatBitmap to hatOff)
        }
        drawByZOrder(base, placements.zOrder, layerMap)
    }
}

private fun buildFrames(
    context: Context,
    sel: AvatarSelection,
    placements: Placements
): List<ImageBitmap> = when (sel.animation) {
    "walk" -> buildWalk(context, sel, placements)
    else -> buildDefault(context, sel, placements)
}

// ---------- Composable ----------
@Composable
fun AvatarAnimator(
    modifier: Modifier = Modifier,
    selection: AvatarSelection,
    fps: Int = if (selection.animation == "walk") 12 else 1,
    loop: Boolean = true
) {
    val ctx = LocalContext.current
    val placements = remember { PlacementLoader.load(ctx) }

    val frames by remember(selection, placements) {
        mutableStateOf(buildFrames(ctx, selection, placements))
    }

    var idx by remember { mutableStateOf(0) }
    LaunchedEffect(frames, fps, loop) {
        if (frames.isEmpty()) return@LaunchedEffect
        idx = 0
        val interval = (1000f / fps).toLong().coerceAtLeast(16L)
        while (true) {
            delay(interval)
            idx++
            if (idx >= frames.size) {
                if (loop) idx = 0 else break
            }
        }
    }

    if (frames.isNotEmpty()) {
        Image(
            bitmap = frames[idx.coerceIn(0, frames.lastIndex)],
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    }
}

// scale 적용
private fun scaleBitmapIfNeeded(src: ImageBitmap, scale: Float): ImageBitmap {
    if (scale == 1f) return src
    val bmp = src.asAndroidBitmap()
    val w = (bmp.width * scale).roundToInt().coerceAtLeast(1)
    val h = (bmp.height * scale).roundToInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bmp, w, h, true).asImageBitmap()
}

// zOrder 순서대로 그리기: "base"는 베이스 프레임, 나머지는 액세서리 키로 매핑
private fun drawByZOrder(
    base: ImageBitmap,
    zOrder: List<String>,
    layers: Map<String, Pair<ImageBitmap, Pair<Int, Int>>>
): ImageBitmap {
    val b = base.asAndroidBitmap()
    val out = Bitmap.createBitmap(b.width, b.height, Bitmap.Config.ARGB_8888)
    val cvs = Canvas(out)

    zOrder.forEach { layer ->
        when (layer) {
            "base" -> cvs.drawBitmap(b, 0f, 0f, null)
            else -> {
                // hat_front / hat_back → hat 으로 fallback
                val key = when (layer) {
                    "hat_front", "hat_back" -> "hat"
                    else -> layer
                }
                layers[key]?.let { (img, off) ->
                    cvs.drawBitmap(img.asAndroidBitmap(), off.first.toFloat(), off.second.toFloat(), null)
                }
            }
        }
    }
    return out.asImageBitmap()
}


private fun resolvedLayerName(
    default: String,           // "hat" or "glasses"
    spec: OffsetSpec?
): String = spec?.layer ?: default