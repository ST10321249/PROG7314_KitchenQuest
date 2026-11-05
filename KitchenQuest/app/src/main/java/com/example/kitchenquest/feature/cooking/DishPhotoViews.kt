package com.example.kitchenquest.feature.cooking

import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.kitchenquest.R
import com.example.kitchenquest.data.AppRepositories
import com.example.kitchenquest.data.photos.PhotoDecoding
import com.example.kitchenquest.ui.theme.KitchenQuestDimens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private object DishPhotoCache {
    private val cache = LruCache<String, ImageBitmap>(24)
    fun get(key: String): ImageBitmap? = cache.get(key)
    fun put(key: String, image: ImageBitmap) {
        cache.put(key, image)
    }
}

@Composable
fun SavedDishPhoto(
    photoFileId: String,
    modifier: Modifier = Modifier,
    height: Dp = 160.dp
) {
    val photos = remember { AppRepositories.photos() }
    val image by produceState(initialValue = DishPhotoCache.get(photoFileId), photoFileId) {
        if (value != null) return@produceState
        value = withContext(Dispatchers.IO) {
            photos.download(photoFileId).getOrNull()
                ?.let { PhotoDecoding.decodeBytes(it, 900) }
                ?.asImageBitmap()
                ?.also { DishPhotoCache.put(photoFileId, it) }
        }
    }
    DishPhotoBox(image = image, modifier = modifier, height = height)
}

@Composable
fun ChosenDishPhoto(
    uri: String,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp
) {
    val context = LocalContext.current
    val image by produceState(initialValue = DishPhotoCache.get(uri), uri) {
        if (value != null) return@produceState
        value = withContext(Dispatchers.IO) {
            runCatching { PhotoDecoding.decodeUri(context, Uri.parse(uri), 900) }.getOrNull()
                ?.asImageBitmap()
                ?.also { DishPhotoCache.put(uri, it) }
        }
    }
    DishPhotoBox(image = image, modifier = modifier, height = height)
}

@Composable
private fun DishPhotoBox(
    image: ImageBitmap?,
    modifier: Modifier,
    height: Dp
) {
    val shape = RoundedCornerShape(KitchenQuestDimens.MediumCorner)
    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = stringResource(R.string.dish_photo_description),
            contentScale = ContentScale.Crop,
            modifier = modifier
                .fillMaxWidth()
                .height(height)
                .clip(shape)
        )
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(height)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    }
}