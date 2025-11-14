package com.example.refocus.feature.appselect

import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.graphics.drawable.Drawable
import com.example.refocus.data.repository.TargetsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AppListViewModel(
    private val pm: PackageManager,
    private val targetsRepository: TargetsRepository
) : ViewModel() {

    data class AppUiModel(
        val label: String,
        val packageName: String,
        val icon: Drawable,
        val isSelected: Boolean
    )

    private val _apps = MutableStateFlow<List<AppUiModel>>(emptyList())
    val apps: StateFlow<List<AppUiModel>> = _apps.asStateFlow()

    // 内部の選択セット
    private val selected = MutableStateFlow<Set<String>>(emptySet())

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val launchIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val activities: List<ResolveInfo> =
                pm.queryIntentActivities(launchIntent, 0)

            val currentTargets = targetsRepository.observeTargets().first()

            selected.value = currentTargets

            val appList = activities.map { info ->
                val label = info.loadLabel(pm).toString()
                val pkg = info.activityInfo.packageName
                val icon = info.loadIcon(pm)
                AppUiModel(
                    label = label,
                    packageName = pkg,
                    icon = icon,
                    isSelected = pkg in currentTargets
                )
            }.sortedBy { it.label.lowercase() }

            _apps.value = appList
        }
    }

    fun toggleSelection(packageName: String) {
        val current = selected.value
        selected.value = if (packageName in current) {
            current - packageName
        } else {
            current + packageName
        }
        // UI state 更新
        _apps.value = _apps.value.map {
            if (it.packageName == packageName) it.copy(isSelected = !it.isSelected) else it
        }
    }

    fun save(onSaved: () -> Unit) {
        viewModelScope.launch {
            targetsRepository.setTargets(selected.value)
            onSaved()
        }
    }
}
