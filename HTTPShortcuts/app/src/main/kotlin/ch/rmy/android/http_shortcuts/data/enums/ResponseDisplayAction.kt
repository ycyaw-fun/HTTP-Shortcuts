package ch.rmy.android.http_shortcuts.data.enums

enum class ResponseDisplayAction(val key: String) {
    RERUN("rerun"),
    SHARE("share"),
    COPY("copy"),
    SAVE("save");

    companion object {
        fun parse(key: String) =
            values().firstOrNull { it.key == key }
    }
}
