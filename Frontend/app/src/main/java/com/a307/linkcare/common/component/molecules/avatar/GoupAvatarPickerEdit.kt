package com.a307.linkcare.common.component.molecules.avatar

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.a307.linkcare.common.theme.*
import com.a307.linkcare.R

@Composable
fun GroupAvatarPickerEdit(
    @DrawableRes defaultRes: Int = R.drawable.main_duck,
    onImagePicked: (Uri?) -> Unit,
    enabled: Boolean = true,            //  편집 가능 여부
    size: Dp = 96.dp,
    initialUri: Uri? = null,
    imageUrl: String? = null
) {
    var selectedImageUri by remember { mutableStateOf<Uri?>(initialUri) }

    // initialUri가 바뀌면 동기화
    LaunchedEffect(initialUri) { selectedImageUri = initialUri }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        selectedImageUri = uri
        onImagePicked(uri)
    }

    val getContent = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedImageUri = uri
        onImagePicked(uri)
    }

    fun launchPicker() {
        if (!enabled) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            photoPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } else {
            getContent.launch("image/*")
        }
    }

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape),
            color = Color(0xFFF2F3F6)
        ) {
            if (selectedImageUri != null) {
                // User picked a new image
                AsyncImage(
                    model = selectedImageUri,
                    contentDescription = "그룹 프로필",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (!imageUrl.isNullOrBlank()) {
                // Load from database URL
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "그룹 프로필",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    placeholder = painterResource(defaultRes),
                    error = painterResource(defaultRes)
                )
            } else {
                // Default image
                Image(
                    painter = painterResource(defaultRes),
                    contentDescription = "기본 그룹 프로필",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (enabled) {
            IconButton(
                onClick = { launchPicker() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(main)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "사진 추가",
                    tint = white
                )
            }
        }
    }
}
