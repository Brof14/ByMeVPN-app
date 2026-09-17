package com.example.bymevpn.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class AppLanguage(val code: String, val titleEn: String, val titleRu: String) {
    SYSTEM("system", "System Default", "Как в системе"),
    RUSSIAN("ru", "Russian (Русский)", "Русский"),
    ENGLISH("en", "English", "English")
}

object LocaleManager {
    private val _currentLanguage = MutableStateFlow(AppLanguage.SYSTEM)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
    }

    /**
     * Helper to determine whether Russian should be displayed given current language and device locale.
     */
    fun isRussian(context: Context? = null): Boolean {
        return when (_currentLanguage.value) {
            AppLanguage.RUSSIAN -> true
            AppLanguage.ENGLISH -> false
            AppLanguage.SYSTEM -> {
                val sysLang = Locale.getDefault().language
                sysLang.startsWith("ru") || sysLang.startsWith("be") || sysLang.startsWith("uk") || sysLang.startsWith("kk")
            }
        }
    }
}
