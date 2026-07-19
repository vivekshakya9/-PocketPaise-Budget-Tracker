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
import com.vsdigitallabs.pocketpaise.stdbt.worker.SyncWorker

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
        val savedId = prefs.getLong("currentUserId", -1L)
        if (savedId != -1L) {
            _currentUserId.value = savedId
            fetchAndSyncData(savedId, {}, {})
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
                    
                    // 2. Cache User locally
                    val user = User(
                        id = 0L, // Let SQLite auto-generate
                        name = name,
                        email = email,
                        mobileNumber = "",
                        collegeName = "",
                        city = "",
                        passwordHash = pass,
                        pocketMoneyLimit = 0.0,
                        currentSavings = 0.0
                    )
                    
                    val localUserId: Long
                    val existing = repository.getUserByEmail(email)
                    if (existing == null) {
                        localUserId = repository.insertUser(user)
                    } else {
                        localUserId = existing.id
                        repository.updateUser(user.copy(id = localUserId))
                    }
                    
                    _currentUserId.value = localUserId
                    prefs.edit().putLong("currentUserId", localUserId).apply()
                    
                    // Navigate immediately, sync in background
                    onSuccess()
                    fetchAndSyncData(localUserId, {}, {})
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
                    
                    val user = User(
                        id = 0L,
                        name = name,
                        email = email,
                        mobileNumber = "",
                        collegeName = "",
                        city = "",
                        passwordHash = "",
                        pocketMoneyLimit = 0.0,
                        currentSavings = 0.0
                    )
                    
                    val localUserId: Long
                    val existing = repository.getUserByEmail(email)
                    if (existing == null) {
                        localUserId = repository.insertUser(user)
                    } else {
                        localUserId = existing.id
                        repository.updateUser(user.copy(id = localUserId))
                    }
                    
                    _currentUserId.value = localUserId
                    prefs.edit().putLong("currentUserId", localUserId).apply()
                    
                    // Navigate immediately, sync in background
                    onSuccess()
                    fetchAndSyncData(localUserId, {}, {})
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
                val syncRes = com.vsdigitallabs.pocketpaise.stdbt.data.network.RetrofitClient.apiService.getSyncData()
                if (syncRes.isSuccessful && syncRes.body()?.success == true) {
                    val data = syncRes.body()?.data
                    if (data != null) {
                        val transactions = data.transactions.map { it.copy(userId = localUserId, isSynced = true, serverId = it.id, id = 0L) }
                        val budgets = data.budgets.map { it.copy(userId = localUserId, isSynced = true, serverId = it.id, id = 0L) }
                        val goals = data.goals.map { it.copy(userId = localUserId, isSynced = true, serverId = it.id, id = 0L) }
                        repository.syncData(localUserId, transactions, budgets, goals)
                        
                        // Calculate total balances correctly after sync
                        val balance = transactions.fold(0.0) { acc, tx ->
                            if (tx.type == "INCOME") acc + tx.amount else acc - tx.amount
                        }
                        val user = repository.getUserByIdSync(localUserId)
                        if (user != null) {
                            repository.updateUser(user.copy(pocketMoneyLimit = balance))
                        }
                    }
                }
                onSuccess()
            } catch (e: Exception) {
                // If sync fails, still let user log in, but show toast or message if possible
                onSuccess()
            }
        }
    }


    fun logout(onComplete: () -> Unit) {
        _currentUserId.value = null
        prefs.edit().remove("currentUserId").apply()
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
        }
    }

    fun addTransaction(title: String, amount: Double, type: String, category: String, notes: String, receiptUri: String? = null) {
        viewModelScope.launch {
            val userId = _currentUserId.value ?: return@launch
            
            // Format for API
            val apiData = mapOf(
                "title" to title,
                "amount" to amount.toString(),
                "category" to category,
                "notes" to notes
            )
            
            try {
                val apiRes = if (type == "INCOME") {
                    com.vsdigitallabs.pocketpaise.stdbt.data.network.RetrofitClient.apiService.createIncome(apiData)
                } else {
                    com.vsdigitallabs.pocketpaise.stdbt.data.network.RetrofitClient.apiService.createExpense(apiData)
                }
                
                var serverId = 0L
                if (apiRes.isSuccessful && apiRes.body()?.success == true) {
                    val data = apiRes.body()?.data as? Map<*, *>
                    serverId = (data?.get("id") as? Number)?.toLong() ?: 0L
                }

                val tx = Transaction(
                    userId = userId,
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    timestamp = System.currentTimeMillis(),
                    notes = notes,
                    receiptUri = receiptUri,
                    serverId = serverId,
                    isSynced = serverId > 0
                )
                repository.insertTransaction(tx)
                if (serverId == 0L) enqueueSyncWorker()
            } catch (e: Exception) {
                // Fallback to local
                val tx = Transaction(
                    userId = userId,
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    timestamp = System.currentTimeMillis(),
                    notes = notes,
                    receiptUri = receiptUri,
                    isSynced = false
                )
                repository.insertTransaction(tx)
                enqueueSyncWorker()
            }
        }
    }

    fun deleteTransaction(tx: Transaction) {
        viewModelScope.launch {
            try {
                if (tx.serverId > 0) {
                    if (tx.type == "INCOME") {
                        com.vsdigitallabs.pocketpaise.stdbt.data.network.RetrofitClient.apiService.deleteIncome(tx.serverId)
                    } else {
                        com.vsdigitallabs.pocketpaise.stdbt.data.network.RetrofitClient.apiService.deleteExpense(tx.serverId)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            repository.deleteTransaction(tx)
        }
    }

    fun updateTransaction(oldTx: Transaction, newTx: Transaction) {
        viewModelScope.launch {
            repository.updateTransaction(oldTx, newTx)
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
            
            val apiData = mapOf(
                "amount" to limitAmount.toString(),
                "month_year" to monthYear
            )
            
            var serverId = 0L
            try {
                val apiRes = com.vsdigitallabs.pocketpaise.stdbt.data.network.RetrofitClient.apiService.createBudget(apiData)
                if (apiRes.isSuccessful && apiRes.body()?.success == true) {
                    val data = apiRes.body()?.data as? Map<*, *>
                    serverId = (data?.get("id") as? Number)?.toLong() ?: 0L
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val budget = Budget(
                userId = userId,
                category = category,
                limitAmount = limitAmount,
                monthYear = monthYear,
                serverId = serverId,
                isSynced = serverId > 0
            )
            repository.insertBudget(budget)
            if (serverId == 0L) enqueueSyncWorker()
        }
    }

    fun deleteBudget(id: Long) {
        viewModelScope.launch {
            val budget = repository.getBudgetByIdSync(id)
            try {
                if (budget != null && budget.serverId > 0) {
                    com.vsdigitallabs.pocketpaise.stdbt.data.network.RetrofitClient.apiService.deleteBudget(budget.serverId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            repository.deleteBudgetById(id)
        }
    }

    // --- Savings Goal Actions ---
    fun addGoal(name: String, targetAmount: Double, targetDate: Long) {
        viewModelScope.launch {
            val userId = _currentUserId.value ?: return@launch
            
            val apiData = mapOf(
                "name" to name,
                "target" to targetAmount.toString(),
                "current" to "0",
                "date" to java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(targetDate))
            )
            
            var serverId = 0L
            try {
                val apiRes = com.vsdigitallabs.pocketpaise.stdbt.data.network.RetrofitClient.apiService.createGoal(apiData)
                if (apiRes.isSuccessful && apiRes.body()?.success == true) {
                    val data = apiRes.body()?.data as? Map<*, *>
                    serverId = (data?.get("id") as? Number)?.toLong() ?: 0L
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val goal = SavingsGoal(
                userId = userId,
                name = name,
                targetAmount = targetAmount,
                currentAmount = 0.0,
                targetDate = targetDate,
                serverId = serverId,
                isSynced = serverId > 0
            )
            repository.insertGoal(goal)
            if (serverId == 0L) enqueueSyncWorker()
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
                isCompleted = isCompleted
            )
            repository.updateGoal(updatedGoal)
            
            // Add a matching tracking transaction for transparency
            repository.insertTransaction(
                Transaction(
                    userId = user.id,
                    title = "Saved for: ${goal.name}",
                    amount = amount,
                    type = "EXPENSE",
                    category = "Savings",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteGoal(id: Long) {
        viewModelScope.launch {
            val goal = repository.getGoalByIdSync(id)
            try {
                if (goal != null && goal.serverId > 0) {
                    com.vsdigitallabs.pocketpaise.stdbt.data.network.RetrofitClient.apiService.deleteGoal(goal.serverId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            repository.deleteGoalById(id)
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

    private fun enqueueSyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val syncWorkRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(getApplication()).enqueue(syncWorkRequest)
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
