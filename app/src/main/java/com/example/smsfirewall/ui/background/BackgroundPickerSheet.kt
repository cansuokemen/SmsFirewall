package com.example.smsfirewall.ui.background

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smsfirewall.R
import com.example.smsfirewall.data.background.BackgroundImageRepository
import com.example.smsfirewall.data.background.BackgroundPresets
import com.example.smsfirewall.data.background.BackgroundSpec
import kotlinx.coroutines.launch

private enum class BackgroundCategory { Gradient, Pattern, Solid, Image }

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
                    BackgroundCategory.Gradient -> GradientGrid(
                        selected = draft,
                        onSelect = { draft = it }
                    )
                    BackgroundCategory.Pattern -> PatternGrid(
                        selected = draft,
                        onSelect = { draft = it }
                    )
                    BackgroundCategory.Solid -> SolidGrid(
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
    is BackgroundSpec.Gradient    -> BackgroundCategory.Gradient
    is BackgroundSpec.Pattern     -> BackgroundCategory.Pattern
    is BackgroundSpec.SolidColor  -> BackgroundCategory.Solid
    is BackgroundSpec.CustomImage -> BackgroundCategory.Image
    BackgroundSpec.Default        -> BackgroundCategory.Gradient
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryChips(
    category: BackgroundCategory,
    onSelect: (BackgroundCategory) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CategoryChip(stringResource(R.string.background_cat_gradient), category == BackgroundCategory.Gradient) {
            onSelect(BackgroundCategory.Gradient)
        }
        CategoryChip(stringResource(R.string.background_cat_pattern), category == BackgroundCategory.Pattern) {
            onSelect(BackgroundCategory.Pattern)
        }
        CategoryChip(stringResource(R.string.background_cat_solid), category == BackgroundCategory.Solid) {
            onSelect(BackgroundCategory.Solid)
        }
        CategoryChip(stringResource(R.string.background_cat_image), category == BackgroundCategory.Image) {
            onSelect(BackgroundCategory.Image)
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
private fun GradientGrid(
    selected: BackgroundSpec,
    onSelect: (BackgroundSpec) -> Unit
) {
    val selectedId = (selected as? BackgroundSpec.Gradient)?.presetId
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(BackgroundPresets.gradients, key = { it.id }) { preset ->
            val brush = remember(preset.id) { BackgroundPresets.gradientBrush(preset.id) }
            val isSelected = preset.id == selectedId
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(brush)
                        .border(
                            width = if (isSelected) 2.dp else 0.5.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onSelect(BackgroundSpec.Gradient(preset.id)) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = Color.White
                        )
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
        items(BackgroundPresets.patterns, key = { it.id }) { preset ->
            val isSelected = preset.id == selectedId
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
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
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

@Composable
private fun SolidGrid(
    selected: BackgroundSpec,
    onSelect: (BackgroundSpec) -> Unit
) {
    val selectedArgb = (selected as? BackgroundSpec.SolidColor)?.argb
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(BackgroundPresets.solidPalette) { color ->
            val argb = color.toArgbLong()
            val isSelected = argb == selectedArgb
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(color)
                    .border(
                        width = if (isSelected) 2.dp else 0.5.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { onSelect(BackgroundSpec.SolidColor(argb)) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = Color.White
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
        AppBackground(spec = spec) { }
    }
}

private fun Color.toArgbLong(): Long {
    val a = (alpha * 255f).toInt() and 0xFF
    val r = (red * 255f).toInt() and 0xFF
    val g = (green * 255f).toInt() and 0xFF
    val b = (blue * 255f).toInt() and 0xFF
    return ((a.toLong() shl 24) or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong())
}
