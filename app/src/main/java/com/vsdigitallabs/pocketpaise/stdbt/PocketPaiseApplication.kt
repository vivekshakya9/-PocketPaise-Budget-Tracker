package com.vsdigitallabs.pocketpaise.stdbt

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase

class PocketPaiseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        try {
            FirebaseApp.initializeApp(this)
            // Enable offline persistence for Firebase Realtime Database
            val database = FirebaseDatabase.getInstance("https://pocketpaise-b7c71-default-rtdb.asia-southeast1.firebasedatabase.app")
            database.setPersistenceEnabled(true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
