package com.example.quickmdcapture

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import java.text.SimpleDateFormat
import java.util.*

class NoteSaver(
    private val context: Context,
    private val settingsViewModel: SettingsViewModel
) {

    fun saveNote(note: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (note.isBlank()) {
            onError(context.getString(R.string.note_error))
            return
        }

        val folderUri = settingsViewModel.folderUri.value
        if (folderUri == context.getString(R.string.folder_not_selected)) {
            onError(context.getString(R.string.folder_not_selected))
            return
        }

        val folder = DocumentFile.fromTreeUri(context, Uri.parse(folderUri))
        if (folder == null || !folder.exists() || !folder.canWrite()) {
            onError(context.getString(R.string.error_selecting_folder))
            return
        }

        // Формируем имя файла
        var filename = getFileNameWithDate(settingsViewModel.noteDateTemplate.value)

        // Добавляем начало текста заметки в имя файла, если включено
        if (settingsViewModel.isNoteTextInFilenameEnabled.value) {
            val noteText = note.take(settingsViewModel.noteTextInFilenameLength.value)
                .replace(Regex("[<>:\"/\\|?*]"), "_") // Заменяем недопустимые символы
                .trim()
            if (noteText.isNotEmpty()) {
                filename = "$filename - $noteText"
            }
        }

        filename = "$filename.md"

        // Проверяем существование файла
        var file = folder.findFile(filename)
        val isNewFile = file == null

        if (isNewFile) {
            file = folder.createFile("text/markdown", filename)
        }

        if (file != null) {
            try {
                val content = StringBuilder()

                // Форматируем текст заметки с учетом пресетов
                val indent = "\t".repeat(settingsViewModel.listItemIndentLevel.value)
                val prependPreset = settingsViewModel.prependPreset.value
                val customPrepend = settingsViewModel.customPrepend.value
                val fallbackListItems = settingsViewModel.isListItemsEnabled.value && prependPreset == "none" && customPrepend.isBlank()

                val formattedNote = when (prependPreset) {
                    "list_dash" -> note.lines().joinToString("\n") { "$indent- $it" }
                    "list_star" -> note.lines().joinToString("\n") { "$indent* $it" }
                    "numbered" -> note.lines().mapIndexed { idx, line -> "$indent${idx + 1}. $line" }.joinToString("\n")
                    "checklist" -> note.lines().joinToString("\n") { "$indent- [ ] $it" }
                    "custom" -> note.lines().joinToString("\n") { indent + customPrepend + it }
                    else -> if (fallbackListItems) {
                        note.lines().joinToString("\n") { "$indent- $it" }
                    } else {
                        note
                    }
                }

                // Добавляем временную метку перед текстом, если включено
                if (settingsViewModel.isTimestampEnabled.value) {
                    val timestamp = getFormattedTimestamp(settingsViewModel.timestampTemplate.value)
                    content.append(timestamp).append("\n")
                }

                // Добавляем отформатированный текст и суффикс, если задан
                content.append(formattedNote)

                val appendPreset = settingsViewModel.appendPreset.value
                val customAppend = settingsViewModel.customAppend.value
                val appendResolved = when (appendPreset) {
                    "date" -> {
                        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        " $dateStr"
                    }
                    "date_time" -> {
                        val dtStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                        " $dtStr"
                    }
                    "custom" -> customAppend
                    else -> ""
                }
                if (appendResolved.isNotEmpty()) {
                    content.append(appendResolved)
                }

                // Открываем файл для записи
                context.contentResolver.openOutputStream(file.uri, if (isNewFile) "w" else "wa")?.use { outputStream ->
                    if (isNewFile) {
                        // Для нового файла добавляем YAML заголовок, если включено
                        if (settingsViewModel.isDateCreatedEnabled.value) {
                            val fullTimeStamp = getFormattedTimestamp(settingsViewModel.dateCreatedTemplate.value)
                            val yamlHeader = "---\n${settingsViewModel.propertyName.value}: $fullTimeStamp\n---\n"
                            outputStream.write(yamlHeader.toByteArray())
                        }
                        outputStream.write(content.toString().toByteArray())
                    } else {
                        // Existing file: read → insert after marker → rewrite

                        val existingText = context.contentResolver
                            .openInputStream(file.uri)
                            ?.bufferedReader()
                            ?.readText()
                            ?: ""

                        val updatedText = insertAfterMarkerOrAppend(
                            originalText = existingText,
                            marker = settingsViewModel.insertAfterMarker.value,
                            newContent = content.toString()
                        )

                        // Rewrite entire file
                        context.contentResolver.openOutputStream(file.uri, "w")?.use { rewriteStream ->
                            rewriteStream.write(updatedText.toByteArray())
                        }
                    }
                }

                settingsViewModel.clearCurrentText()
                settingsViewModel.clearPreviousText()
                onSuccess()
            } catch (e: Exception) {
                onError(context.getString(R.string.note_error))
            }
        } else {
            onError(context.getString(R.string.note_error))
        }
    }

    private fun getFileNameWithDate(template: String): String {
        var result = template

        var startIndex = result.indexOf("{{")
        while (startIndex != -1) {
            val endIndex = result.indexOf("}}", startIndex + 2)
            if (endIndex != -1) {
                val datePart = result.substring(startIndex + 2, endIndex)
                val formattedDate = SimpleDateFormat(datePart, Locale.getDefault()).format(Date())
                result = result.replaceRange(startIndex, endIndex + 2, formattedDate)
                startIndex = result.indexOf("{{", endIndex)
            } else {
                startIndex = -1
            }
        }

        return result.replace(":", "_")
    }

    private fun getFormattedTimestamp(template: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < template.length) {
            if (template[i] == '{' && i < template.length - 1 && template[i + 1] == '{') {
                i += 2
                val endIndex = template.indexOf("}}", i)
                if (endIndex != -1) {
                    val datePart = template.substring(i, endIndex)
                    val formattedDate = SimpleDateFormat(datePart, Locale.getDefault()).format(Date())
                    sb.append(formattedDate)
                    i = endIndex + 2
                } else {
                    sb.append(template[i])
                    i++
                }
            } else {
                sb.append(template[i])
                i++
            }
        }
        return sb.toString()
    }

    private fun insertAfterMarkerOrAppend(originalText: String, marker: String, newContent: String): String {
        val markerIndex = originalText.indexOf(marker)
        return if (markerIndex != -1) {
            val insertPosition = markerIndex + marker.length
            originalText.substring(0, insertPosition) + "\n\n" + newContent + "\n" + originalText.substring(insertPosition)
        } else {
            originalText + "\n\n" + newContent
        }
    }
}