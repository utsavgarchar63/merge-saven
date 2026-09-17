package com.mergeseven.game.data.preferences

/**
 * AF11-01 colourblind palette modes stored in DataStore.
 */
enum class ColourblindMode {
    OFF,
    DEUTERANOPIA,
    PROTANOPIA,
    TRITANOPIA;

    companion object {
        fun fromStorage(raw: String?): ColourblindMode =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: OFF
    }
}
