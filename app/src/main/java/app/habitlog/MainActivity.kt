package app.habitlog

import android.app.Application
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import app.habitlog.data.LocalBackup
import app.habitlog.data.LogRepository
import app.habitlog.data.LogSnapshot
import app.habitlog.model.AppAccent
import app.habitlog.model.today
import app.habitlog.system.DeviceFeatures
import app.habitlog.ui.PivotApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var model: LogViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT))
        model = ViewModelProvider(this)[LogViewModel::class.java]
        if (savedInstanceState == null) openFromIntent(intent)
        setContent { PivotApp(model) }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    model.now.value = today()
                    delay(5_000)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::model.isInitialized) {
            model.now.value = today()
            model.refreshDevice()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openFromIntent(intent)
    }

    private fun openFromIntent(intent: Intent) {
        val habitId = intent.getLongExtra(OPEN_HABIT, 0)
        if (habitId > 0) model.requestedHabit.value = habitId
    }

    companion object {
        const val OPEN_HABIT = "app.habitlog.OPEN_HABIT"
    }
}

data class Feedback(val text: String, val habitId: Long? = null, val day: Long? = null)

class LogViewModel(private val application: Application) : AndroidViewModel(application) {
    private val repository = LogRepository(application)
    val state = repository.snapshots.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val now = MutableStateFlow(today())
    private val selectedAccent = MutableStateFlow(AppAccent.load(application))
    val accent = selectedAccent.asStateFlow()
    val requestedHabit = MutableStateFlow<Long?>(null)
    val pendingBackup = MutableStateFlow<LogSnapshot?>(null)
    private val messageChannel = Channel<Feedback>(Channel.BUFFERED)
    val messages = messageChannel.receiveAsFlow()

    fun write(message: String? = null, operation: suspend LogRepository.() -> Unit) {
        launch("Couldn’t save that change.") {
            repository.operation()
            message?.let { messageChannel.send(Feedback(it)) }
            updateDevice()
        }
    }

    fun checkIn(id: Long, name: String, day: Long) = launch("Couldn’t save the check-in.") {
        if (day != today()) {
            now.value = today()
            return@launch
        }
        if (repository.quickCheckIn(id, day)) {
            messageChannel.send(Feedback("$name checked in", id, day))
        }
        updateDevice()
    }

    fun message(text: String) {
        messageChannel.trySend(Feedback(text))
    }

    fun export(uri: Uri) = launch("Couldn’t write the backup file.") {
        LocalBackup.write(application, uri, repository.snapshot())
        messageChannel.send(Feedback("Backup saved"))
    }

    fun previewBackup(uri: Uri) = launch("That file isn’t a valid Pivot backup.") {
        pendingBackup.value = LocalBackup.read(application, uri)
    }

    fun restore() {
        val snapshot = pendingBackup.value ?: return
        pendingBackup.value = null
        write("Backup restored") { restore(snapshot) }
    }

    fun selectAccent(accent: AppAccent) {
        if (selectedAccent.value == accent) return
        accent.save(application)
        selectedAccent.value = accent
        refreshDevice()
    }

    fun refreshDevice() {
        viewModelScope.launch { updateDevice() }
    }

    private suspend fun updateDevice() {
        try {
            DeviceFeatures.refresh(application)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e("Pivot", "Unable to refresh Android device features", error)
        }
    }

    private fun launch(failureText: String, operation: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                operation()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e("Pivot", failureText, error)
                messageChannel.send(Feedback(failureText))
            }
        }
    }
}
