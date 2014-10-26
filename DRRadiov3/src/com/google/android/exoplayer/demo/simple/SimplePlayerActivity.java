/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.exoplayer.demo.simple;

import android.app.Activity;
import android.media.MediaCodec.CryptoException;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.MediaController;
import android.widget.Toast;

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecTrackRenderer.DecoderInitializationException;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.VideoSurfaceView;
import com.google.android.exoplayer.demo.DemoUtil;

import dk.dr.radio.v3.R;

/**
 * An activity that plays media using {@link com.google.android.exoplayer.ExoPlayer}.
 */
public class SimplePlayerActivity extends Activity implements SurfaceHolder.Callback,
    ExoPlayer.Listener, MediaCodecVideoTrackRenderer.EventListener {

  /**
   * Builds renderers for the player.
   */
  public interface RendererBuilder {

    void buildRenderers(RendererBuilderCallback callback);

  }

  private static final String TAG = "PlayerActivity";

  public static final int TYPE_DASH_VOD = 0;
  public static final int TYPE_SS_VOD = 1;
  public static final int TYPE_OTHER = 2;

  private MediaController mediaController;
  private Handler mainHandler;
  private View shutterView;
  private VideoSurfaceView surfaceView;

  private ExoPlayer player;
  private RendererBuilder builder;
  private RendererBuilderCallback callback;
  private MediaCodecVideoTrackRenderer videoRenderer;

  private boolean autoPlay = true;
  private long playerPosition;

  private Uri contentUri;
  private int contentType;
  private String contentId;

  // Activity lifecycle

  // Public methods

  public Handler getMainHandler() {
    return mainHandler;
  }

  // Internal methods

  private void toggleControlsVisibility() {
    if (mediaController.isShowing()) {
      mediaController.hide();
    } else {
      mediaController.show(0);
    }
  }

  private RendererBuilder getRendererBuilder() {
    String userAgent = DemoUtil.getUserAgent(this);
    switch (contentType) {
      case TYPE_SS_VOD:
        return new SmoothStreamingRendererBuilder(this, userAgent, contentUri.toString(),
            contentId);
      case TYPE_DASH_VOD:
        return new DashVodRendererBuilder(this, userAgent, contentUri.toString(), contentId);
      default:
        return new DefaultRendererBuilder(this, contentUri);
    }
  }

  private void onRenderers(RendererBuilderCallback callback,
                           MediaCodecVideoTrackRenderer videoRenderer, MediaCodecAudioTrackRenderer audioRenderer) {
    if (this.callback != callback) {
      return;
    }
    this.callback = null;
    this.videoRenderer = videoRenderer;
    player.prepare(videoRenderer, audioRenderer);
    maybeStartPlayback();
  }

  private void maybeStartPlayback() {
    Surface surface = surfaceView.getHolder().getSurface();
    if (videoRenderer == null || surface == null || !surface.isValid()) {
      // We're not ready yet.
      return;
    }
    player.sendMessage(videoRenderer, MediaCodecVideoTrackRenderer.MSG_SET_SURFACE, surface);
    if (autoPlay) {
      player.setPlayWhenReady(true);
      autoPlay = false;
    }
  }

  private void onRenderersError(RendererBuilderCallback callback, Exception e) {
    if (this.callback != callback) {
      return;
    }
    this.callback = null;
    onError(e);
  }

  private void onError(Exception e) {
    Log.e(TAG, "Playback failed", e);
    Toast.makeText(this, R.string.failed, Toast.LENGTH_SHORT).show();
    finish();
  }

  // ExoPlayer.Listener implementation

  @Override
  public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
    // Do nothing.
  }

  @Override
  public void onPlayWhenReadyCommitted() {
    // Do nothing.
  }

  @Override
  public void onPlayerError(ExoPlaybackException e) {
    onError(e);
  }

  // MediaCodecVideoTrackRenderer.Listener

  @Override
  public void onVideoSizeChanged(int width, int height, float pixelWidthHeightRatio) {
    surfaceView.setVideoWidthHeightRatio(
        height == 0 ? 1 : (pixelWidthHeightRatio * width) / height);
  }

  @Override
  public void onDrawnToSurface(Surface surface) {
    shutterView.setVisibility(View.GONE);
  }

  @Override
  public void onDroppedFrames(int count, long elapsed) {
    Log.d(TAG, "Dropped frames: " + count);
  }

  @Override
  public void onDecoderInitializationError(DecoderInitializationException e) {
    // This is for informational purposes only. Do nothing.
  }

  @Override
  public void onCryptoError(CryptoException e) {
    // This is for informational purposes only. Do nothing.
  }

  // SurfaceHolder.Callback implementation

  @Override
  public void surfaceCreated(SurfaceHolder holder) {
    maybeStartPlayback();
  }

  @Override
  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    // Do nothing.
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {
    if (videoRenderer != null) {
      player.blockingSendMessage(videoRenderer, MediaCodecVideoTrackRenderer.MSG_SET_SURFACE, null);
    }
  }

  /* package */ final class RendererBuilderCallback {

    public void onRenderers(MediaCodecVideoTrackRenderer videoRenderer,
                            MediaCodecAudioTrackRenderer audioRenderer) {
      SimplePlayerActivity.this.onRenderers(this, videoRenderer, audioRenderer);
    }

    public void onRenderersError(Exception e) {
      SimplePlayerActivity.this.onRenderersError(this, e);
    }

  }

}
