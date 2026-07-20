package com.vsdigitallabs.pocketpaise.stdbt.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vsdigitallabs.pocketpaise.stdbt.data.local.AppDatabase
import com.vsdigitallabs.pocketpaise.stdbt.data.model.*
import com.vsdigitallabs.pocketpaise.stdbt.data.repository.PocketPaiseRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.NetworkType
import androidx.work.Constraints


@OptIn(ExperimentalCoroutinesApi::class)
class PocketPaiseViewModel(
    application: Application,
    private val repository: PocketPaiseRepository
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("PocketPaisePrefs", android.content.Context.MODE_PRIVATE)

    // Authentication States
    private val _currentUserId = MutableStateFlow<Long?>(null)
    val currentUserId: StateFlow<Long?> = _currentUserId.asStateFlow()

    init {
        val savedToken = prefs.getString("authToken", null)
        if (savedToken != null) {
            com.vsdigitallabs.pocketpaise.stdbt.data.network.RetrofitClient.authToken = savedToken
        }
        
        fetchRemoteConfig()

        val savedUserId = prefs.getLong("currentUserId", -1L)
        if (savedUserId != -1L) {
            _currentUserId.value = savedUserId
            fetchAndSyncData(savedUserId, {}, {})
        }
    }

    val currentUser: StateFlow<User?> = _currentUserId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else repository.getUserById(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Transactions list
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    private val _selectedTypeFilter = MutableStateFlow<String?>(null) // "INCOME", "EXPENSE", or null
    val selectedTypeFilter: StateFlow<String?> = _selectedTypeFilter.asStateFlow()

    val allTransactions: StateFlow<List<Transaction>> = _currentUserId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getAllTransactions(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        allTransactions,
        _searchQuery,
        _selectedCategoryFilter,
        _selectedTypeFilter
    ) { txs, query, cat, type ->
        txs.filter { tx ->
            val matchesQuery = query.isEmpty() || 
                    tx.title.contains(query, ignoreCase = true) || 
                    tx.category.contains(query, ignoreCase = true) || 
                    tx.notes.contains(query, ignoreCase = true)
            val matchesCat = cat == null || tx.category == cat
            val matchesType = type == null || tx.type == type
            matchesQuery && matchesCat && matchesType
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Budgets
    private val _currentMonthYear = MutableStateFlow("")
    val currentMonthYear: StateFlow<String> = _currentMonthYear.asStateFlow()

    val currentBudgets: StateFlow<List<Budget>> = combine(_currentUserId, _currentMonthYear) { userId, monthYear ->
        userId to monthYear
    }.flatMapLatest { (userId, monthYear) ->
        if (userId == null || monthYear.isEmpty()) flowOf(emptyList())
        else repository.getBudgetsForMonth(userId, monthYear)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Savings Goals
    val savingsGoals: StateFlow<List<SavingsGoal>> = _currentUserId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getSavingsGoals(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Notifications
    val notifications: StateFlow<List<Notification>> = _currentUserId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getAllNotifications(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationsCount: StateFlow<Int> = _currentUserId
        .flatMapLatest { id ->
            if (id == null) flowOf(0)
            else repository.getUnreadCount(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // UI Configuration States
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _selectedCurrency = MutableStateFlow("₹")
    val selectedCurrency: StateFlow<String> = _selectedCurrency.asStateFlow()

    private val _selectedLanguage = MutableStateFlow("English")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    private val _reminderTime = MutableStateFlow(prefs.getString("daily_reminder_time", "21:00") ?: "21:00")
    val reminderTime: StateFlow<String> = _reminderTime.asStateFlow()

    private val _dateFormat = MutableStateFlow("dd MMM yyyy")
    val dateFormat: StateFlow<String> = _dateFormat.asStateFlow()

    init {
        // Initialize month/year
        val cal = Calendar.getInstance()
        val month = String.format("%02d", cal.get(Calendar.MONTH) + 1)
        val year = cal.get(Calendar.YEAR)
        _currentMonthYear.value = "$month-$year"

        // Auto-login a default user for local demo experience if database is empty
        viewModelScope.launch {
            val existing = repository.getUserByEmail("student@pocketpaise.in")
            if (existing == null) {
                val demoUser = User(
                    name = "Rohan Sharma",
                    email = "student@pocketpaise.in",
                    mobileNumber = "9876543210",
                    collegeName = "IIT Bombay",
                    city = "Mumbai",
                    passwordHash = "password123", // Real simple login
                    pocketMoneyLimit = 12000.0, // Starting balance
                    currentSavings = 2500.0
                )
                val userId = repository.insertUser(demoUser)
                
                // Add initial transactions
                val now = System.currentTimeMillis()
                repository.insertTransaction(
                    Transaction(
                        userId = userId,
                        title = "Pocket Money",
                        amount = 15000.0,
                        type = "INCOME",
                        category = "Parents",
                        timestamp = now - (86400000L * 2)
                    )
                )
                repository.insertTransaction(
                    Transaction(
                        userId = userId,
                        title = "Hostel Rent",
                        amount = 4500.0,
                        type = "EXPENSE",
                        category = "Rent",
                        timestamp = now - (86400000L * 2)
                    )
                )
                repository.insertTransaction(
                    Transaction(
                        userId = userId,
                        title = "Indian Coffee House",
                        amount = 250.0,
                        type = "EXPENSE",
                        category = "Food",
                        timestamp = now - 86400000L
                    )
                )
                repository.insertTransaction(
                    Transaction(
                        userId = userId,
                        title = "Jio Monthly Recharge",
                        amount = 299.0,
                        type = "EXPENSE",
                        category = "Recharge",
                        timestamp = now
                    )
                )

                // Add initial budgets
                repository.insertBudget(Budget(userId = userId, category = "Food", limitAmount = 4000.0, monthYear = "$month-$year"))
                repository.insertBudget(Budget(userId = userId, category = "Recharge", limitAmount = 6000.0, monthYear = "$month-$year"))
                repository.insertBudget(Budget(userId = userId, category = "Entertainment", limitAmount = 2000.0, monthYear = "$month-$year"))

                // Add initial savings goals
                repository.insertGoal(
                    SavingsGoal(
                        userId = userId,
                        name = "New Semester Books",
                        targetAmount = 3000.0,
                        currentAmount = 1500.0,
                        targetDate = now + (86400000L * 30) // 30 days later
                    )
                )
                repository.insertGoal(
                    SavingsGoal(
                        userId = userId,
                        name = "Internship Laptop",
                        targetAmount = 45000.0,
                        currentAmount = 25000.0,
                        targetDate = now + (86400000L * 180) // 180 days later
                    )
                )

                // Initial notifications
                repository.insertNotification(
                    Notification(
                        userId = userId,
                        title = "Welcome to PocketPaise! 🇮🇳",
                        message = "Your student finance companion is ready. We have credited your initial pocket money to start tracking!",
                        timestamp = now,
                        type = "GENERAL"
                    )
                )
            }
        }
    }

    // --- Authentication Actions ---
    fun fetchRemoteConfig() {
        viewModelScope.launch {
            try {
                val response = com.vsdigitallabs.pocketpaise.stdbt.data.network.RetrofitClient.apiService.getConfig()
                if (response.isSuccessful && response.body()?.success == true) {
                    val config = response.body()?.data ?: return@launch
                    val editor = prefs.edit()
                    
                    val appMap = config["app"] as? Map<*, *>
                    if (appMap != null) {
                        editor.putBoolean("maintenance_mode", (appMap["maintenance_mode"] as? Number)?.toInt() == 1)
                        editor.putString("maintenance_message", appMap["maintenance_message"] as? String ?: "")
                        editor.putString("daily_reminder_message", appMap["daily_reminder_message"] as? String ?: "")
                        editor.putString("daily_reminder_time", appMap["daily_reminder_time"] as? String ?: "")
                        editor.putString("support_email", appMap["support_email"] as? String ?: "")
                    }
                    
                    val admobMap = config["admob"] as? Map<*, *>
                    if (admobMap != null) {
                        editor.putBoolean("admob_test_mode", (admobMap["test_mode"] as? Number)?.toInt() == 1)
                        editor.putBoolean("banner_enabled", (admobMap["banner_enabled"] as? Number)?.toInt() == 1)
                        editor.putString("banner_unit_id", admobMap["banner_unit_id"] as? String ?: "")
                    }
                    
                    editor.apply()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun registerUser(
        name: String, email: String, mobile: String, college: String, city: String, pass: String,
        onSuccess: () -> Unit, onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (name.isEmpty() || email.isEmpty() || mobile.isEmpty() || pass.isEmpty()) {
                onError("Please fill in all required fields.")
                return@launch
            }
            try {
                // 1. API Call
                val response = com.vsdigitallabs.pocketpaise.stdbt.data.network.RetrofitClient.apiService.register(
                    mapOf(
                        "name" to name, "email" to email, "password" to pass,
                        "mobile_number" to mobile, "college_name" to college, "city" to city
                    )
                )
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val token = response.body()?.data?.token
                    val serverUserId = response.body()?.data?.user_id?.toLong() ?: 0L
                    
                    com.vsdigitallabs.pocketpaise.stdbt.data.network.RetrofitClient.authToken = token
                    prefs.edit().putString("authToken", token).apply()
                    
                    // 2. Save locally
                    val newUser = User(
                        id = serverUserId,
                        name = name,
                        email = email,
                        mobileNumber = mobile,
                        collegeName = college,
                        city = city,
                        passwordHash = pass,
                        pocketMoneyLimit = 0.0,
                        currentSavings = 0.0
                    )
                    
                    val existing = repository.getUserByEmail(email)
                    if (existing == null) {
                        repository.insertUser(newUser)
                    } else {
                        repository.updateUser(newUser)
                    }
                    
                    _currentUserId.value = serverUserId
                    onSuccess()
                } else {
                    val errorString = response.errorBody()?.string()
                    val errorMessage = if (errorString != null) {
                        try {
                            org.json.JSONObject(errorString).getString("message")
                        } catch (e: Exception) {
                            "Registration failed on server"
                        }
                    } else {
                        "Registration failed on server"
                    }
                    onError(errorMessage)
                }
            } catch (e: java.net.UnknownHostException) {
                onError("No internet connection. Please check your network and try again.")
            } catch (e: java.net.SocketTimeoutException) {
                onError("Connection timed out. Please try again.")
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Registration failed")
            }
        }
    }

    fun loginUser(email: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            if (email.isEmpty() || pass.isEmpty()) {
                onError("Please enter both email and password.")
                return@launch
            }
            try {
                // 1. API Call
                val response = com.vsdigitallabs.pocketpaise.stdbt.data.network.RetrofitClient.apiService.login(
                    mapOf("email" to email, "password" to pass)
                )
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val token = response.body()?.data?.token
                    val serverUserId = response.body()?.data?.user_id?.toLong() ?: 0L
                    val name = response.body()?.data?.name ?: "User"
                    
                    com.vsdigitallabs.pocketpaise.stdbt.data.network.RetrofitClient.authToken = token
                    prefs.edit().putString("authToken", token).apply()
                    
                    // 2. Cache User locally
                    val user = User(
                        id = serverUserId, // Use server ID directly to avoid cross-user data leakage!
                        name = name,
                        email = email,
                        mobileNumber = "",
                        collegeName = "",
                        city = "",
                        passwordHash = pass,
                        pocketMoneyLimit = 0.0,
                        currentSavings = 0.0
                    )
                    
                    val existing = repository.getUserByEmail(email)
                    if (existing == null) {
                        repository.insertUser(user)
                    } else {
                        repository.updateUser(user)
                    }
                    
                    _currentUserId.value = serverUserId
                    prefs.edit().putLong("currentUserId", serverUserId).apply()
                    
                    // Fetch data from server to populate DB
                    fetchAndSyncData(serverUserId, onSuccess = {
                        onSuccess()
                    }, onError = {
                        onSuccess()
                    })
                } else {
                    val errorString = response.errorBody()?.string()
                    val errorMessage = if (errorString != null) {
                        try {
                            org.json.JSONObject(errorString).getString("message")
                        } catch (e: Exception) {
                            "Invalid email or password."
                        }
                    } else {
                        "Invalid email or password."
                    }
                    onError(errorMessage)
                }
            } catch (e: java.net.UnknownHostException) {
                onError("No internet connection. Please check your network and try again.")
            } catch (e: java.net.SocketTimeoutException) {
                onError("Connection timed out. Please try again.")
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Login failed")
            }
        }
    }

    fun googleSignIn(email: String, name: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val user = repository.getUserByEmail(email)
            if (user != null) {
                _currentUserId.value = user.id
                onSuccess()
            } else {
                val newUser = User(
                    name = name,
                    email = email,
                    mobileNumber = "9999999999",
                    collegeName = "National College",
                    city = "Bangalore",
                    passwordHash = "google_auth_placeholder",
                    pocketMoneyLimit = 5000.0,
                    currentSavings = 0.0
                )
                val newId = repository.insertUser(newUser)
                _currentUserId.value = newId
                
                // Greeting notification
                repository.insertNotification(
                    Notification(
                        userId = newId,
                        title = "Signed in with Google 🚀",
                        message = "Welcome to PocketPaise! You logged in securely using your Google account.",
                        timestamp = System.currentTimeMillis(),
                        type = "GENERAL"
                    )
                )
                onSuccess()
            }
        }
    }

    fun forgotPassword(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val user = repository.getUserByEmail(email)
            if (user == null) {
                onError("No account found with this email.")
            } else {
                // Mock password reset OTP / reset link
                onSuccess()
            }
        }
    }

    fun changePassword(oldPass: String, newPass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val user = currentUser.value
            if (user == null) {
                onError("User session expired.")
                return@launch
            }
            if (user.passwordHash != oldPass) {
                onError("Current password is incorrect.")
                return@launch
            }
            if (newPass.length < 4) {
                onError("New password must be at least 4 characters long.")
                return@launch
            }
            val updated = user.copy(passwordHash = newPass)
            repository.updateUser(updated)
            onSuccess()
        }
    }

    fun loginWithGoogle(email: String, name: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = com.vsdigitallabs.pocketpaise.stdbt.data.network.RetrofitClient.apiService.googleAuth(
                    mapOf("email" to email, "name" to name)
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    val token = response.body()?.data?.token
                    val serverUserId = response.body()?.data?.user_id?.toLong() ?: 0L
                    
                    com.vsdigitallabs.pocketpaise.stdbt.data.network.RetrofitClient.authToken = token
                    prefs.edit().putString("authToken", token).apply()
                    
                    val user = User(
                        id = serverUserId,
                        name = name,
                        email = email,
                        mobileNumber = "",
                        collegeName = "",
                        city = "",
                        passwordHash = "",
                        pocketMoneyLimit = 0.0,
                        currentSavings = 0.0
                    )
                    
                    val existing = repository.getUserByEmail(email)
                    if (existing == null) {
                        repository.insertUser(user)
                    } else {
                        repository.updateUser(user)
                    }
                    
                    _currentUserId.value = serverUserId
                    prefs.edit().putLong("currentUserId", serverUserId).apply()
                    
                    // Navigate immediately, sync in background
                    onSuccess()
                    fetchAndSyncData(serverUserId, {}, {})
                } else {
                    val errorString = response.errorBody()?.string()
                    val errorMessage = if (errorString != null) {
                        try {
                            org.json.JSONObject(errorString).getString("message")
                        } catch (e: Exception) {
                            "Google login failed."
                        }
                    } else {
                        "Google login failed."
                    }
                    onError(errorMessage)
                }
            } catch (e: java.net.UnknownHostException) {
                onError("No internet connection. Please check your network and try again.")
            } catch (e: java.net.SocketTimeoutException) {
                onError("Connection timed out. Please try again.")
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Google login failed")
            }
        }
    }

    private fun fetchAndSyncData(localUserId: Long, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                // Fetch from Firebase Realtime Database
                val transactions = com.vsdigitallabs.pocketpaise.stdbt.data.network.FirebaseSyncManager.getTransactions(localUserId)
                val budgets = com.vsdigitallabs.pocketpaise.stdbt.data.network.FirebaseSyncManager.getBudgets(localUserId)
                val goals = com.vsdigitallabs.pocketpaise.stdbt.data.network.FirebaseSyncManager.getGoals(localUserId)
                
                repository.syncData(localUserId, transactions, budgets, goals)
                        
                        // Calculate total balances correctly after sync
                        val balance = transactions.fold(0.0) { acc, tx ->
                            if (tx.type == "INCOME") acc + tx.amount else acc - tx.amount
                        }
                        
                        var updatedUser = repository.getUserByIdSync(localUserId)
                        
                        // Fetch remote profile data to ensure absolute sync
                        try {
                            val profileRes = com.vsdigitallabs.pocketpaise.stdbt.data.network.RetrofitClient.apiService.getProfile()
                            if (profileRes.isSuccessful && profileRes.body()?.success == true) {
                                val profileData = profileRes.body()?.data
                                if (profileData != null && updatedUser != null) {
                                    val rName = profileData["name"] as? String ?: updatedUser.name
                                    val rMobile = profileData["mobile_number"] as? String ?: updatedUser.mobileNumber
                                    val rCollege = profileData["college_name"] as? String ?: updatedUser.collegeName
                                    val rCity = profileData["city"] as? String ?: updatedUser.city
                                    // Override balances if provided by server
                                    val rPocketMoney = (profileData["pocketMoneyLimit"] as? Number)?.toDouble() ?: balance
                                    val rSavings = (profileData["currentSavings"] as? Number)?.toDouble() ?: updatedUser.currentSavings
                                    
                                    updatedUser = updatedUser.copy(
                                        name = rName,
                                        mobileNumber = rMobile,
                                        collegeName = rCollege,
                                        city = rCity,
                                        pocketMoneyLimit = rPocketMoney,
                                        currentSavings = rSavings
                                    )
                                }
                            } else {
                                // If profile fetch fails, fallback to calculated balance
                                updatedUser = updatedUser?.copy(pocketMoneyLimit = balance)
                            }
                        } catch (e: Exception) {
                            updatedUser = updatedUser?.copy(pocketMoneyLimit = balance)
                        }

                        if (updatedUser != null) {
                            repository.updateUser(updatedUser)
                        }
                
                onSuccess()
            } catch (e: Exception) {
                // If sync fails, still let user log in, but show toast or message if possible
                e.printStackTrace()
                onSuccess()
            }
        }
    }


    fun logout(onComplete: () -> Unit) {
        _currentUserId.value = null
        prefs.edit().remove("currentUserId").remove("authToken").apply()
        com.vsdigitallabs.pocketpaise.stdbt.data.network.RetrofitClient.authToken = null
        onComplete()
    }

    fun updateProfile(name: String, college: String, city: String, mobile: String) {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            val updated = user.copy(
                name = name,
                collegeName = college,
                city = city,
                mobileNumber = mobile
            )
            repository.updateUser(updated)
            
            // Push profile update to server
            try {
                val profileData = mapOf(
                    "name" to name,
                    "mobile_number" to mobile,
                    "college_name" to college,
                    "city" to city,
                    "pocketMoneyLimit" to updated.pocketMoneyLimit,
                    "currentSavings" to updated.currentSavings
                )
                com.vsdigitallabs.pocketpaise.stdbt.data.network.RetrofitClient.apiService.updateProfile(profileData)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addTransaction(title: String, amount: Double, type: String, category: String, notes: String, receiptUri: String? = null) {
        viewModelScope.launch {
            val userId = _currentUserId.value ?: return@launch
            
            val tx = Transaction(
                userId = userId,
                title = title,
                amount = amount,
                type = type,
                category = category,
                timestamp = System.currentTimeMillis(),
                notes = notes,
                receiptUri = receiptUri,
                isSynced = true
            )
            repository.insertTransaction(tx)
            com.vsdigitallabs.pocketpaise.stdbt.data.network.FirebaseSyncManager.saveTransaction(userId, tx)
        }
    }

    fun deleteTransaction(tx: Transaction) {
        viewModelScope.launch {
            com.vsdigitallabs.pocketpaise.stdbt.data.network.FirebaseSyncManager.deleteTransaction(tx.userId, tx.uuid)
            repository.deleteTransaction(tx)
        }
    }

    fun updateTransaction(oldTx: Transaction, newTx: Transaction) {
        viewModelScope.launch {
            val txToSave = newTx.copy(isSynced = true)
            repository.updateTransaction(oldTx, txToSave)
            com.vsdigitallabs.pocketpaise.stdbt.data.network.FirebaseSyncManager.saveTransaction(txToSave.userId, txToSave)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(category: String?) {
        _selectedCategoryFilter.value = category
    }

    fun setTypeFilter(type: String?) {
        _selectedTypeFilter.value = type
    }

    // --- Budget Actions ---
    fun addBudget(category: String, limitAmount: Double, monthYear: String) {
        viewModelScope.launch {
            val userId = _currentUserId.value ?: return@launch
            
            val b = Budget(
                userId = userId,
                category = category,
                limitAmount = limitAmount,
                monthYear = monthYear,
                isSynced = true
            )
            repository.insertBudget(b)
            com.vsdigitallabs.pocketpaise.stdbt.data.network.FirebaseSyncManager.saveBudget(userId, b)
        }
    }

    fun deleteBudget(id: Long) {
        viewModelScope.launch {
            val budget = repository.getBudgetByIdSync(id)
            if (budget != null) {
                com.vsdigitallabs.pocketpaise.stdbt.data.network.FirebaseSyncManager.deleteBudget(budget.userId, budget.uuid)
                repository.deleteBudgetById(id)
            }
        }
    }

    // --- Savings Goal Actions ---
    fun addGoal(name: String, targetAmount: Double, targetDate: Long) {
        viewModelScope.launch {
            val userId = _currentUserId.value ?: return@launch
            
            val goal = SavingsGoal(
                userId = userId,
                name = name,
                targetAmount = targetAmount,
                currentAmount = 0.0,
                targetDate = targetDate,
                isSynced = true
            )
            repository.insertGoal(goal)
            com.vsdigitallabs.pocketpaise.stdbt.data.network.FirebaseSyncManager.saveGoal(userId, goal)
        }
    }

    fun addFundsToGoal(goal: SavingsGoal, amount: Double, onError: (String) -> Unit) {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            if (user.pocketMoneyLimit < amount) {
                onError("Insufficient balance in Pocket Money to contribute to this goal.")
                return@launch
            }
            
            // Deduct pocket money
            val updatedUser = user.copy(
                pocketMoneyLimit = user.pocketMoneyLimit - amount,
                currentSavings = user.currentSavings + amount
            )
            repository.updateUser(updatedUser)

            // Update goal
            val updatedGoalAmount = goal.currentAmount + amount
            val isCompleted = updatedGoalAmount >= goal.targetAmount
            val updatedGoal = goal.copy(
                currentAmount = updatedGoalAmount,
                isCompleted = isCompleted,
                isSynced = true
            )
            repository.updateGoal(updatedGoal)
            com.vsdigitallabs.pocketpaise.stdbt.data.network.FirebaseSyncManager.saveGoal(user.id, updatedGoal)
            
            // Add a matching tracking transaction for transparency
            val tx = Transaction(
                userId = user.id,
                title = "Saved for: ${goal.name}",
                amount = amount,
                type = "EXPENSE",
                category = "Savings",
                timestamp = System.currentTimeMillis(),
                isSynced = true
            )
            repository.insertTransaction(tx)
            com.vsdigitallabs.pocketpaise.stdbt.data.network.FirebaseSyncManager.saveTransaction(user.id, tx)
        }
    }

    fun updateGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            val goalToSave = goal.copy(isSynced = true)
            repository.updateGoal(goalToSave)
            com.vsdigitallabs.pocketpaise.stdbt.data.network.FirebaseSyncManager.saveGoal(goalToSave.userId, goalToSave)
        }
    }

    fun deleteGoal(id: Long) {
        viewModelScope.launch {
            val goal = repository.getGoalByIdSync(id)
            if (goal != null) {
                com.vsdigitallabs.pocketpaise.stdbt.data.network.FirebaseSyncManager.deleteGoal(goal.userId, goal.uuid)
                repository.deleteGoalById(id)
            }
        }
    }

    // --- Notification Center Actions ---
    fun markAllNotificationsRead() {
        viewModelScope.launch {
            val userId = _currentUserId.value ?: return@launch
            repository.markAllAsRead(userId)
        }
    }

    fun markNotificationRead(id: Long) {
        viewModelScope.launch {
            repository.markAsRead(id)
        }
    }

    fun deleteNotification(id: Long) {
        viewModelScope.launch {
            repository.deleteNotificationById(id)
        }
    }

    fun clearNotifications() {
        viewModelScope.launch {
            val userId = _currentUserId.value ?: return@launch
            repository.clearAllNotifications(userId)
        }
    }

    // Settings adjustments
    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
    }

    fun setCurrency(symbol: String) {
        _selectedCurrency.value = symbol
    }

    fun setLanguage(language: String) {
        _selectedLanguage.value = language
    }

    fun setReminderTime(context: android.content.Context, time: String) {
        _reminderTime.value = time
        prefs.edit().putString("daily_reminder_time", time).apply()
        com.vsdigitallabs.pocketpaise.stdbt.worker.DailyReminderManager.scheduleReminder(context, time)
    }

    fun setDateFormat(format: String) {
        _dateFormat.value = format
    }

    // Mock notification push trigger for testing FCM feature
    fun triggerMockNotification(title: String, message: String) {
        viewModelScope.launch {
            val userId = _currentUserId.value ?: return@launch
            repository.insertNotification(
                Notification(
                    userId = userId,
                    title = title,
                    message = message,
                    timestamp = System.currentTimeMillis(),
                    type = "GENERAL"
                )
            )
        }
    }
}

// ViewModel Factory to create custom constructor viewModel without heavy DI
class PocketPaiseViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PocketPaiseViewModel::class.java)) {
            val database = AppDatabase.getDatabase(application)
            val repository = PocketPaiseRepository(
                userDao = database.userDao(),
                transactionDao = database.transactionDao(),
                budgetDao = database.budgetDao(),
                savingsGoalDao = database.savingsGoalDao(),
                notificationDao = database.notificationDao()
            )
            @Suppress("UNCHECKED_CAST")
            return PocketPaiseViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
