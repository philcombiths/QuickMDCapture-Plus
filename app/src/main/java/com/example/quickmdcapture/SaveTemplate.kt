package com.example.quickmdcapture

data class SaveTemplate(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val isDefault: Boolean = false,
    val folderUri: String = "",
    val noteDateTemplate: String = "{{yyyy.MM.dd HH_mm_ss}}",
    val isListItemsEnabled: Boolean = false,
    val listItemIndentLevel: Int = 0,
    val isTimestampEnabled: Boolean = false,
    val timestampTemplate: String = "# {{yyyy.MM.dd HH:mm:ss}}",
    val isDateCreatedEnabled: Boolean = false,
    val propertyName: String = "created",
    val dateCreatedTemplate: String = "{{yyyy.MM.dd}}T{{HH:mm:ssZ}}",
    val isNoteTextInFilenameEnabled: Boolean = false,
    val noteTextInFilenameLength: Int = 30,
    val insertAfterMarker: String = "",
    // Formatting presets and custom text
    val prependPreset: String = "none",    // none | list_dash | list_star | numbered | checklist | custom
    val appendPreset: String = "none",     // none | date | date_time | custom
    val customPrepend: String = "",
    val customAppend: String = ""
) 