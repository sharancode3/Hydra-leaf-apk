package com.example.hydraleaf

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

enum class HydraLeafDestination { HOME, GAME, SHOP, CHALLENGES }

@Composable
fun HydraLeafApp(viewModel: GameViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var destination by rememberSaveable { mutableStateOf(HydraLeafDestination.HOME) }
    var launchSettings by rememberSaveable { mutableStateOf(false) }

    Surface(color = MaterialTheme.colorScheme.background) {
        when (destination) {
            HydraLeafDestination.HOME -> HomeScreen(
                uiState = uiState,
                onStartGame = { launchSettings = false; viewModel.continueRun(); destination = HydraLeafDestination.GAME },
                onNewRun = { viewModel.startNewRun(); launchSettings = false; destination = HydraLeafDestination.GAME },
                onOpenSettings = { launchSettings = true; destination = HydraLeafDestination.GAME },
                onOpenShop = { destination = HydraLeafDestination.SHOP },
                onOpenChallenges = { destination = HydraLeafDestination.CHALLENGES },
                onResetProgress = { viewModel.startNewRun() }
            )
            HydraLeafDestination.GAME -> LeafGameScreen(
                viewModel = viewModel,
                onRequestCalibrate = { viewModel.calibrate() },
                showSettingsOnLaunch = launchSettings,
                onSettingsPanelConsumed = { launchSettings = false },
                onBackToMenu = { launchSettings = false; destination = HydraLeafDestination.HOME }
            )
            HydraLeafDestination.SHOP -> ShopScreen(viewModel, { destination = HydraLeafDestination.HOME })
            HydraLeafDestination.CHALLENGES -> ChallengesScreen(uiState, { destination = HydraLeafDestination.HOME })
        }
    }
}

// ── Home Screen ──────────────────────────────────────────────────────────────

@Composable
private fun HomeScreen(
    uiState: GameUiState,
    onStartGame: () -> Unit, onNewRun: () -> Unit, onOpenSettings: () -> Unit,
    onOpenShop: () -> Unit, onOpenChallenges: () -> Unit, onResetProgress: () -> Unit
) {
    val safePadding = WindowInsets.safeDrawing.asPaddingValues()
    val showContinue = uiState.phase == GamePhase.PLAYING || uiState.phase == GamePhase.IDLE
    val primaryLabel = if (uiState.score > 0 && showContinue) "Continue" else "Play"

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF02141F), Color(0xFF0C4F4C), Color(0xFF0DA68C)))).padding(safePadding)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 32.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Hydra Leaf", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = Color.White)
                Text("Glide the leaf, dodge the river hurdles.", style = MaterialTheme.typography.bodyMedium, color = Color(0xCCFFFFFF))
            }
            Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("High score ${uiState.highScore}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        StatPill("Level", uiState.level.toString(), Modifier.weight(1f))
                        StatPill("Hurdles", uiState.obstaclesCleared.toString(), Modifier.weight(1f))
                        StatPill("Mode", uiState.controlSettings.controlMode.name.lowercase().replaceFirstChar { it.uppercase() }, Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        StatPill("\uD83D\uDCA7 Drops", uiState.totalRiverDrops.toString(), Modifier.weight(1f))
                        StatPill("Skin", uiState.leafSkin.displayName, Modifier.weight(1f))
                        StatPill("Theme", uiState.riverTheme.displayName, Modifier.weight(1f))
                    }
                    Text("Last score ${uiState.score}", style = MaterialTheme.typography.bodyMedium)
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Credits", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text("Created by Sharan S \u2022 Hyper Mad Gamerz", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onStartGame, Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text(primaryLabel) }
                Button(onNewRun, Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) { Text("New Run") }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onOpenShop, modifier = Modifier.weight(1f), shape = RoundedCornerShape(18.dp)) { Text("\uD83D\uDECD Shop") }
                    OutlinedButton(onClick = onOpenChallenges, modifier = Modifier.weight(1f), shape = RoundedCornerShape(18.dp)) { Text("\uD83C\uDFC6 Challenges") }
                }
                OutlinedButton(onOpenSettings, Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) { Text("Settings") }
                TextButton(onResetProgress, Modifier.align(Alignment.CenterHorizontally)) { Text("Reset current run") }
            }
        }
    }
}

// ── Shop Screen ──────────────────────────────────────────────────────────────

@Composable
private fun ShopScreen(viewModel: GameViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val ownedSkins by viewModel.playerSettingsStore.ownedSkinsFlow.collectAsState(initial = setOf(LeafSkin.CLASSIC.name))
    val ownedThemes by viewModel.playerSettingsStore.ownedThemesFlow.collectAsState(initial = setOf(RiverTheme.FOREST.name))
    val drops by viewModel.playerSettingsStore.riverDropsFlow.collectAsState(initial = 0)

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onBack) { Icon(Icons.Filled.ArrowBack, "Back") }
                Text("Cosmetic Shop", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("\uD83D\uDCA7 $drops", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(20.dp))
            Text("Leaf Skins", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            LeafSkin.entries.forEach { skin ->
                val owned = ownedSkins.contains(skin.name)
                val active = uiState.leafSkin == skin
                ShopItemCard(skin.displayName, skin.cost, owned, active, drops >= skin.cost,
                    onPurchase = { viewModel.purchaseSkin(skin) }, onSelect = { viewModel.selectSkin(skin) })
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text("River Themes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            RiverTheme.entries.forEach { theme ->
                val owned = ownedThemes.contains(theme.name)
                val active = uiState.riverTheme == theme
                ShopItemCard(theme.displayName, theme.cost, owned, active, drops >= theme.cost,
                    onPurchase = { viewModel.purchaseTheme(theme) }, onSelect = { viewModel.selectTheme(theme) })
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ShopItemCard(name: String, cost: Int, owned: Boolean, active: Boolean, canAfford: Boolean, onPurchase: () -> Unit, onSelect: () -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(
        containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    )) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (!owned && cost > 0) Text("\uD83D\uDCA7 $cost", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            when {
                active -> Icon(Icons.Filled.CheckCircle, "Equipped", tint = MaterialTheme.colorScheme.primary)
                owned -> OutlinedButton(onSelect, shape = RoundedCornerShape(12.dp)) { Text("Equip") }
                cost == 0 -> OutlinedButton(onSelect, shape = RoundedCornerShape(12.dp)) { Text("Free") }
                canAfford -> Button(onPurchase, shape = RoundedCornerShape(12.dp)) { Text("Buy") }
                else -> { Icon(Icons.Filled.Lock, "Locked", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

// ── Challenges Screen ────────────────────────────────────────────────────────

@Composable
private fun ChallengesScreen(uiState: GameUiState, onBack: () -> Unit) {
    val daily = uiState.dailyChallenge
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onBack) { Icon(Icons.Filled.ArrowBack, "Back") }
                Text("Daily Challenges", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(20.dp))
            if (daily != null) {
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Today's Challenge", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(daily.type.description, style = MaterialTheme.typography.bodyLarge)
                        Text("Reward: \uD83D\uDCA7 ${daily.type.rewardDrops} River Drops", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        if (daily.completed) Text("\u2705 Completed!", style = MaterialTheme.typography.titleMedium, color = Color(0xFF26C596))
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("All Challenges", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            ChallengeType.entries.forEach { ch ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(ch.description, style = MaterialTheme.typography.bodyMedium)
                            Text("\uD83D\uDCA7 ${ch.rewardDrops}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}
