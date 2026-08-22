package com.example.utils

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun FloatingXpEffect(
    trigger: Int, 
    xpAmount: Int,
    modifier: Modifier = Modifier
) {
    if (trigger == 0 || xpAmount == 0) return

    var activeTrigger by remember { mutableStateOf(0) }
    var showEffect by remember { mutableStateOf(false) }

    LaunchedEffect(trigger) {
        if (trigger != activeTrigger) {
            activeTrigger = trigger
            showEffect = true
            delay(1500)
            showEffect = false
        }
    }

    if (showEffect) {
        val animProgress = remember { Animatable(0f) }
        
        LaunchedEffect(activeTrigger) {
            animProgress.snapTo(0f)
            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1200, easing = LinearOutSlowInEasing)
            )
        }

        val yOffset = (-50.dp) * animProgress.value
        val alpha = 1f - animProgress.value
        val scale = 0.9f + 0.2f * animProgress.value

        Box(
            modifier = modifier.size(0.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.wrapContentSize(align = Alignment.Center, unbounded = true),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (xpAmount > 0) "+$xpAmount XP" else "$xpAmount XP",
                    color = if (xpAmount > 0) Color(0xFF4CAF50) else Color(0xFFE53935),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .offset(y = yOffset)
                        .graphicsLayer {
                            this.alpha = alpha
                            this.scaleX = scale
                            this.scaleY = scale
                        }
                )
            }
        }
    }
}
