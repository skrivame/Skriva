package me.skriva.ceph.ui.util;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

import java.util.Arrays;

import me.skriva.ceph.R;
import me.skriva.ceph.databinding.MessageReferenceBinding;
import me.skriva.ceph.entities.Conversation;
import me.skriva.ceph.entities.Message;
import me.skriva.ceph.ui.ConversationFragment;
import me.skriva.ceph.ui.XmppActivity;
import me.skriva.ceph.utils.EmojiWrapper;
import me.skriva.ceph.utils.Emoticons;
import me.skriva.ceph.utils.UIHelper;

public class MessageReferenceUtils {

    /**
     * Hide the whole area where a referenced message would be displayed.
     * @param messageReferenceBinding data binding that holds the message reference views
     */
    public static void hideMessageReference(final MessageReferenceBinding messageReferenceBinding) {
        messageReferenceBinding.messageReferenceContainer.setVisibility(View.GONE);
        messageReferenceBinding.messageReferenceText.setVisibility(View.GONE);
        messageReferenceBinding.messageReferenceIcon.setVisibility(View.GONE);
        messageReferenceBinding.messageReferenceImageThumbnail.setVisibility(View.GONE);
        messageReferenceBinding.messageReferencePreviewCancelButton.setVisibility(View.GONE);
    }

    /**
     * Displays the message reference area.
     * @param activity current activity
     * @param messageReferenceBinding binding that was created for the messageReference
     * @param message message that has a messageReference or is null if the messageReference is used for a preview before sending a new message with that messageReference
     * @param referencedMessage message that is referenced by the given message
     * @param darkBackground true if the background (message bubble) of the given message is dark
     */
    public static void displayMessageReference(final XmppActivity activity, final MessageReferenceBinding messageReferenceBinding, final Message message, final Message referencedMessage, final boolean darkBackground) {
        // true if this method is used for a preview of a messageReference area
        final boolean messageReferencePreview = message == null;

        if (darkBackground) {
            // Use different backgrounds depending on the usage of a message bubble.
            // A messageReference (MessageAdapter) of a normal message is inside of a message bubble.
            // A messageReferencePreview (ConversationFragment) is not inside a message bubble.
            final int background;
            if (!messageReferencePreview) {
                background = R.drawable.message_reference_background_white;
            } else {
                background = R.drawable.message_reference_background_dark_grey;
                messageReferenceBinding.messageReferencePreviewCancelButton.setImageDrawable(activity.getResources().getDrawable(R.drawable.ic_send_cancel_offline_white));
            }
            messageReferenceBinding.messageReferenceContainer.setBackground(activity.getResources().getDrawable(background));

            messageReferenceBinding.messageReferenceBar.setBackgroundColor(activity.getResources().getColor(R.color.white70));
            messageReferenceBinding.messageReferenceInfo.setTextAppearance(activity, R.style.TextAppearance_Conversations_Caption_OnDark);
            messageReferenceBinding.messageReferenceText.setTextAppearance(activity, R.style.TextAppearance_Conversations_MessageReferenceText_OnDark);
        } else if (messageReferencePreview) {
            // Set a different background if the background is not dark and the messageReference is for a preview.
            messageReferenceBinding.messageReferenceContainer.setBackground(activity.getResources().getDrawable(R.drawable.message_reference_background_light_grey));
        }

        messageReferenceBinding.messageReferenceContainer.setVisibility(View.VISIBLE);
        createInfo(activity, messageReferenceBinding, referencedMessage);

        if (referencedMessage != null) {
            if (referencedMessage.isFileOrImage() && !activity.xmppConnectionService.getFileBackend().getFile(referencedMessage).exists()) {
                messageReferenceBinding.messageReferenceIcon.setVisibility(View.VISIBLE);
                setMessageReferenceIcon(darkBackground, messageReferenceBinding, activity.getResources().getDrawable(R.drawable.ic_file_deleted), activity.getResources().getDrawable(R.drawable.ic_file_deleted_white));
            } else if (referencedMessage.isImageOrVideo()) {
                displayReferencedImageMessage(activity, messageReferenceBinding, referencedMessage, messageReferencePreview);
            } else if (referencedMessage.isAudio()) {
                messageReferenceBinding.messageReferenceIcon.setVisibility(View.VISIBLE);
                setMessageReferenceIcon(darkBackground, messageReferenceBinding, activity.getResources().getDrawable(R.drawable.ic_attach_record), activity.getResources().getDrawable(R.drawable.ic_attach_record_white));
            } else if (referencedMessage.isGeoUri()) {
                messageReferenceBinding.messageReferenceIcon.setVisibility(View.VISIBLE);
                setMessageReferenceIcon(darkBackground, messageReferenceBinding, activity.getResources().getDrawable(R.drawable.ic_attach_location), activity.getResources().getDrawable(R.drawable.ic_attach_location_white));
            } else if (referencedMessage.treatAsDownloadable()) {
                messageReferenceBinding.messageReferenceIcon.setVisibility(View.VISIBLE);
                setMessageReferenceIcon(darkBackground, messageReferenceBinding, activity.getResources().getDrawable(R.drawable.ic_file_download), activity.getResources().getDrawable(R.drawable.ic_file_download_white));
            } else if (referencedMessage.isText()) {
                messageReferenceBinding.messageReferenceText.setVisibility(View.VISIBLE);
                CharSequence firstTwoLinesOfBody = extractFirstTwoLinesOfBody(referencedMessage);
                if (Emoticons.containsEmoji(firstTwoLinesOfBody.toString())) {
                    firstTwoLinesOfBody = EmojiWrapper.transform(firstTwoLinesOfBody);
                }
                messageReferenceBinding.messageReferenceText.setText(firstTwoLinesOfBody);
            } else {
                messageReferenceBinding.messageReferenceIcon.setVisibility(View.VISIBLE);
                // default icon
                setMessageReferenceIcon(darkBackground, messageReferenceBinding, activity.getResources().getDrawable(R.drawable.ic_attach_document), activity.getResources().getDrawable(R.drawable.ic_attach_document_white));
            }

            final Conversation conversation = (Conversation) referencedMessage.getConversation();
            final ConversationFragment conversationFragment = conversation.getConversationFragment();

            // Add only onClickListeners if a conversationFragment exists.
            // That is especially not the case if this method is called during a message search.
            if (conversationFragment != null) {
                final int jumpingPosition = conversationFragment.getMessagePosition(referencedMessage.firstMergeMessage());

                if (messageReferencePreview) {
                    messageReferenceBinding.messageReferencePreviewCancelButton.setVisibility(View.VISIBLE);

                    // Cancel the referencing of a message.
                    messageReferenceBinding.messageReferencePreviewCancelButton.setOnClickListener(v -> {
                        hideMessageReference(messageReferenceBinding);
                        conversation.setMessageReference(null);
                        conversationFragment.updateChatMsgHint();
                    });

                    // Jump to the referenced message when the message reference preview is clicked.
                    messageReferenceBinding.messageReferenceContainer.setOnClickListener(v -> conversationFragment.setSelection(jumpingPosition, false));
                } else {
                    // Jump to the referenced message when the message reference is clicked.
                    messageReferenceBinding.messageReferenceContainer.setOnClickListener(v -> {
                        if (jumpingPosition == -1) {
                            activity.xmppConnectionService.loadMoreMessages(referencedMessage, conversationFragment.getOnMoreMessagesLoadedImpl(conversationFragment.getView().findViewById(R.id.messages_view), referencedMessage));
                        } else {
                            conversationFragment.setSelection(jumpingPosition, false);
                        }
                    });
                }
            }

        }
    }

    /**
     * Creates an info text that contains the sender and the date for a given referenced message
     * or a hint if the referenced message is null.
     * @param activity current activity
     * @param messageReferenceBinding data binding that holds the message reference views
     * @param referencedMessage referenced message for that the info text is generated
     */
    private static void createInfo(final XmppActivity activity, final MessageReferenceBinding messageReferenceBinding, final Message referencedMessage) {
        String info;

        if (referencedMessage == null) {
            info = activity.getResources().getString(R.string.message_not_found);
        } else {
            // Set the name of the author of the message as the tag.
            info = UIHelper.getMessageDisplayName(referencedMessage);

            // Replace the name of the author with a standard identifier for the user if the user is the author of the message.
            if (info.equals(((Conversation) referencedMessage.getConversation()).getMucOptions().getSelf().getName())) {
                info = activity.getString(R.string.me);
            }

            // Add the time when the message was sent to the tag.
            info += "\n" + UIHelper.readableTimeDifferenceFull(activity, referencedMessage.getMergedTimeSent());
        }

        messageReferenceBinding.messageReferenceInfo.setText(info);
    }

    /**
     * Sets the image for the message reference icon depending on the color of its background.
     * @param darkBackground specifies if the background of the message reference icon is dark
     * @param messageReferenceBinding data binding that holds the message reference views
     * @param defaultDrawable drawable that will be used as the message reference icon if its background is light
     * @param drawableForDarkBackground drawable that will be used as the message reference icon if its background is dark
     */
    private static void setMessageReferenceIcon(final boolean darkBackground, final MessageReferenceBinding messageReferenceBinding, final Drawable defaultDrawable, final Drawable drawableForDarkBackground) {
        if (darkBackground) {
            messageReferenceBinding.messageReferenceIcon.setBackground(drawableForDarkBackground);
        } else {
            messageReferenceBinding.messageReferenceIcon.setBackground(defaultDrawable);
        }
    }

    /**
     * Displays a thumbnail for the image or video of the referenced message.
     */
    private static void displayReferencedImageMessage(final XmppActivity activity, final MessageReferenceBinding messageReferenceBinding, final Message referencedMessage, final boolean messageReferencePreview) {
        if (messageReferencePreview) {
            // Set the scale type manually only for the message reference preview since a common scale type cannot be used.
            messageReferenceBinding.messageReferenceImageThumbnail.setScaleType(ImageView.ScaleType.FIT_START);

            // Remove the rounded corners of the thumbnail in the message reference preview.
            messageReferenceBinding.messageReferenceImageThumbnail.setCornerRadius(0);
        }

        activity.loadBitmapForReferencedImageMessage(referencedMessage, messageReferenceBinding.messageReferenceImageThumbnail);
        messageReferenceBinding.messageReferenceImageThumbnail.setVisibility(View.VISIBLE);
    }

    /**
     * Creates a string with newlines for each string of a given array of strings.
     * @param allLines string array that contains all strings
     * @param indexOfFirstLineToTake position (inclusive) of the first string to be taken for the newly created string
     * @param indexOfLastLineToTake position (exclusive) of the last string to be taken for the newly created string
     * @return string with the desired lines
     */
    private static String createStringWithLinesOutOfStringArray(final String[] allLines, final int indexOfFirstLineToTake, final int indexOfLastLineToTake) {
        StringBuilder takingBuilder = new StringBuilder();
        for (String line : Arrays.copyOfRange(allLines, indexOfFirstLineToTake, indexOfLastLineToTake)) {
            takingBuilder.append("\n").append(line);
        }

        // Delete the first newline ("\n") because it is not needed.
        takingBuilder.delete(0, 1);

        return takingBuilder.toString();
    }

    /**
     * Extracts the first lines of a message's body.
     * This can be used to show only a specific number of body lines in the message reference text view
     * since the truncation with "ellipsize" and "maxLines" does not adjust the width for the first lines.
     * Instead the width is adjusted for the longest line even if that line will not be displayed
     * and this leaves an space on the right side.
     * @param message message for that the first lines of its body will be extracted
     * @param linesToBeExtracted number of lines to be extracted
     * @return extracted lines
     */
    private static String extractFirstLinesOfBody(final Message message, int linesToBeExtracted) {
        String[] bodyLines = message.getBody().split("\n");

        // Reduce the number of lines to be extracted if the body has less lines than that number.
        if (linesToBeExtracted > bodyLines.length) {
            linesToBeExtracted = bodyLines.length;
        }

        String firstLinesOfBody = createStringWithLinesOutOfStringArray(bodyLines, 0, linesToBeExtracted);

        if (linesToBeExtracted < bodyLines.length) {
            firstLinesOfBody += "...";
        }

        return firstLinesOfBody;
    }

    /**
     * Extracts the first two lines of a message's body.
     * @param message message for that the first lines of its body will be extracted
     * @return extracted lines
     */
    private static String extractFirstTwoLinesOfBody(final Message message) {
        return extractFirstLinesOfBody(message, 2);
    }

    /**
     * Deletes legacy quotation added for backward compatibility if present but preserve independent quotations.
     */
    public static void deleteLegacyQuotation(final XmppActivity activity, final Message message, final Message referencedMessage) {
        if (referencedMessage != null && referencedMessage.getEncryption() != Message.ENCRYPTION_DECRYPTION_FAILED && referencedMessage.getEncryption() != Message.ENCRYPTION_AXOLOTL_NOT_FOR_THIS_DEVICE) {
            String messageBody = message.getBody();
            String[] messageBodyLines = messageBody.split("\n");
            int numberOfMessageBodyLines = messageBodyLines.length;
            String[] referencedMessageBodyLines = referencedMessage.getBody().split("\n");
            int numberOfReferencedMessageBodyLines = referencedMessageBodyLines.length;

            if (messageBody.length() > 0 && numberOfMessageBodyLines >= numberOfReferencedMessageBodyLines && (messageBody.charAt(0) == '>' || messageBody.charAt(0) == '\u00bb')) {

                // If the referenced message is a file message
                // and the first quoted line is the URL of the referenced file message,
                // remove that line from the message's body.
                // This is necessary as a separate case for image messages since the URL can be compared without other FileParams like the dimensions.
                // If the referenced message is not a file message, remove all quoted lines from the message's body
                // that are lines of the referenced message.
                if (referencedMessage.hasFileOnRemoteHost()) {
                    String line = messageBodyLines[0];
                    if (UIHelper.isQuotationLine(line)) {
                        if (line.substring(1).trim().equals(referencedMessage.getFileParams().url.toString())) {
                            message.setBody(MessageReferenceUtils.createStringWithLinesOutOfStringArray(messageBodyLines, 1, messageBodyLines.length));
                        }
                    }
                } else {
                    // Take only the part of the body that contains the comment without legacy quotation.
                    int currentLine = 0;
                    boolean quotationEqualsReferencedMessage = true;
                    for (String line : messageBodyLines) {
                        if (currentLine < numberOfReferencedMessageBodyLines) {
                            if (!(UIHelper.isQuotationLine(line) && line.substring(1).trim().equals(referencedMessageBodyLines[currentLine].trim()))) {
                                quotationEqualsReferencedMessage = false;
                                break;
                            }
                        } else {
                            break;
                        }
                        currentLine++;
                    }

                    // Delete the legacy quotation of the message and update the message in the database.
                    if (quotationEqualsReferencedMessage) {
                        message.setBody(MessageReferenceUtils.createStringWithLinesOutOfStringArray(messageBodyLines, currentLine, messageBodyLines.length));
                        activity.xmppConnectionService.updateMessage(message);
                    }
                }
            }
        }
    }
}