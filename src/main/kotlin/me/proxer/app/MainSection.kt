package me.proxer.app

/**
 * The sections of the app, which can be opened through [MainActivity.getSectionIntent], e.g. from notifications, app
 * shortcuts and widgets. The ids are persisted as the start page preference and must not change.
 */
enum class MainSection(val id: Long) {
    NEWS(0L),
    CHAT(1L),
    MESSENGER(1L),
    BOOKMARKS(2L),
    ANIME(3L),
    SCHEDULE(4L),
    MANGA(5L),
    INFO(10L),
    SETTINGS(11L);

    companion object {
        fun fromIdOrNull(id: Long?) = values().firstOrNull { it.id == id }
        fun fromIdOrDefault(id: Long?) = fromIdOrNull(id) ?: NEWS
    }
}
