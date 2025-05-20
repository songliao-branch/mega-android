package mega.privacy.android.app.presentation.photos.albums.photosselection

import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR
import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.albums.photosselection.AlbumPhotosSelectionViewModel.Companion.MAX_SELECTION_NUM
import mega.privacy.android.app.presentation.photos.model.UIPhoto
import mega.privacy.android.app.presentation.photos.model.ZoomLevel
import mega.privacy.android.app.presentation.photos.timeline.model.TimelinePhotosSource
import mega.privacy.android.app.presentation.photos.timeline.model.TimelinePhotosSource.ALL_PHOTOS
import mega.privacy.android.app.presentation.photos.timeline.model.TimelinePhotosSource.CAMERA_UPLOAD
import mega.privacy.android.app.presentation.photos.timeline.model.TimelinePhotosSource.CLOUD_DRIVE
import mega.privacy.android.app.presentation.photos.view.PhotosGridView
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.legacy.core.ui.controls.dialogs.MegaDialog
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.theme.black
import mega.privacy.android.shared.original.core.ui.theme.grey_alpha_054
import mega.privacy.android.shared.original.core.ui.theme.grey_alpha_087
import mega.privacy.android.shared.original.core.ui.theme.accent_900
import mega.privacy.android.shared.original.core.ui.theme.extensions.accent_900_accent_050
import mega.privacy.android.shared.original.core.ui.theme.white
import mega.privacy.android.shared.original.core.ui.theme.white_alpha_054
import mega.privacy.android.shared.original.core.ui.theme.white_alpha_087
import mega.privacy.mobile.analytics.event.AddItemsToExistingAlbumFABEvent
import mega.privacy.mobile.analytics.event.AddItemsToNewAlbumFABEvent
import mega.privacy.mobile.analytics.event.AlbumPhotosSelectionAllLocationsButtonEvent
import mega.privacy.mobile.analytics.event.AlbumPhotosSelectionCameraUploadsButtonEvent
import mega.privacy.mobile.analytics.event.AlbumPhotosSelectionCloudDriveButtonEvent
import mega.privacy.mobile.analytics.event.AlbumPhotosSelectionFilterMenuToolbarEvent
import mega.privacy.mobile.analytics.event.AlbumPhotosSelectionScreenEvent


@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun AlbumPhotosSelectionScreen(
    viewModel: AlbumPhotosSelectionViewModel = viewModel(),
    onBackClicked: () -> Unit = {},
    onCompletion: (albumId: AlbumId, numCommittedPhotos: Int) -> Unit = { _, _ -> },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isLight = MaterialTheme.colors.isLight
    val lazyGridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    var showSelectLocationDialog by rememberSaveable { mutableStateOf(false) }
    var showMoreMenu by rememberSaveable { mutableStateOf(false) }
    var showMaxSelectionDialog by rememberSaveable { mutableStateOf(false) }

    if (state.isInvalidAlbum) onBackClicked()

    LaunchedEffect(Unit) {
        Analytics.tracker.trackEvent(AlbumPhotosSelectionScreenEvent)
    }

    HandleSelectLocationDialog(
        showSelectLocationDialog = showSelectLocationDialog,
        selectedLocation = state.selectedLocation,
        onLocationSelected = { location ->
            showSelectLocationDialog = false
            coroutineScope.launch { lazyGridState.scrollToItem(0) }

            viewModel.updateLocation(location)
            viewModel.filterPhotos()
        },
        onDialogDismissed = {
            showSelectLocationDialog = false
        },
    )
    HandleMaxSelectionDialog(
        showMaxSelectionDialog = showMaxSelectionDialog,
        onDialogDismissed = {
            showMaxSelectionDialog = false
        }
    )

    HandleAddPhotosCompletion(
        album = state.album,
        isSelectionCompleted = state.isSelectionCompleted,
        numCommittedPhotos = state.numCommittedPhotos,
        onCompletion = onCompletion,
    )

    MegaScaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            AlbumPhotosSelectionHeader(
                album = state.album,
                selectedLocation = state.selectedLocation,
                isLocationDetermined = state.isLocationDetermined,
                numSelectedPhotos = state.selectedPhotoIds.size,
                showFilterMenu = state.showFilterMenu,
                showMoreMenu = showMoreMenu,
                showSelectAllMenu = false,
                onBackClicked = {
                    if (state.selectedPhotoIds.isEmpty()) {
                        onBackClicked()
                    } else {
                        viewModel.clearSelection()
                    }
                },
                onFilterClicked = {
                    Analytics.tracker.trackEvent(AlbumPhotosSelectionFilterMenuToolbarEvent)
                    showSelectLocationDialog = true
                },
                onMoreClicked = {
                    showMoreMenu = true
                },
                onMoreDismissed = {
                    showMoreMenu = false
                },
                onSelectAllClicked = {
                    showMoreMenu = false
                    viewModel.selectAllPhotos()
                },
                onClearSelectionClicked = {
                    showMoreMenu = false
                    viewModel.clearSelection()
                },
            )
        },
        floatingActionButton = {
            val album = state.album
            val photos = state.photos
            val albumFlow = state.albumFlow
            val selectedPhotoIds = state.selectedPhotoIds

            if (album != null && photos.isNotEmpty() && (albumFlow == AlbumFlow.Creation || albumFlow == AlbumFlow.Addition && selectedPhotoIds.isNotEmpty())) {
                FloatingActionButton(
                    onClick = {
                        when (albumFlow) {
                            AlbumFlow.Creation -> Analytics.tracker.trackEvent(
                                AddItemsToNewAlbumFABEvent
                            )

                            AlbumFlow.Addition -> Analytics.tracker.trackEvent(
                                AddItemsToExistingAlbumFABEvent
                            )
                        }

                        viewModel.addPhotos(
                            album = album,
                            selectedPhotoIds = state.selectedPhotoIds,
                        )
                    },
                    backgroundColor = MaterialTheme.colors.accent_900_accent_050,
                ) {
                    Icon(
                        painter = painterResource(id = iconPackR.drawable.ic_check_medium_regular_outline),
                        contentDescription = null,
                        tint = white.takeIf { isLight } ?: black,
                    )
                }
            }
        },
        content = {
            if (state.photos.isEmpty() && !state.isLoading) {
                EmptyStateContent()
            } else {
                AlbumPhotosSelectionContent(
                    lazyGridState = lazyGridState,
                    uiPhotos = state.uiPhotos,
                    selectedPhotoIds = state.selectedPhotoIds,
                    shouldApplySensitiveMode = state.hiddenNodeEnabled
                            && state.accountType?.isPaid == true
                            && !state.isBusinessAccountExpired,
                    onPhotoDownload = viewModel::downloadPhoto,
                    onPhotoSelection = { photo ->
                        if (photo.id in state.selectedPhotoIds) {
                            viewModel.unselectPhoto(photo)
                        } else {
                            if (state.selectedPhotoIds.size < MAX_SELECTION_NUM) {
                                viewModel.selectPhoto(photo)
                            } else {
                                showMaxSelectionDialog = true
                            }
                        }
                    },
                )
            }
        },
    )
}

@Composable
private fun AlbumPhotosSelectionHeader(
    album: Album.UserAlbum?,
    selectedLocation: TimelinePhotosSource,
    isLocationDetermined: Boolean,
    numSelectedPhotos: Int,
    showFilterMenu: Boolean,
    showMoreMenu: Boolean,
    showSelectAllMenu: Boolean,
    onBackClicked: () -> Unit,
    onFilterClicked: () -> Unit,
    onMoreClicked: () -> Unit,
    onMoreDismissed: () -> Unit,
    onSelectAllClicked: () -> Unit,
    onClearSelectionClicked: () -> Unit,
) {
    val isLight = MaterialTheme.colors.isLight

    TopAppBar(
        title = {
            Column {
                if (numSelectedPhotos > 0) {
                    Text(
                        text = "$numSelectedPhotos",
                        color = MaterialTheme.colors.accent_900_accent_050,
                        fontWeight = FontWeight.W500,
                        style = MaterialTheme.typography.subtitle1,
                    )
                } else {
                    Text(
                        text = stringResource(
                            id = R.string.photos_album_selection_title,
                            album?.title.orEmpty(),
                        ),
                        fontWeight = FontWeight.W500,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = MaterialTheme.typography.subtitle1,
                    )

                    if (isLocationDetermined) {
                        Text(
                            text = selectedLocation.text(),
                            color = grey_alpha_054.takeIf { isLight } ?: white_alpha_054,
                            style = MaterialTheme.typography.caption,
                        )
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClicked) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back_white),
                    contentDescription = null,
                    tint = MaterialTheme.colors.accent_900_accent_050,
                )
            }
        },
        actions = {
            if (showFilterMenu) {
                IconButton(onClick = onFilterClicked) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_filter_light),
                        contentDescription = null,
                        tint = accent_900.takeIf {
                            numSelectedPhotos > 0
                        } ?: (black.takeIf { isLight } ?: white),
                    )
                }
            }

            if (numSelectedPhotos > 0) {
                IconButton(onClick = onMoreClicked) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_dots_vertical_white),
                        contentDescription = null,
                        tint = accent_900,
                    )
                }

                DropdownMenu(expanded = showMoreMenu, onDismissRequest = onMoreDismissed) {
                    if (showSelectAllMenu) {
                        DropdownMenuItem(onClick = onSelectAllClicked) {
                            Text(text = stringResource(id = R.string.action_select_all))
                        }
                    }
                    DropdownMenuItem(onClick = onClearSelectionClicked) {
                        Text(text = stringResource(id = R.string.action_unselect_all))
                    }
                }
            }
        },
        elevation = 0.dp,
    )
}

@Composable
private fun AlbumPhotosSelectionContent(
    lazyGridState: LazyGridState,
    uiPhotos: List<UIPhoto>,
    shouldApplySensitiveMode: Boolean,
    selectedPhotoIds: Set<Long>,
    onPhotoDownload: PhotoDownload,
    onPhotoSelection: (Photo) -> Unit,
) {
    PhotosGridView(
        currentZoomLevel = ZoomLevel.Grid_3,
        photoDownland = { _, photo, callback ->
            onPhotoDownload(photo, callback)
        },
        lazyGridState = lazyGridState,
        onClick = onPhotoSelection,
        onLongPress = onPhotoSelection,
        selectedPhotoIds = selectedPhotoIds,
        uiPhotoList = uiPhotos,
        isBlurUnselectItem = selectedPhotoIds.size >= MAX_SELECTION_NUM,
        shouldApplySensitiveMode = shouldApplySensitiveMode,
    )
}

@Composable
private fun HandleSelectLocationDialog(
    showSelectLocationDialog: Boolean,
    selectedLocation: TimelinePhotosSource,
    onLocationSelected: (TimelinePhotosSource) -> Unit,
    onDialogDismissed: () -> Unit,
) {
    if (showSelectLocationDialog) {
        SelectLocationDialog(
            selectedLocation = selectedLocation,
            onLocationSelected = onLocationSelected,
            onDialogDismissed = onDialogDismissed,
        )
    }
}

@Composable
private fun SelectLocationDialog(
    selectedLocation: TimelinePhotosSource,
    onLocationSelected: (TimelinePhotosSource) -> Unit,
    onDialogDismissed: () -> Unit,
) {
    val isLight = MaterialTheme.colors.isLight

    Dialog(onDismissRequest = onDialogDismissed) {
        Surface(shape = RoundedCornerShape(4.dp), elevation = 24.dp) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.size(12.dp))
                TimelinePhotosSource.values().forEach { location ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                locationFilterAnalytics(location)
                                if (location == selectedLocation) {
                                    onDialogDismissed()
                                } else {
                                    onLocationSelected(location)
                                }
                            }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = CenterVertically,
                    ) {
                        RadioButton(
                            selected = location == selectedLocation,
                            onClick = null,
                        )
                        Spacer(modifier = Modifier.size(16.dp))
                        Text(
                            text = location.text(),
                            color = if (isLight) {
                                grey_alpha_087.takeIf {
                                    location == selectedLocation
                                } ?: grey_alpha_054
                            } else {
                                white_alpha_087.takeIf {
                                    location == selectedLocation
                                } ?: white_alpha_054
                            },
                            fontWeight = FontWeight.W400,
                            style = MaterialTheme.typography.subtitle2,
                        )
                    }
                    Spacer(modifier = Modifier.size(4.dp))
                }
                Text(
                    text = stringResource(id = sharedR.string.general_dialog_cancel_button),
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 8.dp, end = 16.dp, bottom = 16.dp)
                        .clickable { onDialogDismissed() },
                    color = accent_900,
                    fontWeight = FontWeight.W500,
                    style = MaterialTheme.typography.button,
                )
            }
        }
    }
}

@Composable
private fun HandleMaxSelectionDialog(
    showMaxSelectionDialog: Boolean,
    onDialogDismissed: () -> Unit,
) {
    if (showMaxSelectionDialog) {
        MaxSelectionDialog(
            onDialogDismissed = onDialogDismissed,
        )
    }
}

@Composable
private fun MaxSelectionDialog(
    onDialogDismissed: () -> Unit = {},
) {
    MegaDialog(
        body = {
            Text(
                text = stringResource(id = R.string.photos_album_selection_dialog_body),
                color = grey_alpha_054.takeIf { MaterialTheme.colors.isLight } ?: white_alpha_054,
            )
        },
        onDismissRequest = onDialogDismissed,
        titleString = stringResource(id = R.string.photos_album_selection_dialog_title),
        fontWeight = FontWeight.W500,
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDialogDismissed,
            ) {
                Text(
                    text = stringResource(id = R.string.general_ok),
                    style = MaterialTheme.typography.button,
                    color = if (!MaterialTheme.colors.isLight) colorResource(id = R.color.accent_050) else colorResource(
                        id = R.color.accent_900
                    )
                )
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
    )
}

@Composable
private fun HandleAddPhotosCompletion(
    album: Album.UserAlbum?,
    isSelectionCompleted: Boolean,
    numCommittedPhotos: Int,
    onCompletion: (albumId: AlbumId, numCommittedPhotos: Int) -> Unit,
) {
    val albumId = album?.id
    if (albumId != null && isSelectionCompleted) {
        onCompletion(album.id, numCommittedPhotos)
    }
}

@Composable
private fun TimelinePhotosSource.text(): String = when (this) {
    ALL_PHOTOS -> stringResource(id = R.string.filter_button_all_source)
    CLOUD_DRIVE -> stringResource(id = R.string.filter_button_cd_only)
    CAMERA_UPLOAD -> stringResource(id = R.string.photos_filter_camera_uploads)
}

private fun locationFilterAnalytics(location: TimelinePhotosSource) =
    Analytics.tracker.trackEvent(
        when (location) {
            ALL_PHOTOS -> AlbumPhotosSelectionAllLocationsButtonEvent
            CLOUD_DRIVE -> AlbumPhotosSelectionCloudDriveButtonEvent
            CAMERA_UPLOAD -> AlbumPhotosSelectionCameraUploadsButtonEvent
        }
    )

@Composable
private fun EmptyStateContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_no_images),
            contentDescription = "Empty",
            colorFilter = ColorFilter.tint(
                color = if (MaterialTheme.colors.isLight) {
                    Color(0xFFDADADA)
                } else {
                    Color(0xFFEAEFEF)
                }
            ),
            alpha = if (MaterialTheme.colors.isLight) 1F else 0.16F
        )

        Row(
            modifier = Modifier
                .wrapContentWidth()
                .padding(top = 42.dp)
        ) {
            val placeHolderStart = "[B]"
            val placeHolderEnd = "[/B]"

            val text = stringResource(id = R.string.timeline_empty_media)

            Text(
                text = text.substring(0, text.indexOf(placeHolderStart)),
                color = colorResource(id = R.color.grey_054_white_054),
            )

            Text(
                text = text.substring(
                    text.indexOf(placeHolderStart),
                    text.indexOf(placeHolderEnd)
                ).replace("[B]", ""),
                color = colorResource(id = R.color.grey_087_white_087),
                fontWeight = FontWeight.ExtraBold,
            )

            Text(
                text = text.substring(text.indexOf(placeHolderEnd)).replace("[/B]", ""),
                color = colorResource(id = R.color.grey_054_white_054),
            )
        }
    }
}
