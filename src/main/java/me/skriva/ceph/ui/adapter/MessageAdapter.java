package me.skriva.ceph.ui.adapter;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.base.Strings;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.skriva.ceph.Config;
import me.skriva.ceph.R;
import me.skriva.ceph.crypto.axolotl.FingerprintStatus;
import me.skriva.ceph.databinding.MessageReferenceBinding;
import me.skriva.ceph.entities.Account;
import me.skriva.ceph.entities.Conversation;
import me.skriva.ceph.entities.Conversational;
import me.skriva.ceph.entities.DownloadableFile;
import me.skriva.ceph.entities.Message;
import me.skriva.ceph.entities.Message.FileParams;
import me.skriva.ceph.entities.Transferable;
import me.skriva.ceph.http.P1S3UrlStreamHandler;
import me.skriva.ceph.persistance.FileBackend;
import me.skriva.ceph.services.MessageArchiveService;
import me.skriva.ceph.services.NotificationService;
import me.skriva.ceph.ui.ConversationFragment;
import me.skriva.ceph.ui.ConversationsActivity;
import me.skriva.ceph.ui.FullscreenImageActivity;
import me.skriva.ceph.ui.XmppActivity;
import me.skriva.ceph.ui.service.AudioPlayer;
import me.skriva.ceph.ui.text.DividerSpan;
import me.skriva.ceph.ui.text.QuoteSpan;
import me.skriva.ceph.ui.util.AvatarWorkerTask;
import me.skriva.ceph.ui.util.MessageReferenceUtils;
import me.skriva.ceph.ui.util.MyLinkify;
import me.skriva.ceph.ui.util.ViewUtil;
import me.skriva.ceph.ui.widget.ClickableMovementMethod;
import me.skriva.ceph.ui.widget.CopyTextView;
import me.skriva.ceph.ui.widget.ListSelectionManager;
import me.skriva.ceph.utils.CryptoHelper;
import me.skriva.ceph.utils.EmojiWrapper;
import me.skriva.ceph.utils.Emoticons;
import me.skriva.ceph.utils.GeoHelper;
import me.skriva.ceph.utils.StylingHelper;
import me.skriva.ceph.utils.UIHelper;
import me.skriva.ceph.xmpp.mam.MamReference;
import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;
import rocks.xmpp.addr.Jid;

public class MessageAdapter extends ArrayAdapter<Message> implements CopyTextView.CopyHandler {

	public static final String DATE_SEPARATOR_BODY = "DATE_SEPARATOR";
	private static final int SENT = 0;
	private static final int RECEIVED = 1;
	private static final int STATUS = 2;
	private static final int DATE_SEPARATOR = 3;
	private final XmppActivity activity;
	private final ListSelectionManager listSelectionManager = new ListSelectionManager();
	private final AudioPlayer audioPlayer;
	private List<String> highlightedTerm = null;
	private final DisplayMetrics metrics;
	private OnContactPictureClicked mOnContactPictureClickedListener;
	private OnContactPictureLongClicked mOnContactPictureLongClickedListener;
	private boolean mIndicateReceived = false;
	private boolean mUseGreenBackground = false;
	private OnCommentListener onCommentListener;
	public MessageAdapter(XmppActivity activity, List<Message> messages) {
		super(activity, 0, messages);
		this.audioPlayer = new AudioPlayer(this);
		this.activity = activity;
		metrics = getContext().getResources().getDisplayMetrics();
		updatePreferences();
	}



	private static void resetClickListener(View... views) {
		for (View view : views) {
			view.setOnClickListener(null);
		}
	}

	public void flagScreenOn() {
		activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	public void flagScreenOff() {
		activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	public void setOnContactPictureClicked(OnContactPictureClicked listener) {
		this.mOnContactPictureClickedListener = listener;
	}

	public Activity getActivity() {
		return activity;
	}

	public void setOnContactPictureLongClicked(
			OnContactPictureLongClicked listener) {
		this.mOnContactPictureLongClickedListener = listener;
	}

	public void setOnCommentListener(OnCommentListener listener) {
		this.onCommentListener = listener;
	}

	@Override
	public int getViewTypeCount() {
		return 4;
	}

	private int getItemViewType(Message message) {
		if (message.getType() == Message.TYPE_STATUS) {
			if (DATE_SEPARATOR_BODY.equals(message.getBody())) {
				return DATE_SEPARATOR;
			} else {
				return STATUS;
			}
		} else if (message.getStatus() <= Message.STATUS_RECEIVED) {
			return RECEIVED;
		}

		return SENT;
	}

	@Override
	public int getItemViewType(int position) {
		return this.getItemViewType(getItem(position));
	}

	private int getMessageTextColor(boolean onDark) {
		if (onDark) {
			return ContextCompat.getColor(activity, false ? R.color.white : R.color.white70);
		} else {
			return ContextCompat.getColor(activity, false ? R.color.black87 : R.color.black54);
		}
	}

	private void displayStatus(ViewHolder viewHolder, Message message, int type, boolean darkBackground) {
		String filesize = null;
		String info = null;
		boolean error = false;
		if (viewHolder.indicatorReceived != null) {
			viewHolder.indicatorReceived.setVisibility(View.GONE);
		}

		if (viewHolder.edit_indicator != null) {
			if (message.edited()) {
				viewHolder.edit_indicator.setVisibility(View.VISIBLE);
				viewHolder.edit_indicator.setImageResource(darkBackground ? R.drawable.ic_mode_edit_white_18dp : R.drawable.ic_mode_edit_black_18dp);
				viewHolder.edit_indicator.setAlpha(darkBackground ? 0.7f : 0.57f);
			} else {
				viewHolder.edit_indicator.setVisibility(View.GONE);
			}
		}
		final Transferable transferable = message.getTransferable();
		boolean multiReceived = message.getConversation().getMode() == Conversation.MODE_MULTI
				&& message.getMergedStatus() <= Message.STATUS_RECEIVED;
		if (message.isFileOrImage() || transferable != null) {
			FileParams params = message.getFileParams();
			filesize = params.size > 0 ? UIHelper.filesizeToString(params.size) : null;
			if (transferable != null && transferable.getStatus() == Transferable.STATUS_FAILED) {
				error = true;
			}
		}
		switch (message.getMergedStatus()) {
			case Message.STATUS_WAITING:
				info = getContext().getString(R.string.waiting);
				break;
			case Message.STATUS_UNSEND:
				if (transferable != null) {
					info = getContext().getString(R.string.sending_file, transferable.getProgress());
				} else {
					info = getContext().getString(R.string.sending);
				}
				break;
			case Message.STATUS_OFFERED:
				info = getContext().getString(R.string.offering);
				break;
			case Message.STATUS_SEND_RECEIVED:
				if (mIndicateReceived) {
					viewHolder.indicatorReceived.setVisibility(View.VISIBLE);
				}
				break;
			case Message.STATUS_SEND_DISPLAYED:
				if (mIndicateReceived) {
					viewHolder.indicatorReceived.setVisibility(View.VISIBLE);
				}
				break;
			case Message.STATUS_SEND_FAILED:
				final String errorMessage = message.getErrorMessage();
				if (Message.ERROR_MESSAGE_CANCELLED.equals(errorMessage)) {
					info = getContext().getString(R.string.cancelled);
				} else if (errorMessage != null) {
					final String[] errorParts = errorMessage.split("\\u001f", 2);
					if (errorParts.length == 2) {
						if ("file-too-large".equals(errorParts[0])) {
							info = getContext().getString(R.string.file_too_large);
						} else {
							info = getContext().getString(R.string.send_failed);
						}
					} else {
						info = getContext().getString(R.string.send_failed);
					}
				} else {
					info = getContext().getString(R.string.send_failed);
				}
				error = true;
				break;
			default:
				if (multiReceived) {
					info = UIHelper.getMessageDisplayName(message);
				}
				break;
		}
		if (error && type == SENT) {
			if (darkBackground) {
				viewHolder.time.setTextAppearance(getContext(), R.style.TextAppearance_Conversations_Caption_Warning_OnDark);
			} else {
				viewHolder.time.setTextAppearance(getContext(), R.style.TextAppearance_Conversations_Caption_Warning);
			}
		} else {
			if (darkBackground) {
				viewHolder.time.setTextAppearance(getContext(), R.style.TextAppearance_Conversations_Caption_OnDark);
			} else {
				viewHolder.time.setTextAppearance(getContext(), R.style.TextAppearance_Conversations_Caption);
			}
			viewHolder.time.setTextColor(this.getMessageTextColor(darkBackground));
		}
		if (message.getEncryption() == Message.ENCRYPTION_NONE) {
			viewHolder.indicator.setVisibility(View.GONE);
		} else {
			boolean verified = false;
			if (message.getEncryption() == Message.ENCRYPTION_AXOLOTL) {
				final FingerprintStatus status = message.getConversation()
						.getAccount().getAxolotlService().getFingerprintTrust(
								message.getFingerprint());
				if (status != null && status.isVerified()) {
					verified = true;
				}
			}
			if (verified) {
				viewHolder.indicator.setImageResource(darkBackground ? R.drawable.ic_verified_user_white_18dp : R.drawable.ic_verified_user_black_18dp);
			} else {
				viewHolder.indicator.setImageResource(darkBackground ? R.drawable.ic_lock_white_18dp : R.drawable.ic_lock_black_18dp);
			}
			if (darkBackground) {
				viewHolder.indicator.setAlpha(0.7f);
			} else {
				viewHolder.indicator.setAlpha(0.57f);
			}
			viewHolder.indicator.setVisibility(View.VISIBLE);
		}

		String formatedTime = UIHelper.readableTimeDifferenceFull(getContext(), message.getMergedTimeSent());
		if (message.getStatus() <= Message.STATUS_RECEIVED) {
			if ((filesize != null) && (info != null)) {
				viewHolder.time.setText(formatedTime + " \u00B7 " + filesize + " \u00B7 " + info);
			} else if ((filesize == null) && (info != null)) {
				viewHolder.time.setText(formatedTime + " \u00B7 " + info);
			} else if ((filesize != null) && (info == null)) {
				viewHolder.time.setText(formatedTime + " \u00B7 " + filesize);
			} else {
				viewHolder.time.setText(formatedTime);
			}
		} else {
			if ((filesize != null) && (info != null)) {
				viewHolder.time.setText(filesize + " \u00B7 " + info);
			} else if ((filesize == null) && (info != null)) {
				if (error) {
					viewHolder.time.setText(info + " \u00B7 " + formatedTime);
				} else {
					viewHolder.time.setText(info);
				}
			} else if ((filesize != null) && (info == null)) {
				viewHolder.time.setText(filesize + " \u00B7 " + formatedTime);
			} else {
				viewHolder.time.setText(formatedTime);
			}
		}
	}

	private void displayInfoMessage(ViewHolder viewHolder, CharSequence text, boolean darkBackground) {
		viewHolder.download_button.setVisibility(View.GONE);
		viewHolder.audioPlayer.setVisibility(View.GONE);
		viewHolder.image.setVisibility(View.GONE);
		viewHolder.gifImage.setVisibility(View.GONE);
		viewHolder.gif_btn.setVisibility(View.GONE);
		viewHolder.messageBody.setVisibility(View.VISIBLE);
		viewHolder.messageBody.setText(text);
		if (darkBackground) {
			viewHolder.messageBody.setTextAppearance(getContext(), R.style.TextAppearance_Conversations_Body1_Secondary_OnDark);
		} else {
			viewHolder.messageBody.setTextAppearance(getContext(), R.style.TextAppearance_Conversations_Body1_Secondary);
		}
		viewHolder.messageBody.setTextIsSelectable(false);
	}

	private void displayEmojiMessage(final ViewHolder viewHolder, final String body, final boolean darkBackground) {
		viewHolder.download_button.setVisibility(View.GONE);
		viewHolder.audioPlayer.setVisibility(View.GONE);
		viewHolder.image.setVisibility(View.GONE);
		viewHolder.gifImage.setVisibility(View.GONE);
		viewHolder.gif_btn.setVisibility(View.GONE);
		viewHolder.messageBody.setVisibility(View.VISIBLE);
		if (darkBackground) {
			viewHolder.messageBody.setTextAppearance(getContext(), R.style.TextAppearance_Conversations_Body1_Emoji_OnDark);
		} else {
			viewHolder.messageBody.setTextAppearance(getContext(), R.style.TextAppearance_Conversations_Body1_Emoji);
		}
		Spannable span = new SpannableString(body);
		float size = Emoticons.isEmoji(body) ? 3.0f : 2.0f;
		span.setSpan(new RelativeSizeSpan(size), 0, body.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		viewHolder.messageBody.setText(EmojiWrapper.transform(span));
	}

	private void applyQuoteSpan(SpannableStringBuilder body, int start, int end, boolean darkBackground) {
		if (start > 1 && !"\n\n".equals(body.subSequence(start - 2, start).toString())) {
			body.insert(start++, "\n");
			body.setSpan(new DividerSpan(false), start - 2, start, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			end++;
		}
		if (end < body.length() - 1 && !"\n\n".equals(body.subSequence(end, end + 2).toString())) {
			body.insert(end, "\n");
			body.setSpan(new DividerSpan(false), end, end + 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		int color = darkBackground ? this.getMessageTextColor(darkBackground)
				: ContextCompat.getColor(activity, R.color.green700_desaturated);
		DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
		body.setSpan(new QuoteSpan(color, metrics), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	}

	/**
	 * Applies QuoteSpan to group of lines which starts with > or » characters.
	 * Appends likebreaks and applies DividerSpan to them to show a padding between quote and text.
	 */
	private boolean handleTextQuotes(SpannableStringBuilder body, boolean darkBackground) {
		boolean startsWithQuote = false;
		char previous = '\n';
		int lineStart = -1;
		int lineTextStart = -1;
		int quoteStart = -1;
		for (int i = 0; i <= body.length(); i++) {
			char current = body.length() > i ? body.charAt(i) : '\n';
			if (lineStart == -1) {
				if (previous == '\n') {
					if ((current == '>' && UIHelper.isPositionFollowedByQuoteableCharacter(body, i))
							|| current == '\u00bb' && !UIHelper.isPositionFollowedByQuote(body, i)) {
						// Line start with quote
						lineStart = i;
						if (quoteStart == -1) quoteStart = i;
						if (i == 0) startsWithQuote = true;
					} else if (quoteStart >= 0) {
						// Line start without quote, apply spans there
						applyQuoteSpan(body, quoteStart, i - 1, darkBackground);
						quoteStart = -1;
					}
				}
			} else {
				// Remove extra spaces between > and first character in the line
				// > character will be removed too
				if (current != ' ' && lineTextStart == -1) {
					lineTextStart = i;
				}
				if (current == '\n') {
					body.delete(lineStart, lineTextStart);
					i -= lineTextStart - lineStart;
					if (i == lineStart) {
						// Avoid empty lines because span over empty line can be hidden
						body.insert(i++, " ");
					}
					lineStart = -1;
					lineTextStart = -1;
				}
			}
			previous = current;
		}
		if (quoteStart >= 0) {
			// Apply spans to finishing open quote
			applyQuoteSpan(body, quoteStart, body.length(), darkBackground);
		}
		return startsWithQuote;
	}

	/**
	 * Displays the text, image, preview image or tag of a referenced message next to a bar that indicates the referencing
	 * and underneath the comment on that message.
	 * Or displays only an info message for a message reference that has no associated message.
	 */
	private void displayReferencingMessage(final ViewHolder viewHolder, final Message message, final Message referencedMessage, boolean darkBackground, int type) {
		// Show the message reference area.
		MessageReferenceUtils.displayMessageReference(activity, viewHolder.messageReferenceBinding, message, referencedMessage, darkBackground);

		// Show the comment on the referenced message.
		displayTextMessage(viewHolder, message, darkBackground, type);
	}

	private void displayTextMessage(final ViewHolder viewHolder, final Message message, boolean darkBackground, int type) {
		viewHolder.download_button.setVisibility(View.GONE);
		viewHolder.image.setVisibility(View.GONE);
		viewHolder.gifImage.setVisibility(View.GONE);
		viewHolder.gif_btn.setVisibility(View.GONE);
		viewHolder.audioPlayer.setVisibility(View.GONE);
		viewHolder.messageBody.setVisibility(View.VISIBLE);

		if (darkBackground) {
			viewHolder.messageBody.setTextAppearance(getContext(), R.style.TextAppearance_Conversations_Body1_OnDark);
		} else {
			viewHolder.messageBody.setTextAppearance(getContext(), R.style.TextAppearance_Conversations_Body1);
		}
		viewHolder.messageBody.setHighlightColor(ContextCompat.getColor(activity, darkBackground
				? (type == SENT || !mUseGreenBackground ? R.color.black26 : R.color.grey800) : R.color.grey500));
		viewHolder.messageBody.setTypeface(null, Typeface.NORMAL);

		if (message.getBody() != null) {
			final String nick = UIHelper.getMessageDisplayName(message);
			SpannableStringBuilder body = message.getMergedBody();
			boolean hasMeCommand = message.hasMeCommand();
			if (hasMeCommand) {
				body = body.replace(0, Message.ME_COMMAND.length(), nick + " ");
			}
			if (body.length() > Config.MAX_DISPLAY_MESSAGE_CHARS) {
				body = new SpannableStringBuilder(body, 0, Config.MAX_DISPLAY_MESSAGE_CHARS);
				body.append("\u2026");
			}
			Message.MergeSeparator[] mergeSeparators = body.getSpans(0, body.length(), Message.MergeSeparator.class);
			for (Message.MergeSeparator mergeSeparator : mergeSeparators) {
				int start = body.getSpanStart(mergeSeparator);
				int end = body.getSpanEnd(mergeSeparator);
				body.setSpan(new DividerSpan(true), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			boolean startsWithQuote = handleTextQuotes(body, darkBackground);
			if (!message.isPrivateMessage()) {
				if (hasMeCommand) {
					body.setSpan(new StyleSpan(Typeface.BOLD_ITALIC), 0, nick.length(),
							Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
			} else {
				String privateMarker;
				if (message.getStatus() <= Message.STATUS_RECEIVED) {
					privateMarker = activity.getString(R.string.private_message);
				} else {
					Jid cp = message.getCounterpart();
					privateMarker = activity.getString(R.string.private_message_to, Strings.nullToEmpty(cp == null ? null : cp.getResource()));
				}
				body.insert(0, privateMarker);
				int privateMarkerIndex = privateMarker.length();
				if (startsWithQuote) {
					body.insert(privateMarkerIndex, "\n\n");
					body.setSpan(new DividerSpan(false), privateMarkerIndex, privateMarkerIndex + 2,
							Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				} else {
					body.insert(privateMarkerIndex, " ");
				}
				body.setSpan(new ForegroundColorSpan(getMessageTextColor(darkBackground)), 0, privateMarkerIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				body.setSpan(new StyleSpan(Typeface.BOLD), 0, privateMarkerIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				if (hasMeCommand) {
					body.setSpan(new StyleSpan(Typeface.BOLD_ITALIC), privateMarkerIndex + 1,
							privateMarkerIndex + 1 + nick.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
			}
			if (message.getConversation().getMode() == Conversation.MODE_MULTI && message.getStatus() == Message.STATUS_RECEIVED) {
				if (message.getConversation() instanceof Conversation) {
					final Conversation conversation = (Conversation) message.getConversation();
					Pattern pattern = NotificationService.generateNickHighlightPattern(conversation.getMucOptions().getActualNick());
					Matcher matcher = pattern.matcher(body);
					while (matcher.find()) {
						body.setSpan(new StyleSpan(Typeface.BOLD), matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					}
				}
			}
			Matcher matcher = Emoticons.getEmojiPattern(body).matcher(body);
			while (matcher.find()) {
				if (matcher.start() < matcher.end()) {
					body.setSpan(new RelativeSizeSpan(1.2f), matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
			}

			StylingHelper.format(body, viewHolder.messageBody.getCurrentTextColor());
			if (highlightedTerm != null) {
				StylingHelper.highlight(activity, body, highlightedTerm, StylingHelper.isDarkText(viewHolder.messageBody));
			}
			MyLinkify.addLinks(body,true);
			viewHolder.messageBody.setAutoLinkMask(0);
			viewHolder.messageBody.setText(EmojiWrapper.transform(body));
			viewHolder.messageBody.setTextIsSelectable(true);
			viewHolder.messageBody.setMovementMethod(ClickableMovementMethod.getInstance());
			listSelectionManager.onUpdate(viewHolder.messageBody, message);
		} else {
			viewHolder.messageBody.setText("");
			viewHolder.messageBody.setTextIsSelectable(false);
		}
	}

	private void displayDownloadableMessage(ViewHolder viewHolder, final Message message, String text, final boolean darkBackground) {
		toggleWhisperInfo(viewHolder, message, darkBackground);
		viewHolder.image.setVisibility(View.GONE);
		viewHolder.gifImage.setVisibility(View.GONE);
		viewHolder.gif_btn.setVisibility(View.GONE);
		viewHolder.audioPlayer.setVisibility(View.GONE);
		viewHolder.download_button.setVisibility(View.VISIBLE);
		viewHolder.download_button.setText(text);
		viewHolder.download_button.setOnClickListener(v -> ConversationFragment.downloadFile(activity, message));
	}

	private void displayOpenableMessage(ViewHolder viewHolder, final Message message, final boolean darkBackground) {
		toggleWhisperInfo(viewHolder, message, darkBackground);
		viewHolder.image.setVisibility(View.GONE);
		viewHolder.gifImage.setVisibility(View.GONE);
		viewHolder.gif_btn.setVisibility(View.GONE);
		viewHolder.audioPlayer.setVisibility(View.GONE);
		viewHolder.download_button.setVisibility(View.VISIBLE);
		viewHolder.download_button.setText(activity.getString(R.string.open_x_file, UIHelper.getFileDescriptionString(activity, message)));
		viewHolder.download_button.setOnClickListener(v -> openDownloadable(message));
	}

	private void displayLocationMessage(ViewHolder viewHolder, final Message message, final boolean darkBackground) {
		toggleWhisperInfo(viewHolder, message, darkBackground);
		viewHolder.image.setVisibility(View.GONE);
		viewHolder.gifImage.setVisibility(View.GONE);
		viewHolder.gif_btn.setVisibility(View.GONE);
		viewHolder.audioPlayer.setVisibility(View.GONE);
		viewHolder.download_button.setVisibility(View.VISIBLE);
		viewHolder.download_button.setText(R.string.show_location);
		viewHolder.download_button.setOnClickListener(v -> showLocation(message));
	}

	private void displayAudioMessage(ViewHolder viewHolder, Message message, boolean darkBackground) {
		toggleWhisperInfo(viewHolder, message, darkBackground);
		viewHolder.image.setVisibility(View.GONE);
		viewHolder.gifImage.setVisibility(View.GONE);
		viewHolder.gif_btn.setVisibility(View.GONE);
		viewHolder.download_button.setVisibility(View.GONE);
		final RelativeLayout audioPlayer = viewHolder.audioPlayer;
		audioPlayer.setVisibility(View.VISIBLE);
		AudioPlayer.ViewHolder.get(audioPlayer).setDarkBackground(darkBackground);
		this.audioPlayer.init(audioPlayer, message);
	}

	@SuppressLint("ClickableViewAccessibility")
	private void displayImageMessage(ViewHolder viewHolder, final Message message, final boolean darkBackground) {
		toggleWhisperInfo(viewHolder, message, darkBackground);
		viewHolder.download_button.setVisibility(View.GONE);
		viewHolder.audioPlayer.setVisibility(View.GONE);
		DownloadableFile file = activity.xmppConnectionService.getFileBackend().getFile(message);
		if (!file.exists()) {
			Toast.makeText(activity, R.string.file_deleted, Toast.LENGTH_SHORT).show();
			return;
		}
		double target = metrics.density * 288;
		String mime = file.getMimeType();
		if (mime != null && mime.equals("image/gif")) {
			Log.d(Config.LOGTAG, "GIF Image");
			viewHolder.image.setVisibility(View.GONE);
			viewHolder.gifImage.setVisibility(View.VISIBLE);
		} else {
			Log.d(Config.LOGTAG, "Image");
			viewHolder.image.setVisibility(View.VISIBLE);
			viewHolder.gifImage.setVisibility(View.GONE);
			viewHolder.gif_btn.setVisibility(View.GONE);
		}
		FileParams params = message.getFileParams();
		int scaledW;
		int scaledH;
		if (Math.max(params.height, params.width) * metrics.density <= target) {
			scaledW = (int) (params.width * metrics.density);
			scaledH = (int) (params.height * metrics.density);
		} else if (!mime.equals("image/gif") && Math.max(params.height, params.width) <= target) {
			scaledW = params.width;
			scaledH = params.height;
		} else if (params.width <= params.height) {
			scaledW = (int) (params.width / ((double) params.height / target));
			scaledH = (int) target;
		} else {
			scaledW = (int) target;
			scaledH = (int) (params.height / ((double) params.width / target));
		}
		if (mime != null && mime.equals("image/gif")) {
			FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(scaledW, scaledH);
			layoutParams.setMargins(0, (int) (metrics.density * 4), 0, (int) (metrics.density * 4));
			viewHolder.gifImage.setLayoutParams(layoutParams);
			if (message.getMimeType() != null && message.getMimeType().endsWith("/gif")) {
				GifDrawable drawable;
				try {
					drawable = new GifDrawable(file);
				} catch (IOException error) {
					Log.d(Config.LOGTAG, "File not found.");
					return;
				}
				drawable.stop();
				viewHolder.gif_btn.setVisibility(View.VISIBLE);
				viewHolder.gifImage.setImageDrawable(drawable);
				viewHolder.gifImage.setOnClickListener(v -> {
					// Play/Pause
					if (drawable.isRunning()) {
						drawable.stop();
						viewHolder.gif_btn.setVisibility(View.VISIBLE);
					} else {
						drawable.start();
						viewHolder.gif_btn.setVisibility(View.INVISIBLE);
					}

				});
			}
		} else {
			FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(scaledW, scaledH);
			layoutParams.setMargins(0, (int) (metrics.density * 4), 0, (int) (metrics.density * 4));
			viewHolder.image.setLayoutParams(layoutParams);
			activity.loadBitmap(message, viewHolder.image);
			if (mime != null && mime.startsWith("image/") && !mime.contains("gif")) {
				viewHolder.image.setOnClickListener(v -> showImage(message));
			} else {
				viewHolder.image.setOnClickListener(v -> openDownloadable(message));
			}
		}
	}

	private void toggleWhisperInfo(ViewHolder viewHolder, final Message message, final boolean darkBackground) {
		if (message.isPrivateMessage()) {
			final String privateMarker;
			if (message.getStatus() <= Message.STATUS_RECEIVED) {
				privateMarker = activity.getString(R.string.private_message);
			} else {
				Jid cp = message.getCounterpart();
				privateMarker = activity.getString(R.string.private_message_to, Strings.nullToEmpty(cp == null ? null : cp.getResource()));
			}
			final SpannableString body = new SpannableString(privateMarker);
			body.setSpan(new ForegroundColorSpan(getMessageTextColor(darkBackground)), 0, privateMarker.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			body.setSpan(new StyleSpan(Typeface.BOLD), 0, privateMarker.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			viewHolder.messageBody.setText(body);
			viewHolder.messageBody.setVisibility(View.VISIBLE);
		} else {
			viewHolder.messageBody.setVisibility(View.GONE);
		}
	}

	private void loadMoreMessages(Conversation conversation) {
		conversation.setLastClearHistory(0, null);
		activity.xmppConnectionService.updateConversation(conversation);
		conversation.setHasMessagesLeftOnServer(true);
		conversation.setFirstMamReference(null);
		long timestamp = conversation.getLastMessageTransmitted().getTimestamp();
		if (timestamp == 0) {
			timestamp = System.currentTimeMillis();
		}
		conversation.messagesLoaded.set(true);
		MessageArchiveService.Query query = activity.xmppConnectionService.getMessageArchiveService().query(conversation, new MamReference(0), timestamp, false);
		if (query != null) {
			Toast.makeText(activity, R.string.fetching_history_from_server, Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(activity, R.string.not_fetching_history_retention_period, Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public View getView(int position, View view, ViewGroup parent) {
		final Message message = getItem(position);
		final boolean omemoEncryption = message.getEncryption() == Message.ENCRYPTION_AXOLOTL;
		final boolean isInValidSession = message.isValidInSession() && (!omemoEncryption || message.isTrusted());
		final Conversational conversation = message.getConversation();
		final Account account = conversation.getAccount();
		final int type = getItemViewType(position);
		ViewHolder viewHolder;
		if (view == null) {
			viewHolder = new ViewHolder();
			switch (type) {
				case DATE_SEPARATOR:
					view = activity.getLayoutInflater().inflate(R.layout.message_date_bubble, parent, false);
					viewHolder.status_message = view.findViewById(R.id.message_body);
					viewHolder.message_box = view.findViewById(R.id.message_box);
					break;
				case SENT:
					view = activity.getLayoutInflater().inflate(R.layout.message_sent, parent, false);
					viewHolder.message_box = view.findViewById(R.id.message_box);
					viewHolder.messageReferenceBinding = MessageReferenceBinding.bind(view.findViewById(R.id.message_reference));
					viewHolder.contact_picture = view.findViewById(R.id.message_photo);
					viewHolder.download_button = view.findViewById(R.id.download_button);
					viewHolder.indicator = view.findViewById(R.id.security_indicator);
					viewHolder.edit_indicator = view.findViewById(R.id.edit_indicator);
					viewHolder.image = view.findViewById(R.id.message_image);
					viewHolder.gif_btn = view.findViewById(R.id.message_gif_btn);
					viewHolder.gifImage = view.findViewById(R.id.message_image_gif);
					viewHolder.messageBody = view.findViewById(R.id.message_body);
					viewHolder.time = view.findViewById(R.id.message_time);
					viewHolder.indicatorReceived = view.findViewById(R.id.indicator_received);
					viewHolder.audioPlayer = view.findViewById(R.id.audio_player);
					break;
				case RECEIVED:
					view = activity.getLayoutInflater().inflate(R.layout.message_received, parent, false);
					viewHolder.message_box = view.findViewById(R.id.message_box);
					viewHolder.messageReferenceBinding = MessageReferenceBinding.bind(view.findViewById(R.id.message_reference));
					viewHolder.contact_picture = view.findViewById(R.id.message_photo);
					viewHolder.download_button = view.findViewById(R.id.download_button);
					viewHolder.indicator = view.findViewById(R.id.security_indicator);
					viewHolder.edit_indicator = view.findViewById(R.id.edit_indicator);
					viewHolder.image = view.findViewById(R.id.message_image);
					viewHolder.gif_btn = view.findViewById(R.id.message_gif_btn);
					viewHolder.gifImage = view.findViewById(R.id.message_image_gif);
					viewHolder.messageBody = view.findViewById(R.id.message_body);
					viewHolder.time = view.findViewById(R.id.message_time);
					viewHolder.indicatorReceived = view.findViewById(R.id.indicator_received);
					viewHolder.encryption = view.findViewById(R.id.message_encryption);
					viewHolder.audioPlayer = view.findViewById(R.id.audio_player);
					break;
				case STATUS:
					view = activity.getLayoutInflater().inflate(R.layout.message_status, parent, false);
					viewHolder.contact_picture = view.findViewById(R.id.message_photo);
					viewHolder.status_message = view.findViewById(R.id.status_message);
					viewHolder.load_more_messages = view.findViewById(R.id.load_more_messages);
					break;
				default:
					throw new AssertionError("Unknown view type");
			}
			if (viewHolder.messageBody != null) {
				listSelectionManager.onCreate(viewHolder.messageBody,
						new MessageBodyActionModeCallback(message, viewHolder.messageBody));
				viewHolder.messageBody.setCopyHandler(this);
			}
			view.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) view.getTag();
			if (viewHolder == null) {
				return view;
			}
		}

		boolean darkBackground = type == RECEIVED && (!isInValidSession || mUseGreenBackground) || activity.isDarkTheme();

		if (type == DATE_SEPARATOR) {
			if (UIHelper.today(message.getTimeSent())) {
				viewHolder.status_message.setText(R.string.today);
			} else if (UIHelper.yesterday(message.getTimeSent())) {
				viewHolder.status_message.setText(R.string.yesterday);
			} else {
				viewHolder.status_message.setText(DateUtils.formatDateTime(activity, message.getTimeSent(), DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR));
			}
			viewHolder.message_box.setBackgroundResource(activity.isDarkTheme() ? R.drawable.date_bubble_grey : R.drawable.date_bubble_white);
			return view;
		} else if (type == STATUS) {
			if ("LOAD_MORE".equals(message.getBody())) {
				viewHolder.status_message.setVisibility(View.GONE);
				viewHolder.contact_picture.setVisibility(View.GONE);
				viewHolder.load_more_messages.setVisibility(View.VISIBLE);
				viewHolder.load_more_messages.setOnClickListener(v -> loadMoreMessages((Conversation) message.getConversation()));
			} else {
				viewHolder.status_message.setVisibility(View.VISIBLE);
				viewHolder.load_more_messages.setVisibility(View.GONE);
				viewHolder.status_message.setText(message.getBody());
				boolean showAvatar;
				if (conversation.getMode() == Conversation.MODE_SINGLE) {
					showAvatar = true;
					AvatarWorkerTask.loadAvatar(message, viewHolder.contact_picture, R.dimen.avatar_on_status_message);
				} else if (message.getCounterpart() != null || message.getTrueCounterpart() != null || (message.getCounterparts() != null && message.getCounterparts().size() > 0)) {
					showAvatar = true;
					AvatarWorkerTask.loadAvatar(message, viewHolder.contact_picture, R.dimen.avatar_on_status_message);
				} else {
					showAvatar = false;
				}
				if (showAvatar) {
					viewHolder.contact_picture.setAlpha(0.5f);
					viewHolder.contact_picture.setVisibility(View.VISIBLE);
				} else {
					viewHolder.contact_picture.setVisibility(View.GONE);
				}
			}
			return view;
		} else {
			AvatarWorkerTask.loadAvatar(message, viewHolder.contact_picture, R.dimen.avatar);
		}

		resetClickListener(viewHolder.message_box, viewHolder.messageBody);

		viewHolder.contact_picture.setOnClickListener(v -> {
			if (MessageAdapter.this.mOnContactPictureClickedListener != null) {
				MessageAdapter.this.mOnContactPictureClickedListener
						.onContactPictureClicked(message);
			}

		});
		viewHolder.contact_picture.setOnLongClickListener(v -> {
			if (MessageAdapter.this.mOnContactPictureLongClickedListener != null) {
				MessageAdapter.this.mOnContactPictureLongClickedListener
						.onContactPictureLongClicked(v, message);
				return true;
			} else {
				return false;
			}
		});

		// Hide all referencing message views to make them individually visible later.
		MessageReferenceUtils.hideMessageReference(viewHolder.messageReferenceBinding);

		final Transferable transferable = message.getTransferable();
		if (message.isDeleted() || (transferable != null && transferable.getStatus() != Transferable.STATUS_UPLOADING)) {
			if (transferable != null && transferable.getStatus() == Transferable.STATUS_OFFER) {
				displayDownloadableMessage(viewHolder, message, activity.getString(R.string.download_x_file, UIHelper.getFileDescriptionString(activity, message)), darkBackground);
			} else if (transferable != null && transferable.getStatus() == Transferable.STATUS_OFFER_CHECK_FILESIZE) {
				displayDownloadableMessage(viewHolder, message, activity.getString(R.string.check_x_filesize, UIHelper.getFileDescriptionString(activity, message)), darkBackground);
			} else {
				displayInfoMessage(viewHolder, UIHelper.getMessagePreview(activity, message).first, darkBackground);
			}
			// Display a referenced message and a comment for it if the referencing message has a message reference that matches a locally available message-
			// Otherwise display the referencing message normally.
		} else if (message.hasMessageReference() && message.getEncryption() != Message.ENCRYPTION_AXOLOTL_NOT_FOR_THIS_DEVICE) {
			Message referencedMessage = ((Conversation) conversation).findMessageWithUuidOrRemoteMsgId(message.getMessageReference());

			// Try to load the referenced message from the DB if it is null and could not be found in the currently loaded conversation.
			// If it cannot be loaded from the DB it will remain null.
			if(referencedMessage == null){
				referencedMessage = activity.xmppConnectionService.databaseBackend.getMsgByUuidOrRemoteMsgId((Conversation) conversation, message.getMessageReference());
			}

			MessageReferenceUtils.deleteLegacyQuotation(activity, message, referencedMessage);

			displayReferencingMessage(viewHolder, message, referencedMessage, darkBackground, type);
		} else if (message.isFileOrImage()) {
			if (message.isImageOrVideo()) {
				displayImageMessage(viewHolder, message, darkBackground);
			} else if (message.isAudio()) {
				displayAudioMessage(viewHolder, message, darkBackground);
			} else {
				displayOpenableMessage(viewHolder, message, darkBackground);
			}
		} else if (message.getEncryption() == Message.ENCRYPTION_AXOLOTL_NOT_FOR_THIS_DEVICE) {
			displayInfoMessage(viewHolder, activity.getString(R.string.not_encrypted_for_this_device), darkBackground);
		} else if (message.getEncryption() == Message.ENCRYPTION_AXOLOTL_FAILED) {
			displayInfoMessage(viewHolder, activity.getString(R.string.omemo_decryption_failed), darkBackground);
		} else {
			if (message.isGeoUri()) {
				displayLocationMessage(viewHolder, message, darkBackground);
			} else if (message.bodyIsOnlyEmojis() && message.getType() != Message.TYPE_PRIVATE) {
				displayEmojiMessage(viewHolder, message.getBody().trim(), darkBackground);
			} else if (message.treatAsDownloadable()) {
				try {
					URL url = new URL(message.getBody());
					if (P1S3UrlStreamHandler.PROTOCOL_NAME.equalsIgnoreCase(url.getProtocol())) {
						displayDownloadableMessage(viewHolder,
								message,
								activity.getString(R.string.check_x_filesize,UIHelper.getFileDescriptionString(activity, message)),
								darkBackground);
					} else {
						displayDownloadableMessage(viewHolder,
								message,
								activity.getString(R.string.check_x_filesize_on_host,
										UIHelper.getFileDescriptionString(activity, message),url.getHost()),
								darkBackground);
					}
				} catch (Exception e) {
					displayDownloadableMessage(viewHolder,
							message,
							activity.getString(R.string.check_x_filesize,
									UIHelper.getFileDescriptionString(activity, message)),
									darkBackground);
				}
			} else {
				displayTextMessage(viewHolder, message, darkBackground, type);
			}
		}

		if (type == RECEIVED) {
			if (isInValidSession) {
				int bubble;
				if (!mUseGreenBackground) {
					bubble = activity.getThemeResource(R.attr.message_bubble_received_monochrome, R.drawable.message_bubble_received_white);
				} else {
					bubble = activity.getThemeResource(R.attr.message_bubble_received_green, R.drawable.message_bubble_received);
				}
				viewHolder.message_box.setBackgroundResource(bubble);
				viewHolder.encryption.setVisibility(View.GONE);
			} else {
				viewHolder.message_box.setBackgroundResource(R.drawable.message_bubble_received_warning);
				viewHolder.encryption.setVisibility(View.VISIBLE);
				if (omemoEncryption && !message.isTrusted()) {
					viewHolder.encryption.setText(R.string.not_trusted);
				} else {
					viewHolder.encryption.setText(CryptoHelper.encryptionTypeToText(message.getEncryption()));
				}
			}
		}

		displayStatus(viewHolder, message, type, darkBackground);

		return view;
	}

	@Override
	public void notifyDataSetChanged() {
		listSelectionManager.onBeforeNotifyDataSetChanged();
		super.notifyDataSetChanged();
		listSelectionManager.onAfterNotifyDataSetChanged();
	}

	private String transformText(CharSequence text, int start, int end, boolean forCopy) {
		SpannableStringBuilder builder = new SpannableStringBuilder(text);
		Object copySpan = new Object();
		builder.setSpan(copySpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		DividerSpan[] dividerSpans = builder.getSpans(0, builder.length(), DividerSpan.class);
		for (DividerSpan dividerSpan : dividerSpans) {
			builder.replace(builder.getSpanStart(dividerSpan), builder.getSpanEnd(dividerSpan),
					dividerSpan.isLarge() ? "\n\n" : "\n");
		}
		start = builder.getSpanStart(copySpan);
		end = builder.getSpanEnd(copySpan);
		if (start == -1 || end == -1) return "";
		builder = new SpannableStringBuilder(builder, start, end);
		if (forCopy) {
			QuoteSpan[] quoteSpans = builder.getSpans(0, builder.length(), QuoteSpan.class);
			for (QuoteSpan quoteSpan : quoteSpans) {
				builder.insert(builder.getSpanStart(quoteSpan), "> ");
			}
		}
		return builder.toString();
	}

	@Override
	public String transformTextForCopy(CharSequence text, int start, int end) {
		if (text instanceof Spanned) {
			return transformText(text, start, end, true);
		} else {
			return text.toString().substring(start, end);
		}
	}

	public FileBackend getFileBackend() {
		return activity.xmppConnectionService.getFileBackend();
	}

	public void stopAudioPlayer() {
		audioPlayer.stop();
	}

	public void unregisterListenerInAudioPlayer() {
		audioPlayer.unregisterListener();
	}

	public void startStopPending() {
		audioPlayer.startStopPending();
	}

	public void openDownloadable(Message message) {
		if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			ConversationFragment.registerPendingMessage(activity, message);
			ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, ConversationsActivity.REQUEST_OPEN_MESSAGE);
			return;
		}
		final DownloadableFile file = activity.xmppConnectionService.getFileBackend().getFile(message);
		ViewUtil.view(activity, file);
	}

	private void showImage(Message message) {
		final DownloadableFile file = activity.xmppConnectionService.getFileBackend().getFile(message);
		FullscreenImageActivity.start(getContext(), String.valueOf(file));
	}

	private void showLocation(Message message) {
		for (Intent intent : GeoHelper.createGeoIntentsFromMessage(activity, message)) {
			if (intent.resolveActivity(getContext().getPackageManager()) != null) {
				getContext().startActivity(intent);
				return;
			}
		}
		Toast.makeText(activity, R.string.no_application_found_to_display_location, Toast.LENGTH_SHORT).show();
	}

	public void updatePreferences() {
		SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(activity);
		this.mIndicateReceived = p.getBoolean("indicate_received", activity.getResources().getBoolean(R.bool.indicate_received));
		this.mUseGreenBackground = p.getBoolean("use_green_background", activity.getResources().getBoolean(R.bool.use_green_background));
	}


	public void setHighlightedTerm(List<String> terms) {
		this.highlightedTerm = terms == null ? null : StylingHelper.filterHighlightedWords(terms);
	}

	public interface OnCommentListener {
		void onComment(Message message, boolean quoteMessage);
	}

	public interface OnContactPictureClicked {
		void onContactPictureClicked(Message message);
	}

	public interface OnContactPictureLongClicked {
		void onContactPictureLongClicked(View v, Message message);
	}

	private static class ViewHolder {

		Button load_more_messages;
		ImageView edit_indicator;
		RelativeLayout audioPlayer;
		LinearLayout message_box;
		MessageReferenceBinding messageReferenceBinding;
		Button download_button;
		ImageView image;
		GifImageView gifImage;
		ImageView gif_btn;
		ImageView indicator;
		ImageView indicatorReceived;
		TextView time;
		CopyTextView messageBody;
		ImageView contact_picture;
		TextView status_message;
		TextView encryption;
	}


	private class MessageBodyActionModeCallback implements ActionMode.Callback {

		private final Message message;
		private final TextView textView;

		MessageBodyActionModeCallback(Message message, TextView textView) {
			this.message = message;
			this.textView = textView;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			if (onCommentListener  != null) {
				int quoteResId = activity.getThemeResource(R.attr.icon_quote, R.drawable.ic_action_reply);
				// 3rd item is placed after "copy" item
				menu.add(0, android.R.id.button1, 3, R.string.quote).setIcon(quoteResId)
						.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
			}
			return false;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			if (item.getItemId() == android.R.id.button1) {
				int start = textView.getSelectionStart();
				int end = textView.getSelectionEnd();
				if (end > start) {
					String text = transformText(textView.getText(), start, end, false);
					if (onCommentListener != null) {
						message.setBody(text);
						onCommentListener.onComment(message, true);
					}
					mode.finish();
				}
				return true;
			}
			return false;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
		}
	}
}
