package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FriendConnectionEntity
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SocialScreen(
    viewModel: TrackWiseViewModel,
    modifier: Modifier = Modifier
) {
    var activeSubTab by remember { mutableStateOf("friends") } // "friends", "achievements"
    val friends by viewModel.friendConnections.collectAsState()
    val authError by viewModel.authError.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    
    // Fetch stats for achievements progress
    val tasks by viewModel.allTasks.collectAsState()
    val habits by viewModel.allHabits.collectAsState()
    val waterLogs by viewModel.waterLogs.collectAsState()
    val exerciseLogs by viewModel.exerciseLogs.collectAsState()
    val alarms by viewModel.allAlarms.collectAsState()

    var friendEmailInput by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
            }
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Header ---
        Column(modifier = Modifier.padding(top = 8.dp)) {
            Text(
                text = "Social & Progress",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Challenge friends, share habits, and track milestones.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        // --- Custom Sub-Tab Toggle Row ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("friends" to "My Friends", "achievements" to "Achievements").forEach { (tabId, tabName) ->
                val isSelected = activeSubTab == tabId
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) BrandViolet else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable { activeSubTab = tabId }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (tabId == "friends") Icons.Default.Group else Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tabName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // --- Active Section Content ---
        if (activeSubTab == "friends") {
            // Retrieve all habits to map real friend badges, falling back to rich simulated badges
            val allHabitsInSystem by viewModel.allHabitsInSystem.collectAsState()

            val standardBadges = listOf(
                BadgeSpec(1, "The Spark", "🥉", "The Launchpad", "Ignited the habit"),
                BadgeSpec(3, "Three's Company", "🥉", "The Launchpad", "Overcame day-two slump"),
                BadgeSpec(5, "Workweek Warrior", "🥉", "The Launchpad", "Five consecutive days"),
                BadgeSpec(7, "Weekly Wonder", "🥉", "The Launchpad", "Completed full week"),
                BadgeSpec(14, "Fortnight Force", "🥈", "The Builder", "Two weeks dedication"),
                BadgeSpec(21, "Habit Former", "🥈", "The Builder", "Avg days to lock routine"),
                BadgeSpec(30, "Calendar Crusher", "🥈", "The Builder", "One full month"),
                BadgeSpec(45, "Halfway Hero", "🥈", "The Builder", "Momentum past 1 month"),
                BadgeSpec(60, "Iron Will", "🥇", "The Master", "Two months unbroken"),
                BadgeSpec(90, "Seasoned Pro", "🥇", "The Master", "Seasonal commitment"),
                BadgeSpec(100, "Centurion", "🥇", "The Master", "Triple-digit milestone"),
                BadgeSpec(365, "Immortal", "🥇", "The Master", "One full year")
            )

            // Dynamic Leaderboard mapping & Sorting descending based on badge count!
            val friendsWithBadges = remember(friends, allHabitsInSystem) {
                friends.map { friend ->
                    val friendRealHabits = allHabitsInSystem.filter { it.userId == friend.friendUserId }
                    
                    val earnedBadges = if (friendRealHabits.isNotEmpty()) {
                        val milestoneDays = friendRealHabits.flatMap {
                            com.example.utils.TrackWiseUtils.deserializeIntList(it.badgesEarnedJson)
                        }.distinct()
                        standardBadges.filter { milestoneDays.contains(it.days) }
                    } else {
                        val hash = friend.friendUserId.hashCode().let { if (it < 0) -it else it }
                        val badgeCount = (hash % 6) + 2 // Deterministically assign 2 to 7 badges
                        standardBadges.take(badgeCount)
                    }

                    val friendStreak = if (friendRealHabits.isNotEmpty()) {
                        friendRealHabits.maxOfOrNull { it.streak } ?: 0
                    } else {
                        (friend.friendUserId.hashCode().let { if (it < 0) -it else it } % 14) + 1
                    }

                    val friendScore = if (friendRealHabits.isNotEmpty()) {
                        val completedDaysCount = friendRealHabits.sumOf {
                            com.example.utils.TrackWiseUtils.deserializeStringList(it.daysCompletedJson).size
                        }
                        completedDaysCount * 15 + 10
                    } else {
                        (friend.friendUserId.hashCode().let { if (it < 0) -it else it } % 95) + 25
                    }

                    FriendLeaderboardItem(
                        connection = friend,
                        earnedBadges = earnedBadges,
                        streak = friendStreak,
                        score = friendScore
                    )
                }.sortedByDescending { it.earnedBadges.size } // ORDERED DESCENDING!
            }

            var inspectedBadge by remember { mutableStateOf<BadgeSpec?>(null) }
            var inspectorFriendName by remember { mutableStateOf("") }

            if (inspectedBadge != null) {
                AlertDialog(
                    onDismissRequest = { inspectedBadge = null },
                    confirmButton = {
                        Button(
                            onClick = { inspectedBadge = null },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandViolet),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Inspiring!", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(text = inspectedBadge!!.medal, fontSize = 40.sp)
                            Column {
                                Text(text = inspectedBadge!!.name, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                                Text(text = inspectedBadge!!.tier, fontSize = 11.sp, color = BrandViolet, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "This is one of $inspectorFriendName's badges!",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(BrandViolet.copy(alpha = 0.08f))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = inspectedBadge!!.description,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "Requires staying consistent on a habit for ${inspectedBadge!!.days} days.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    },
                    shape = RoundedCornerShape(20.dp)
                )
            }

            // Friends section
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Add Friend input card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "ADD NEW FRIEND",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandCyan
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = friendEmailInput,
                                    onValueChange = { friendEmailInput = it },
                                    label = { Text("Friend's Email Address") },
                                    placeholder = { Text("friend@trackwise.com") },
                                    singleLine = true,
                                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = BrandViolet) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BrandViolet,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("friend_email_input")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (friendEmailInput.isNotBlank()) {
                                            viewModel.addFriend(friendEmailInput.trim())
                                            friendEmailInput = ""
                                            focusManager.clearFocus()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandViolet),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .height(56.dp)
                                        .testTag("add_friend_btn"),
                                    contentPadding = PaddingValues(horizontal = 16.dp)
                                ) {
                                    Text("Add", fontWeight = FontWeight.Bold)
                                }
                            }

                            if (authError != null) {
                                Text(
                                    text = authError!!,
                                    color = BrandRose,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Header for friend list
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Leaderboard & Badges (${friends.size})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Ordered by Badge Count 🏆",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandPink
                        )
                    }
                }

                // Friends list
                if (friendsWithBadges.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GroupAdd,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "Your Circle is Empty",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "Add friends to see their achievements and stay motivated!",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(friendsWithBadges) { index, item ->
                        val friend = item.connection
                        val rank = index + 1

                        // Rank designators (Glow & Crown for first place!)
                        val isFirstPlace = rank == 1
                        val cardGlowBorderColor = when (rank) {
                            1 -> Color(0xFFFED700) // Golden
                            2 -> Color(0xFFC0C0C0) // Silver
                            3 -> Color(0xFFCD7F32) // Bronze
                            else -> Color.Transparent
                        }

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isFirstPlace) Color(0xFFFED700).copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (rank <= 3) 2.dp else 1.dp,
                                    color = if (rank <= 3) cardGlowBorderColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // Rank visual
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (rank <= 3) cardGlowBorderColor.copy(alpha = 0.2f)
                                                    else MaterialTheme.colorScheme.surfaceVariant
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = when (rank) {
                                                    1 -> "👑"
                                                    2 -> "🥈"
                                                    3 -> "🥉"
                                                    else -> "#$rank"
                                                },
                                                fontSize = if (rank <= 3) 14.sp else 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        // Avatar placeholder
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.linearGradient(
                                                        colors = if (isFirstPlace) {
                                                            listOf(Color(0xFFFED700), BrandPink)
                                                        } else {
                                                            listOf(BrandViolet, BrandPink)
                                                        }
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = friend.displayName.take(1).uppercase(),
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White,
                                                fontSize = 16.sp
                                            )
                                        }

                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = friend.displayName,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                if (isFirstPlace) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "LEADER",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = Color(0xFFD4AF37),
                                                        modifier = Modifier
                                                            .background(Color(0xFFFED700).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.LocalFireDepartment,
                                                        contentDescription = "Streak",
                                                        tint = BrandRose,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text(
                                                        text = "${item.streak}d",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = BrandRose
                                                    )
                                                }
                                                Text(
                                                    text = "·",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                                )
                                                Text(
                                                    text = "${item.score} pts",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = BrandCyan
                                                )
                                                Text(
                                                    text = "·",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                                )
                                                Text(
                                                    text = "🏆 ${item.earnedBadges.size} Badges",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = BrandPink
                                                )
                                            }
                                        }
                                    }

                                    IconButton(
                                        onClick = { viewModel.removeFriend(friend.friendUserId) },
                                        modifier = Modifier.testTag("remove_friend_${friend.friendUserId}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete Friend",
                                            tint = BrandRose.copy(alpha = 0.7f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                // Friend's Badge Showcase Row (Dynamic & Clickable details dialog!)
                                if (item.earnedBadges.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Column {
                                        Text(
                                            text = "COLLECTED BADGES (Tap to inspect)",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                            letterSpacing = 0.5.sp,
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            item.earnedBadges.forEach { badge ->
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                        .clickable {
                                                            inspectedBadge = badge
                                                            inspectorFriendName = friend.displayName
                                                        }
                                                        .border(
                                                            width = 1.dp,
                                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Text(text = badge.medal, fontSize = 14.sp)
                                                        Text(
                                                            text = badge.name,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Achievements section
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Calculate real metrics
                val habitsCheckedToday = habits.count {
                    val completedDays = com.example.utils.TrackWiseUtils.deserializeStringList(it.daysCompletedJson)
                    completedDays.contains(com.example.utils.TrackWiseUtils.getTodayString())
                }
                val completedTasksCount = tasks.count { it.completed }
                val hasActiveAlarm = alarms.any { it.isEnabled }
                val socialCircleSize = friends.size
                
                val achievementsData = listOf(
                    AchievementItem(
                        title = "Streak Champion",
                        desc = "Develop consistent routines by checking off habits.",
                        progress = habitsCheckedToday,
                        target = 1,
                        icon = Icons.Default.LocalFireDepartment,
                        iconColor = BrandRose
                    ),
                    AchievementItem(
                        title = "Getting Things Done",
                        desc = "Mark at least 3 tasks as completed.",
                        progress = completedTasksCount,
                        target = 3,
                        icon = Icons.Default.TaskAlt,
                        iconColor = BrandGreen
                    ),
                    AchievementItem(
                        title = "Stay Structured",
                        desc = "Keep an active structured habit schedule.",
                        progress = habits.size,
                        target = 2,
                        icon = Icons.Default.Star,
                        iconColor = BrandViolet
                    ),
                    AchievementItem(
                        title = "Social Networker",
                        desc = "Connect with 3 friends to share goals.",
                        progress = socialCircleSize,
                        target = 3,
                        icon = Icons.Default.Group,
                        iconColor = BrandCyan
                    ),
                    AchievementItem(
                        title = "Early Bird Alarm",
                        desc = "Enable at least one active alarm.",
                        progress = if (hasActiveAlarm) 1 else 0,
                        target = 1,
                        icon = Icons.Default.AccessTime,
                        iconColor = BrandAmber
                    ),
                    AchievementItem(
                        title = "Task Master",
                        desc = "Complete 5 distinct tasks.",
                        progress = completedTasksCount,
                        target = 5,
                        icon = Icons.Default.CheckCircle,
                        iconColor = BrandPink
                    )
                )

                items(achievementsData) { achievement ->
                    val isUnlocked = achievement.progress >= achievement.target
                    val percentage = (achievement.progress.toFloat() / achievement.target.toFloat()).coerceIn(0f, 1f)
                    
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUnlocked) BrandViolet.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = if (isUnlocked) BrandViolet.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isUnlocked) achievement.iconColor.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = achievement.icon,
                                    contentDescription = null,
                                    tint = if (isUnlocked) achievement.iconColor else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = achievement.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isUnlocked) BrandViolet else MaterialTheme.colorScheme.onSurface
                                    )
                                    
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isUnlocked) BrandViolet.copy(alpha = 0.15f)
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isUnlocked) "UNLOCKED" else "LOCKED",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isUnlocked) BrandViolet else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }

                                Text(
                                    text = achievement.desc,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LinearProgressIndicator(
                                        progress = { percentage },
                                        color = if (isUnlocked) BrandViolet else achievement.iconColor,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                    )
                                    Text(
                                        text = "${achievement.progress}/${achievement.target}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class AchievementItem(
    val title: String,
    val desc: String,
    val progress: Int,
    val target: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val iconColor: androidx.compose.ui.graphics.Color
)

private data class FriendLeaderboardItem(
    val connection: FriendConnectionEntity,
    val earnedBadges: List<BadgeSpec>,
    val streak: Int,
    val score: Int
)
