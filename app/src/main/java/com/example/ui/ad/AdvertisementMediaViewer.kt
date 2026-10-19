package com.example.ui.ad

import android.media.MediaPlayer
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.Advertisement

@Composable
fun AdvertisementMediaViewer(
    ad: Advertisement,
    modifier: Modifier = Modifier,
    height: Int = 180,
    allowVideoPlayback: Boolean = true
) {
    val context = LocalContext.current
    val isVideo = ad.mediaType.equals("VIDEO", ignoreCase = true)

    var isPlaying by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center
    ) {
        if (!isVideo || !allowVideoPlayback || hasError) {
            // Image or Video Poster Fallback
            val displayUrl = if (isVideo && ad.posterUrl.isNotBlank()) ad.posterUrl else ad.mediaUrl
            val placeholderRes = R.drawable.hero_delivery

            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(displayUrl.ifBlank { placeholderRes })
                    .crossfade(true)
                    .error(placeholderRes)
                    .placeholder(placeholderRes)
                    .build(),
                contentDescription = ad.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Video overlay badge if this is a video in poster mode
            if (isVideo) {
                if (hasError) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.75f),
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.VideocamOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Video preview unavailable", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                } else {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.65f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Video Ad",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Video", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        } else {
            // Interactive Video Playback using Android VideoView
            var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setVideoURI(Uri.parse(ad.mediaUrl))
                        setOnPreparedListener { mp ->
                            mediaPlayerRef = mp
                            mp.isLooping = true
                            // Sensible playback: muted by default so audio never blasts unexpectedly
                            if (isMuted) {
                                mp.setVolume(0f, 0f)
                            } else {
                                mp.setVolume(1f, 1f)
                            }
                            start()
                            isPlaying = true
                        }
                        setOnErrorListener { _, _, _ ->
                            hasError = true
                            false
                        }
                    }.also { videoViewRef = it }
                },
                update = { view ->
                    if (isPlaying && !view.isPlaying) {
                        view.start()
                    } else if (!isPlaying && view.isPlaying) {
                        view.pause()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Playback controls overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        isPlaying = !isPlaying
                        if (isPlaying) videoViewRef?.start() else videoViewRef?.pause()
                    }
            ) {
                // Top controls: Obvious Mute / Unmute Sound Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clickable {
                            isMuted = !isMuted
                            mediaPlayerRef?.let { mp ->
                                if (isMuted) mp.setVolume(0f, 0f) else mp.setVolume(1f, 1f)
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = if (isMuted) "Unmute Audio" else "Mute Audio",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isMuted) "Tap for Sound" else "Sound On",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                // Center Play/Pause indicator when paused
                AnimatedVisibility(
                    visible = !isPlaying,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.65f),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                // Video tag bottom corner
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Promo Video", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
