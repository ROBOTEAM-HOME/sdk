package com.robotemi.sdk.constants

import com.robotemi.sdk.constants.SdkConstants.PAGE_ALL_APPS
import com.robotemi.sdk.constants.SdkConstants.PAGE_CONTACTS
import com.robotemi.sdk.constants.SdkConstants.PAGE_HOME
import com.robotemi.sdk.constants.SdkConstants.PAGE_LOCATIONS
import com.robotemi.sdk.constants.SdkConstants.PAGE_MAP_EDITOR
import com.robotemi.sdk.constants.SdkConstants.PAGE_SETTINGS
import com.robotemi.sdk.constants.SdkConstants.PAGE_TOURS

object SdkConstants {
    // metadata key
    const val METADATA_ACTIONS = "com.robotemi.sdk.metadata.ACTIONS"
    const val METADATA_CONTEXTS = "com.robotemi.sdk.metadata.CONTEXTS"
    const val METADATA_HINTS = "com.robotemi.sdk.metadata.HINTS"
    const val METADATA_VISIBILITY = "com.robotemi.sdk.metadata.VISIBILITY"
    const val METADATA_SKILL = "com.robotemi.sdk.metadata.SKILL"
    const val METADATA_KIOSK = "com.robotemi.sdk.metadata.KIOSK"
    const val METADATA_ASSISTANT = "com.robotemi.sdk.metadata.ASSISTANT"
    const val METADATA_UI_MODE = "com.robotemi.sdk.metadata.UI_MODE"
    const val METADATA_UI_FLAG = "com.robotemi.sdk.metadata.UI_FLAG"
    const val METADATA_OPEN_WITHOUT_INTERNET = "com.robotemi.sdk.metadata.OPEN_WITHOUT_INTERNET"
    const val METADATA_OVERRIDE_NLU = "com.robotemi.sdk.metadata.OVERRIDE_NLU"
    const val METADATA_OVERRIDE_STT = "com.robotemi.sdk.metadata.OVERRIDE_STT"
    const val METADATA_OVERRIDE_TTS = "com.robotemi.sdk.metadata.OVERRIDE_TTS"
    const val METADATA_OVERRIDE_CONVERSATION_LAYER =
        "com.robotemi.sdk.metadata.OVERRIDE_CONVERSATION_LAYER"
    const val METADATA_PERMISSIONS = "com.robotemi.sdk.metadata.PERMISSIONS"

    /**
     * Show everything.
     * `R.integer.metadata_ui_flag_default`
     */
    const val METADATA_UI_FLAG_DEFAULT = 0

    /**
     * Hide top bar always.
     * `R.integer.metadata_ui_flag_hide_top_bar`
     */
    const val METADATA_UI_FLAG_HIDE_TOP_BAR = 1

    /**
     * All UI elements are visible.
     * If this mode is set other Flags are ignored.
     * `R.integer.metadata_ui_mode_default`
     */
    const val METADATA_UI_MODE_DEFAULT = 0

    /**
     * All UI elements are hidden.
     * If this mode is set other Flags are ignored.
     * `R.integer.metadata_ui_mode_fullscreen`
     */
    const val METADATA_UI_MODE_FULLSCREEN = 1

    /**
     * UI elements can be shown/hidden when requested.
     * This mode can be used in conjunction with Flags.
     * `R.integer.metadata_ui_mode_immersive`
     */
    const val METADATA_UI_MODE_IMMERSIVE = 2

    /**
     * All UI elements are visible.
     * If this mode is set other Flags are ignored.
     * `R.integer.metadata_ui_mode_blend`
     */
    const val METADATA_UI_MODE_BLEND = 3

    const val METADATA_UI_MODE_HIDE_ALL_TOP_BAR_ELEMENTS = 4

    const val PLAY = -1
    const val PAUSE = -2
    const val NEXT = -3
    const val BACK = -4
    const val EXTRA_NLP_RESULT = "com.robotemi.sdk.extra.NLP_RESULT"
    const val TEMI_USA = "com.roboteam.teamy.usa"
    const val TEMI_CHINA = "com.roboteam.teamy.china"

    const val DETECTION_DISTANCE_DEFAULT = 0.8F

    const val PROVIDER_AUTHORITY = "com.robotemi.sdk.provider"
    const val PROVIDER_PARAMETER_MEDIA_KEY = "mediaKey"
    const val PROVIDER_PARAMETER_MAP_DATA = "mapData"

    const val LOCATION_HOME_BASE = "home base"

    const val PAGE_SETTINGS = "com.robotemi.page.settings"
    const val PAGE_MAP_EDITOR = "com.robotemi.page.map_editor"
    const val PAGE_CONTACTS = "com.robotemi.page.contacts"
    const val PAGE_LOCATIONS = "com.robotemi.page.locations"
    const val PAGE_ALL_APPS = "com.robotemi.page.all_apps"
    const val PAGE_HOME = "com.robotemi.page.home"
    const val PAGE_TOURS = "com.robotemi.page.tours"

    const val INTENT_ACTION_GREET_MODE_STATE = "greet_mode_state"

    const val NOT_SET = 0
    const val TRUE = 1
    const val FALSE = -1

    const val ACTION_TEMI_BOOT_COMPLETED = "com.robotemi.intent.action.BOOT_COMPLETED"
}

enum class ContentType(val path: String) {
    FACE_RECOGNITION_IMAGE("face_recognition_image"),
    MAP_DATA_IMAGE("map_data_image"),
    MAP_BACKUP("map_backup_file"),
}

enum class Platform(val value: Int) {
    MOBILE(0),
    TEMI_CENTER(1)
}

enum class Page(val value: String) {
    SETTINGS(PAGE_SETTINGS),
    MAP_EDITOR(PAGE_MAP_EDITOR),
    CONTACTS(PAGE_CONTACTS),
    LOCATIONS(PAGE_LOCATIONS),
    ALL_APPS(PAGE_ALL_APPS),
    HOME(PAGE_HOME),
    TOURS(PAGE_TOURS)
}

enum class SoundMode(val value: Int) {
    NORMAL(0),
    VIDEO_CALL(1);

    companion object {

        @JvmField
        val DEFAULT = NORMAL

        @JvmStatic
        fun valueToEnum(value: Int) = if (value == 1) VIDEO_CALL else DEFAULT
    }
}

enum class HardButton(val value: Int) {
    MAIN(1),
    POWER(2),
    VOLUME(3),
    EMERGENCY_STOP(4),
    VOLUME_UP(5),
    VOLUME_DOWN(6)
    ;

    companion object {
        @JvmStatic
        fun valueToEnum(value: Int? = 1) = when (value) {
            1 -> MAIN
            2 -> POWER
            3 -> VOLUME
            4 -> EMERGENCY_STOP
            5 -> VOLUME_UP
            6 -> VOLUME_DOWN
            else -> MAIN
        }
    }

    enum class Mode(val value: Int) {
        ENABLED(0),
        DISABLED(1),
        MAIN_BLOCK_FOLLOW(2);  // Only works for the main button.

        companion object {

            @JvmField
            val DEFAULT = ENABLED

            @JvmStatic
            fun valueToEnum(value: Int? = 0) = when (value) {
                1 -> DISABLED
                2 -> MAIN_BLOCK_FOLLOW
                else -> DEFAULT
            }
        }
    }

    enum class Status(val value: Int) {
        UNKNOWN(0),
        HOLD(1),
        RELEASED(2),
        CLICKED(3),
        ;
        companion object {
            fun valueToEnum(value: Int? = 0) = when (value) {
                0 -> UNKNOWN
                1 -> HOLD
                2 -> RELEASED
                3 -> CLICKED
                else -> UNKNOWN
            }
        }
    }
}

enum class Mode(val value: Int) {
    DEFAULT(0),
    GREET(1),
    PRIVACY(2);

    companion object {
        @JvmStatic
        fun valueToEnum(value: Int? = 0) = when (value) {
            1 -> GREET
            2 -> PRIVACY
            else -> DEFAULT
        }
    }
}

/**
 * Commands for sequence controller [com.robotemi.sdk.Robot.controlSequence]
 */
enum class SequenceCommand {
    STOP,
    PLAY,
    PAUSE,
    STEP_FORWARD,
    STEP_BACKWARD;
}

/**
 * Mode for cliff sensor, for [com.robotemi.sdk.Robot.cliffSensorMode]
 */
enum class CliffSensorMode {
    OFF,
    LOW_SENSITIVITY,
    HIGH_SENSITIVITY
}

/**
 * Sensitivity level.
 */
enum class SensitivityLevel {
    HIGH,
    LOW
}

/**
 * [GIRL] and [BOY] is added in 135 Chinese version.
 * will fallback to [FEMALE] or [MALE] if not supported by 135 launcher when setting TTS voice with them
 * or just fail to set the voice on pre-135 launchers.
 */
enum class Gender {
    FEMALE,
    MALE,
    GIRL,
    BOY,
    UNKNOWN
}

/**
 * Added in 134.
 *
 * When turning Kiosk mode off, can assign a home screen mode to be set. Cannot use [HomeScreenMode.APPLICATION] on such case
 */
enum class HomeScreenMode {
    DEFAULT,
    CLEAR,
    CUSTOM_SCREEN,
    URL,
    APPLICATION,

    ;
    companion object {
        fun getHomeScreenMode(homeScreenMode: String?): HomeScreenMode? {
            return when (homeScreenMode) {
                DEFAULT.name -> DEFAULT
                CUSTOM_SCREEN.name -> CUSTOM_SCREEN
                APPLICATION.name -> APPLICATION
                URL.name -> URL
                CLEAR.name -> CLEAR
                else -> null
            }
        }
    }
}

enum class ReposeStatus(val value: Int) {
    UNKNOWN(0),
    IDLE(1),
    REPOSING(2),
    REPOSE_REQUIRED(3);

    companion object {
        fun valueToEnum(value: Int? = 0) = when (value) {
            1 -> IDLE
            2 -> REPOSING
            3 -> REPOSE_REQUIRED
            else -> UNKNOWN
        }
    }
}