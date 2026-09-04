package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.database.AppDatabase
import com.example.data.repository.ManufacturingRepository
import com.example.ui.dialogs.QuickActionsModalBottomSheet
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.MoreHubScreen
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentBlueGlow
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkBorderSubtle
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardElevated
import com.example.ui.theme.DarkSecondaryBg
import com.example.ui.theme.LocalCustomColors
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MainTab
import com.example.viewmodel.ManufacturingViewModel
import com.example.viewmodel.QuickActionType
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val database = AppDatabase.getDatabase(applicationContext)
    val repository = ManufacturingRepository(database)

    setContent {
      val viewModel = remember { ManufacturingViewModel(repository) }
      val isDarkTheme by viewModel.isDarkTheme.collectAsState()
      MyApplicationTheme(darkTheme = isDarkTheme) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
          MainAppScreen(viewModel = viewModel)
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: ManufacturingViewModel) {
  val customColors = LocalCustomColors.current
  val selectedTab by viewModel.selectedTab.collectAsState()
  val activeQuickAction by viewModel.activeQuickAction.collectAsState()
  val notification by viewModel.notification.collectAsState()

  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  var showQuickActionsSheet by remember { mutableStateOf(false) }

  // Sync sheet visibility with active quick action
  LaunchedEffect(activeQuickAction) {
    showQuickActionsSheet = (activeQuickAction != QuickActionType.NONE)
  }

  // Display notification message
  LaunchedEffect(notification) {
    notification?.let {
      snackbarHostState.showSnackbar(it.message)
      viewModel.dismissNotification()
    }
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = customColors.bg,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    snackbarHost = { SnackbarHost(snackbarHostState) },
    bottomBar = {
      ExecutiveBottomNavigation(
        selectedTab = selectedTab,
        onTabSelected = { viewModel.setTab(it) },
        onOpenOperations = {
          viewModel.openQuickAction(QuickActionType.WAREHOUSE_HUB)
        }
      )
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(
          top = 40.dp,
          bottom = innerPadding.calculateBottomPadding()
        )
    ) {
      AnimatedContent(
        targetState = selectedTab,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "tab_transition"
      ) { targetTab ->
        when (targetTab) {
          MainTab.DASHBOARD -> DashboardScreen(viewModel = viewModel)
          MainTab.ANALYTICS -> AnalyticsScreen(viewModel = viewModel)
          MainTab.INVENTORY -> InventoryScreen(viewModel = viewModel)
          MainTab.MORE -> MoreHubScreen(viewModel = viewModel)
        }
      }
    }

    if (showQuickActionsSheet) {
      QuickActionsModalBottomSheet(
        viewModel = viewModel,
        activeAction = activeQuickAction,
        sheetState = sheetState,
        onDismiss = {
          scope.launch {
            sheetState.hide()
            viewModel.closeQuickAction()
            showQuickActionsSheet = false
          }
        }
      )
    }
  }
}

/**
 * Executive Bottom Navigation Bar with Editorial Aesthetic Floating Action Button
 */
@Composable
fun ExecutiveBottomNavigation(
  selectedTab: MainTab,
  onTabSelected: (MainTab) -> Unit,
  onOpenOperations: () -> Unit
) {
  val customColors = LocalCustomColors.current

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .background(customColors.card)
      .border(
        width = 1.dp,
        color = customColors.border,
        shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp)
      )
      .padding(horizontal = 16.dp, vertical = 8.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // 1. Home / Dashboard
      BottomNavItem(
        title = "داشبورد",
        icon = Icons.Default.Home,
        isSelected = selectedTab == MainTab.DASHBOARD,
        onClick = { onTabSelected(MainTab.DASHBOARD) },
        tag = "nav_dashboard"
      )

      // 2. Reports / Analytics
      BottomNavItem(
        title = "گزارشات",
        icon = Icons.Default.Analytics,
        isSelected = selectedTab == MainTab.ANALYTICS,
        onClick = { onTabSelected(MainTab.ANALYTICS) },
        tag = "nav_analytics"
      )

      // 3. Center Prominent Operation Action: Dedicated Warehouse & Operations Hub
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .padding(bottom = 2.dp)
          .clickable(onClick = onOpenOperations)
      ) {
        Box(
          modifier = Modifier
            .size(50.dp)
            .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = AccentBlueGlow)
            .clip(RoundedCornerShape(16.dp))
            .background(AccentIndigo)
            .testTag("nav_operations_center"),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "مرکز انبارداری و عملیات",
            tint = Color.White,
            modifier = Modifier.size(28.dp)
          )
        }
        Text(
          text = "انبارداری +",
          style = MaterialTheme.typography.labelSmall,
          color = AccentIndigo,
          fontWeight = FontWeight.Bold,
          fontSize = 10.sp,
          modifier = Modifier.padding(top = 2.dp)
        )
      }

      // 4. Inventory
      BottomNavItem(
        title = "انبار کالا",
        icon = Icons.Default.Inventory2,
        isSelected = selectedTab == MainTab.INVENTORY,
        onClick = { onTabSelected(MainTab.INVENTORY) },
        tag = "nav_inventory"
      )

      // 5. More Hub
      BottomNavItem(
        title = "تنظیمات و بیش",
        icon = Icons.Default.Apps,
        isSelected = selectedTab == MainTab.MORE,
        onClick = { onTabSelected(MainTab.MORE) },
        tag = "nav_more"
      )
    }
  }
}

@Composable
fun BottomNavItem(
  title: String,
  icon: ImageVector,
  isSelected: Boolean,
  onClick: () -> Unit,
  tag: String
) {
  val customColors = LocalCustomColors.current

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp),
    modifier = Modifier
      .clickable(onClick = onClick)
      .padding(horizontal = 8.dp, vertical = 4.dp)
      .testTag(tag)
  ) {
    Icon(
      imageVector = icon,
      contentDescription = title,
      tint = if (isSelected) AccentIndigo else customColors.textMuted.copy(alpha = 0.6f),
      modifier = Modifier.size(22.dp)
    )
    Text(
      text = title,
      style = MaterialTheme.typography.labelSmall,
      color = if (isSelected) AccentIndigo else customColors.textMuted.copy(alpha = 0.6f),
      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
      fontSize = 10.sp
    )
  }
}

