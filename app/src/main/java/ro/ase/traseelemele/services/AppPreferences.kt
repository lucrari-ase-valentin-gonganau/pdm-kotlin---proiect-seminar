package ro.ase.traseelemele.services

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

class AppPreferences(private val context: Context) {

    private val dataStore = context.dataStore

    suspend fun save(key: String, value: String) {
        val prefKey = stringPreferencesKey(key)
        dataStore.edit { preferences ->
            preferences[prefKey] = value
        }
    }

    suspend fun saveBoolean(key: String, value: Boolean) {
        val prefKey = booleanPreferencesKey(key)
        dataStore.edit { preferences ->
            preferences[prefKey] = value
        }
    }

    fun get(key: String): Flow<String?> {
        val prefKey = stringPreferencesKey(key)
        return dataStore.data.map { preferences ->
            preferences[prefKey]
        }
    }

    fun getBoolean(key: String, default: Boolean = false): Flow<Boolean> {
        val prefKey = booleanPreferencesKey(key)
        return dataStore.data.map { preferences ->
            preferences[prefKey] ?: default
        }
    }

    suspend fun getOnce(key: String, default: String = ""): String {
        val prefKey = stringPreferencesKey(key)
        val preferences = dataStore.data.first()
        return preferences[prefKey] ?: default
    }

    suspend fun getBooleanOnce(key: String, default: Boolean = false): Boolean {
        val prefKey = booleanPreferencesKey(key)
        val preferences = dataStore.data.first()
        return preferences[prefKey] ?: default
    }
}
