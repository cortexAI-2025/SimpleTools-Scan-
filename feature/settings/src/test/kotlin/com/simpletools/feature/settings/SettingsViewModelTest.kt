package com.simpletools.feature.settings

import com.simpletools.core.settings.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val repository = FakeSettingsRepository()
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = SettingsViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts loading then exposes stored settings`() = runTest(UnconfinedTestDispatcher()) {
        assertEquals(SettingsUiState.Loading, viewModel.uiState.value)

        backgroundScope.launch { viewModel.uiState.collect() }

        assertEquals(SettingsUiState.Ready(repository.settings.value), viewModel.uiState.value)
    }

    @Test
    fun `theme and dynamic color changes are persisted`() = runTest(UnconfinedTestDispatcher()) {
        backgroundScope.launch { viewModel.uiState.collect() }

        viewModel.setThemeMode(ThemeMode.DARK)
        viewModel.setDynamicColor(true)

        val state = viewModel.uiState.value as SettingsUiState.Ready
        assertEquals(ThemeMode.DARK, state.settings.themeMode)
        assertTrue(state.settings.dynamicColor)
    }
}
