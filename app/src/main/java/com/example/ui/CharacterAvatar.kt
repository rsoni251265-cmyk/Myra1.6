package com.example.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.CharacterOutfit
import com.example.model.CharacterState
import com.example.ui.theme.AnimeGlowCyan
import com.example.ui.theme.AnimeGlowPink
import com.example.ui.theme.AnimeGlowPurple
import com.example.ui.theme.AnimeListeningPulse
import com.example.ui.theme.AnimeSpeakingWave
import com.example.ui.theme.AnimeThinkingGold

@Composable
fun CharacterAvatar(
    characterState: CharacterState,
    audioLevel: Float,
    isSpeaking: Boolean,
    outfit: CharacterOutfit,
    assistantName: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "avatar_anim")

    // Gentle breathing / floating vertical offset
    val floatingOffset by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floating"
    )

    // Pulsing aura ring for active states
    val auraScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (characterState == CharacterState.LISTENING || isSpeaking) 1.14f else 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_pulse"
    )

    // Dynamic glow color depending on state
    val glowColor = when (characterState) {
        CharacterState.LISTENING -> AnimeListeningPulse
        CharacterState.SPEAKING -> AnimeSpeakingWave
        CharacterState.THINKING -> AnimeThinkingGold
        CharacterState.HAPPY -> AnimeGlowPink
        CharacterState.ERROR -> Color(0xFFEF4444)
        else -> AnimeGlowPurple
    }

    // Dynamic portrait resource based on state
    val imageDrawableRes = when (characterState) {
        CharacterState.LISTENING -> R.drawable.img_heena_listening
        CharacterState.SPEAKING, CharacterState.HAPPY -> R.drawable.img_heena_happy
        else -> R.drawable.ic_launcher_heena
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(190.dp)
                .offset(y = floatingOffset.dp)
        ) {
            // Outer dynamic glowing halo
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .scale(auraScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                glowColor.copy(alpha = if (characterState == CharacterState.LISTENING || isSpeaking) 0.55f else 0.25f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Inner border ring with cyber aesthetic
            Box(
                modifier = Modifier
                    .size(158.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = 3.dp,
                        brush = Brush.sweepGradient(
                            listOf(
                                AnimeGlowCyan,
                                glowColor,
                                AnimeGlowPink,
                                AnimeGlowCyan
                            )
                        ),
                        shape = CircleShape
                    )
                    .shadow(elevation = 12.dp, shape = CircleShape)
            ) {
                Image(
                    painter = painterResource(id = imageDrawableRes),
                    contentDescription = "$assistantName Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .testTag("heena_avatar_image")
                )
            }

            // Audio level pulse indicator ring when microphone is active
            if (characterState == CharacterState.LISTENING && audioLevel > 0.05f) {
                Box(
                    modifier = Modifier
                        .size((160 + (audioLevel * 30)).dp)
                        .border(
                            width = 2.dp,
                            color = AnimeListeningPulse.copy(alpha = 0.8f),
                            shape = CircleShape
                        )
                )
            }

            // Active state icon badge (bottom right)
            Surface(
                shape = CircleShape,
                color = glowColor,
                contentColor = Color.White,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-16).dp, y = (-6).dp)
                    .size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    when {
                        isSpeaking -> Icon(Icons.Default.VolumeUp, contentDescription = "Speaking", modifier = Modifier.size(20.dp))
                        characterState == CharacterState.LISTENING -> Icon(Icons.Default.Mic, contentDescription = "Listening", modifier = Modifier.size(20.dp))
                        else -> Text(text = characterState.moodEmoji, fontSize = 18.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // State & Assistant Status Tag
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(glowColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$assistantName • ${characterState.title}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}
