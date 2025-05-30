package mega.privacy.android.app.utils;

import static mega.privacy.android.app.utils.Constants.CHAT_ID;
import static mega.privacy.android.app.utils.Constants.DISABLED_RETENTION_TIME;
import static mega.privacy.android.app.utils.Constants.FROM_CHAT;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE;
import static mega.privacy.android.app.utils.Constants.INVALID_POSITION;
import static mega.privacy.android.app.utils.Constants.MAX_ALLOWED_CHARACTERS_AND_EMOJIS;
import static mega.privacy.android.app.utils.Constants.MESSAGE_ID;
import static mega.privacy.android.app.utils.Constants.NOTIFICATIONS_1_HOUR;
import static mega.privacy.android.app.utils.Constants.NOTIFICATIONS_24_HOURS;
import static mega.privacy.android.app.utils.Constants.NOTIFICATIONS_30_MINUTES;
import static mega.privacy.android.app.utils.Constants.NOTIFICATIONS_6_HOURS;
import static mega.privacy.android.app.utils.Constants.NOTIFICATIONS_DISABLED;
import static mega.privacy.android.app.utils.Constants.NOTIFICATIONS_DISABLED_UNTIL_THIS_MORNING;
import static mega.privacy.android.app.utils.Constants.NOTIFICATIONS_DISABLED_UNTIL_TOMORROW_MORNING;
import static mega.privacy.android.app.utils.Constants.NOTIFICATIONS_DISABLED_X_TIME;
import static mega.privacy.android.app.utils.Constants.NOTIFICATIONS_ENABLED;
import static mega.privacy.android.app.utils.Constants.SECONDS_IN_DAY;
import static mega.privacy.android.app.utils.Constants.SECONDS_IN_HOUR;
import static mega.privacy.android.app.utils.Constants.SECONDS_IN_MONTH_30;
import static mega.privacy.android.app.utils.Constants.SECONDS_IN_WEEK;
import static mega.privacy.android.app.utils.Constants.SECONDS_IN_YEAR;
import static mega.privacy.android.app.utils.ContactUtil.isContact;
import static mega.privacy.android.app.utils.FileUtil.getLocalFile;
import static mega.privacy.android.app.utils.FileUtil.shareFile;
import static mega.privacy.android.app.utils.MegaNodeUtil.startShareIntent;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;
import static mega.privacy.android.app.utils.TextUtil.removeFormatPlaceholder;
import static mega.privacy.android.app.utils.TimeUtils.getCorrectStringDependingOnOptionSelected;
import static mega.privacy.android.app.utils.TimeUtils.isUntilThisMorning;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;
import static nz.mega.sdk.MegaUser.VISIBILITY_VISIBLE;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.util.TypedValue;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.MarqueeTextView;
import mega.privacy.android.app.components.twemoji.EmojiManager;
import mega.privacy.android.app.components.twemoji.EmojiRange;
import mega.privacy.android.app.components.twemoji.EmojiUtilsShortcodes;
import mega.privacy.android.app.listeners.ExportListener;
import mega.privacy.android.app.main.controllers.ChatController;
import mega.privacy.android.app.main.megachat.GroupChatInfoActivity;
import mega.privacy.android.app.main.megachat.NodeAttachmentHistoryActivity;
import mega.privacy.android.app.textEditor.TextEditorActivity;
import mega.privacy.android.domain.entity.settings.ChatSettings;
import mega.privacy.android.shared.original.core.ui.controls.controlssliders.MegaSwitch;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatContainsMeta;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaPushNotificationSettings;
import timber.log.Timber;

public class ChatUtil {
    private static final float DOWNSCALE_IMAGES_PX = 2000000f;
    public static final int AUDIOFOCUS_DEFAULT = AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE;
    public static final int STREAM_MUSIC_DEFAULT = AudioManager.STREAM_MUSIC;

    /**
     * Where is the status icon placed, according to the design,
     * according to the design,
     * on dark mode the status icon image is different based on the place where it's placed.
     */
    public enum StatusIconLocation {

        /**
         * On chat list
         * Contact list
         * Contact info
         * Flat app bar no chat room
         */
        STANDARD,

        /**
         * Raised app bar on chat room
         */
        APPBAR,

        /**
         * On nav drawer
         * Bottom sheets
         */
        DRAWER
    }

    public static boolean isVoiceClip(String name) {
        return MimeTypeList.typeForName(name).isAudioVoiceClip();
    }

    public static long getVoiceClipDuration(MegaNode node) {
        return node.getDuration() <= 0 ? 0 : node.getDuration() * 1000;
    }

    /* Get the height of the action bar */
    public static int getActionBarHeight(Activity activity, Resources resources) {
        int actionBarHeight = 0;
        TypedValue tv = new TypedValue();
        if (activity != null && activity.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, resources.getDisplayMetrics());
        }
        return actionBarHeight;
    }

    private static int getRealLength(CharSequence text) {
        int length = text.length();

        List<EmojiRange> emojisFound = EmojiManager.getInstance().findAllEmojis(text);
        int count = 0;
        if (emojisFound.size() > 0) {
            for (int i = 0; i < emojisFound.size(); i++) {
                count = count + (emojisFound.get(i).end - emojisFound.get(i).start);
            }
            return length + count;

        }
        return length;
    }

    public static int getMaxAllowed(@Nullable CharSequence text) {
        int realLength = getRealLength(text);
        if (realLength > MAX_ALLOWED_CHARACTERS_AND_EMOJIS) {
            return text.length();
        }
        return MAX_ALLOWED_CHARACTERS_AND_EMOJIS;
    }

    public static boolean isAllowedTitle(String text) {
        return getMaxAllowed(text) != text.length() || getRealLength(text) == MAX_ALLOWED_CHARACTERS_AND_EMOJIS;
    }

    public static void showConfirmationRemoveChatLink(final Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.action_delete_link)
                .setMessage(R.string.context_remove_chat_link_warning_text)
                .setPositiveButton(R.string.delete_button, (dialog, which) -> {
                    if (context instanceof GroupChatInfoActivity) {
                        ((GroupChatInfoActivity) context).removeChatLink();
                    }
                })
                .setNegativeButton(mega.privacy.android.shared.resources.R.string.general_dialog_cancel_button, null).show();
    }

    public static MegaChatMessage getMegaChatMessage(Context context, MegaChatApiAndroid megaChatApi, long chatId, long messageId) {
        if (context instanceof NodeAttachmentHistoryActivity) {
            return megaChatApi.getMessageFromNodeHistory(chatId, messageId);
        } else {
            return megaChatApi.getMessage(chatId, messageId);
        }

    }

    public static String converterShortCodes(String text) {
        if (text == null || text.isEmpty()) return text;
        return EmojiUtilsShortcodes.emojify(text);
    }

    public static boolean areDrawablesIdentical(Drawable drawableA, Drawable drawableB) {
        if (drawableA == null || drawableB == null)
            return false;

        Drawable.ConstantState stateA = drawableA.getConstantState();
        Drawable.ConstantState stateB = drawableB.getConstantState();
        return (stateA != null && stateA.equals(stateB)) || getBitmap(drawableA).sameAs(getBitmap(drawableB));
    }

    private static Bitmap getBitmap(Drawable drawable) {
        Bitmap result;
        if (drawable instanceof BitmapDrawable) {
            result = ((BitmapDrawable) drawable).getBitmap();
        } else {
            int width = drawable.getIntrinsicWidth();
            int height = drawable.getIntrinsicHeight();
            if (width <= 0) {
                width = 1;
            }
            if (height <= 0) {
                height = 1;
            }

            result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(result);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
        }
        return result;
    }

    /**
     * Gets the image compress format depending on the file extension.
     *
     * @param name Name of the image file including the extension.
     * @return Image compress format.
     */
    private static Bitmap.CompressFormat getCompressFormat(String name) {
        String[] s = name.split("\\.");

        if (s.length > 1) {
            String ext = s[s.length - 1];
            switch (ext) {
                case "jpeg":
                case "jpg":
                default:
                    return Bitmap.CompressFormat.JPEG;

                case "png":
                    return Bitmap.CompressFormat.PNG;

                case "webp":
                    return Bitmap.CompressFormat.WEBP;
            }
        }
        return Bitmap.CompressFormat.JPEG;
    }

    /**
     * Sets the contact status icon
     *
     * @param userStatus       contact's status
     * @param contactStateIcon view in which the status icon has to be set
     * @param where            Where the icon is placed.
     */
    public static void setContactStatus(int userStatus, ImageView contactStateIcon, StatusIconLocation where) {
        if (contactStateIcon == null) {
            return;
        }

        Context context = contactStateIcon.getContext();
        contactStateIcon.setVisibility(View.VISIBLE);

        int statusImageResId = getIconResourceIdByLocation(context, userStatus, where);

        // Hide the icon ImageView.
        if (statusImageResId == 0) {
            contactStateIcon.setVisibility(View.GONE);
        } else {
            contactStateIcon.setImageResource(statusImageResId);
        }
    }

    /**
     * Sets the contact status icon and status text
     *
     * @param userStatus          contact's status
     * @param textViewContactIcon view in which the status icon has to be set
     * @param contactStateText    view in which the status text has to be set
     * @param where               The status icon image resource is different based on the place where it's placed.
     */
    public static void setContactStatusParticipantList(int userStatus, final ImageView textViewContactIcon, TextView contactStateText, StatusIconLocation where) {
        MegaApplication app = MegaApplication.getInstance();
        Context context = app.getApplicationContext();
        int statusImageResId = getIconResourceIdByLocation(context, userStatus, where);

        if (statusImageResId == 0) {
            textViewContactIcon.setVisibility(View.GONE);
        } else {
            Drawable drawable = ContextCompat.getDrawable(MegaApplication.getInstance().getApplicationContext(), statusImageResId);
            textViewContactIcon.setImageDrawable(drawable);
            textViewContactIcon.setVisibility(View.VISIBLE);
        }

        if (contactStateText == null) {
            return;
        }

        contactStateText.setVisibility(View.VISIBLE);

        switch (userStatus) {
            case MegaChatApi.STATUS_ONLINE:
                contactStateText.setText(context.getString(R.string.online_status));
                break;

            case MegaChatApi.STATUS_AWAY:
                contactStateText.setText(context.getString(R.string.away_status));
                break;

            case MegaChatApi.STATUS_BUSY:
                contactStateText.setText(context.getString(R.string.busy_status));
                break;

            case MegaChatApi.STATUS_OFFLINE:
                contactStateText.setText(context.getString(R.string.offline_status));
                break;

            case MegaChatApi.STATUS_INVALID:
            default:
                contactStateText.setVisibility(View.GONE);
        }
    }

    /**
     * Get status icon image resource id by display mode and where the icon is placed.
     *
     * @param context    Context object.
     * @param userStatus User online status.
     * @param where      Where the icon is placed.
     * @return Image resource id based on where the icon is placed.
     * NOTE: when the user has an invalid online status, returns 0.
     * Caller should verify the return value, 0 is an invalid value for resource id.
     */
    public static int getIconResourceIdByLocation(Context context, int userStatus, StatusIconLocation where) {
        int statusImageResId = 0;

        switch (userStatus) {
            case MegaChatApi.STATUS_ONLINE:
                if (Util.isDarkMode(context)) {
                    switch (where) {
                        case STANDARD:
                            statusImageResId = R.drawable.ic_online_dark_standard;
                            break;

                        case DRAWER:
                            statusImageResId = R.drawable.ic_online_dark_drawer;
                            break;

                        case APPBAR:
                            statusImageResId = R.drawable.ic_online_dark_appbar;
                            break;
                    }
                } else {
                    statusImageResId = R.drawable.ic_online_light;
                }
                break;

            case MegaChatApi.STATUS_AWAY:
                if (Util.isDarkMode(context)) {
                    switch (where) {
                        case STANDARD:
                            statusImageResId = R.drawable.ic_away_dark_standard;
                            break;

                        case DRAWER:
                            statusImageResId = R.drawable.ic_away_dark_drawer;
                            break;

                        case APPBAR:
                            statusImageResId = R.drawable.ic_away_dark_appbar;
                            break;
                    }
                } else {
                    statusImageResId = R.drawable.ic_away_light;
                }
                break;

            case MegaChatApi.STATUS_BUSY:
                if (Util.isDarkMode(context)) {
                    switch (where) {
                        case STANDARD:
                            statusImageResId = R.drawable.ic_busy_dark_standard;
                            break;

                        case DRAWER:
                            statusImageResId = R.drawable.ic_busy_dark_drawer;
                            break;

                        case APPBAR:
                            statusImageResId = R.drawable.ic_busy_dark_appbar;
                            break;
                    }
                } else {
                    statusImageResId = R.drawable.ic_busy_light;
                }
                break;

            case MegaChatApi.STATUS_OFFLINE:
                if (Util.isDarkMode(context)) {
                    switch (where) {
                        case STANDARD:
                            statusImageResId = R.drawable.ic_offline_dark_standard;
                            break;

                        case DRAWER:
                            statusImageResId = R.drawable.ic_offline_dark_drawer;
                            break;

                        case APPBAR:
                            statusImageResId = R.drawable.ic_offline_dark_appbar;
                            break;
                    }
                } else {
                    statusImageResId = R.drawable.ic_offline_light;
                }
                break;

            case MegaChatApi.STATUS_INVALID:
            default:
                // Do nothing, let statusImageResId be 0.
        }

        return statusImageResId;
    }

    /**
     * Sets the contact status icon and status text
     *
     * @param userStatus       contact's status
     * @param contactStateIcon view in which the status icon has to be set
     * @param contactStateText view in which the status text has to be set
     * @param where            The status icon image resource is different based on the place where it's placed.
     */
    public static void setContactStatus(int userStatus, ImageView contactStateIcon, TextView contactStateText, StatusIconLocation where) {
        MegaApplication app = MegaApplication.getInstance();
        setContactStatus(userStatus, contactStateIcon, where);

        if (contactStateText == null) {
            return;
        }

        contactStateText.setVisibility(View.VISIBLE);

        switch (userStatus) {
            case MegaChatApi.STATUS_ONLINE:
                contactStateText.setText(app.getString(R.string.online_status));
                break;

            case MegaChatApi.STATUS_AWAY:
                contactStateText.setText(app.getString(R.string.away_status));
                break;

            case MegaChatApi.STATUS_BUSY:
                contactStateText.setText(app.getString(R.string.busy_status));
                break;

            case MegaChatApi.STATUS_OFFLINE:
                contactStateText.setText(app.getString(R.string.offline_status));
                break;

            case MegaChatApi.STATUS_INVALID:
            default:
                contactStateText.setVisibility(View.GONE);
        }
    }

    /**
     * If the contact has last green, sets is as status text
     *
     * @param context          current Context
     * @param userStatus       contact's status
     * @param lastGreen        contact's last green
     * @param contactStateText view in which the last green has to be set
     */
    public static void setContactLastGreen(Context context, int userStatus, String lastGreen, MarqueeTextView contactStateText) {
        if (contactStateText == null || isTextEmpty(lastGreen)) {
            return;
        }

        if (userStatus != MegaChatApi.STATUS_ONLINE && userStatus != MegaChatApi.STATUS_BUSY && userStatus != MegaChatApi.STATUS_INVALID) {
            contactStateText.setText(lastGreen);
            contactStateText.isMarqueeIsNecessary(context);
        }
    }

    /**
     * Method for obtaining the AudioFocusRequest when get the focus audio.
     *
     * @param listener  The listener.
     * @param focusType Type of focus.
     * @return The AudioFocusRequest.
     */
    public static AudioFocusRequest getRequest(AudioManager.OnAudioFocusChangeListener listener,
                                               int focusType) {
        AudioAttributes mAudioAttributes =
                new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build();
        return new AudioFocusRequest.Builder(focusType)
                .setAudioAttributes(mAudioAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setWillPauseWhenDucked(true)
                .setOnAudioFocusChangeListener(listener)
                .build();
    }

    /**
     * Knowing if permits have been successfully got.
     *
     * @return True, if it has been successful. False, if not.
     */
    public static boolean getAudioFocus(AudioManager audioManager,
                                        AudioManager.OnAudioFocusChangeListener listener,
                                        AudioFocusRequest request, int focusType, int streamType) {
        if (audioManager == null) {
            Timber.w("Audio Manager is NULL");
            return false;
        }

        if (request == null) {
            Timber.w("Audio Focus Request is NULL");
            return false;
        }
        int focusRequest = audioManager.requestAudioFocus(request);
        return focusRequest == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    /**
     * Method for leaving the audio focus.
     */
    public static void abandonAudioFocus(AudioManager.OnAudioFocusChangeListener listener,
                                         AudioManager audioManager, AudioFocusRequest request) {
        if (request != null) {
            audioManager.abandonAudioFocusRequest(request);
        }
    }

    /**
     * Method for obtaining the title of a MegaChatRoom.
     *
     * @param chat The chat room.
     * @return String with the title.
     */
    public static String getTitleChat(MegaChatRoom chat) {
        if (chat == null) {
            Timber.e("chat is null");
            return "";
        }

        return chat.getTitle();
    }

    /**
     * Method for obtaining the title of a MegaChatListItem.
     *
     * @param chat The chat room.
     * @return String with the title.
     */
    public static String getTitleChat(MegaChatListItem chat) {
        if (chat == null) {
            Timber.e("chat is null");
            return "";
        }

        return chat.getTitle();
    }

    /**
     * Method to know if the chat notifications are activated or deactivated.
     *
     * @return The type of mute.
     */
    public static String getGeneralNotification() {
        MegaApplication app = MegaApplication.getInstance();
        MegaPushNotificationSettings pushNotificationSettings = MegaApplication.getPushNotificationSettingManagement().getPushNotificationSetting();
        if (pushNotificationSettings != null) {
            if (!pushNotificationSettings.isGlobalChatsDndEnabled() || pushNotificationSettings.getGlobalChatsDnd() == -1) {
                ChatSettings chatSettings = app.getDbH().getChatSettings();
                if (chatSettings == null) {
                    chatSettings = new ChatSettings();
                    app.getDbH().setChatSettings(chatSettings);
                }

                return NOTIFICATIONS_ENABLED;
            }

            if (pushNotificationSettings.getGlobalChatsDnd() == 0) {
                return NOTIFICATIONS_DISABLED;
            }

            return NOTIFICATIONS_DISABLED_X_TIME;
        }

        return NOTIFICATIONS_ENABLED;
    }

    /**
     * Method to display a dialog to mute a specific chat.
     *
     * @param context Context of Activity.
     * @param chatId  Chat ID.
     */
    public static void createMuteNotificationsAlertDialogOfAChat(Activity context, long chatId) {
        ArrayList<MegaChatListItem> chats = new ArrayList<>();
        MegaChatListItem chat = MegaApplication.getInstance().getMegaChatApi().getChatListItem(chatId);
        if (chat != null) {
            chats.add(chat);
            createMuteNotificationsChatAlertDialog(context, chats);
        }
    }

    /**
     * Method to display a dialog to mute a list of chats.
     *
     * @param context Context of Activity.
     * @param chatIds Chat IDs.
     */
    public static void createMuteNotificationsAlertDialogOfChats(Activity context, List<Long> chatIds) {
        ArrayList<MegaChatListItem> chats = (ArrayList<MegaChatListItem>) chatIds.stream().map(chatId ->
                MegaApplication.getInstance().getMegaChatApi().getChatListItem(chatId)).collect(Collectors.toList());
        if (!chats.isEmpty()) {
            createMuteNotificationsChatAlertDialog(context, chats);
        }
    }


    /**
     * Method to display a dialog to mute general chat notifications or several specific chats.
     *
     * @param context Context of Activity.
     * @param chats   Chats. If the chats is null, it's for the general chats notifications.
     */
    public static void createMuteNotificationsChatAlertDialog(Activity context, ArrayList<MegaChatListItem> chats) {

        final AlertDialog muteDialog;
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(context);
        if (chats == null) {
            View view = context.getLayoutInflater().inflate(R.layout.title_mute_notifications, null);
            dialogBuilder.setCustomTitle(view);
        } else {
            dialogBuilder.setTitle(chats.get(0).isMeeting() ?
                    context.getString(R.string.meetings_mute_notifications_dialog_title) :
                    context.getString(R.string.title_dialog_mute_chatroom_notifications));
        }

        boolean isUntilThisMorning = isUntilThisMorning();
        String optionUntil = chats != null ?
                context.getString(R.string.mute_chatroom_notification_option_forever) :
                (isUntilThisMorning ? context.getString(R.string.mute_chatroom_notification_option_until_this_morning) :
                        context.getString(R.string.mute_chatroom_notification_option_until_tomorrow_morning));

        String optionSelected = chats != null ?
                NOTIFICATIONS_DISABLED :
                (isUntilThisMorning ? NOTIFICATIONS_DISABLED_UNTIL_THIS_MORNING :
                        NOTIFICATIONS_DISABLED_UNTIL_TOMORROW_MORNING);

        AtomicReference<Integer> itemClicked = new AtomicReference<>();

        ArrayList<String> stringsArray = new ArrayList<>();
        stringsArray.add(0, removeFormatPlaceholder(context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_minutes, 30, 30)));
        stringsArray.add(1, removeFormatPlaceholder(context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_hours, 1, 1)));
        stringsArray.add(2, removeFormatPlaceholder(context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_hours, 6, 6)));
        stringsArray.add(3, removeFormatPlaceholder(context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_hours, 24, 24)));
        stringsArray.add(4, optionUntil);

        ArrayAdapter<String> itemsAdapter = new ArrayAdapter<>(context, R.layout.checked_text_view_dialog_button, stringsArray);
        ListView listView = new ListView(context);
        listView.setAdapter(itemsAdapter);

        dialogBuilder.setSingleChoiceItems(itemsAdapter, INVALID_POSITION, (dialog, item) -> {
            itemClicked.set(item);
            ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
        });

        dialogBuilder.setPositiveButton(context.getString(R.string.general_ok),
                (dialog, which) -> {
                    ArrayList<Long> chatIds = null;
                    if (chats != null) {
                        chatIds = new ArrayList<>();
                        for (int i = 0; i < chats.size(); i++) {
                            MegaChatListItem chat = chats.get(i);
                            if (chat != null) {
                                chatIds.add(chat.getChatId());
                            }
                        }
                    }

                    MegaApplication.getPushNotificationSettingManagement()
                            .controlMuteNotifications(
                                    context,
                                    getTypeMute(itemClicked.get(), optionSelected),
                                    chatIds
                            );
                    dialog.dismiss();
                });
        dialogBuilder.setNegativeButton(context.getString(mega.privacy.android.shared.resources.R.string.general_dialog_cancel_button), (dialog, which) -> dialog.dismiss());

        muteDialog = dialogBuilder.create();
        muteDialog.show();
        muteDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
    }

    /**
     * Method for getting the string depending on the selected mute option.
     *
     * @param option The selected mute option.
     * @return The appropriate string.
     */
    public static String getMutedPeriodString(String option) {
        Context context = MegaApplication.getInstance().getBaseContext();
        switch (option) {
            case NOTIFICATIONS_30_MINUTES:
                return removeFormatPlaceholder(context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_minutes, 30, 30));
            case NOTIFICATIONS_1_HOUR:
                return removeFormatPlaceholder(context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_hours, 1, 1));
            case NOTIFICATIONS_6_HOURS:
                return removeFormatPlaceholder(context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_hours, 6, 6));
            case NOTIFICATIONS_24_HOURS:
                return removeFormatPlaceholder(context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_hours, 24, 24));
        }

        return null;
    }

    /**
     * Method for getting the selected mute option depending on the selected item.
     *
     * @param itemClicked    The selected item.
     * @param optionSelected The right choice when you select the fifth option.
     * @return The right mute option.
     */
    private static String getTypeMute(int itemClicked, String optionSelected) {
        switch (itemClicked) {
            case 0:
                return NOTIFICATIONS_30_MINUTES;
            case 1:
                return NOTIFICATIONS_1_HOUR;
            case 2:
                return NOTIFICATIONS_6_HOURS;
            case 3:
                return NOTIFICATIONS_24_HOURS;
            case 4:
                return optionSelected;
            default:
                return NOTIFICATIONS_ENABLED;
        }
    }

    /**
     * Method to mute a specific chat or general notifications chat for a specific period of time.
     *
     * @param context    Context of Activity.
     * @param muteOption The selected mute option.
     */
    public static void muteChat(Context context, String muteOption) {
        new ChatController(context).muteChat(muteOption);
    }

    /**
     * Method to know if the notifications of a specific chat are activated or muted.
     *
     * @param chatId Chat id.
     * @return True, if notifications are activated. False in the opposite case
     */
    public static boolean isEnableChatNotifications(long chatId) {
        MegaPushNotificationSettings megaPushNotificationSettings = MegaApplication.getPushNotificationSettingManagement().getPushNotificationSetting();
        return megaPushNotificationSettings == null || !megaPushNotificationSettings.isChatDndEnabled(chatId);
    }

    /**
     * Method to checking when chat notifications are enabled and update the UI elements.
     *
     * @param chatHandle            Chat ID.
     * @param notificationsSwitch   The MegaSwitch.
     * @param notificationsSubTitle The TextView with the info.
     */
    public static void checkSpecificChatNotifications(long chatHandle, final MegaSwitch notificationsSwitch, final TextView notificationsSubTitle, @NonNull Context context) {
        if (MegaApplication.getPushNotificationSettingManagement().getPushNotificationSetting() != null) {
            updateSwitchButton(chatHandle, notificationsSwitch, notificationsSubTitle, context);
        }
    }

    /**
     * Method to update the switch element related to the notifications of a specific chat.
     *
     * @param chatId                The chat ID.
     * @param notificationsSwitch   The MegaSwitch.
     * @param notificationsSubTitle The TextView with the info.
     */
    public static void updateSwitchButton(long chatId, final MegaSwitch notificationsSwitch, final TextView notificationsSubTitle, @NonNull Context context) {
        MegaPushNotificationSettings push = MegaApplication.getPushNotificationSettingManagement().getPushNotificationSetting();
        if (push == null)
            return;

        if (push.isChatDndEnabled(chatId)) {
            notificationsSwitch.setChecked(false);
            long timestampMute = push.getChatDnd(chatId);
            notificationsSubTitle.setVisibility(View.VISIBLE);
            notificationsSubTitle.setText(timestampMute == 0 ?
                    MegaApplication.getInstance().getString(R.string.mute_chatroom_notification_option_off) :
                    getCorrectStringDependingOnOptionSelected(timestampMute, context));
        } else {
            notificationsSwitch.setChecked(true);
            notificationsSubTitle.setVisibility(View.GONE);
        }
    }

    /**
     * Gets the user's online status.
     *
     * @param userHandle handle of the user
     * @return The user's status.
     */
    public static int getUserStatus(long userHandle) {
        return isContact(userHandle)
                ? MegaApplication.getInstance().getMegaChatApi().getUserOnlineStatus(userHandle)
                : MegaChatApi.STATUS_INVALID;
    }

    /**
     * Method for obtaining the contact status bitmap.
     *
     * @param userStatus The contact status.
     * @return The final bitmap.
     */
    public static Bitmap getStatusBitmap(int userStatus) {
        Resources resources = MegaApplication.getInstance().getBaseContext().getResources();
        boolean isDarkMode = Util.isDarkMode(MegaApplication.getInstance());
        switch (userStatus) {
            case MegaChatApi.STATUS_ONLINE:
                return BitmapFactory.decodeResource(resources,
                        isDarkMode ? R.drawable.ic_online_dark_standard
                                : R.drawable.ic_online_light);
            case MegaChatApi.STATUS_AWAY:
                return BitmapFactory.decodeResource(resources,
                        isDarkMode ? R.drawable.ic_away_dark_standard
                                : R.drawable.ic_away_light);
            case MegaChatApi.STATUS_BUSY:
                return BitmapFactory.decodeResource(resources,
                        isDarkMode ? R.drawable.ic_busy_dark_standard
                                : R.drawable.ic_busy_light);
            case MegaChatApi.STATUS_OFFLINE:
                return BitmapFactory.decodeResource(resources,
                        isDarkMode ? R.drawable.ic_offline_dark_standard
                                : R.drawable.ic_offline_light);
            case MegaChatApi.STATUS_INVALID:
            default:
                return null;
        }
    }

    /**
     * Gets the right message to show in case MegaChatContainsMeta type is CONTAINS_META_INVALID.
     *
     * @param message MegaChatMessage containing meta with type CONTAINS_META_INVALID.
     * @return String to show for invalid meta message.
     */
    public static String getInvalidMetaMessage(MegaChatMessage message, @NonNull Context context) {
        String invalidMetaMessage = context.getString(R.string.error_meta_message_invalid);

        if (message == null) {
            return invalidMetaMessage;
        }

        String contentMessage = message.getContent();
        if (!isTextEmpty(contentMessage)) {
            return contentMessage;
        }

        MegaChatContainsMeta meta = message.getContainsMeta();

        String metaTextMessage = meta != null ? meta.getTextMessage() : null;
        if (!isTextEmpty(metaTextMessage)) {
            return metaTextMessage;
        }

        return invalidMetaMessage;
    }

    /**
     * Gets retention time for a particular chat.
     *
     * @param idChat The chat ID.
     * @return The retention time in seconds.
     */
    public static long getUpdatedRetentionTimeFromAChat(long idChat) {
        MegaChatRoom chat = MegaApplication.getInstance().getMegaChatApi().getChatRoom(idChat);
        if (chat != null) {
            return chat.getRetentionTime();
        }

        return DISABLED_RETENTION_TIME;
    }

    /**
     * Method for getting the appropriate String from the seconds of rentention time.
     *
     * @param seconds The retention time in seconds
     * @return The right text
     * @deprecated Use RetentionTimeUpdatedMessageView.getRetentionTimeString instead.
     */
    @Deprecated
    public static String transformSecondsInString(long seconds) {
        if (seconds == DISABLED_RETENTION_TIME)
            return "";

        long hours = seconds % SECONDS_IN_HOUR;
        long days = seconds % SECONDS_IN_DAY;
        long weeks = seconds % SECONDS_IN_WEEK;
        long months = seconds % SECONDS_IN_MONTH_30;
        long years = seconds % SECONDS_IN_YEAR;

        if (years == 0) {
            return MegaApplication.getInstance().getBaseContext().getResources().getString(R.string.subtitle_properties_manage_chat_label_year);
        }

        if (months == 0) {
            int month = (int) (seconds / SECONDS_IN_MONTH_30);
            return MegaApplication.getInstance().getBaseContext().getResources().getQuantityString(R.plurals.subtitle_properties_manage_chat_label_months, month, month);
        }

        if (weeks == 0) {
            int week = (int) (seconds / SECONDS_IN_WEEK);
            return MegaApplication.getInstance().getBaseContext().getResources().getQuantityString(R.plurals.subtitle_properties_manage_chat_label_weeks, week, week);
        }

        if (days == 0) {
            int day = (int) (seconds / SECONDS_IN_DAY);
            return MegaApplication.getInstance().getBaseContext().getResources().getQuantityString(R.plurals.label_time_in_days_full, day, day);
        }

        if (hours == 0) {
            int hour = (int) (seconds / SECONDS_IN_HOUR);
            return MegaApplication.getInstance().getBaseContext().getResources().getQuantityString(R.plurals.subtitle_properties_manage_chat_label_hours, hour, hour);
        }

        return "";
    }

    /**
     * Method for updating the Time retention layout.
     *
     * @param time The retention time in seconds.
     */
    public static void updateRetentionTimeLayout(final TextView retentionTimeText, long time, @NonNull Context context) {
        String timeFormatted = transformSecondsInString(time);
        if (isTextEmpty(timeFormatted)) {
            retentionTimeText.setVisibility(View.GONE);
        } else {
            String subtitleText = context.getString(R.string.subtitle_properties_manage_chat) + " " + timeFormatted;
            retentionTimeText.setText(subtitleText);
            retentionTimeText.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Method to share a node from the chat.
     *
     * @param context Context of Activity.
     * @param node    The node to be shared
     * @param chatId  The ID of a chat room.
     */
    public static void shareNodeFromChat(Context context, MegaNode node, long chatId, long msgId) {
        if (!MegaNodeUtil.shouldContinueWithoutError(context, node)) {
            return;
        }

        String path = getLocalFile(node);
        if (!isTextEmpty(path)) {
            Timber.d("Node is downloaded, so share the file");
            shareFile(context, new File(path), node.getName());
        } else if (node.isExported()) {
            Timber.d("Node is exported, so share the public link");
            startShareIntent(context, new Intent(android.content.Intent.ACTION_SEND), node.getPublicLink(), node.getName());
        } else {
            if (msgId == MEGACHAT_INVALID_HANDLE) {
                return;
            }

            Timber.d("Node is not exported, so export Node");
            MegaApplication.getInstance().getMegaApi().exportNode(node, new ExportListener(context, new Intent(android.content.Intent.ACTION_SEND), msgId, chatId));
        }
    }

    /**
     * Authorizes the node if the chat is on preview mode.
     *
     * @param node        Node to authorize.
     * @param megaChatApi MegaChatApiAndroid instance.
     * @param megaApi     MegaApiAndroid instance.
     * @param chatId      Chat identifier to check.
     * @return The authorized node if preview, same node otherwise.
     */
    public static MegaNode authorizeNodeIfPreview(MegaNode node, MegaChatApiAndroid megaChatApi,
                                                  MegaApiAndroid megaApi, long chatId) {
        MegaChatRoom chatRoom = megaChatApi.getChatRoom(chatId);

        if (chatRoom != null && chatRoom.isPreview()) {
            MegaNode nodeAuthorized = megaApi.authorizeChatNode(node, chatRoom.getAuthorizationToken());

            if (nodeAuthorized != null) {
                Timber.d("Authorized");
                return nodeAuthorized;
            }
        }

        return node;
    }

    /**
     * Remove an attachment message from chat.
     *
     * @param activity Android activity
     * @param chatId   chat id
     * @param message  chat message
     */
    public static void removeAttachmentMessage(Activity activity, long chatId,
                                               MegaChatMessage message) {
        new MaterialAlertDialogBuilder(activity)
                .setMessage(activity.getString(R.string.confirmation_delete_one_attachment))
                .setPositiveButton(activity.getString(R.string.context_remove), (dialog, which) -> {
                    new ChatController(activity).deleteMessage(message, chatId);
                    activity.finish();
                })
                .setNegativeButton(activity.getString(mega.privacy.android.shared.resources.R.string.general_dialog_cancel_button), null)
                .show();
    }

    /**
     * Launches an Intent to open TextFileEditorActivity.
     *
     * @param context Current context.
     * @param msgId   Message identifier.
     * @param chatId  Chat identifier.
     */
    public static void manageTextFileIntent(Context context, long msgId, long chatId) {
        context.startActivity(new Intent(context, TextEditorActivity.class)
                .putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, FROM_CHAT)
                .putExtra(MESSAGE_ID, msgId)
                .putExtra(CHAT_ID, chatId));
    }

    /**
     * Method to find out if I am participating in a chat room
     *
     * @param chatId The chat ID
     * @return True, if I am joined to the chat. False, if not
     */
    public static boolean amIParticipatingInAChat(long chatId) {
        MegaChatRoom chatRoom = MegaApplication.getInstance().getMegaChatApi().getChatRoom(chatId);
        if (chatRoom == null)
            return false;

        if (chatRoom.isPreview()) {
            return false;
        }

        int myPrivileges = chatRoom.getOwnPrivilege();
        return myPrivileges == MegaChatRoom.PRIV_RO || myPrivileges == MegaChatRoom.PRIV_STANDARD || myPrivileges == MegaChatRoom.PRIV_MODERATOR;
    }

    /**
     * Method to get the initial state of megaChatApi and, if necessary, initiates it.
     *
     * @param session User session
     */
    public static void initMegaChatApi(String session) {
        initMegaChatApi(session, null);
    }

    /**
     * Method to get the initial state of megaChatApi and, if necessary, initiates it.
     *
     * @param session  User session
     * @param listener MegaChat listener for logout request.
     */
    public static void initMegaChatApi(String session, @Nullable MegaChatRequestListenerInterface listener) {
        MegaChatApiAndroid megaChatApi = MegaApplication.getInstance().getMegaChatApi();

        int state = megaChatApi.getInitState();
        if (state == MegaChatApi.INIT_NOT_DONE || state == MegaChatApi.INIT_ERROR) {
            state = megaChatApi.init(session);
            Timber.d("result of init ---> %s", state);
            switch (state) {
                case MegaChatApi.INIT_NO_CACHE:
                    Timber.d("INIT_NO_CACHE");
                    break;
                case MegaChatApi.INIT_ERROR:
                    Timber.d("INIT_ERROR");
                    if (listener != null) {
                        megaChatApi.logout(listener);
                    } else {
                        megaChatApi.logout();
                    }
                    break;
                default:
                    Timber.d("Chat correctly initialized");
                    break;
            }
        }
    }

    /**
     * Method to check if all user's contacts are participants of the chat.
     *
     * @param chatId Chat id
     * @return True if all user's contacts are participants of the chat room or false otherwise.
     */
    public static boolean areAllMyContactsChatParticipants(Long chatId) {
        var chat = MegaApplication.getInstance().getMegaChatApi().getChatRoom(chatId);
        return areAllMyContactsChatParticipants(chat);
    }

    /**
     * Method to check if all user's contacts are participants of the chat. use [AreAllParticipantsInContactUseCase] instead
     *
     * @param chatRoom MegaChatRoom to check
     * @return True if all user's contacts are participants of the chat room or false otherwise.
     */
    @Deprecated
    public static boolean areAllMyContactsChatParticipants(MegaChatRoom chatRoom) {
        if (chatRoom == null) {
            return false;
        }

        var contacts = MegaApplication.getInstance().getMegaApi().getContacts();
        var peerCount = chatRoom.getPeerCount();
        var areAllMyContactsChatParticipants = true;

        for (int i = 0; i < contacts.size(); i++) {
            if (contacts.get(i).getVisibility() == VISIBILITY_VISIBLE) {
                var contactIsParticipant = false;
                for (int j = 0; j < peerCount; j++) {
                    if (contacts.get(i).getHandle() == chatRoom.getPeerHandle(j)) {
                        contactIsParticipant = true;
                        break;
                    }
                }
                if (!contactIsParticipant) {
                    areAllMyContactsChatParticipants = false;
                    break;
                }
            }
        }

        return areAllMyContactsChatParticipants;
    }
}
