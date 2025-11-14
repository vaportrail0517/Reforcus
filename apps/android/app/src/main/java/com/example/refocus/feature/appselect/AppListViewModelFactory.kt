package com.example.refocus.feature.appselect

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.refocus.data.datastore.TargetsDataStore
import com.example.refocus.data.repository.TargetsRepository

class AppListViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppListViewModel::class.java)) {
            val targetsDataStore = TargetsDataStore(application)
            val targetsRepository = TargetsRepository(targetsDataStore)
            return AppListViewModel(application = application, targetsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
