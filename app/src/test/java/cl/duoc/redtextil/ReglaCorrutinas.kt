package cl.duoc.redtextil

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

// Reemplaza Dispatchers.Main para que viewModelScope funcione en la JVM y comparta el reloj virtual de runTest
@OptIn(ExperimentalCoroutinesApi::class)
class ReglaCorrutinas(val dispatcher: TestDispatcher = UnconfinedTestDispatcher()) : TestWatcher() {

    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)

    override fun finished(description: Description) = Dispatchers.resetMain()
}

// Los uiState usan SharingStarted.WhileSubscribed: sin alguien suscrito no se calculan
@OptIn(ExperimentalCoroutinesApi::class)
fun TestScope.suscribir(flujo: Flow<*>) {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flujo.collect {} }
}
