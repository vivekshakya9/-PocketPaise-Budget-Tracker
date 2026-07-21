@file:Suppress("DEPRECATION")
package com.vsdigitallabs.pocketpaise.stdbt

import androidx.fragment.app.FragmentActivity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.vsdigitallabs.pocketpaise.stdbt.ui.navigation.Screen
import com.vsdigitallabs.pocketpaise.stdbt.ui.screens.*
import com.vsdigitallabs.pocketpaise.stdbt.ui.theme.PocketPaiseTheme
import com.vsdigitallabs.pocketpaise.stdbt.viewmodel.PocketPaiseViewModel
import com.vsdigitallabs.pocketpaise.stdbt.viewmodel.PocketPaiseViewModelFactory
import android.Manifest
import android.os.Build
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.ads.MobileAds

@OptIn(ExperimentalPermissionsApi::class)
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        FirebaseMessaging.getInstance().subscribeToTopic("global")
        MobileAds.initialize(this) {}
        com.vsdigitallabs.pocketpaise.stdbt.utils.AppOpenAdManager(application)
        com.vsdigitallabs.pocketpaise.stdbt.utils.InterstitialAdManager.loadAd(this)
        enableEdgeToEdge()

        // Enqueue Daily Budget Alert Worker
        val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.vsdigitallabs.pocketpaise.stdbt.worker.BudgetAlertWorker>(24, java.util.concurrent.TimeUnit.HOURS).build()
        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork("BudgetAlert", androidx.work.ExistingPeriodicWorkPolicy.KEEP, workRequest)

        // Schedule Daily Expense Reminder Notification (Time controlled by Admin)
        val prefs = getSharedPreferences("PocketPaisePrefs", android.content.Context.MODE_PRIVATE)
        val reminderTimeStr = prefs.getString("daily_reminder_time", "21:00") ?: "21:00"
        com.vsdigitallabs.pocketpaise.stdbt.worker.DailyReminderManager.scheduleReminder(this, reminderTimeStr)

        setContent {
            val viewModel: PocketPaiseViewModel = viewModel(
                factory = PocketPaiseViewModelFactory(application)
            )
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            
            val prefs = getSharedPreferences("PocketPaisePrefs", android.content.Context.MODE_PRIVATE)
            val isMaintenanceMode = prefs.getBoolean("maintenance_mode", false)
            val maintenanceMessage = prefs.getString("maintenance_message", "") ?: ""

            PocketPaiseTheme(darkTheme = isDarkMode) {
                if (isMaintenanceMode) {
                    MaintenanceScreen(message = maintenanceMessage)
                } else {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val notificationPermissionState = rememberPermissionState(
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                    LaunchedEffect(Unit) {
                        if (!notificationPermissionState.status.isGranted) {
                            notificationPermissionState.launchPermissionRequest()
                        }
                    }
                }

                // Bottom bar visibility control
                val showBottomBar = currentRoute in listOf(
                    Screen.Dashboard.route,
                    Screen.Transactions.route,
                    Screen.Budget.route,
                    Screen.Reports.route,
                    Screen.Profile.route
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 0.dp,
                                modifier = Modifier
                                    .testTag("bottom_nav")
                                    .windowInsetsPadding(WindowInsets.navigationBars)
                            ) {
                                NavigationBarItem(
                                    selected = currentRoute == Screen.Dashboard.route,
                                    onClick = {
                                        navController.navigate(Screen.Dashboard.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                    label = { Text("Home", maxLines = 1, softWrap = false, fontSize = 9.sp) },
                                    alwaysShowLabel = true,
                                    modifier = Modifier.testTag("nav_home")
                                )

                                NavigationBarItem(
                                    selected = currentRoute == Screen.Transactions.route,
                                    onClick = {
                                        navController.navigate(Screen.Transactions.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Transactions") },
                                    label = { Text("Transactions", maxLines = 1, softWrap = false, fontSize = 9.sp) },
                                    alwaysShowLabel = true,
                                    modifier = Modifier.testTag("nav_transactions")
                                )

                                NavigationBarItem(
                                    selected = currentRoute == Screen.Budget.route,
                                    onClick = {
                                        navController.navigate(Screen.Budget.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(Icons.Default.AddModerator, contentDescription = "Budget") },
                                    label = { Text("Budget", maxLines = 1, softWrap = false, fontSize = 9.sp) },
                                    alwaysShowLabel = true,
                                    modifier = Modifier.testTag("nav_budget")
                                )

                                NavigationBarItem(
                                    selected = currentRoute == Screen.Reports.route,
                                    onClick = {
                                        navController.navigate(Screen.Reports.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(Icons.Default.Analytics, contentDescription = "Reports") },
                                    label = { Text("Reports", maxLines = 1, softWrap = false, fontSize = 9.sp) },
                                    alwaysShowLabel = true,
                                    modifier = Modifier.testTag("nav_reports")
                                )

                                NavigationBarItem(
                                    selected = currentRoute == Screen.Profile.route,
                                    onClick = {
                                        navController.navigate(Screen.Profile.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                                    label = { Text("Profile", maxLines = 1, softWrap = false, fontSize = 9.sp) },
                                    alwaysShowLabel = true,
                                    modifier = Modifier.testTag("nav_profile")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Splash.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Splash.route) {
                            SplashScreen(navController, viewModel)
                        }
                        composable(Screen.Login.route) {
                            LoginScreen(navController, viewModel)
                        }
                        composable(Screen.Register.route) {
                            RegisterScreen(navController, viewModel)
                        }
                        composable(Screen.ForgotPassword.route) {
                            ForgotPasswordScreen(navController, viewModel)
                        }
                        composable(Screen.Dashboard.route) {
                            DashboardScreen(navController, viewModel)
                        }
                        composable(Screen.Transactions.route) {
                            TransactionsScreen(navController, viewModel)
                        }
                        composable(Screen.Budget.route) {
                            BudgetScreen(navController, viewModel)
                        }
                        composable(Screen.Savings.route) {
                            SavingsScreen(navController, viewModel)
                        }
                        composable(Screen.Reports.route) {
                            ReportsScreen(navController, viewModel)
                        }
                        composable(Screen.Calendar.route) {
                            CalendarScreen(navController, viewModel)
                        }
                        composable(Screen.Notification.route) {
                            NotificationScreen(navController, viewModel)
                        }
                        composable(Screen.Profile.route) {
                            ProfileScreen(navController, viewModel)
                        }
                        composable(Screen.Settings.route) {
                            SettingsScreen(navController, viewModel)
                        }
                        composable(Screen.PrivacyPolicy.route) {
                            PrivacyPolicyScreen(navController)
                        }
                        composable(Screen.Terms.route) {
                            TermsAndConditionsScreen(navController)
                        }
                        composable(Screen.About.route) {
                            AboutAppScreen(navController)
                        }
                        composable(Screen.Contact.route) {
                            ContactSupportScreen(navController)
                        }
                    }
                }
                } // End of else block for maintenance mode
            }
        }
    }
}
