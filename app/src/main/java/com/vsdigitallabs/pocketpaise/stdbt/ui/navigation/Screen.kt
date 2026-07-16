package com.vsdigitallabs.pocketpaise.stdbt.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")
    object Dashboard : Screen("dashboard")
    object Transactions : Screen("transactions")
    object Budget : Screen("budget")
    object Savings : Screen("savings")
    object Reports : Screen("reports")
    object Notification : Screen("notification")
    object Calendar : Screen("calendar")
    object Profile : Screen("profile")
    object Settings : Screen("settings")
    object PrivacyPolicy : Screen("privacy_policy")
    object Terms : Screen("terms")
    object About : Screen("about")
    object Contact : Screen("contact")
}
