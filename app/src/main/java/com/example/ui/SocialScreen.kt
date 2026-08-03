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
    val activeSubTab by viewModel.socialSubTab.collectAsState()
    val friends by viewModel.friendConnections.collectAsState()
    val authError by viewModel.authError.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    
    // Fetch stats for achievements progress
    val tasks by viewModel.allTasks.collectAsState()
    val habits by viewModel.allHabits.collectAsState()
    val badHabits by viewModel.badHabits.collectAsState()
    val waterLogs by viewModel.waterLogs.collectAsState()
    val exerciseLogs by viewModel.exerciseLogs.collectAsState()
    val alarms by viewModel.allAlarms.collectAsState()
    val allFinanceLogs by viewModel.allFinanceLogs.collectAsState()
    val allWishlist by viewModel.allWishlist.collectAsState()
    val allBirthdays by viewModel.allBirthdays.collectAsState()
    val streakHistory by viewModel.streakHistory.collectAsState()
    val todayScore by viewModel.todayScore.collectAsState()

    var friendEmailInput by remember { mutableStateOf("") }
    var showFriendErrors by remember { mutableStateOf(false) }
    val friendEmailError = if (friendEmailInput.isBlank()) {
        "Email is required"
    } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(friendEmailInput).matches()) {
        "Please enter a valid email address"
    } else null

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
                        .clickable { viewModel.setSocialSubTab(tabId) }
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
                                    onValueChange = { 
                                        friendEmailInput = it 
                                        showFriendErrors = false
                                    },
                                    label = { Text("Friend's Email *") },
                                    placeholder = { Text("friend@trackwise.com") },
                                    singleLine = true,
                                    isError = showFriendErrors && friendEmailError != null,
                                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = BrandViolet) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BrandViolet,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                        .testTag("friend_email_input")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (friendEmailError == null) {
                                            viewModel.addFriend(friendEmailInput.trim())
                                            friendEmailInput = ""
                                            showFriendErrors = false
                                            focusManager.clearFocus()
                                        } else {
                                            showFriendErrors = true
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

                            if (showFriendErrors && friendEmailError != null) {
                                Text(
                                    text = friendEmailError,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
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
                                                    text = "${item.score} XP",
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
                
                val totalXP = streakHistory.sumOf { it.score } + todayScore
                val netWorth = allFinanceLogs.filter { it.type == "income" || it.type == "savings" }.sumOf { it.amount } - allFinanceLogs.filter { it.type == "expense" }.sumOf { it.amount }
                val achievementsData = getAllSystemAchievements(
                    allTasks = tasks,
                    allHabits = habits,
                    allFinanceLogs = allFinanceLogs,
                    allWishlist = allWishlist,
                    allBirthdays = allBirthdays,
                    badHabits = badHabits,
                    socialCircleSize = friends.size,
                    hasActiveAlarm = alarms.any { it.isEnabled },
                    alarmsCount = alarms.size,
                    waterLogs = waterLogs,
                    streakHistory = streakHistory,
                    userLevel = (streakHistory.sumOf { it.score } + todayScore) / 1000 + 1,
                    totalXP = totalXP,
                    netWorth = netWorth
                ).map { spec ->
                    AchievementItem(
                        title = spec.title,
                        desc = spec.desc,
                        progress = spec.progress,
                        target = spec.target,
                        icon = spec.icon,
                        iconColor = spec.iconColor,
                        tier = spec.tier
                    )
                }
                val legacyAchievementsData = emptyList<AchievementItem>()
                if (false) {
                    listOf(
                    // --- 1. Habiteers & Streaks (PUBG Inspired) ---
                    AchievementItem(
                        title = "First Blood: Streak Champion",
                        desc = "Develop consistent routines by checking off habits today.",
                        progress = habitsCheckedToday,
                        target = 1,
                        icon = Icons.Default.LocalFireDepartment,
                        iconColor = BrandRose,
                        tier = "RARE"
                    ),
                    AchievementItem(
                        title = "Recruit: Habit Novice",
                        desc = "Create 3 habits to structure your life.",
                        progress = habits.size,
                        target = 3,
                        icon = Icons.Default.MilitaryTech,
                        iconColor = BrandViolet,
                        tier = "COMMON"
                    ),
                    AchievementItem(
                        title = "Commando: Habit Enthusiast",
                        desc = "Build consistency with 5 active habits.",
                        progress = habits.size,
                        target = 5,
                        icon = Icons.Default.Shield,
                        iconColor = BrandAmber,
                        tier = "EPIC"
                    ),
                    AchievementItem(
                        title = "Apex Predator: Habit Master",
                        desc = "Reach peak self-discipline with 10 active habits.",
                        progress = habits.size,
                        target = 10,
                        icon = Icons.Default.WorkspacePremium,
                        iconColor = BrandCyan,
                        tier = "LEGENDARY"
                    ),
                    AchievementItem(
                        title = "Overachiever: 5-Day Streak",
                        desc = "Reach a 5-day maximum streak on any habit.",
                        progress = habits.maxOfOrNull { it.maxStreak } ?: 0,
                        target = 5,
                        icon = Icons.Default.FlashOn,
                        iconColor = BrandViolet,
                        tier = "EPIC"
                    ),
                    AchievementItem(
                        title = "Berserker: 15-Day Unbroken",
                        desc = "Reach a 15-day streak to lock in your routine.",
                        progress = habits.maxOfOrNull { it.maxStreak } ?: 0,
                        target = 15,
                        icon = Icons.Default.Whatshot,
                        iconColor = BrandRose,
                        tier = "MYTHIC"
                    ),
                    AchievementItem(
                        title = "Winner Winner Chicken Dinner: 30-Day Streak",
                        desc = "Maintain a 30-day streak — legendary unbreakable performance!",
                        progress = habits.maxOfOrNull { it.maxStreak } ?: 0,
                        target = 30,
                        icon = Icons.Default.EmojiEvents,
                        iconColor = BrandAmber,
                        tier = "MYTHIC"
                    ),

                    // --- 2. Tasks & Execution ---
                    AchievementItem(
                        title = "Gunslinger: Task Specialist",
                        desc = "Mark at least 3 tasks as completed.",
                        progress = completedTasksCount,
                        target = 3,
                        icon = Icons.Default.GpsFixed,
                        iconColor = BrandGreen,
                        tier = "COMMON"
                    ),
                    AchievementItem(
                        title = "Deadeye: Task Journeyman",
                        desc = "Complete 10 total tasks successfully.",
                        progress = completedTasksCount,
                        target = 10,
                        icon = Icons.Default.GpsFixed,
                        iconColor = BrandPink,
                        tier = "RARE"
                    ),
                    AchievementItem(
                        title = "Terminator: Task Master",
                        desc = "Complete 25 distinct tasks.",
                        progress = completedTasksCount,
                        target = 25,
                        icon = Icons.Default.PrecisionManufacturing,
                        iconColor = BrandViolet,
                        tier = "EPIC"
                    ),
                    AchievementItem(
                        title = "Conqueror: Task Overlord",
                        desc = "Execute 50 tasks to dominate your workflow.",
                        progress = completedTasksCount,
                        target = 50,
                        icon = Icons.Default.MilitaryTech,
                        iconColor = BrandCyan,
                        tier = "LEGENDARY"
                    ),
                    AchievementItem(
                        title = "Tactical Master: Super Planner",
                        desc = "Draft 10 tasks to organize your future.",
                        progress = tasks.size,
                        target = 10,
                        icon = Icons.Default.AutoAwesome,
                        iconColor = BrandRose,
                        tier = "EPIC"
                    ),
                    AchievementItem(
                        title = "Warlord: Architect of Life",
                        desc = "Build a massive plan of 30 total recorded tasks.",
                        progress = tasks.size,
                        target = 30,
                        icon = Icons.Default.Shield,
                        iconColor = BrandAmber,
                        tier = "MYTHIC"
                    ),
                    AchievementItem(
                        title = "Sharpshooter: High Priority Hero",
                        desc = "Complete 1 critical high-priority task.",
                        progress = tasks.count { it.completed && it.priority.lowercase() == "high" },
                        target = 1,
                        icon = Icons.Default.ElectricBolt,
                        iconColor = BrandRose,
                        tier = "RARE"
                    ),
                    AchievementItem(
                        title = "Rampage: Elite Executor",
                        desc = "Complete 5 critical high-priority tasks.",
                        progress = tasks.count { it.completed && it.priority.lowercase() == "high" },
                        target = 5,
                        icon = Icons.Default.Whatshot,
                        iconColor = BrandViolet,
                        tier = "EPIC"
                    ),
                    AchievementItem(
                        title = "Dominator: Apex Achiever",
                        desc = "Conquer 15 high-priority tasks under pressure.",
                        progress = tasks.count { it.completed && it.priority.lowercase() == "high" },
                        target = 15,
                        icon = Icons.Default.Diamond,
                        iconColor = BrandAmber,
                        tier = "MYTHIC"
                    ),

                    // --- 3. Hydration & Health ---
                    AchievementItem(
                        title = "Hydration Initiate",
                        desc = "Drink your first glass of water of the journey.",
                        progress = waterLogs.sumOf { it.glasses },
                        target = 1,
                        icon = Icons.Default.Info,
                        iconColor = BrandCyan
                    ),
                    AchievementItem(
                        title = "Hydration Hero",
                        desc = "Drink 8 glasses of water in total.",
                        progress = waterLogs.sumOf { it.glasses },
                        target = 8,
                        icon = Icons.Default.Info,
                        iconColor = BrandCyan
                    ),
                    AchievementItem(
                        title = "Waterfall",
                        desc = "Log 40 total glasses of water.",
                        progress = waterLogs.sumOf { it.glasses },
                        target = 40,
                        icon = Icons.Default.Info,
                        iconColor = BrandViolet
                    ),
                    AchievementItem(
                        title = "Aquarius",
                        desc = "Achieve prime hydration with 100 total logged glasses.",
                        progress = waterLogs.sumOf { it.glasses },
                        target = 100,
                        icon = Icons.Default.Info,
                        iconColor = BrandRose
                    ),

                    // --- 4. Fitness & Exercise ---
                    AchievementItem(
                        title = "Fitness Rookie",
                        desc = "Record 1 completed exercise session.",
                        progress = exerciseLogs.size,
                        target = 1,
                        icon = Icons.Default.Favorite,
                        iconColor = BrandGreen
                    ),
                    AchievementItem(
                        title = "Fitness Buff",
                        desc = "Stay active by logging 5 workouts.",
                        progress = exerciseLogs.size,
                        target = 5,
                        icon = Icons.Default.Favorite,
                        iconColor = BrandViolet
                    ),
                    AchievementItem(
                        title = "Iron Athlete",
                        desc = "Complete 15 workout sessions.",
                        progress = exerciseLogs.size,
                        target = 15,
                        icon = Icons.Default.Favorite,
                        iconColor = BrandRose
                    ),
                    AchievementItem(
                        title = "Olympian Elite",
                        desc = "Log 30 distinct workout sessions.",
                        progress = exerciseLogs.size,
                        target = 30,
                        icon = Icons.Default.Favorite,
                        iconColor = BrandAmber
                    ),
                    AchievementItem(
                        title = "Active Mover",
                        desc = "Exercise for a total of 150 minutes.",
                        progress = exerciseLogs.sumOf { it.durationMinutes },
                        target = 150,
                        icon = Icons.Default.AccessTime,
                        iconColor = BrandCyan
                    ),
                    AchievementItem(
                        title = "Cardio Legend",
                        desc = "Burn sweat for 500 minutes in total.",
                        progress = exerciseLogs.sumOf { it.durationMinutes },
                        target = 500,
                        icon = Icons.Default.AccessTime,
                        iconColor = BrandRose
                    ),
                    AchievementItem(
                        title = "Endurance Master",
                        desc = "Exercise for a grand total of 1200 minutes.",
                        progress = exerciseLogs.sumOf { it.durationMinutes },
                        target = 1200,
                        icon = Icons.Default.AccessTime,
                        iconColor = BrandViolet
                    ),

                    // --- 5. Social & Community ---
                    AchievementItem(
                        title = "Social Networker",
                        desc = "Connect with 3 friends to share goals.",
                        progress = socialCircleSize,
                        target = 3,
                        icon = Icons.Default.Group,
                        iconColor = BrandCyan
                    ),
                    AchievementItem(
                        title = "Social Influencer",
                        desc = "Grow your community by adding 5 friends.",
                        progress = socialCircleSize,
                        target = 5,
                        icon = Icons.Default.Group,
                        iconColor = BrandViolet
                    ),
                    AchievementItem(
                        title = "Community Pillar",
                        desc = "Become a mentor with 10 connected friends.",
                        progress = socialCircleSize,
                        target = 10,
                        icon = Icons.Default.Group,
                        iconColor = BrandAmber
                    ),

                    // --- 6. Specialized Habits ---
                    AchievementItem(
                        title = "Healthy Spirit",
                        desc = "Build 3 distinct wellness habits.",
                        progress = habits.count { it.category.lowercase().contains("wellness") || it.category.lowercase().contains("health") },
                        target = 3,
                        icon = Icons.Default.FavoriteBorder,
                        iconColor = BrandRose
                    ),
                    AchievementItem(
                        title = "Mind Gym",
                        desc = "Cultivate intellect with 3 learning habits.",
                        progress = habits.count { it.category.lowercase().contains("learning") || it.category.lowercase().contains("mind") },
                        target = 3,
                        icon = Icons.Default.Info,
                        iconColor = BrandViolet
                    ),
                    AchievementItem(
                        title = "Productivity Ninja",
                        desc = "Track 3 professional work habits.",
                        progress = habits.count { it.category.lowercase().contains("work") || it.category.lowercase().contains("productivity") },
                        target = 3,
                        icon = Icons.Default.Build,
                        iconColor = BrandCyan
                    ),

                    // --- 7. Utility & Alarms ---
                    AchievementItem(
                        title = "Early Bird Alarm",
                        desc = "Enable at least one active alarm.",
                        progress = if (hasActiveAlarm) 1 else 0,
                        target = 1,
                        icon = Icons.Default.AccessTime,
                        iconColor = BrandAmber
                    ),
                    AchievementItem(
                        title = "Snooze Defier",
                        desc = "Set up 3 custom timers/alarms.",
                        progress = alarms.size,
                        target = 3,
                        icon = Icons.Default.AccessTime,
                        iconColor = BrandViolet
                    ),
                    AchievementItem(
                        title = "Stay Structured",
                        desc = "Keep an active structured habit schedule.",
                        progress = habits.size,
                        target = 2,
                        icon = Icons.Default.List,
                        iconColor = BrandViolet
                    ),

                    // --- 8. Break Bad Habits ---
                    AchievementItem(
                        title = "Honest Tracker",
                        desc = "Keep at least 2 custom bad habits under surveillance.",
                        progress = badHabits.size,
                        target = 2,
                        icon = Icons.Default.Verified,
                        iconColor = BrandGreen,
                        tier = "COMMON"
                    ),
                    AchievementItem(
                        title = "Self-Awareness Mirror",
                        desc = "Log your first slip-up honestly.",
                        progress = badHabits.sumOf { it.logs.size },
                        target = 1,
                        icon = Icons.Default.LockOpen,
                        iconColor = BrandCyan,
                        tier = "RARE"
                    ),
                    AchievementItem(
                        title = "Truth Seeker",
                        desc = "Log a slip-up for 'Lying' honestly to break the cycle.",
                        progress = badHabits.filter { it.name.lowercase().contains("lie") || it.name.lowercase().contains("lying") }.sumOf { it.logs.size },
                        target = 1,
                        icon = Icons.Default.History,
                        iconColor = BrandAmber,
                        tier = "EPIC"
                    ),
                    AchievementItem(
                        title = "Breaking Chains",
                        desc = "Build deep self-discipline by logging 5 honest checks.",
                        progress = badHabits.sumOf { it.logs.size },
                        target = 5,
                        icon = Icons.Default.LinkOff,
                        iconColor = BrandRose,
                        tier = "LEGENDARY"
                    )
                    )
                }

                items(achievementsData) { achievement ->
                    val isUnlocked = achievement.progress >= achievement.target
                    val percentage = (achievement.progress.toFloat() / achievement.target.toFloat()).coerceIn(0f, 1f)
                    
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isUnlocked) 2.dp else 1.dp,
                                color = if (isUnlocked) BrandViolet else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                                PubgAchievementEmblem(achievement = achievement, isUnlocked = isUnlocked)

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = achievement.title,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (isUnlocked) BrandViolet else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        
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
                                                text = if (isUnlocked) "UNLOCKED" else achievement.tier,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (isUnlocked) BrandViolet else achievement.iconColor
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

@Composable
private fun PubgAchievementEmblem(
    achievement: AchievementItem,
    isUnlocked: Boolean,
    modifier: Modifier = Modifier
) {
    val tierColor = when (achievement.tier) {
        "MYTHIC" -> Color(0xFFFF3B30) // Crimson Mythic
        "LEGENDARY" -> Color(0xFFFFD700) // Gold Legendary
        "EPIC" -> Color(0xFFAF52DE) // Purple Epic
        "RARE" -> Color(0xFF007AFF) // Blue Rare
        else -> Color(0xFF34C759) // Green Common
    }
    
    val starCount = when (achievement.tier) {
        "MYTHIC" -> 3
        "LEGENDARY" -> 2
        "EPIC" -> 1
        else -> 0
    }

    val gradientBrush = if (isUnlocked) {
        Brush.verticalGradient(
            colors = when (achievement.tier) {
                "MYTHIC" -> listOf(Color(0xFFFF3B30), Color(0xFF8B0000))
                "LEGENDARY" -> listOf(Color(0xFFFFD700), Color(0xFFFF8C00))
                "EPIC" -> listOf(Color(0xFFAF52DE), Color(0xFF4A0E4E))
                "RARE" -> listOf(Color(0xFF007AFF), Color(0xFF003366))
                else -> listOf(Color(0xFF34C759), Color(0xFF1E5631))
            }
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF3A3A3C), Color(0xFF1C1C1E))
        )
    }

    val glowBorderColor = if (isUnlocked) tierColor else Color.Gray.copy(alpha = 0.3f)

    Box(
        modifier = modifier
            .size(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(gradientBrush)
            .border(
                width = 2.dp,
                brush = Brush.radialGradient(
                    colors = listOf(glowBorderColor, glowBorderColor.copy(alpha = 0.6f))
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = achievement.icon,
                contentDescription = null,
                tint = if (isUnlocked) Color.White else Color.Gray,
                modifier = Modifier.size(26.dp)
            )
            
            Spacer(modifier = Modifier.height(2.dp))
            
            if (starCount > 0 && isUnlocked) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(starCount) {
                        Text(
                            text = "★",
                            fontSize = 8.sp,
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 0.5.dp)
                        )
                    }
                }
            } else {
                Text(
                    text = achievement.tier,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isUnlocked) Color.White.copy(alpha = 0.8f) else Color.Gray,
                    letterSpacing = 0.5.sp
                )
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
    val iconColor: androidx.compose.ui.graphics.Color,
    val tier: String = "EPIC"
)

private data class FriendLeaderboardItem(
    val connection: FriendConnectionEntity,
    val earnedBadges: List<BadgeSpec>,
    val streak: Int,
    val score: Int
)
