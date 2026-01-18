package com.example.quickmdcapture

fun insertAfterMarkerOrAppend(
    originalText: String,
    marker: String?,
    newContent: String
): String {
    if (marker.isNullOrBlank()) {
        return originalText.trimEnd() + "\n" + newContent
    }

    val markerIndex = originalText.indexOf(marker)
    if (markerIndex == -1) {
        return originalText.trimEnd() + "\n" + newContent
    }

    val lineEndIndex = originalText.indexOf('\n', markerIndex)
        .let { if (it == -1) originalText.length else it + 1 }

    return buildString {
        append(originalText.substring(0, lineEndIndex))
        append(newContent)
        append('\n')
        append(originalText.substring(lineEndIndex))
    }
}
