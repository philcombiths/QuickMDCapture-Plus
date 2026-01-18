package com.example.quickmdcapture

import android.app.Dialog
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.ViewModelProvider
import com.example.quickmdcapture.TemplateAdapter
import com.example.quickmdcapture.SpeechRecognitionManager
import java.util.*

class NoteDialog(
    private val activity: AppCompatActivity, 
    private val isAutoSaveEnabled: Boolean,
    private val isFromReminder: Boolean
) : Dialog(activity) {

    private lateinit var settingsViewModel: SettingsViewModel
    private val speechRecognitionManager by lazy {
        SpeechRecognitionManager(
            context = context,
            onListeningStateChanged = { isListening ->
                isSpeechRecognitionActive = isListening
                updateSpeechButtonIcon()
                updateSaveButtonState()
            },
            onTextUpdated = { text -> updateNoteText(text) },
            onAutoSave = { text ->
                noteSaver.saveNote(
                    note = text,
                    onSuccess = {
                        hasAutoSaved = true
                        updateSaveButtonState()
                        Toast.makeText(context, R.string.note_saved, Toast.LENGTH_SHORT).show()
                        // Allow user to see the captured text briefly before closing
                        handler.postDelayed({ dismiss() }, 2000)
                    },
                    onError = { error ->
                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    }
    private val noteSaver by lazy { NoteSaver(context, settingsViewModel) }
    private val etNote by lazy { findViewById<EditText>(R.id.etNote) }
    private val btnSpeech by lazy { findViewById<ImageButton>(R.id.btnSpeech) }
    private val btnRestore by lazy { findViewById<ImageButton>(R.id.btnRestore) }
    private val templateSpinner by lazy { findViewById<Spinner>(R.id.templateSpinner) }
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var textWatcher: TextWatcher

    // State tracking for button enabling
    private var isSpeechRecognitionActive = false
    private var hasAutoSaved = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settingsViewModel = ViewModelProvider(activity)[SettingsViewModel::class.java]

        if (settingsViewModel.currentText.value.isNotEmpty()) {
            settingsViewModel.updatePreviousText(settingsViewModel.currentText.value)
            settingsViewModel.clearCurrentText()
        }

        setContentView(R.layout.note_dialog)

        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        window?.attributes?.apply {
            width = (context.resources.displayMetrics.widthPixels * 0.9).toInt()
        }

        window?.setBackgroundDrawableResource(R.drawable.rounded_dialog_background)

        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnCancel = findViewById<Button>(R.id.btnCancel)

        // Setup template spinner
        val templates = settingsViewModel.templates.value
        
        // Apply theme
        val theme = settingsViewModel.theme.value
        val dialogLayout = findViewById<LinearLayout>(R.id.noteDialogLayout)
        val buttonBackground = ContextCompat.getDrawable(context, R.drawable.rounded_button_background)
        when (theme) {
            "light" -> {
                dialogLayout.setBackgroundResource(R.drawable.rounded_dialog_background)
                etNote.setTextColor(ContextCompat.getColor(context, R.color.black))
                etNote.setHintTextColor(ContextCompat.getColor(context, R.color.black))
                btnSpeech.background = buttonBackground
                btnRestore.background = buttonBackground
                btnSpeech.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_mic))
                templateSpinner.setBackgroundResource(R.drawable.rounded_dialog_background)
                templateSpinner.setPopupBackgroundResource(R.drawable.rounded_dialog_background)
                templateSpinner.adapter = TemplateAdapter(
                    context,
                    templates.map { it.name },
                    ContextCompat.getColor(context, R.color.black)
                )
            }
            "dark" -> {
                dialogLayout.setBackgroundResource(R.drawable.rounded_dialog_background_dark)
                etNote.setTextColor(ContextCompat.getColor(context, R.color.light_gray))
                etNote.setHintTextColor(ContextCompat.getColor(context, R.color.light_gray))
                btnSpeech.background = buttonBackground
                btnRestore.background = buttonBackground
                val micDrawable = ContextCompat.getDrawable(context, R.drawable.ic_mic)
                DrawableCompat.setTint(micDrawable!!, ContextCompat.getColor(context, R.color.light_gray))
                btnSpeech.setImageDrawable(micDrawable)
                templateSpinner.setBackgroundResource(R.drawable.rounded_dialog_background_dark)
                templateSpinner.setPopupBackgroundResource(R.drawable.rounded_dialog_background_dark)
                templateSpinner.adapter = TemplateAdapter(
                    context,
                    templates.map { it.name },
                    ContextCompat.getColor(context, R.color.white)
                )

                // Изменение цвета каемки кнопки
                if (buttonBackground != null) {
                    val strokeColor = ContextCompat.getColor(context, R.color.dark_gray)
                    DrawableCompat.setTint(DrawableCompat.wrap(buttonBackground).mutate(), strokeColor)
                    btnSpeech.background = buttonBackground
                    btnRestore.background = buttonBackground
                }
            }
            else -> {
                when (AppCompatDelegate.getDefaultNightMode()) {
                    AppCompatDelegate.MODE_NIGHT_YES -> {
                        dialogLayout.setBackgroundResource(R.drawable.rounded_dialog_background_dark)
                        etNote.setTextColor(ContextCompat.getColor(context, R.color.light_gray))
                        etNote.setHintTextColor(ContextCompat.getColor(context, R.color.light_gray))
                        btnSpeech.background = buttonBackground
                        btnRestore.background = buttonBackground
                        val micDrawable = ContextCompat.getDrawable(context, R.drawable.ic_mic)
                        DrawableCompat.setTint(micDrawable!!, ContextCompat.getColor(context, R.color.light_gray))
                        btnSpeech.setImageDrawable(micDrawable)
                        templateSpinner.setBackgroundResource(R.drawable.rounded_dialog_background_dark)
                        templateSpinner.setPopupBackgroundResource(R.drawable.rounded_dialog_background_dark)
                        templateSpinner.adapter = TemplateAdapter(
                            context,
                            templates.map { it.name },
                            ContextCompat.getColor(context, R.color.white)
                        )

                        // Изменение цвета каемки кнопки
                        if (buttonBackground != null) {
                            val strokeColor = ContextCompat.getColor(context, R.color.dark_gray)
                            DrawableCompat.setTint(DrawableCompat.wrap(buttonBackground).mutate(), strokeColor)
                            btnSpeech.background = buttonBackground
                            btnRestore.background = buttonBackground
                        }
                    }
                    else -> {
                        dialogLayout.setBackgroundResource(R.drawable.rounded_dialog_background)
                        etNote.setTextColor(ContextCompat.getColor(context, R.color.black))
                        etNote.setHintTextColor(ContextCompat.getColor(context, R.color.black))
                        btnSpeech.background = buttonBackground
                        btnRestore.background = buttonBackground
                        btnSpeech.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_mic))
                        templateSpinner.setBackgroundResource(R.drawable.rounded_dialog_background)
                        templateSpinner.setPopupBackgroundResource(R.drawable.rounded_dialog_background)
                        templateSpinner.adapter = TemplateAdapter(
                            context,
                            templates.map { it.name },
                            ContextCompat.getColor(context, R.color.black)
                        )
                    }
                }
            }
        }
        
        // Set initial selection based on source
        val templateIndex = if (isFromReminder) {
            // Use reminder template if available, otherwise fall back to default
            val reminderTemplateId = settingsViewModel.selectedReminderTemplateId.value
            if (reminderTemplateId != null) {
                templates.indexOfFirst { it.id == reminderTemplateId }
            } else {
                templates.indexOfFirst { it.isDefault }
            }
        } else {
            // Use default template for regular notifications
            templates.indexOfFirst { it.isDefault }
        }
        
        if (templateIndex != -1) {
            templateSpinner.setSelection(templateIndex)
            settingsViewModel.selectTemplate(templates[templateIndex].id)
        }

        templateSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedTemplate = templates[position]
                settingsViewModel.selectTemplate(selectedTemplate.id)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnRestore.visibility = if (settingsViewModel.previousText.value.isNotEmpty()) {
            ImageButton.VISIBLE
        } else {
            ImageButton.GONE
        }

        btnRestore.setOnClickListener {
            etNote.setText(settingsViewModel.previousText.value)
            etNote.setSelection(etNote.text.length)
            settingsViewModel.clearPreviousText()
            btnRestore.visibility = ImageButton.GONE
        }

        etNote.setText("")

        textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s?.toString() ?: ""
                settingsViewModel.updateCurrentText(text)
                if (text.length >= 10 && settingsViewModel.previousText.value.isNotEmpty()) {
                    settingsViewModel.clearPreviousText()
                    btnRestore.visibility = ImageButton.GONE
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        }
        etNote.addTextChangedListener(textWatcher)

        etNote.requestFocus()
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        btnSave.setOnClickListener {
            val note = etNote.text.toString()
            if (note.isNotEmpty()) {
                noteSaver.saveNote(
                    note = note,
                    onSuccess = {
                        Toast.makeText(
                            context,
                            R.string.note_saved,
                            Toast.LENGTH_SHORT
                        ).show()
                        dismiss()
                    },
                    onError = { error ->
                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                dismissWithMessage(context.getString(R.string.note_error))
            }
        }

        // Initialize save button state
        updateSaveButtonState()

        btnCancel.setOnClickListener {
            stopSpeechRecognition()
            dismiss()
        }

        btnSpeech.setOnClickListener {
            if (speechRecognitionManager.isListeningState) {
                stopSpeechRecognition()
            } else {
                (activity as? TransparentActivity)?.startSpeechRecognition()
            }
        }

        window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // Remove TextWatcher to prevent memory leaks
        if (::textWatcher.isInitialized) {
            etNote.removeTextChangedListener(textWatcher)
        }
        // Clear all pending Handler callbacks
        handler.removeCallbacksAndMessages(null)
        // Stop and clean up speech recognition
        if (speechRecognitionManager.isListeningState) {
            stopSpeechRecognition()
        }
        speechRecognitionManager.destroy()
    }

    private fun updateSaveButtonState() {
        val btnSave = findViewById<Button>(R.id.btnSave)
        btnSave.isEnabled = !(isAutoSaveEnabled && (isSpeechRecognitionActive || hasAutoSaved))
        btnSave.text = if (isSpeechRecognitionActive && isAutoSaveEnabled) "Auto-saving..." else "Save"
    }

    private fun updateSpeechButtonIcon() {
        val icon = if (speechRecognitionManager.isListeningState) {
            R.drawable.ic_mic_on
        } else {
            R.drawable.ic_mic
        }
        btnSpeech.setImageDrawable(ContextCompat.getDrawable(context, icon))
    }

    private fun updateNoteText(text: String) {
        etNote.setText(text)
        etNote.setSelection(etNote.text.length)
    }

    fun startSpeechRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Toast.makeText(context, context.getString(R.string.speech_recognition_not_available), Toast.LENGTH_SHORT).show()
            return
        }
        speechRecognitionManager.startListening()
    }

    private fun stopSpeechRecognition() {
        speechRecognitionManager.stopListening()
    }

    private fun dismissWithMessage(message: String) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etNote.windowToken, 0)
        etNote.setText(message)
        handler.postDelayed({
            if (message == context.getString(R.string.note_saved) || message == context.getString(R.string.note_appended)) {
                settingsViewModel.clearCurrentText()
                settingsViewModel.clearPreviousText()
            } else {
                settingsViewModel.updateCurrentText(settingsViewModel.tempText.value)
            }

            settingsViewModel.clearTempText()
            dismiss()
        }, 1000)
    }

    private fun isScreenLocked(): Boolean {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            keyguardManager.isKeyguardLocked
        } else {
            keyguardManager.isKeyguardSecure
        }
    }
}