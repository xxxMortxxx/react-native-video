package com.remobile.video;

import android.os.Handler;
import android.util.Log;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.yqritc.scalablevideoview.ScalableType;
import com.yqritc.scalablevideoview.ScalableVideoView;

import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.MediaPlayer.*;
import io.vov.vitamio.Vitamio;


public class RCTVideoView extends ScalableVideoView implements OnPreparedListener, OnCompletionListener, OnErrorListener, OnBufferingUpdateListener, OnSeekCompleteListener {
    public enum Events {
        EVENT_LOAD_START("onVideoLoadStart"),
        EVENT_LOAD("onVideoLoad"),
        EVENT_ERROR("onVideoError"),
        EVENT_PROGRESS("onVideoProgress"),
        EVENT_SEEK("onVideoSeek"),
        EVENT_END("onVideoEnd");

        private final String mName;

        Events(final String name) {
            mName = name;
        }

        @Override
        public String toString() {
            return mName;
        }
    }

    public static final String EVENT_PROP_FAST_FORWARD = "canPlayFastForward";
    public static final String EVENT_PROP_SLOW_FORWARD = "canPlaySlowForward";
    public static final String EVENT_PROP_SLOW_REVERSE = "canPlaySlowReverse";
    public static final String EVENT_PROP_REVERSE = "canPlayReverse";
    public static final String EVENT_PROP_STEP_FORWARD = "canStepForward";
    public static final String EVENT_PROP_STEP_BACKWARD = "canStepBackward";

    public static final String EVENT_PROP_DURATION = "duration";
    public static final String EVENT_PROP_PLAYABLE_DURATION = "playableDuration";
    public static final String EVENT_PROP_CURRENT_TIME = "currentTime";
    public static final String EVENT_PROP_SEEK_TIME = "seekTime";

    public static final String EVENT_PROP_ERROR = "error";
    public static final String EVENT_PROP_WHAT = "what";
    public static final String EVENT_PROP_EXTRA = "extra";

    private RCTEventEmitter mEventEmitter;

    private Handler mProgressUpdateHandler = new Handler();
    private Runnable mProgressUpdateRunnable = null;

    private String mSrcUriString = null;
    private String mSrcType = "mp4";
    private boolean mSrcIsNetwork = false;
    private boolean mSrcIsAsset = false;
    private ScalableType mResizeMode = ScalableType.LEFT_TOP;
    private boolean mRepeat = false;
    private boolean mMuted = false;
    private float mVolume = 1.0f;
    private float mRate = 1.0f;
    private long m_msec = 0;

    private boolean mMediaPlayerValid = false; // True if mMediaPlayer is in prepared, started, or paused state.

    private int mVideoDuration = 0;
    private int mVideoBufferedDuration = 0;

    public RCTVideoView(ThemedReactContext themedReactContext) {
        super(themedReactContext);
        mEventEmitter = themedReactContext.getJSModule(RCTEventEmitter.class);
        Vitamio.isInitialized(themedReactContext);

        mProgressUpdateRunnable = new Runnable() {
            @Override
            public void run() {

                if (mMediaPlayerValid) {
                    WritableMap event = Arguments.createMap();
                    event.putDouble(EVENT_PROP_CURRENT_TIME, getCurrentPosition() / 1000.0);
                    event.putDouble(EVENT_PROP_PLAYABLE_DURATION, mVideoBufferedDuration / 1000.0);
                    mEventEmitter.receiveEvent(getId(), Events.EVENT_PROGRESS.toString(), event);
                }
                mProgressUpdateHandler.postDelayed(mProgressUpdateRunnable, 250);
            }
        };
        mProgressUpdateHandler.post(mProgressUpdateRunnable);
    }


    public void setSrc(final String uriString, final String type, final boolean isNetwork, final boolean isAsset) {
        mSrcUriString = uriString;
        mSrcType = type;
        mSrcIsNetwork = isNetwork;
        mSrcIsAsset = isAsset;
        mVideoDuration = 0;
        mVideoBufferedDuration = 0;
    }

    public void setResizeModeModifier(final ScalableType resizeMode) {
        mResizeMode = resizeMode;

        if (mMediaPlayerValid) {
            setScalableType(resizeMode);
            invalidate();
        }
    }

    public void setRepeatModifier(final boolean repeat) {
        mRepeat = repeat;

        if (mMediaPlayerValid) {
            setLooping(repeat);
        }
    }

    public void setPausedModifier(final boolean paused) {
        mPaused = paused;

        if (!mMediaPlayerValid) {
            return;
        }

        if (mPaused) {
            if (isPlaying()) {
                pause();
            }
        } else {
            if (!isPlaying()) {
                start();
            }
        }
    }

    public void setMutedModifier(final boolean muted) {
        mMuted = muted;

        if (!mMediaPlayerValid) {
            return;
        }

        if (mMuted) {
            setVolume(0, 0);
        } else {
            setVolume(mVolume, mVolume);
        }
    }

    public void setVolumeModifier(final float volume) {
        mVolume = volume;
        setMutedModifier(mMuted);
    }

    public void setRateModifier(final float rate) {
        mRate = rate;

        if (mMediaPlayerValid) {
            // TODO: Implement this.
            Log.d(RCTVideoViewManager.REACT_CLASS, "Setting playback rate is not yet supported on Android");
        }
    }

    public void applyModifiers() {
        setResizeModeModifier(mResizeMode);
        setRepeatModifier(mRepeat);
        setPausedModifier(mPaused);
        setMutedModifier(mMuted);
        setRateModifier(mRate);
    }


    private void setListeners() {
        setOnPreparedListener(this);
        setOnCompletionListener(this);
        setOnErrorListener(this);
        setOnBufferingUpdateListener(this);
        setOnSeekCompleteListener(this);
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mVideoDuration = (int)mp.getDuration();
        mMediaPlayerValid = true;
        mp.setPlaybackSpeed(1.0f);

        WritableMap event = Arguments.createMap();
        event.putDouble(EVENT_PROP_DURATION, mVideoDuration / 1000.0);
        event.putDouble(EVENT_PROP_CURRENT_TIME, mp.getCurrentPosition() / 1000.0);
        // TODO: Actually check if you can.
        event.putBoolean(EVENT_PROP_FAST_FORWARD, true);
        event.putBoolean(EVENT_PROP_SLOW_FORWARD, true);
        event.putBoolean(EVENT_PROP_SLOW_REVERSE, true);
        event.putBoolean(EVENT_PROP_REVERSE, true);
        event.putBoolean(EVENT_PROP_FAST_FORWARD, true);
        event.putBoolean(EVENT_PROP_STEP_BACKWARD, true);
        event.putBoolean(EVENT_PROP_STEP_FORWARD, true);
        mEventEmitter.receiveEvent(getId(), Events.EVENT_LOAD.toString(), event);

        applyModifiers();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        WritableMap error = Arguments.createMap();
        error.putInt(EVENT_PROP_WHAT, what);
        error.putInt(EVENT_PROP_EXTRA, extra);
        WritableMap event = Arguments.createMap();
        event.putMap(EVENT_PROP_ERROR, error);
        mEventEmitter.receiveEvent(getId(), Events.EVENT_ERROR.toString(), event);
        return true;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        mVideoBufferedDuration = (int) Math.round((double) (mVideoDuration * percent) / 100.0);
    }

    @Override
    public void seekTo(long msec) {
        if (mMediaPlayerValid) {
            m_msec = msec;
            super.seekTo(msec);
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mEventEmitter.receiveEvent(getId(), Events.EVENT_END.toString(), null);
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        WritableMap event = Arguments.createMap();
        event.putDouble(EVENT_PROP_CURRENT_TIME, getCurrentPosition() / 1000.0);
        event.putDouble(EVENT_PROP_SEEK_TIME, m_msec / 1000.0);
        mEventEmitter.receiveEvent(getId(), Events.EVENT_SEEK.toString(), event);
    }

    @Override
    protected void onDetachedFromWindow() {
        mMediaPlayerValid = false;
        super.onDetachedFromWindow();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        setListeners();
        setVideoPath(mSrcUriString);
        requestFocus();

        WritableMap src = Arguments.createMap();
        src.putString(RCTVideoViewManager.PROP_SRC_URI, mSrcUriString);
        src.putString(RCTVideoViewManager.PROP_SRC_TYPE, mSrcType);
        src.putBoolean(RCTVideoViewManager.PROP_SRC_IS_NETWORK, mSrcIsNetwork);
        WritableMap event = Arguments.createMap();
        event.putMap(RCTVideoViewManager.PROP_SRC, src);
        mEventEmitter.receiveEvent(getId(), Events.EVENT_LOAD_START.toString(), event);
    }
}
