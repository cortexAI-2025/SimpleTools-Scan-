package com.simpletools.core.billing.play

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.simpletools.core.billing.LicenseStore
import java.io.File
import kotlinx.coroutines.flow.first

/**
 * The remembered entitlement, in the app's private storage. It only records a purchase that Google
 * Play confirmed; it is what keeps PRO available offline.
 */
class DataStoreLicenseStore(private val dataStore: DataStore<Preferences>) : LicenseStore {

    override suspend fun rememberedOwnership(): Boolean = runCatching {
        dataStore.data.first()[OWNED] ?: false
    }.getOrDefault(false)

    override suspend fun remember(owned: Boolean) {
        runCatching {
            dataStore.edit { prefs ->
                if (prefs[OWNED] != owned) prefs[OWNED] = owned
            }
        }
    }

    companion object {
        private val OWNED: Preferences.Key<Boolean> = booleanPreferencesKey("product_owned")

        /** Store file of one product line: [fileName] keeps the licences of different apps apart. */
        fun create(context: Context, fileName: String = "license.preferences_pb"): DataStoreLicenseStore =
            DataStoreLicenseStore(
                PreferenceDataStoreFactory.create(
                    corruptionHandler = null,
                    produceFile = { File(context.applicationContext.filesDir, fileName) },
                ),
            )
    }
}
