package com.example.smsfirewall.ui.background

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smsfirewall.R
import com.example.smsfirewall.data.background.BackgroundImageRepository
import com.example.smsfirewall.data.background.BackgroundPresets
import com.example.smsfirewall.data.background.BackgroundSpec
import com.example.smsfirewall.ui.animation.MorphingCheckmark
import kotlinx.coroutines.launch

private enum class BackgroundCategory { Pattern, Image }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackgroundPickerSheet(
    initial: BackgroundSpec,
    imageRepository: BackgroundImageRepository,
    onDismiss: () -> Unit,
    onApply: (BackgroundSpec) -> Unit,
    onReset: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var category by remember { mutableStateOf(initialCategoryFor(initial)) }
    var draft by remember { mutableStateOf(initial) }

    val context = LocalContext.current
    val pickMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val name = imageRepository.importImage(uri)
                if (name != null) {
                    draft = BackgroundSpec.CustomImage(name)
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text  = stringResource(R.string.background_picker_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))

            CategoryChips(category = category, onSelect = { category = it })
            Spacer(Modifier.height(16.dp))

            Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                when (category) {
                    BackgroundCategory.Pattern -> PatternGrid(
                        selected = draft,
                        onSelect = { draft = it }
                    )
                    BackgroundCategory.Image -> ImagePicker(
                        currentDraft = draft,
                        onPickClick = {
                            pickMedia.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            PreviewStrip(spec = draft)
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        draft = BackgroundSpec.Default
                        onReset()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(R.string.background_reset))
                }
                Button(
                    onClick = {
                        onApply(draft)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(R.string.background_apply))
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun initialCategoryFor(spec: BackgroundSpec): BackgroundCategory = when (spec) {
    is BackgroundSpec.Pattern     -> BackgroundCategory.Pattern
    is BackgroundSpec.CustomImage -> BackgroundCategory.Image
    else                          -> BackgroundCategory.Pattern
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryChips(
    category: BackgroundCategory,
    onSelect: (BackgroundCategory) -> Unit
) {
    val entries = BackgroundCategory.entries
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val totalWidth = maxWidth
        val gap        = 8.dp
        val chipWidth  = (totalWidth - gap * (entries.size - 1)) / entries.size
        val density    = LocalDensity.current
        val targetIndex = entries.indexOf(category)
        val indicatorOffset = remember { Animatable(0f) }
        LaunchedEffect(targetIndex) {
            val targetPx = with(density) { ((chipWidth + gap) * targetIndex).toPx() }
            indicatorOffset.animateTo(
                targetValue   = targetPx,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
            )
        }
        Column {
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                entries.forEach { cat ->
                    val label = when (cat) {
                        BackgroundCategory.Pattern -> R.string.background_cat_pattern
                        BackgroundCategory.Image   -> R.string.background_cat_image
                    }
                    Box(modifier = Modifier.size(width = chipWidth, height = 36.dp)) {
                        CategoryChip(stringResource(label), category == cat) { onSelect(cat) }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Box(modifier = Modifier.fillMaxWidth().height(3.dp)) {
                Box(
                    modifier = Modifier
                        .size(width = chipWidth, height = 3.dp)
                        .graphicsLayer { translationX = indicatorOffset.value }
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
        )
    )
}

@Composable
private fun StaggeredEntry(index: Int, content: @Composable () -> Unit) {
    val scale = remember { Animatable(0.6f) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index.coerceAtMost(8) * 45L)
        launch { scale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) }
        launch { alpha.animateTo(1f, animationSpec = tween(280)) }
    }
    Box(
        modifier = Modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
            this.alpha = alpha.value
        }
    ) {
        content()
    }
}

@Composable
private fun PatternGrid(
    selected: BackgroundSpec,
    onSelect: (BackgroundSpec) -> Unit
) {
    val selectedId = (selected as? BackgroundSpec.Pattern)?.patternId
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(BackgroundPresets.patterns, key = { _, item -> item.id }) { index, preset ->
            val isSelected = preset.id == selectedId
            StaggeredEntry(index = index) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = if (isSelected) 2.dp else 0.5.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onSelect(BackgroundSpec.Pattern(preset.id)) }
                    ) {
                        AppBackground(spec = BackgroundSpec.Pattern(preset.id)) { }
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                        ) {
                            MorphingCheckmark(visible = isSelected, size = 24.dp)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = preset.displayName,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun ImagePicker(currentDraft: BackgroundSpec, onPickClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    RoundedCornerShape(16.dp)
                )
                .clickable(onClick = onPickClick),
            contentAlignment = Alignment.Center
        ) {
            if (currentDraft is BackgroundSpec.CustomImage) {
                AppBackground(spec = currentDraft) { }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text  = stringResource(R.string.background_pick_image_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onPickClick) {
            Icon(imageVector = Icons.Outlined.AddPhotoAlternate, contentDescription = null)
            Spacer(Modifier.size(6.dp))
            Text(stringResource(R.string.background_pick_image))
        }
    }
}

@Composable
private fun PreviewStrip(spec: BackgroundSpec) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(
                0.5.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                RoundedCornerShape(16.dp)
            )
    ) {
        Crossfade(
            targetState   = spec,
            animationSpec = tween(durationMillis = 320),
            label         = "previewCrossfade",
            modifier      = Modifier.fillMaxSize()
        ) { current ->
            AppBackground(spec = current) { }
        }
    }
}

private fun Color.toArgbLong(): Long {
    val a = (alpha * 255f).toInt() and 0xFF
    val r = (red * 255f).toInt() and 0xFF
    val g = (green * 255f).toInt() and 0xFF
    val b = (blue * 255f).toInt() and 0xFF
    return ((a.toLong() shl 24) or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong())
}
