package com.ganot.app

import androidx.compose.runtime.*
import com.ganot.app.ui.AcademicViewModel
import com.ganot.app.ui.MainAppUI
import com.ganot.app.ui.theme.MyApplicationTheme

@Composable
fun App(viewModel: AcademicViewModel) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    MyApplicationTheme(darkTheme = isDarkTheme) {
        MainAppUI(viewModel = viewModel)
    }
}
