package com.example.neuralnotesproject

import android.app.Application
import com.google.firebase.FirebaseApp

class NeuralNotesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
} 