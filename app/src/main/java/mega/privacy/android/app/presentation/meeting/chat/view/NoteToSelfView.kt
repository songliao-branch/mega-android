package mega.privacy.android.app.presentation.meeting.chat.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R as sharedR
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.shared.original.core.ui.controls.chip.MegaChip
import mega.privacy.android.shared.original.core.ui.controls.chip.TagChipStyle
import mega.privacy.android.shared.original.core.ui.theme.extensions.subtitle1medium


/**
 * View to show a note to self item
 * @param onNoteToSelfClicked is invoked when the view is clicked
 * @param modifier
 */

@Composable
internal fun NoteToSelfView(
    onNoteToSelfClicked: (() -> Unit)? = null,
    isHint: Boolean = false,
    isNew: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .then(
                    if (onNoteToSelfClicked != null) {
                        Modifier.clickable(onClick = onNoteToSelfClicked)
                    } else {
                        Modifier
                    }
                )
                .fillMaxWidth()
                .padding(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                NoteToSelfAvatarView(
                    modifier = Modifier
                        .padding(
                            horizontal = if (isHint) 24.dp else 16.dp,
                            vertical = 8.dp
                        )
                        .size(if (isHint) 24.dp else 40.dp),
                    isHint = isHint
                )
            }
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MegaText(
                        modifier = Modifier
                            .testTag(NOTE_TO_SELF_ITEM_TITLE_TEXT)
                            .padding(top = 2.dp)
                            .weight(1f),
                        text = stringResource(id = sharedR.string.chat_note_to_self_chat_title),
                        textColor = TextColor.Primary,
                        style = MaterialTheme.typography.subtitle1medium,
                    )

                    if (isNew) {
                        MegaChip(
                            selected = true,
                            text = stringResource(sharedR.string.notifications_notification_item_new_tag),
                            style = TagChipStyle,
                            modifier = Modifier
                                .testTag(NOTE_TO_SELF_ITEM_NEW_LABEL)
                        )
                    }
                }
            }
        }

    }
}

@CombinedTextAndThemePreviews
@Composable
private fun PreviewNoteToSelfView() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        NoteToSelfView(
            isHint = true,
            isNew = true,
            onNoteToSelfClicked = {}
        )
    }
}

@CombinedTextAndThemePreviews
@Composable
private fun PreviewNoteToSelfAvatarView() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        NoteToSelfView(
            isHint = false,
            isNew = true,
            onNoteToSelfClicked = {}
        )
    }
}

internal const val NOTE_TO_SELF_ITEM_TITLE_TEXT = "note_to_self_item:title_text"
internal const val NOTE_TO_SELF_ITEM_NEW_LABEL = "note_to_self_item:new_label"

