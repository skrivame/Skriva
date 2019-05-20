package me.skriva.ceph.ui.util;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.widget.ImageView;

import androidx.annotation.DimenRes;

import java.lang.ref.WeakReference;
import java.util.concurrent.RejectedExecutionException;

import me.skriva.ceph.services.AvatarService;
import me.skriva.ceph.ui.XmppActivity;

public class AvatarWorkerTask extends AsyncTask<AvatarService.Avatarable, Void, Bitmap> {
    private final WeakReference<ImageView> imageViewReference;
    private AvatarService.Avatarable avatarable = null;
    private @DimenRes
    final int size;

    private AvatarWorkerTask(ImageView imageView, @DimenRes int size) {
        imageViewReference = new WeakReference<>(imageView);
        this.size = size;
    }

    @Override
    protected Bitmap doInBackground(AvatarService.Avatarable... params) {
        this.avatarable = params[0];
        final XmppActivity activity = XmppActivity.find(imageViewReference);
        if (activity == null) {
            return null;
        }
        return activity.avatarService().get(avatarable, (int) activity.getResources().getDimension(size), isCancelled());
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (bitmap != null && !isCancelled()) {
            final ImageView imageView = imageViewReference.get();
            if (imageView != null) {
                imageView.setImageBitmap(bitmap);
                imageView.setBackgroundColor(0x00000000);
            }
        }
    }

    private static boolean cancelPotentialWork(AvatarService.Avatarable avatarable, ImageView imageView) {
        final AvatarWorkerTask workerTask = getBitmapWorkerTask(imageView);

        if (workerTask != null) {
            final AvatarService.Avatarable old= workerTask.avatarable;
            if (old == null || avatarable != old) {
                workerTask.cancel(true);
            } else {
                return false;
            }
        }
        return true;
    }

    private static AvatarWorkerTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getAvatarWorkerTask();
            }
        }
        return null;
    }

    public static void loadAvatar(final AvatarService.Avatarable avatarable, final ImageView imageView, final @DimenRes int size) {
        if (cancelPotentialWork(avatarable, imageView)) {
            final XmppActivity activity = XmppActivity.find(imageView);
            if (activity == null) {
                return;
            }
            final Bitmap bm = activity.avatarService().get(avatarable, (int) activity.getResources().getDimension(size), true);
            if (bm != null) {
                cancelPotentialWork(avatarable, imageView);
                imageView.setImageBitmap(bm);
                imageView.setBackgroundColor(0x00000000);
            } else {
                imageView.setBackgroundColor(avatarable.getAvatarBackgroundColor());
                imageView.setImageDrawable(null);
                final AvatarWorkerTask task = new AvatarWorkerTask(imageView, size);
                final AsyncDrawable asyncDrawable = new AsyncDrawable(activity.getResources(), task);
                imageView.setImageDrawable(asyncDrawable);
                try {
                    task.execute(avatarable);
                } catch (final RejectedExecutionException ignored) {
                }
            }
        }
    }

    static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<AvatarWorkerTask> avatarWorkerTaskReference;

        AsyncDrawable(Resources res, AvatarWorkerTask workerTask) {
            super(res, (Bitmap) null);
            avatarWorkerTaskReference = new WeakReference<>(workerTask);
        }

        AvatarWorkerTask getAvatarWorkerTask() {
            return avatarWorkerTaskReference.get();
        }
    }
}
