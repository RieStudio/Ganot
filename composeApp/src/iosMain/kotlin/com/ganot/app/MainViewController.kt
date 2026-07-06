package com.ganot.app

import androidx.compose.ui.window.ComposeUIViewController
import com.ganot.app.data.*
import com.ganot.app.ui.AcademicViewModel

fun MainViewController() = ComposeUIViewController {
    val dbFactory = DatabaseFactory()
    val database = getRoomDatabase(dbFactory.createDatabase())
    val repository = AcademicRepository(database)
    val viewModel = AcademicViewModel(repository)
    
    App(viewModel = viewModel)
}
