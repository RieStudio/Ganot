package com.ganot.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ganot.app.ui.AcademicViewModel
import com.ganot.app.ui.MainAppUI
import com.ganot.app.ui.theme.MyApplicationTheme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ganot.app.data.AcademicRepository
import com.ganot.app.data.AppDatabase
import com.ganot.app.data.DatabaseFactory
import com.ganot.app.data.getRoomDatabase

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    initSettings(this)
    initPlatformHelper(this)
    
    val dbFactory = DatabaseFactory(this)
    val database = getRoomDatabase(dbFactory.createDatabase())
    val repository = AcademicRepository(database)
    
    enableEdgeToEdge()
    setContent {
      val viewModel: AcademicViewModel = viewModel(
          factory = object : ViewModelProvider.Factory {
              override fun <T : ViewModel> create(modelClass: Class<T>): T {
                  return AcademicViewModel(repository) as T
              }
          }
      )
      App(viewModel = viewModel)
    }
  }
}
