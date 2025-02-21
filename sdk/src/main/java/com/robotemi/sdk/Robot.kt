package com.robotemi.sdk

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.RemoteException
import android.util.Base64
import android.util.Log
import androidx.annotation.*
import androidx.annotation.IntRange
import androidx.annotation.RestrictTo.Scope.LIBRARY
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.robotemi.sdk.activitystream.ActivityStreamObject
import com.robotemi.sdk.activitystream.ActivityStreamPublishMessage
import com.robotemi.sdk.activitystream.ActivityStreamUtils
import com.robotemi.sdk.constants.*
import com.robotemi.sdk.constants.SdkConstants.FALSE
import com.robotemi.sdk.constants.SdkConstants.NOT_SET
import com.robotemi.sdk.constants.SdkConstants.TRUE
import com.robotemi.sdk.exception.OnSdkExceptionListener
import com.robotemi.sdk.exception.SdkException
import com.robotemi.sdk.face.ContactModel
import com.robotemi.sdk.face.OnContinuousFaceRecognizedListener
import com.robotemi.sdk.face.OnFaceRecognizedListener
import com.robotemi.sdk.face.compatible
import com.robotemi.sdk.listeners.*
import com.robotemi.sdk.map.*
import com.robotemi.sdk.map.Layer.CREATOR.roundByCategory
import com.robotemi.sdk.mediabar.AidlMediaBarController
import com.robotemi.sdk.mediabar.MediaBarData
import com.robotemi.sdk.model.CallEventModel
import com.robotemi.sdk.model.DetectionData
import com.robotemi.sdk.model.MemberStatusModel
import com.robotemi.sdk.model.RecentCallModel
import com.robotemi.sdk.navigation.listener.OnCurrentPositionChangedListener
import com.robotemi.sdk.navigation.listener.OnDistanceToDestinationChangedListener
import com.robotemi.sdk.navigation.listener.OnDistanceToLocationChangedListener
import com.robotemi.sdk.navigation.listener.OnReposeStatusChangedListener
import com.robotemi.sdk.navigation.model.Position
import com.robotemi.sdk.navigation.model.SafetyLevel
import com.robotemi.sdk.navigation.model.SpeedLevel
import com.robotemi.sdk.notification.AlertNotification
import com.robotemi.sdk.notification.NormalNotification
import com.robotemi.sdk.notification.NotificationCallback
import com.robotemi.sdk.permission.OnRequestPermissionResultListener
import com.robotemi.sdk.permission.Permission
import com.robotemi.sdk.sequence.OnSequencePlayStatusChangedListener
import com.robotemi.sdk.sequence.SequenceModel
import com.robotemi.sdk.sequence.compatible
import com.robotemi.sdk.telepresence.CallState
import com.robotemi.sdk.telepresence.LinkBasedMeeting
import com.robotemi.sdk.telepresence.Participant
import com.robotemi.sdk.tourguide.TourModel
import com.robotemi.sdk.tourguide.compatible
import com.robotemi.sdk.voice.ITtsService
import com.robotemi.sdk.voice.WakeupOrigin
import com.robotemi.sdk.voice.WakeupRequest
import com.robotemi.sdk.voice.model.TtsVoice
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.reflect.Type
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CopyOnWriteArraySet
import java.util.zip.GZIPInputStream
import kotlin.concurrent.thread

@SuppressWarnings("unused")
class Robot private constructor(private val context: Context) {

    private val applicationInfo: ApplicationInfo

    private val uiHandler = Handler(Looper.getMainLooper())

    private var mediaBar = AidlMediaBarController(null)

    private var mediaButtonListener: MediaButtonListener? = null

    private var sdkService: ISdkService? = null

    private val listenersMap = HashMap<String, NotificationListener>()

    private val conversationViewAttachesListeners =
        CopyOnWriteArraySet<ConversationViewAttachesListener>()

    private val ttsListeners = CopyOnWriteArraySet<TtsListener>()

    private val asrListeners = CopyOnWriteArraySet<AsrListener>()

    private val nlpListeners = CopyOnWriteArraySet<NlpListener>()

    private val wakeUpWordListeners = CopyOnWriteArraySet<WakeupWordListener>()

    private val onRobotReadyListeners = CopyOnWriteArraySet<OnRobotReadyListener>()

    private val onBeWithMeStatusChangeListeners =
        CopyOnWriteArraySet<OnBeWithMeStatusChangedListener>()

    private val onGoToLocationStatusChangeListeners =
        CopyOnWriteArraySet<OnGoToLocationStatusChangedListener>()

    private val onTelepresenceStatusChangedListeners =
        CopyOnWriteArraySet<OnTelepresenceStatusChangedListener>()

    private val onTelepresenceEventChangedListener =
        CopyOnWriteArraySet<OnTelepresenceEventChangedListener>()

    private val onLocationsUpdatedListeners = CopyOnWriteArraySet<OnLocationsUpdatedListener>()

    private val onUsersUpdatedListeners = CopyOnWriteArraySet<OnUsersUpdatedListener>()

    private val onBatteryStatusChangedListeners =
        CopyOnWriteArraySet<OnBatteryStatusChangedListener>()

    private val onPrivacyModeStateChangedListeners =
        CopyOnWriteArraySet<OnPrivacyModeChangedListener>()

    private val onConstraintBeWithStatusChangedListeners =
        CopyOnWriteArraySet<OnConstraintBeWithStatusChangedListener>()

    private val onUserInteractionChangedListeners =
        CopyOnWriteArraySet<OnUserInteractionChangedListener>()

    private val onDetectionStateChangedListeners =
        CopyOnWriteArraySet<OnDetectionStateChangedListener>()

    private val onRequestPermissionResultListeners =
        CopyOnWriteArraySet<OnRequestPermissionResultListener>()

    private val onDistanceToLocationChangedListeners =
        CopyOnWriteArraySet<OnDistanceToLocationChangedListener>()

    private val onCurrentPositionChangedListeners =
        CopyOnWriteArraySet<OnCurrentPositionChangedListener>()

    private val onSequencePlayStatusChangedListeners =
        CopyOnWriteArraySet<OnSequencePlayStatusChangedListener>()

    private val onRobotLiftedListeners = CopyOnWriteArraySet<OnRobotLiftedListener>()

    private val onDetectionDataChangedListeners =
        CopyOnWriteArraySet<OnDetectionDataChangedListener>()

    private val onFaceRecognizedListeners = CopyOnWriteArraySet<OnFaceRecognizedListener>()

    private val onContinuousFaceRecognizedListeners =
        CopyOnWriteArraySet<OnContinuousFaceRecognizedListener>()

    private val onSdkExceptionListeners = CopyOnWriteArraySet<OnSdkExceptionListener>()

    private val onConversationStatusChangedListeners =
        CopyOnWriteArraySet<OnConversationStatusChangedListener>()

    private val onTtsVisualizerWaveFormDataChangedListeners =
        CopyOnWriteArraySet<OnTtsVisualizerWaveFormDataChangedListener>()

    private val onTtsVisualizerFftDataChangedListeners =
        CopyOnWriteArraySet<OnTtsVisualizerFftDataChangedListener>()

    private val onReposeStatusChangedListeners =
        CopyOnWriteArraySet<OnReposeStatusChangedListener>()

    private val onLoadMapStatusChangedListeners =
        CopyOnWriteArraySet<OnLoadMapStatusChangedListener>()

    private val onMapStatusChangedListeners =
        CopyOnWriteArraySet<OnMapStatusChangedListener>()

    private val onMapElementsChangedListeners =
        CopyOnWriteArraySet<OnMapElementsChangedListener>()

    private val onMapNameChangedListeners =
        CopyOnWriteArraySet<OnMapNameChangedListener>()

    private val onDisabledFeatureListUpdatedListeners =
        CopyOnWriteArraySet<OnDisabledFeatureListUpdatedListener>()

    private val onMovementVelocityChangedListeners =
        CopyOnWriteArraySet<OnMovementVelocityChangedListener>()

    private val onMovementStatusChangedListeners =
        CopyOnWriteArraySet<OnMovementStatusChangedListener>()

    private val onGreetModeStateChangedListeners =
        CopyOnWriteArraySet<OnGreetModeStateChangedListener>()

    private val onLoadFloorStatusChangedListeners =
        CopyOnWriteArraySet<OnLoadFloorStatusChangedListener>()

    private val onDistanceToDestinationChangedListeners =
        CopyOnWriteArraySet<OnDistanceToDestinationChangedListener>()

    private val onSerialRawDataListeners =
        CopyOnWriteArraySet<OnSerialRawDataListener>()

    private val onRobotDragStateChangedListeners =
        CopyOnWriteArraySet<OnRobotDragStateChangedListener>()

    private val onButtonStatusChangedListeners =
        CopyOnWriteArraySet<OnButtonStatusChangedListener>()

    private val onButtonModeChangedListeners =
        CopyOnWriteArraySet<OnButtonModeChangedListener>()

    private val onGoToNavPathChangedListeners =
        CopyOnWriteArraySet<OnGoToNavPathChangedListener>()

    private var ttsService: ITtsService? = null

    private var activityStreamPublishListener: ActivityStreamPublishListener? = null

    private val gson: Gson =
        GsonBuilder().registerTypeAdapter(Date::class.java, GsonUTCDateAdapter()).create()

    init {
        val appContext = context.applicationContext
        val packageName = appContext.packageName
        val packageManager = appContext.packageManager
        try {
            this.applicationInfo =
                packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        } catch (e: PackageManager.NameNotFoundException) {
            throw RuntimeException(e)
        }
    }

    private val sdkServiceCallback = object : ISdkServiceCallback.Stub() {

        /*****************************************/
        /*                 Voice                 */
        /*****************************************/

        override fun onTtsStatusChanged(ttsRequest: TtsRequest): Boolean {
            if (ttsListeners.isEmpty()) return false
            uiHandler.post {
                for (ttsListener in ttsListeners) {
                    ttsListener.onTtsStatusChanged(ttsRequest)
                }
            }
            return true
        }

        override fun onWakeupWord(wakeupWord: String, direction: Int, origin: String?): Boolean {
            if (wakeUpWordListeners.isEmpty()) return false

            val wakeupOrigin = WakeupOrigin.parse(origin)

            uiHandler.post {
                for (wakeupWordListener in wakeUpWordListeners) {
                    wakeupWordListener.onWakeupWord(wakeupWord, direction, wakeupOrigin)
                }
            }
            return true
        }

        override fun onNlpCompleted(nlpResult: NlpResult): Boolean {
            if (nlpListeners.isEmpty()) return false
            uiHandler.post {
                for (nlpListener in nlpListeners) {
                    nlpListener.onNlpCompleted(nlpResult)
                }
            }
            return true
        }

        override fun onAsrResult(asrText: String, language: Int): Boolean {
            if (asrListeners.isEmpty()) return false
            uiHandler.post {
                for (asrListener in asrListeners) {
                    asrListener.onAsrResult(asrText, SttLanguage.valueToEnum(language))
                }
            }
            return true
        }

        override fun onConversationViewAttaches(isAttached: Boolean): Boolean {
            if (conversationViewAttachesListeners.isEmpty()) return false
            uiHandler.post {
                for (conversationViewAttachesListener in conversationViewAttachesListeners) {
                    conversationViewAttachesListener.onConversationAttaches(isAttached)
                }
            }
            return true
        }

        override fun hasActiveNlpListeners(): Boolean {
            return nlpListeners.isNotEmpty()
        }

        override fun onConversationStatusChanged(status: Int, text: String): Boolean {
            if (onConversationStatusChangedListeners.isEmpty()) return false
            uiHandler.post {
                for (onConversationStatusChangedListener in onConversationStatusChangedListeners) {
                    onConversationStatusChangedListener.onConversationStatusChanged(status, text)
                }
            }
            return true
        }

        override fun onTtsVisualizerWaveFormDataChanged(wave: ByteArray): Boolean {
            if (onTtsVisualizerWaveFormDataChangedListeners.isEmpty()) return false
            uiHandler.post {
                for (onTtsVisualizerWaveFormDataChangedListener in onTtsVisualizerWaveFormDataChangedListeners) {
                    onTtsVisualizerWaveFormDataChangedListener.onTtsVisualizerWaveFormDataChanged(
                        wave
                    )
                }
            }
            return true
        }

        override fun onTtsVisualizerFftDataChanged(fft: ByteArray): Boolean {
            if (onTtsVisualizerFftDataChangedListeners.isEmpty()) return false
            uiHandler.post {
                for (onTtsVisualizerFftDataChangedListener in onTtsVisualizerFftDataChangedListeners) {
                    onTtsVisualizerFftDataChangedListener.onTtsVisualizerFftDataChanged(fft)
                }
            }
            return false
        }

        override fun onTtsSpeak(ttsRequest: TtsRequest) {
            ttsService?.speak(ttsRequest)
        }

        override fun onTtsCancel() {
            ttsService?.cancel()
        }

        override fun onTtsPause() {
            ttsService?.pause()
        }

        override fun onTtsResume() {
            ttsService?.resume()
        }

        /*****************************************/
        /*               Navigation              */
        /*****************************************/

        override fun onGoToLocationStatusChanged(
            location: String,
            status: String,
            descriptionId: Int,
            description: String
        ): Boolean {
            if (onGoToLocationStatusChangeListeners.isEmpty()) return false
            uiHandler.post {
                for (listener in onGoToLocationStatusChangeListeners) {
                    listener.onGoToLocationStatusChanged(
                        location,
                        status,
                        descriptionId,
                        description
                    )
                }
            }
            return true
        }

        override fun onLocationsUpdated(locations: List<String>): Boolean {
            if (onLocationsUpdatedListeners.isEmpty()) return false
            uiHandler.post {
                for (listener in onLocationsUpdatedListeners) {
                    listener.onLocationsUpdated(locations)
                }
            }
            return true
        }

        override fun onDistanceToLocationChanged(distances: MutableMap<Any?, Any?>): Boolean {
            if (onDistanceToLocationChangedListeners.isEmpty()) return false
            @Suppress("UNCHECKED_CAST")
            val distancesMap: Map<String, Float> = distances as Map<String, Float>
            uiHandler.post {
                for (listener in onDistanceToLocationChangedListeners) {
                    listener.onDistanceToLocationChanged(distancesMap)
                }
            }
            return true
        }

        override fun onCurrentPositionChanged(position: Position): Boolean {
            if (onCurrentPositionChangedListeners.isEmpty()) return false
            uiHandler.post {
                for (listener in onCurrentPositionChangedListeners) {
                    listener.onCurrentPositionChanged(position)
                }
            }
            return true
        }

        override fun onReposeStatusChanged(status: Int, description: String): Boolean {
            if (onReposeStatusChangedListeners.isEmpty()) return false
            uiHandler.post {
                for (listener in onReposeStatusChangedListeners) {
                    listener.onReposeStatusChanged(status, description)
                }
            }
            return true
        }

        override fun onDistanceToDestinationChanged(location: String, distance: Float): Boolean {
            if (onDistanceToDestinationChangedListeners.isEmpty()) return false
            uiHandler.post {
                for (listener in onDistanceToDestinationChangedListeners) {
                    listener.onDistanceToDestinationChanged(location, distance)
                }
            }
            return true
        }

        override fun onGoToNavPathChanged(path: String): Boolean {
            if (onGoToNavPathChangedListeners.isEmpty()) return false
            val pathDecoded = decodeBase64UngzipJson<Layer>(path)?.layerPoses ?: listOf()
            uiHandler.post {
                for (listener in onGoToNavPathChangedListeners) {
                    listener.onGoToNavPathChanged(pathDecoded)
                }
            }
            return true
        }

        /*****************************************/
        /*            Movement & Follow          */
        /*****************************************/

        override fun onBeWithMeStatusChanged(status: String): Boolean {
            if (onBeWithMeStatusChangeListeners.isEmpty()) return false
            uiHandler.post {
                for (listener in onBeWithMeStatusChangeListeners) {
                    listener.onBeWithMeStatusChanged(status)
                }
            }
            return true
        }

        override fun onConstraintBeWithStatusChanged(isContraint: Boolean): Boolean {
            if (onConstraintBeWithStatusChangedListeners.isEmpty()) return false
            uiHandler.post {
                for (listener in onConstraintBeWithStatusChangedListeners) {
                    listener.onConstraintBeWithStatusChanged(isContraint)
                }
            }
            return true
        }

        override fun onRobotLifted(isLifted: Boolean, reason: String): Boolean {
            if (onRobotLiftedListeners.isEmpty()) return false
            uiHandler.post {
                for (listener in onRobotLiftedListeners) {
                    listener.onRobotLifted(isLifted, reason)
                }
            }
            return true
        }

        override fun onMovementVelocityChanged(linear: Float): Boolean {
            if (onMovementVelocityChangedListeners.isEmpty()) return false
            uiHandler.post {
                for (listener in onMovementVelocityChangedListeners) {
                    listener.onMovementVelocityChanged(linear)
                }
            }
            return true
        }

        override fun onMovementStatusChanged(type: String, status: String): Boolean {
            if (onMovementStatusChangedListeners.isEmpty()) return false
            uiHandler.post {
                for (listener in onMovementStatusChangedListeners) {
                    listener.onMovementStatusChanged(type, status)
                }
            }
            return true
        }

        /*****************************************/
        /*           Users & Telepresence        */
        /*****************************************/

        override fun onTelepresenceStatusChanged(callState: CallState): Boolean {
            if (onTelepresenceStatusChangedListeners.isEmpty()) return false
            uiHandler.post {
                for (listener in onTelepresenceStatusChangedListeners) {
                    listener?.onTelepresenceStatusChanged(callState)
                }
            }
            return true
        }

        override fun onTelepresenceEventChanged(callEventModel: CallEventModel): Boolean {
            if (onTelepresenceEventChangedListener.isEmpty()) return false
            uiHandler.post {
                for (listener in onTelepresenceEventChangedListener) {
                    listener.onTelepresenceEventChanged(callEventModel)
                }
            }
            return true
        }

        override fun onUserUpdated(user: UserInfo): Boolean {
            if (onUsersUpdatedListeners.isEmpty()) return false
            uiHandler.post {
                for (listener in onUsersUpdatedListeners) {
                    val isValidListener = listener != null && (listener.userIds == null
                            || listener.userIds!!.isEmpty()
                            || listener.userIds!!.contains(user.userId))
                    if (isValidListener) {
                        listener!!.onUserUpdated(user)
                    }
                }
            }
            return true
        }

        /*****************************************/
        /*                 System                */
        /*****************************************/

        override fun onNotificationBtnClicked(notificationCallback: NotificationCallback) {
            uiHandler.post {
                val notificationListener = listenersMap[notificationCallback.notificationId]
                if (notificationListener != null) {
                    notificationListener.onNotificationBtnClicked(notificationCallback.event)
                    listenersMap.remove(notificationCallback.notificationId)
                }
            }
        }

        override fun onPrivacyModeStateChanged(state: Boolean): Boolean {
            if (onPrivacyModeStateChangedListeners.isEmpty()) return false
            uiHandler.post {
                for (listener in onPrivacyModeStateChangedListeners) {
                    listener.onPrivacyModeChanged(state)
                }
            }
            return true
        }

        override fun onBatteryStatusChanged(batteryData: BatteryData): Boolean {
            if (onBatteryStatusChangedListeners.isEmpty()) return false
            uiHandler.post {
                for (listener in onBatteryStatusChangedListeners) {
                    listener.onBatteryStatusChanged(batteryData)
                }
            }
            return true
        }

        override fun onDisabledFeatureListUpdated(disabledFeatureList: MutableList<String>): Boolean {
            if (onDisabledFeatureListUpdatedListeners.isEmpty()) return false
            uiHandler.post {
                for (listener in onDisabledFeatureListUpdatedListeners) {
                    listener.onDisabledFeatureListUpdated(disabledFeatureList)
                }
            }
            return true
        }

        override fun onGreetModeStateChanged(state: Int): Boolean {
            if (onGreetModeStateChangedListeners.isEmpty()) return false
            uiHandler.post {
                onGreetModeStateChangedListeners.forEach {
                    it.onGreetModeStateChanged(state)
                }
            }
            return true
        }

        override fun onButtonStatusChanged(buttonType: Int, buttonStatus: Int): Boolean {
            if (onButtonStatusChangedListeners.isEmpty()) return false
            uiHandler.post {
                onButtonStatusChangedListeners.forEach {
                    it.onButtonStatusChanged(HardButton.valueToEnum(buttonType), HardButton.Status.valueToEnum(buttonStatus))
                }
            }
            return true
        }

        override fun onButtonModeChanged(buttonType: Int, buttonMode: Int): Boolean {
            if (onButtonModeChangedListeners.isEmpty()) return false
            uiHandler.post {
                onButtonModeChangedListeners.forEach {
                    it.onButtonModeChanged(HardButton.valueToEnum(buttonType), HardButton.Mode.valueToEnum(buttonMode))
                }
            }
            return true
        }

        /*****************************************/
        /*               Permission              */
        /*****************************************/

        override fun onRequestPermissionResult(
            permission: String,
            grantResult: Int,
            requestCode: Int
        ): Boolean {
            if (onRequestPermissionResultListeners.isEmpty()) return false
            uiHandler.post {
                for (listener in onRequestPermissionResultListeners) {
                    listener.onRequestPermissionResult(
                        Permission.valueToEnum(permission),
                        grantResult,
                        requestCode
                    )
                }
            }
            return true
        }

        /*****************************************/
        /*            Activity Stream            */
        /*****************************************/

        override fun onActivityStreamPublish(message: ActivityStreamPublishMessage) {
            if (activityStreamPublishListener != null) {
                activityStreamPublishListener!!.onPublish(message)
            }
        }

        /*****************************************/
        /*                 Media                 */
        /*****************************************/

        override fun onPlayButtonClicked(play: Boolean) {
            uiHandler.post {
                if (mediaButtonListener != null) {
                    mediaButtonListener!!.onPlayButtonClicked(play)
                }
            }
        }

        override fun onNextButtonClicked() {
            uiHandler.post {
                if (mediaButtonListener != null) {
                    mediaButtonListener!!.onNextButtonClicked()
                }
            }
        }

        override fun onBackButtonClicked() {
            uiHandler.post {
                if (mediaButtonListener != null) {
                    mediaButtonListener!!.onBackButtonClicked()
                }
            }
        }

        override fun onTrackBarChanged(position: Int) {
            uiHandler.post {
                if (mediaButtonListener != null) {
                    mediaButtonListener!!.onTrackBarChanged(position)
                }
            }
        }

        /*****************************************/
        /*             Detection Mode            */
        /*****************************************/

        override fun onUserInteractionStatusChanged(isInteracting: Boolean): Boolean {
            if (onUserInteractionChangedListeners.isEmpty()) return false
            uiHandler.post {
                for (listener in onUserInteractionChangedListeners) {
                    listener.onUserInteraction(isInteracting)
                }
            }
            return true
        }

        override fun onDetectionStateChanged(state: Int): Boolean {
            if (onDetectionStateChangedListeners.isEmpty()) return false
            uiHandler.post {
                for (listener in onDetectionStateChangedListeners) {
                    listener.onDetectionStateChanged(state)
                }
            }
            return true
        }

        override fun onDetectionDataChanged(detectionData: DetectionData): Boolean {
            if (onDetectionDataChangedListeners.isEmpty()) return false
            uiHandler.post {
                for (listener in onDetectionDataChangedListeners) {
                    listener.onDetectionDataChanged(detectionData)
                }
            }
            return true
        }

        /*****************************************/
        /*                Sequence               */
        /*****************************************/

        override fun onSequencePlayStatusChanged(status: Int): Boolean {
            if (onSequencePlayStatusChangedListeners.isEmpty()) return false
            uiHandler.post {
                for (listener in onSequencePlayStatusChangedListeners) {
                    listener.onSequencePlayStatusChanged(status)
                }
            }
            return true
        }

        /*****************************************/
        /*            Face Recognition           */
        /*****************************************/

        override fun onFaceRecognized(contactModelList: MutableList<ContactModel>): Boolean {
            if (onFaceRecognizedListeners.isEmpty()) return false
            uiHandler.post {
                for (listener in onFaceRecognizedListeners) {
                    listener.onFaceRecognized(contactModelList.map { it.compatible() })
                }
            }
            return true
        }

        override fun onContinuousFaceRecognized(contactModelList: MutableList<ContactModel>): Boolean {
            if (onContinuousFaceRecognizedListeners.isEmpty()) return false
            uiHandler.post {
                for (listener in onContinuousFaceRecognizedListeners) {
                    listener.onContinuousFaceRecognized(contactModelList.map { it.compatible() })
                }
            }
            return true
        }

        /*****************************************/
        /*                 Common                */
        /*****************************************/

        override fun onSdkError(sdkException: SdkException): Boolean {
            if (onSdkExceptionListeners.isEmpty()) return false
            uiHandler.post {
                for (listener in onSdkExceptionListeners) {
                    listener.onSdkError(sdkException)
                }
            }
            return true
        }

        /*****************************************/
        /*                  Map                  */
        /*****************************************/

        override fun onLoadMapStatusChanged(status: Int, requestId: String?): Boolean {
            if (onLoadMapStatusChangedListeners.isEmpty()) return false
            uiHandler.post {
                for (listener in onLoadMapStatusChangedListeners) {
                    listener.onLoadMapStatusChanged(status, requestId ?: "")
                }
            }
            return true
        }

        override fun onLoadFloorStatusChanged(status: Int): Boolean {
            if (onLoadFloorStatusChangedListeners.isEmpty()) return false
            uiHandler.post {
                onLoadFloorStatusChangedListeners.forEach {
                    it.onLoadFloorStatusChanged(status)
                }
            }
            return true
        }

        override fun onMapStatusChanged(isLost: Boolean, isLocked: Boolean): Boolean {
            if (onMapStatusChangedListeners.isEmpty()) return false
            uiHandler.post {
                onMapStatusChangedListeners.forEach {
                    it.onMapStatusChanged(isLost, isLocked)
                }
            }
            return true
        }

        override fun onMapElementsChanged(): Boolean {
            if (onMapElementsChangedListeners.isEmpty()) return false
            uiHandler.post {
                onMapElementsChangedListeners.forEach {
                    it.onMapElementsChanged()
                }
            }
            return true
        }

        override fun onMapNameChanged(mapName: String?): Boolean {
            if (onMapNameChangedListeners.isEmpty()) return false
            uiHandler.post {
                onMapNameChangedListeners.forEach {
                    it.onMapNameChanged(mapName ?: "")
                }
            }
            return true
        }

        /*****************************************/
        /*                Serial                 */

        /**
         * Receive raw serial data from hardware components on temi GO.
         *
         */
        override fun onSerialRawData(data: ByteArray) {
            if (onSerialRawDataListeners.isEmpty()) return
            uiHandler.post {
                onSerialRawDataListeners.forEach {
                    it.onSerialRawData(data)
                }
            }
        }

        override fun onDragStateChanged(isDragged: Boolean) {
            if (onRobotDragStateChangedListeners.isEmpty()) return
            uiHandler.post {
                onRobotDragStateChangedListeners.forEach {
                    it.onRobotDragStateChanged(isDragged)
                }
            }
        }

    }

    /*****************************************/
    /*                  Init                 */
    /*****************************************/

    @get:CheckResult
    val isReady
        get() = sdkService != null

    @UiThread
    fun onStart(activityInfo: ActivityInfo) {
        try {
            sdkService?.onStart(activityInfo)
        } catch (e: RemoteException) {
            Log.e(TAG, "onStart(ActivityInfo) - Binder invocation exception.")
        }
    }

    @RestrictTo(LIBRARY)
    @UiThread
    internal fun setSdkService(sdkService: ISdkService?) {
        this.sdkService = sdkService
        mediaBar = AidlMediaBarController(sdkService)
        registerCallback()
        onRobotReadyListeners.forEach { it.onRobotReady(sdkService != null) }
    }

    @UiThread
    private fun registerCallback() {
        try {
            sdkService?.register(applicationInfo, sdkServiceCallback)
        } catch (e: RemoteException) {
            Log.e(TAG, "Remote invocation error")
        }
    }

    @UiThread
    fun addOnRobotReadyListener(onRobotReadyListener: OnRobotReadyListener) {
        onRobotReadyListeners.add(onRobotReadyListener)
        onRobotReadyListener.onRobotReady(isReady)
    }

    @UiThread
    fun removeOnRobotReadyListener(onRobotReadyListener: OnRobotReadyListener) {
        onRobotReadyListeners.remove(onRobotReadyListener)
    }

    /*****************************************/
    /*                 Voice                 */
    /*****************************************/

    /**
     * To ask temi to speak something.
     *
     * @param ttsRequest Which contains all the TTS information temi needs to in order to speak.
     */
    fun speak(ttsRequest: TtsRequest) {
        try {
            sdkService?.speak(ttsRequest.apply { packageName = applicationInfo.packageName })
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to invoke remote call speak()")
        }
    }

    private val isOverridingTts = applicationInfo.metaData != null
            && applicationInfo.metaData.getBoolean(SdkConstants.METADATA_OVERRIDE_TTS, false)

    fun setTtsService(ttsService: ITtsService?) {
        if (!isOverridingTts) {
            sdkServiceCallback.onSdkError(SdkException.illegalArgument("'com.robotemi.sdk.metadata.OVERRIDE_TTS' should be declared in AndroidManifest.xml"))
            return
        }
        this.ttsService = ttsService
    }

    fun publishTtsStatus(ttsRequest: TtsRequest) {
        if (!isOverridingTts) {
            sdkServiceCallback.onSdkError(SdkException.illegalArgument("'com.robotemi.sdk.metadata.OVERRIDE_TTS' should be declared in AndroidManifest.xml"))
            return
        }
        try {
            sdkService?.publishTtsStatus(applicationInfo.packageName, ttsRequest)
        } catch (e: RemoteException) {
            Log.e(TAG, "publishTtsStatus() error")
        }
    }

    /**
     * The wakeup word of the temi's assistant.
     */
    @get:CheckResult
    val wakeupWord: String
        get() {
            try {
                return sdkService?.wakeupWord ?: ""
            } catch (e: RemoteException) {
                Log.e(TAG, "getWakeupWord() error")
            }
            return ""
        }

    /**
     * Trigger temi's wakeup programmatically.
     *
     * @param languages - List of languages to be used for ASR. Overrides the setAsrLanguages() languages.
     * If empty, then the system default languages will be used.
     * @param wakeupRequest - Wakeup request, control the behavior of wakeup.
     */
    fun wakeup(languages: List<SttLanguage> = emptyList(), wakeupRequest: WakeupRequest? = null) {
        try {
            sdkService?.wakeup(languages.map { it.value }.toIntArray(),
                SttRequest(
                    languages = languages,
                    timeout = 0,
                    multipleConversation = false
                ),
                wakeupRequest)
        } catch (e: RemoteException) {
            Log.e(TAG, "wakeup() error")
        }
    }

    /**
     * Trigger temi's wakeup programmatically. Added in 133 version, as an alternative to [wakeup(languages: List<SttLanguage>)]
     *
     * @param sttRequest - STT request, define STT languages, listening timeout, multiple conversation.
     * @param wakeupRequest - Wakeup request, control the behavior of wakeup.
     */
    fun wakeup(sttRequest: SttRequest, wakeupRequest: WakeupRequest? = null) {
        try {
            sdkService?.wakeup(sttRequest.languages.map { it.value }.toIntArray(),
                sttRequest,
                wakeupRequest)
        } catch (e: RemoteException) {
            Log.e(TAG, "wakeup() error")
        }
    }

    /**
     * Require Kiosk Permission
     *
     * @param languages - List of languages to be used for ASR while the app is in kiosk.
     * If empty, then the system default languages will be used.
     */
    fun setAsrLanguages(languages: List<SttLanguage> = emptyList()): Int {
        try {
            return sdkService?.setAsrLanguages(
                applicationInfo.packageName,
                languages.map { it.value }.toIntArray()
            ) ?: -1
        } catch (e: RemoteException) {
            Log.e(TAG, "setAsrLanguages() error")
        }
        return -1
    }

    /**
     * Stops currently processed TTS request and empty the queue.
     */
    fun cancelAllTtsRequests() {
        try {
            sdkService?.cancelAll()
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to invoke remote call cancelAllTtsRequest()")
        }
    }

    /**
     * Request to lock contexts even if skill screen is dismissed.
     * Useful for services running in the background without UI.
     *
     * @param contextsToLock - List of contexts names to lock.
     */
    @Deprecated("No longer supported")
    fun lockContexts(contextsToLock: List<String>) {
        try {
            sdkService?.lockContexts(contextsToLock)
        } catch (e: RemoteException) {
            Log.e(TAG, "lockContexts(List<String>) error")
        }
    }

    /**
     * Release previously locked contexts. See [.lockContexts].
     *
     * @param contextsToRelease - List of contexts names to release.
     */
    @Deprecated("No longer supported")
    fun releaseContexts(contextsToRelease: List<String>) {
        try {
            sdkService?.releaseContexts(contextsToRelease)
        } catch (e: RemoteException) {
            Log.e(TAG, "releaseContexts(List<String>) error")
        }
    }

    /**
     * Start a conversation.
     *
     * @param question - First question from robot.
     */
    fun askQuestion(question: String) {
        try {
            sdkService?.askQuestion(question,
                TtsRequest.create(question).apply { packageName = applicationInfo.packageName }, null)
        } catch (e: RemoteException) {
            Log.e(TAG, "Ask question call failed")
        }
    }

    /**
     * Start a conversation. Added in 133 version, as an alternative to [askQuestion(::String)]
     *
     * @param question - First question from robot.
     * @param sttRequest, after [question] is spoken, [sttRequest] will define the following speech to text session. similar to [wakeup(::SttRequest)]
     */
    fun askQuestion(question: TtsRequest, sttRequest: SttRequest? = null) {
        try {
            sdkService?.askQuestion(question.speech,
                question.apply { packageName = applicationInfo.packageName }, sttRequest)
        } catch (e: RemoteException) {
            Log.e(TAG, "Ask question call failed")
        }
    }

    /**
     * Finish conversation.
     */
    fun finishConversation() {
        try {
            sdkService?.finishConversation()
        } catch (e: RemoteException) {
            Log.e(TAG, "Finish conversation call failed")
        }
    }

    /**
     * Trigger temi Launcher's default NLU service.
     *
     * @param text The text to be processed.
     * @param sttLanguage，the language you want the text to be recognized. Added in 133 version.
     */
    fun startDefaultNlu(text: String, sttLanguage: SttLanguage = SttLanguage.SYSTEM) {
        try {
            sdkService?.startDefaultNlu(applicationInfo.packageName, text, sttLanguage.value)
        } catch (e: RemoteException) {
            Log.e(TAG, "startDefaultNlu() error")
        }
    }

    /**
     * Get TTS voice, speed, and pitch.
     */
    @Nullable
    fun getTtsVoice(): TtsVoice? {
        return try {
            sdkService?.ttsVoice
        } catch (e: RemoteException) {
            Log.e(TAG, "getTtsVoice() error")
            null
        }
    }

    private val speedRange: FloatArray = FloatArray(16) { index ->
        (index + 5) / 10f
    }

    private val pitchRange: IntArray = IntArray(21) { index ->
        index - 10
    }

    /**
     * Set TTS voice, speed, and pitch. Require [Permission.SETTINGS]
     * @return true if set is successful
     */
    fun setTtsVoice(ttsVoice: TtsVoice): Boolean {
        with(ttsVoice) {
            if (gender == Gender.UNKNOWN) {
                Log.e(TAG, "Gender $gender is invalid")
                return false
            }
            if (speedRange.any { it == speed }.not()) {
                Log.e(TAG, "Speed $speed is invalid, range is ${speedRange.toList()}")
                return false
            }
            if (pitch !in pitchRange) {
                Log.e(TAG, "Pitch $pitch is invalid, range is ${pitchRange.toList()}")
                return false
            }
        }

        return try {
            sdkService?.setTtsVoice(applicationInfo.packageName, ttsVoice) ?: false
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "setTtsVoice() set specific voice error")
            false
        } catch (e: RemoteException) {
            Log.e(TAG, "setTtsVoice() error")
            false
        }
    }

    /**
     * Create a link based meeting.
     *
     * @return  response code, 200: is OK.
     *                         403: not allowed, [Permission.MEETINGS] required.
     *                         429: request too frequently. Shall be 5 seconds interval.
     *          meeting link, like "https://center.robotemi.com/meetings/{linkId}"
     *
     */
    @WorkerThread
    fun createLinkBasedMeeting(linkBasedMeeting: LinkBasedMeeting): Pair<Int, String> {
        try {
            val json = gson.toJson(linkBasedMeeting)
            val resp: String = sdkService?.createLinkBasedMeeting(applicationInfo.packageName, json)
                ?: return 400 to "failed to create"
            if (resp.startsWith("https")) {
                return 200 to resp
            } else {
                val respCode = resp.toIntOrNull() ?: return 400 to "invalid response"
                return respCode to "failed to create"
            }
        } catch (e: RemoteException) {
            Log.e(TAG, "createLinkBasedMeeting() error")
            return 500 to "robot not read"
        } catch (e: IOException) {
            Log.e(TAG, "createLinkBasedMeeting() serialization error", e)
            return 400 to "invalid request"
        }
    }

    @Deprecated(message = "Use [addOnConversationStatusChangedListener] instead")
    @UiThread
    fun addConversationViewAttachesListenerListener(conversationViewAttachesListener: ConversationViewAttachesListener) {
        conversationViewAttachesListeners.add(conversationViewAttachesListener)
    }

    @Deprecated(message = "Use [removeConversationViewAttachesListener] instead")
    @UiThread
    fun removeConversationViewAttachesListenerListener(conversationViewAttachesListener: ConversationViewAttachesListener) {
        conversationViewAttachesListeners.remove(conversationViewAttachesListener)
    }

    @UiThread
    fun addConversationViewAttachesListener(conversationViewAttachesListener: ConversationViewAttachesListener) {
        conversationViewAttachesListeners.add(conversationViewAttachesListener)
    }

    @UiThread
    fun removeConversationViewAttachesListener(conversationViewAttachesListener: ConversationViewAttachesListener) {
        conversationViewAttachesListeners.remove(conversationViewAttachesListener)
    }

    @UiThread
    fun addNlpListener(nlpListener: NlpListener) {
        nlpListeners.add(nlpListener)
    }

    @UiThread
    fun removeNlpListener(nlpListener: NlpListener) {
        nlpListeners.remove(nlpListener)
    }

    @UiThread
    fun addTtsListener(ttsListener: TtsListener) {
        ttsListeners.add(ttsListener)
    }

    @UiThread
    fun removeTtsListener(ttsListener: TtsListener) {
        ttsListeners.remove(ttsListener)
    }

    @UiThread
    fun addWakeupWordListener(wakeupWordListener: WakeupWordListener) {
        wakeUpWordListeners.add(wakeupWordListener)
    }

    @UiThread
    fun removeWakeupWordListener(wakeupWordListener: WakeupWordListener) {
        wakeUpWordListeners.remove(wakeupWordListener)
    }

    @UiThread
    fun removeAsrListener(asrListener: AsrListener) {
        asrListeners.remove(asrListener)
    }

    @UiThread
    fun addAsrListener(asrListener: AsrListener) {
        asrListeners.add(asrListener)
    }

    @UiThread
    fun addOnConversationStatusChangedListener(onConversationStatusChangedListener: OnConversationStatusChangedListener) {
        onConversationStatusChangedListeners.add(onConversationStatusChangedListener)
    }

    @UiThread
    fun removeOnConversationStatusChangedListener(onConversationStatusChangedListener: OnConversationStatusChangedListener) {
        onConversationStatusChangedListeners.remove(onConversationStatusChangedListener)
    }

    @UiThread
    fun addOnTtsVisualizerWaveFormDataChangedListener(onTtsVisualizerWaveFormDataChangedListener: OnTtsVisualizerWaveFormDataChangedListener) {
        onTtsVisualizerWaveFormDataChangedListeners.add(onTtsVisualizerWaveFormDataChangedListener)
    }

    @UiThread
    fun removeOnTtsVisualizerWaveFormDataChangedListener(onTtsVisualizerWaveFormDataChangedListener: OnTtsVisualizerWaveFormDataChangedListener) {
        onTtsVisualizerWaveFormDataChangedListeners.remove(
            onTtsVisualizerWaveFormDataChangedListener
        )
    }

    @UiThread
    fun addOnTtsVisualizerFftDataChangedListener(onTtsVisualizerFftDataChangedListener: OnTtsVisualizerFftDataChangedListener) {
        onTtsVisualizerFftDataChangedListeners.add(onTtsVisualizerFftDataChangedListener)
    }

    @UiThread
    fun removeOnTtsVisualizerFftDataChangedListener(onTtsVisualizerFftDataChangedListener: OnTtsVisualizerFftDataChangedListener) {
        onTtsVisualizerFftDataChangedListeners.remove(onTtsVisualizerFftDataChangedListener)
    }

    /*****************************************/
    /*               Navigation              */
    /*****************************************/

    /**
     * Get Position.
     *
     * @return Result of current position or Position(0, 0, 0, 0) if failed.
     */
    fun getPosition(): Position {
        return sdkService?.getPosition(applicationInfo.packageName) ?: Position(0f, 0f, 0f, 0)
    }

    /**
     * Save location.
     *
     * @param name Location name.
     * @return Result of a successful or failed operation.
     */
    fun saveLocation(name: String): Boolean {
        try {
            return sdkService?.saveLocation(name) ?: false
        } catch (e: RemoteException) {
            Log.e(TAG, "saveLocation(String) error")
        }
        return true
    }

    /**
     * Delete location.
     *
     * @param name Location name.
     * @return Result of a successful or failed operation.
     */
    fun deleteLocation(name: String): Boolean {
        if (name == SdkConstants.LOCATION_HOME_BASE) {
            sdkServiceCallback.onSdkError(SdkException.illegalArgument("Can not delete $name"))
            return false
        }
        try {
            return sdkService?.deleteLocation(name) ?: false
        } catch (e: RemoteException) {
            Log.e(TAG, "deleteLocation(String) error")
        }
        return false
    }

    /**
     * Retrieve list of previously saved locations.
     *
     * @return List of saved locations.
     */
    @get:CheckResult
    val locations: List<String>
        get() {
            try {
                return sdkService?.locations ?: emptyList()
            } catch (e: RemoteException) {
                Log.e(TAG, "getLocations() error")
            }
            return emptyList()
        }

    /**
     * Send robot to previously saved location.
     *
     * @param location Saved location name.
     * @param backwards if `true` will walk backwards to the destination. `false` by default.
     * @param noBypass if `true` will disallow bypass the obstacles during go to.
     *   Pass `null` to follow the  *Settings -> Navigation Settings*
     * @param speedLevel the speed level of this single go to session.
     *   Pass `null` to start with the speed level in *Settings -> Navigation Settings* (see [speedLevel]).
     * @param highAccuracyArrival if `true` will report arrival only when both location and rotation on destination are accurate.
     *      *   Pass `null` to follow the  *Settings -> Navigation Settings*. Added in 135 version.
     * @param noRotationAtEnd if `true` will skip rotate to destination's saved angle. Added in 135 version.
     */
    @JvmOverloads
    fun goTo(
        location: String,
        backwards: Boolean? = null,
        noBypass: Boolean? = null,
        speedLevel: SpeedLevel? = null,
        highAccuracyArrival: Boolean? = null,
        noRotationAtEnd: Boolean? = null,
    ) {
        require(location.isNotBlank()) { "Location can not be null or empty." }
        try {
            val allowBackwardsInt =
                if (backwards == null) NOT_SET else if (backwards) TRUE else FALSE
            val noBypassInt = if (noBypass == null) NOT_SET else if (noBypass) TRUE else FALSE
            val highAccuracyArrivalInt = if (highAccuracyArrival == null) NOT_SET else if (highAccuracyArrival) TRUE else FALSE
            val noRotationAtEndInt = if (noRotationAtEnd == null) NOT_SET else if (noRotationAtEnd) TRUE else FALSE
            sdkService?.goTo(location,
                allowBackwardsInt,
                noBypassInt,
                speedLevel?.value ?: "",
                highAccuracyArrivalInt,
                noRotationAtEndInt,
            )
        } catch (e: RemoteException) {
            Log.e(TAG, "goTo(String) error")
        }
    }

    /**
     * Go to a specific position with (x,y,yaw).
     *
     * @param position Position holds (x,y,yaw). Set yaw to 999 will cancel the rotataion on arrival.
     * @param backwards if `true` will walk backwards to the destination. `false` by default.
     * @param noBypass if `true` will disallow bypass the obstacles during go to.
     *   Pass `null` to follow the  *Settings -> Navigation Settings*
     * @param speedLevel the speed level of this single go to session.
     *   Pass `null` to start with the speed level in *Settings -> Navigation Settings* (see [speedLevel]).
     * @param highAccuracyArrival if `true` will report arrival only when both location and rotation on destination are accurate.
     *   Pass `null` to follow the  *Settings -> Navigation Settings*. Added in 135 version.
     */
    @JvmOverloads
    fun goToPosition(
        position: Position,
        backwards: Boolean? = null,
        noBypass: Boolean? = null,
        speedLevel: SpeedLevel? = null,
        highAccuracyArrival: Boolean? = null,
    ) {
        try {
            val allowBackwardsInt =
                if (backwards == null) NOT_SET else if (backwards) TRUE else FALSE
            val noBypassInt = if (noBypass == null) NOT_SET else if (noBypass) TRUE else FALSE
            val highAccuracyArrivalInt = if (highAccuracyArrival == null) NOT_SET else if (highAccuracyArrival) TRUE else FALSE
            sdkService?.goToPosition(
                position,
                allowBackwardsInt,
                noBypassInt,
                speedLevel?.value ?: "",
                highAccuracyArrivalInt
            )
        } catch (e: RemoteException) {
            Log.e(TAG, "goToPosition() error")
        }
    }

    /**
     * @param locations, at least 3 valid locations, can be duplicated.
     *                   Home base will be ignored and should not be included.
     * @param nonstop, if set as true, it will just arrive at the position of location, without tilt and turn, and then immediately head to next location.
     * @param times, how many times should it go on the route,
     *               0, infinite,
     *               1, once
     *               and so on.
     * @param waiting, If [nonstop] is false, the time in seconds it should wait on each location. Range from 3 - 60, default is 3
     *
     * @return true if patrol is executed.
     */
    fun patrol(
        locations: List<String>,
        nonstop: Boolean = false,
        @IntRange(from = 0) times: Int,
        @IntRange(from = 3, to = 60) waiting: Int = 3
    ): Boolean {
        return try {
            sdkService?.patrol(locations, nonstop, times, waiting) == 0
        } catch (e: RemoteException) {
            Log.e(TAG, "patrol() error")
            false
        }
    }

    /**
     *  Set navigation safety level.
     */
    @get:CheckResult
    var navigationSafety: SafetyLevel
        get() {
            try {
                return SafetyLevel.valueToEnum(
                    sdkService?.navigationSafety ?: SafetyLevel.DEFAULT.value
                )
            } catch (e: RemoteException) {
                Log.e(TAG, "getNavigationSafety() error")
            }
            return SafetyLevel.DEFAULT
        }
        set(safetyLevel) {
            try {
                sdkService?.setNavigationSafety(applicationInfo.packageName, safetyLevel.value)
            } catch (e: RemoteException) {
                Log.e(TAG, "setNavigationSafety() error")
            }
        }

    /**
     * Get / Set navigation speed level.
     */
    @get:CheckResult
    var goToSpeed: SpeedLevel
        get() {
            try {
                return SpeedLevel.valueToEnum(
                    sdkService?.goToSpeed ?: SpeedLevel.DEFAULT.value
                )
            } catch (e: RemoteException) {
                Log.e(TAG, "getGoToSpeed() error")
            }
            return SpeedLevel.DEFAULT
        }
        set(speedLevel) {
            try {
                sdkService?.setGoToSpeed(applicationInfo.packageName, speedLevel.value)
            } catch (e: RemoteException) {
                Log.e(TAG, "setGoToSpeed() error")
            }
        }

    /**
     * Get follow speed level.
     *
     * Added in 135 version.
     *
     */
    fun getFollowSpeed(): SpeedLevel {
        try {
            return SpeedLevel.valueToEnum(
                sdkService?.followSpeed ?: SpeedLevel.DEFAULT.value
            )
        } catch (e: RemoteException) {
            Log.e(TAG, "getFollowSpeed() error")
        }
        return SpeedLevel.DEFAULT
    }

    /**
     * Set follow speed level
     *
     * Added in 135 version.
     *
     * @return 0 if the operation is not supported by current launcher
     *         200 success
     *         400 invalid parameter
     *         403 SETTINGS permission required
     */
    fun setFollowSpeed(speedLevel: SpeedLevel): Int {
        try {
            val resp = sdkService?.setFollowSpeed(applicationInfo.packageName, speedLevel.value)?.toIntOrNull() ?: 0
            Log.d(TAG, "setFollowSpeed() response: $resp")
            return resp
        } catch (e: RemoteException) {
            Log.e(TAG, "setFollowSpeed() error")
        }
        return 0
    }

    /**
     * Start positing to locate the position of temi.
     *
     * @param position, a suggested position to instruct the algorithm to do location position.
     *        e.g. repose(Position(0.0, 0.0, 0.0)) will start a reposition process
     *        with initial location from coordinate (0, 0), direction 0, and do a quick search from the give coordinate.
     *        It is null by default, meaning only global searching will be performed.
     *        This parameter is supported in launcher from 134 version.
     */
    @JvmOverloads
    fun repose(position: Position? = null) {
        try {
            sdkService?.repose(position)
        } catch (e: RemoteException) {
            Log.e(TAG, "repose() error")
        }
    }

    /**
     * Get the status of the repose process.
     *
     * @return the status of the repose process.
     */
    fun getReposeStatus(): ReposeStatus {
        try {
            return ReposeStatus.valueToEnum(sdkService?.reposeStatus ?: ReposeStatus.UNKNOWN.value)
        } catch (e: RemoteException) {
            Log.e(TAG, "getReposeStatus() error")
        }
        return ReposeStatus.UNKNOWN
    }

    @UiThread
    fun addOnGoToLocationStatusChangedListener(listener: OnGoToLocationStatusChangedListener) {
        onGoToLocationStatusChangeListeners.add(listener)
    }

    @UiThread
    fun removeOnGoToLocationStatusChangedListener(listener: OnGoToLocationStatusChangedListener) {
        onGoToLocationStatusChangeListeners.remove(listener)
    }

    @UiThread
    fun addOnLocationsUpdatedListener(listener: OnLocationsUpdatedListener) {
        onLocationsUpdatedListeners.add(listener)
    }

    @UiThread
    fun removeOnLocationsUpdateListener(listener: OnLocationsUpdatedListener) {
        onLocationsUpdatedListeners.remove(listener)
    }

    @UiThread
    fun addOnDistanceToLocationChangedListener(listener: OnDistanceToLocationChangedListener) {
        onDistanceToLocationChangedListeners.add(listener)
    }

    @UiThread
    fun removeOnDistanceToLocationChangedListener(listener: OnDistanceToLocationChangedListener) {
        onDistanceToLocationChangedListeners.remove(listener)
    }

    @UiThread
    fun addOnCurrentPositionChangedListener(listener: OnCurrentPositionChangedListener) {
        onCurrentPositionChangedListeners.add(listener)
        listener.onCurrentPositionChanged(getPosition())
    }

    @UiThread
    fun removeOnCurrentPositionChangedListener(listener: OnCurrentPositionChangedListener) {
        onCurrentPositionChangedListeners.remove(listener)
    }

    @UiThread
    fun addOnReposeStatusChangedListener(listener: OnReposeStatusChangedListener) {
        onReposeStatusChangedListeners.add(listener)
    }

    @UiThread
    fun removeOnReposeStatusChangedListener(listener: OnReposeStatusChangedListener) {
        onReposeStatusChangedListeners.remove(listener)
    }

    @UiThread
    fun addOnDistanceToDestinationChangedListener(listener: OnDistanceToDestinationChangedListener) {
        onDistanceToDestinationChangedListeners.add(listener)
    }

    @UiThread
    fun removeOnDistanceToDestinationChangedListener(listener: OnDistanceToDestinationChangedListener) {
        onDistanceToDestinationChangedListeners.remove(listener)
    }

    /**
     * Show a navigation path of the robot during go to location/coordinates
     * Added in 134 version.
     *
     */
    @UiThread
    fun addOnGoToNavPathChangedListener(listener: OnGoToNavPathChangedListener) {
        onGoToNavPathChangedListeners.add(listener)
    }

    @UiThread
    fun removeOnGoToNavPathChangedListener(listener: OnGoToNavPathChangedListener) {
        onGoToNavPathChangedListeners.remove(listener)
    }

    /*****************************************/
    /*            Movement & Follow          */
    /*****************************************/

    /**
     * Request robot to follow user around.
     * Add [OnBeWithMeStatusChangedListener] to listen for status changes.
     *
     * @param speedLevel, the speed of following, added in 135 version.
     */
    fun beWithMe(
        speedLevel: SpeedLevel? = null
    ) {
        try {
            sdkService?.beWithMe(speedLevel?.value ?: "")
        } catch (e: RemoteException) {
            Log.e(TAG, "beWithMe() error")
        }
    }

    /**
     * Start constraint follow.
     * Add [OnConstraintBeWithStatusChangedListener] to listen for status changed.
     */
    fun constraintBeWith() {
        try {
            sdkService?.constraintBeWith()
        } catch (e: RemoteException) {
            Log.e(TAG, "constraintBeWith() error")
        }
    }

    /**
     * Request robot to stop any movement.
     */
    fun stopMovement() {
        try {
            sdkService?.stopMovement()
        } catch (e: RemoteException) {
            Log.e(TAG, "stopMovement() error")
        }
    }

    /**
     * Joystick commands.
     *
     * @param x Move on the x axis from -1 to 1.
     * @param y Move on the y axis from -1 to 1.
     * @param smart Moving with bypassing the obstacles
     */
    @JvmOverloads
    fun skidJoy(
        @FloatRange(from = -1.0, to = 1.0) x: Float,
        @FloatRange(from = -1.0, to = 1.0) y: Float,
        smart: Boolean = false
    ) {
        try {
            sdkService?.skidJoy(x, y, smart)
        } catch (e: RemoteException) {
            Log.e(TAG, "skidJoy(float, float, smart) (x=$x, y=$y, smart=$smart) error")
        }
    }

    /**
     * To turn temi by a specific degree.
     *
     * @param degrees the degree amount you want the robot to turn
     * @param speed Coefficient of maximum speed, between 0 to 1
     */
    @JvmOverloads
    fun turnBy(degrees: Int, @FloatRange(from = 0.0, to = 1.0) speed: Float = 1f) {
        try {
            sdkService?.turnBy(degrees, speed)
        } catch (e: RemoteException) {
            Log.e(TAG, "turnBy(int) (degrees=$degrees) error")
        }
    }

    /**
     * To tilt temi's head to a specific angle.
     *
     * @param degrees the degree which you want the robot to tilt to, between 55 and -25
     * @param speed Coefficient of maximum speed, between 0 to 1
     */
    @JvmOverloads
    fun tiltAngle(degrees: Int, @FloatRange(from = 0.0, to = 1.0) speed: Float = 1f) {
        try {
            sdkService?.tiltAngle(degrees, speed)
        } catch (e: RemoteException) {
            Log.e(TAG, "turnBy(int, float) (degrees=$degrees, speed=$speed) error")
        }
    }

    /**
     * To tilt temi's head to by a specific degree.
     *
     * @param degrees The degree amount you want the robot to tilt
     * @param speed Coefficient of maximum speed, between 0 to 1
     */
    @JvmOverloads
    fun tiltBy(degrees: Int, @FloatRange(from = 0.0, to = 1.0) speed: Float = 1f) {
        try {
            sdkService?.tiltBy(degrees, speed)
        } catch (e: RemoteException) {
            Log.e(TAG, "tiltBy(int, float) (degrees=$degrees, speed=$speed) error")
        }
    }

    @UiThread
    fun addOnBeWithMeStatusChangedListener(listener: OnBeWithMeStatusChangedListener) {
        onBeWithMeStatusChangeListeners.add(listener)
    }

    @UiThread
    fun removeOnBeWithMeStatusChangedListener(listener: OnBeWithMeStatusChangedListener) {
        onBeWithMeStatusChangeListeners.remove(listener)
    }

    @UiThread
    fun addOnConstraintBeWithStatusChangedListener(listener: OnConstraintBeWithStatusChangedListener) {
        onConstraintBeWithStatusChangedListeners.add(listener)
    }

    @UiThread
    fun removeOnConstraintBeWithStatusChangedListener(listener: OnConstraintBeWithStatusChangedListener) {
        onConstraintBeWithStatusChangedListeners.remove(listener)
    }

    @UiThread
    fun addOnRobotLiftedListener(listener: OnRobotLiftedListener) {
        onRobotLiftedListeners.add(listener)
    }

    @UiThread
    fun removeOnRobotLiftedListener(listener: OnRobotLiftedListener) {
        onRobotLiftedListeners.remove(listener)
    }

    @UiThread
    fun addOnMovementVelocityChangedListener(listener: OnMovementVelocityChangedListener) {
        onMovementVelocityChangedListeners.add(listener)
    }

    @UiThread
    fun removeOnMovementVelocityChangedListener(listener: OnMovementVelocityChangedListener) {
        onMovementVelocityChangedListeners.remove(listener)
    }

    @UiThread
    fun addOnMovementStatusChangedListener(listener: OnMovementStatusChangedListener) {
        onMovementStatusChangedListeners.add(listener)
    }

    @UiThread
    fun removeOnMovementStatusChangedListener(listener: OnMovementStatusChangedListener) {
        onMovementStatusChangedListeners.remove(listener)
    }

    /*****************************************/
    /*           Users & Telepresence        */
    /*****************************************/

    /**
     * Get the information of temi's admin.
     */
    @get:CheckResult
    val adminInfo: UserInfo?
        get() {
            try {
                return sdkService?.adminInfo
            } catch (e: RemoteException) {
                Log.e(TAG, "getAdminInfo() error")
            }
            return null
        }

    /**
     * Fetch all the temi contacts.
     */
    @get:CheckResult
    val allContact: List<UserInfo>
        get() {
            val contactList = ArrayList<UserInfo>()
            try {
                contactList.addAll(sdkService?.allContacts ?: emptyList())
            } catch (e: RemoteException) {
                Log.e(TAG, "getAllContacts() error")
            }
            return contactList
        }

    /**
     * Fetch recent calls.
     */
    @get:CheckResult
    val recentCalls: List<RecentCallModel>
        get() {
            try {
                return sdkService?.recentCalls ?: emptyList()
            } catch (e: RemoteException) {
                Log.e(TAG, "getRecentCalls() error")
            }
            return emptyList()
        }

    /**
     * Start a video call to the temi user.
     *
     * @param displayName Name of temi user.
     * @param peerId ID of temi user ID.
     * @param platform Platform of the target user.
     * @return
     */
    @Deprecated("From 131 version, startMeeting() shall be used.")
    @JvmOverloads
    fun startTelepresence(
        displayName: String,
        peerId: String,
        platform: Platform = Platform.MOBILE
    ): String {
        try {
            return sdkService?.startTelepresence(displayName, peerId, platform.value) ?: ""
        } catch (e: RemoteException) {
            Log.e(TAG, "startTelepresence() error")
        }
        return ""
    }

    /**
     * Stop the current video call
     * @return Status code, 200 is ok, 400 is failed to verify the app package name.
     * 404 is currently no video call, 403 is meeting permission verification failed.
     * 500 is SDK internal error.
     */
    fun stopTelepresence(): Int {
        return try {
            sdkService?.stopTelepresence(applicationInfo.packageName) ?: 500
        } catch (e: RemoteException) {
            Log.e(TAG, "stopTelepresence() error")
            500
        }
    }

    @get:CheckResult
    val membersStatus: List<MemberStatusModel>
        get() {
            try {
                return sdkService?.membersStatus ?: emptyList()
            } catch (e: RemoteException) {
                Log.e(TAG, "getMembersStatus() error")
            }
            return emptyList()
        }

    /**
     * Start a video call to the temi user. Require MEETINGS permission.
     *
     * @param participants list of user names and peer ids.
     * @param firstParticipantJoinedAsHost, Set it as true to automatically assign first participant joined as meeting host.
     *                          Otherwise robot will be the host.
     * @param blockRobotInteraction, supported by version 132 launcher,
     *                       disable some launcher buttons in the call.
     *                       Prevent user to interrupt the call.
     * @return 403 require MEETINGS permission
     *         200 OK.
     */
    fun startMeeting(
        participants: List<Participant>,
        firstParticipantJoinedAsHost: Boolean,
        blockRobotInteraction: Boolean = false,
    ): String {
        try {
            return sdkService?.startMeeting(applicationInfo.packageName,
                participants,
                firstParticipantJoinedAsHost,
                blockRobotInteraction) ?: ""
        } catch (e: RemoteException) {
            Log.e(TAG, "startMeeting() error")
        }
        return ""
    }

    /**
     * Start listening for Telepresence Status changes.
     *
     * @param listener The listener you want to add.
     */
    @UiThread
    fun addOnTelepresenceStatusChangedListener(listener: OnTelepresenceStatusChangedListener) {
        onTelepresenceStatusChangedListeners.add(listener)
    }

    /**
     * Stop listening for Telepresence Status changes.
     *
     * @param listener The listener you added before.
     */
    @UiThread
    fun removeOnTelepresenceStatusChangedListener(listener: OnTelepresenceStatusChangedListener) {
        onTelepresenceStatusChangedListeners.remove(listener)
    }

    /**
     * Start listening for user information updates.
     *
     * @param listener The listener you want to add.
     */
    @UiThread
    fun addOnUsersUpdatedListener(listener: OnUsersUpdatedListener) {
        onUsersUpdatedListeners.add(listener)
    }

    /**
     * Stop listening for user information updates.
     *
     * @param listener The listener you added before.
     */
    @UiThread
    fun removeOnUsersUpdatedListener(listener: OnUsersUpdatedListener) {
        onUsersUpdatedListeners.remove(listener)
    }

    @UiThread
    fun addOnTelepresenceEventChangedListener(listener: OnTelepresenceEventChangedListener) {
        onTelepresenceEventChangedListener.add(listener)
    }

    @UiThread
    fun removeOnTelepresenceEventChangedListener(listener: OnTelepresenceEventChangedListener) {
        onTelepresenceEventChangedListener.remove(listener)
    }

    /*****************************************/
    /*                 System                */
    /*****************************************/

    /**
     * Request robot's serial number as a String.
     *
     * @return The serial number of the robot.
     */
    @get:CheckResult
    val serialNumber: String?
        get() {
            try {
                return sdkService?.serialNumber
            } catch (e: RemoteException) {
                Log.e(TAG, "getSerialNumber() error")
            }
            return null
        }

    /**
     * Request the robot to provide current battery status.
     *
     * @return The battery data of the robot.
     */
    @get:CheckResult
    val batteryData: BatteryData?
        get() {
            try {
                return sdkService?.batteryData
            } catch (e: RemoteException) {
                Log.e(TAG, "getBatteryData() error")
            }
            return null
        }

    /**
     * Go to the App list of Launcher.
     */
    fun showAppList() {
        try {
            sdkService?.showAppList()
        } catch (e: RemoteException) {
            Log.e(TAG, "showAppList() error")
        }
    }

    /**
     * Show the top bar of Launcher.
     */
    fun showTopBar() {
        try {
            sdkService?.showTopBar()
        } catch (e: RemoteException) {
            Log.e(TAG, "showTopBar() error")
        }
    }

    /**
     * Hide the top bar of Launcher.
     */
    fun hideTopBar() {
        try {
            sdkService?.hideTopBar()
        } catch (e: RemoteException) {
            Log.e(TAG, "hideTopBar() error")
        }
    }

    /**
     * Toggle privacy mode on temi.
     */
    @get:CheckResult
    var privacyMode: Boolean
        get() {
            try {
                return sdkService?.privacyModeState ?: false
            } catch (e: RemoteException) {
                Log.e(TAG, "getPrivacyModeState() error")
            }
            return false
        }
        set(on) {
            try {
                sdkService?.togglePrivacyMode(on, applicationInfo.packageName)
            } catch (e: RemoteException) {
                Log.e(TAG, "togglePrivacyMode() error")
            }
        }

    /**
     * Get(Set) HardButtons enabled(disabled).
     */
    @get:CheckResult
    var isHardButtonsDisabled: Boolean
        get() {
            try {
                return sdkService?.isHardButtonsDisabled ?: false
            } catch (e: RemoteException) {
                Log.e(TAG, "setHardButtonsDisabled() error")
            }
            return false
        }
        set(disable) {
            try {
                sdkService?.toggleHardButtons(disable, applicationInfo.packageName)
            } catch (e: RemoteException) {
                Log.e(TAG, "isHardButtonsEnabled() error")
            }
        }

    fun setHardButtonMode(type: HardButton, mode: HardButton.Mode) {
        try {
            sdkService?.setHardButtonMode(applicationInfo.packageName, type.value, mode.value)
        } catch (e: RemoteException) {
            Log.e(TAG, "setHardButtonMode() error")
        }
    }

    /**
     * Get hard button status
     * Added in 134 version.
     * @param type, in 134 version it only supports [HardButton.EMERGENCY_STOP]
     *
     * @return hard button status,
     *      [HardButton.Status.UNKNOWN] mean the version doesn't support it, or the robot doesn't have a emergency button
     */
    fun getButtonStatus(type: HardButton): HardButton.Status {
        try {
            val resp = sdkService?.getHardButtonStatus(applicationInfo.packageName, type.value)
            return if (resp == null) {
                // Launcher/Robox doesn't support this operation.
                HardButton.Status.UNKNOWN
            } else {
                try {
                    HardButton.Status.valueOf(resp)
                } catch (e: Exception) {
                    Log.e(TAG, "getButtonStatus() error, resp >$resp<")
                    HardButton.Status.UNKNOWN
                }
            }
        } catch (e: RemoteException) {
            Log.e(TAG, "getButtonStatus() error")
        }
        return HardButton.Status.UNKNOWN
    }

    /**
     * Get current mode of the specific hard button.
     *
     * @param type See [HardButton.MAIN], [HardButton.POWER], [HardButton.VOLUME]
     * @return hard button mode, whether the button is disabled or not.
     */
    @CheckResult
    fun getHardButtonMode(type: HardButton): HardButton.Mode {
        return try {
            HardButton.Mode.valueToEnum(sdkService?.getHardButtonMode(type.value))
        } catch (e: RemoteException) {
            Log.e(TAG, "getHardButtonMode() error")
            HardButton.Mode.DEFAULT
        }
    }

    /**
     * Get version of the Launcher.
     */
    @get:CheckResult
    val launcherVersion: String
        get() {
            try {
                return sdkService?.launcherVersion ?: ""
            } catch (e: RemoteException) {
                Log.e(TAG, "getLauncherVersion() error")
            }
            return ""
        }

    /**
     * Get version of the Robox.
     */
    @get:CheckResult
    val roboxVersion: String
        get() {
            try {
                return sdkService?.roboxVersion ?: ""
            } catch (e: RemoteException) {
                Log.e(TAG, "getRoboxVersion() error")
            }
            return ""
        }

    /**
     * Show or hide the green badge(Movement indicator such as navigation, follow...) at the top of the screen.
     */
    @get:JvmName("isTopBadgeEnabled")
    @get:CheckResult
    var topBadgeEnabled: Boolean
        get() {
            try {
                return sdkService?.isTopBadgeEnabled ?: true
            } catch (e: RemoteException) {
                Log.e(TAG, "isTopBadgeEnabled() error")
            }
            return true
        }
        /**
         * @param enabled true to enable, false to disable the green badge.
         */
        set(enabled) {
            if (!isMetaDataKiosk) return
            try {
                sdkService?.setTopBadgeEnabled(applicationInfo.packageName, enabled)
            } catch (e: RemoteException) {
                Log.e(TAG, "setTopBadgeEnabled() error")
            }
        }

    /**
     * Turn on/off detection mode.
     */
    @get:JvmName("isDetectionModeOn")
    @get:CheckResult
    var detectionModeOn: Boolean
        get() {
            try {
                return sdkService?.isDetectionModeOn ?: false
            } catch (e: RemoteException) {
                Log.e(TAG, "isDetectionModeOn() error")
            }
            return false
        }
        /**
         * @param on true to turn on, false to turn off.
         */
        set(on) {
            setDetectionModeOn(on, SdkConstants.DETECTION_DISTANCE_DEFAULT)
        }

    /**
     * Turn on/off detection mode with distance.
     *
     * @param on true to turn on, false to turn off.
     * @param distance  Maximum detection distance(0.5~2.0), person will be detected when the distance
     *                  to temi less than this value only.
     */
    fun setDetectionModeOn(on: Boolean, distance: Float) {
        try {
            sdkService?.setDetectionModeOn(applicationInfo.packageName, on, distance)
        } catch (e: RemoteException) {
            Log.e(TAG, "setDetectionModeOn() error")
        }
    }

    /**
     * Turn on/off track user(welcome mode).
     */
    @get:JvmName("isTrackUserOn")
    @get:CheckResult
    var trackUserOn: Boolean
        get() {
            try {
                return sdkService?.isTrackUserOn ?: false
            } catch (e: RemoteException) {
                Log.e(TAG, "isTrackUser() error")
            }
            return false
        }
        /**
         * @param on true to turn on, false to turn off.
         */
        set(on) {
            try {
                sdkService?.setTrackUserOn(applicationInfo.packageName, on)
            } catch (e: RemoteException) {
                Log.e(TAG, "setTrackUserOn() error")
            }
        }

    /**
     * Turn on/off auto return.
     */
    @get:JvmName("isAutoReturnOn")
    var autoReturnOn: Boolean
        @CheckResult
        get() {
            try {
                return sdkService?.isAutoReturnOn ?: false
            } catch (e: RemoteException) {
                Log.e(TAG, "isAutoReturnOn() error")
            }
            return false
        }
        /**
         * @param on true to turn on, false to turn off.
         */
        set(on) {
            try {
                sdkService?.setAutoReturnOn(applicationInfo.packageName, on)
            } catch (e: RemoteException) {
                Log.e(TAG, "setAutoReturnOn() error")
            }
        }

    /**
     * Volume of Launcher OS.
     */
    @get:CheckResult
    var volume: Int
        get() {
            try {
                return sdkService?.volume ?: 0
            } catch (e: RemoteException) {
                Log.e(TAG, "getVolume() error")
            }
            return 0
        }
        /**
         * @param volume the volume you want to set to Launcher.
         * Require [Permission.SETTINGS] permission to set volume
         */
        set(volume) {
            try {
                val validVolume = when {
                    volume < 0 -> 0
                    volume > 10 -> 10
                    else -> volume
                }
                sdkService?.setVolume(applicationInfo.packageName, validVolume)
            } catch (e: RemoteException) {
                Log.e(TAG, "setVolume() error")
            }
        }

    /**
     * Set microphone gain level to X1(default)-X4. Added in 133 version
     *
     * Require [Permission.SETTINGS] permission to change the value
     * @return  0 Operation failed.
     *          1 Operation succeed.
     *          403 SETTINGS permission required
     *          429 Too many requests, should be longer than 2 seconds between 2 calls
     */
    fun setMicGainLevel(@IntRange(from = 1, to = 4) micGainLevel: Int): Int {
        try {
            val validMicGainLevel = when {
                micGainLevel < 1 -> 1
                micGainLevel > 4 -> 4
                else -> micGainLevel
            }
            val result = sdkService?.setMicGainLevel(applicationInfo.packageName, validMicGainLevel)
            Log.d(TAG, "setMicGainLevel() result: $result")
            return result ?: 0
        } catch (e: RemoteException) {
            Log.e(TAG, "setMicGainLevel() error")
            return 0
        }
    }

    /**
     * Restart temi.
     *
     */
    fun restart() {
        if (!isMetaDataKiosk) {
            Log.w(TAG, "Only Kiosk App can restart temi")
            sdkServiceCallback.onSdkError(SdkException.permissionDenied("Kiosk Mode"))
            return
        }
        try {
            sdkService?.restart(applicationInfo.packageName)
        } catch (e: RemoteException) {
            Log.e(TAG, "restart() error")
        }
    }

    /**
     * Start Launcher's internal page.
     *
     * @param page Target page.
     */
    fun startPage(page: Page) {
        try {
            sdkService?.startPage(applicationInfo.packageName, page.value)
        } catch (e: RemoteException) {
            Log.e(TAG, "startPage() error")
        }
    }

    /**
     * Check if temi is locked.
     */
    @get:JvmName("isLocked")
    @get:CheckResult
    var locked: Boolean
        get() {
            return try {
                sdkService?.isLocked ?: false
            } catch (e: RemoteException) {
                Log.e(TAG, "isLocked() error")
                false
            }
        }
        /**
         * @param lock true(false) to lock(unlock) temi
         */
        set(lock) {
            try {
                sdkService?.lock(applicationInfo.packageName, lock)
            } catch (e: RemoteException) {
                Log.e(TAG, "setLocked() error")
            }
        }

    /**
     * Mute Alexa's MIC. Only useful in Global version with Alexa assistant。
     */
    fun muteAlexa() {
        try {
            sdkService?.muteAlexa(applicationInfo.packageName)
        } catch (e: RemoteException) {
            Log.e(TAG, "muteAlexa() error")
        }
    }

    /**
     * Shut down temi.
     *
     */
    fun shutdown() {
        try {
            sdkService?.shutdown(applicationInfo.packageName)
        } catch (e: RemoteException) {
            Log.e(TAG, "shutdown() error")
        }
    }

    /**
     * Sound mode
     *
     * @param soundMode
     */
    fun setSoundMode(soundMode: SoundMode) {
        try {
            sdkService?.setSoundMode(applicationInfo.packageName, soundMode.value)
        } catch (e: RemoteException) {
            Log.e(TAG, "setSoundMode() error")
        }
    }

    /**
     * Get temi's nick name
     *
     * @return
     */
    @CheckResult
    fun getNickName(): String {
        return try {
            sdkService?.getNickName(applicationInfo.packageName) ?: ""
        } catch (e: RemoteException) {
            Log.e(TAG, "getNickName() error")
            ""
        }
    }

    /**
     * Set system mode.
     *
     * @param mode Default, Greet, Privacy
     */
    fun setMode(mode: Mode) {
        try {
            sdkService?.setMode(applicationInfo.packageName, mode.value)
        } catch (e: RemoteException) {
            Log.e(TAG, "setMode() error")
        }
    }

    /**
     * Get system mode.
     *
     * @return Default, Greet, Privacy
     */
    @CheckResult
    fun getMode(): Mode {
        return try {
            Mode.valueToEnum(sdkService?.mode)
        } catch (e: RemoteException) {
            Log.e(TAG, "getMode() error")
            Mode.DEFAULT
        }
    }

    /**
     * Get StandBy status
     *
     * @return true if under standBy status,
     *         false if not under standBy status,
     *         null if the check is failed.
     */
    fun isStandByOn(): Boolean? {
        return sdkService?.isStandByOn
    }

    /**
     * Start StandBy
     *
     * Require [Permission.SETTINGS] Permission
     *
     * @return request result
     * <ul>
     *   <li> -1 for failed to request, maybe robot is not ready
     *   <li> 0 for standBy is started
     *   <li> 1 for standBy was already running
     *   <li> 2 for standby if disabled in settings
     *   <li> 3 for robot is busy, e.g. OTA, Greet Mode
     *   <li> 403 for SETTINGS permission required
     *   <li> 429 for too many requests, should be longer than 5 seconds between 2 calls
     * </ul>
     */
    fun startStandBy(): Int {
        return sdkService?.startStandBy(applicationInfo.packageName) ?: -1
    }

    /**
     * Stop StandBy with optional password
     *
     * Require [Permission.SETTINGS] Permission
     *
     * @param password default as empty
     *                 when temi requires password to unlock, this method only works when here a valid password is passed.
     *
     * @return request result
     * <ul>
     *   <li> -1 for failed to request, maybe robot is not ready
     *   <li> 0 for standBy is stopped
     *   <li> 1 for standBy was not running
     *   <li> 2 for password required
     *   <li> 3 for wrong password
     *   <li> 403 for SETTINGS permission required
     *   <li> 429 for too many requests, should be longer than 5 seconds between 2 calls
     * </ul>
     */
    fun stopStandBy(password: String = ""): Int {
        return sdkService?.stopStandBy(applicationInfo.packageName, password) ?: -1
    }

    /**
     * Enable/Disable StandBy
     * Require Permission.SETTINGS Permission, Require password to stop standBy if password has been set.
     * @return -1 robot is not ready.
     *          0 operation failed.
     *          1 operation succeed.
     *          2 for password required
     *          3 for wrong password
     *          403 SETTINGS permission required
     *          429 for too many requests, should be longer than 5 seconds between 2 calls
     */
    fun enableStandBy(enabled: Boolean, password: String = ""): Int {
        return sdkService?.enableStandBy(applicationInfo.packageName, enabled, password) ?: -1
    }

    /**
     * Get the supported latin keyboards
     *
     * @return
     */
    @CheckResult
    fun getSupportedLatinKeyboards(): Map<String, Boolean> {
        return try {
            @Suppress("UNCHECKED_CAST")
            sdkService?.supportedLatinKeyboards as Map<String, Boolean>
        } catch (e: RemoteException) {
            Log.e(TAG, "getSupportedLatinKeyboards() error")
            emptyMap()
        }
    }

    /**
     * Enabled latin keyboards
     *
     * @param keyboards should be from the keys of map by [getSupportedLatinKeyboards]
     *
     * And the first element will be the selected keyboard
     */
    fun enabledLatinKeyboards(keyboards: List<String>) {
        try {
            sdkService?.enabledLatinKeyboards(applicationInfo.packageName, keyboards)
        } catch (e: RemoteException) {
            Log.e(TAG, "enabledLatinKeyboards() error")
        }
    }

    /**
     * Check if the ground depth cliff detection is enabled.
     */
    @get:JvmName("isGroundDepthCliffDetectionEnabled")
    @get:CheckResult
    var groundDepthCliffDetectionEnabled: Boolean
        get() {
            return try {
                sdkService?.isGroundDepthCliffDetectionEnabled ?: false
            } catch (e: RemoteException) {
                Log.e(TAG, "isGroundDepthCliffDetectionEnabled() error")
                false
            }
        }
        /**
         * @param enabled Set `true` to enabled the ground depth cliff detection, `false` otherwise.
         */
        set(enabled) {
            try {
                sdkService?.setGroundDepthCliffDetectionEnabled(
                    applicationInfo.packageName,
                    enabled
                )
            } catch (e: RemoteException) {
                Log.e(TAG, "setGroundDepthCliffDetectionEnabled() error")
            }
        }

    /**
     * Check if the robot has the cliff sensor.
     */
    @CheckResult
    fun hasCliffSensor(): Boolean {
        return try {
            sdkService?.hasCliffSensor() ?: false
        } catch (e: RemoteException) {
            Log.e(TAG, "hasCliffSensor() error")
            false
        }
    }

    /**
     * Check and set the mode of cliff sensor, see [CliffSensorMode]
     */
    @get:CheckResult
    var cliffSensorMode: CliffSensorMode
        get() {
            var mode = try {
                sdkService?.cliffSensorMode ?: 0
            } catch (e: RemoteException) {
                Log.d(TAG, "getCliffSensorMode() error")
                0
            }
            if (mode < 0 || mode >= CliffSensorMode.values().size) {
                mode = 0
            }
            return CliffSensorMode.values()[mode]
        }
        set(mode) {
            try {
                sdkService?.setCliffSensorMode(applicationInfo.packageName, mode.ordinal)
            } catch (e: RemoteException) {
                Log.d(TAG, "setCliffSensorMode() error")
            }
        }

    /**
     * Get or set the sensitivity of head depth.
     */
    @get:CheckResult
    var headDepthSensitivity: SensitivityLevel
        get() {
            var level = try {
                sdkService?.headDepthSensitivity ?: 0
            } catch (e: RemoteException) {
                Log.d(TAG, "getHeadDepthSensitivity() error")
                0
            }
            if (level < 0 || level >= SensitivityLevel.values().size) {
                level = 0
            }
            return SensitivityLevel.values()[level]
        }
        set(sensitivityLevel) {
            try {
                sdkService?.setHeadDepthSensitivity(
                    applicationInfo.packageName,
                    sensitivityLevel.ordinal
                )
            } catch (e: RemoteException) {
                Log.d(TAG, "setHeadDepthSensitivity() error")
            }
        }

    /**
     * Check or set if the front TOF is enabled.
     */
    @get:JvmName("isFrontTOFEnabled")
    @get:CheckResult
    var frontTOFEnabled: Boolean
        get() {
            return try {
                sdkService?.isFrontTOFEnabled ?: true
            } catch (e: RemoteException) {
                Log.d(TAG, "isFrontTOFEnabled() error")
                true
            }
        }
        set(enabled) {
            try {
                sdkService?.setFrontTOFEnabled(applicationInfo.packageName, enabled)
            } catch (e: RemoteException) {
                Log.d(TAG, "setFrontTOFEnabled() error")
            }
        }

    /**
     * Check or set if the back TOF is enabled.
     * Require [Permission.SETTINGS] permission to change the value
     */
    @get:JvmName("isBackTOFEnabled")
    @get:CheckResult
    var backTOFEnabled: Boolean
        get() {
            return try {
                sdkService?.isBackTOFEnabled ?: true
            } catch (e: RemoteException) {
                Log.d(TAG, "isBackTOFEnabled() error")
                true
            }
        }
        set(enabled) {
            try {
                sdkService?.setBackTOFEnabled(applicationInfo.packageName, enabled)
            } catch (e: RemoteException) {
                Log.d(TAG, "setBackTOFEnabled() error")
            }
        }

    /**
     * Getter and setter of minimumObstacleDistance
     *
     * Range shall be 0 to 100, with step of 5.
     *
     *
     * If it is not supported in your robot version yet, value will be 0.
     *
     * Require [Permission.SETTINGS] permission to change the value
     */
    @get:JvmName("minimumObstacleDistance")
    @get:CheckResult
    var minimumObstacleDistance: Int
        get() {
            return try {
                sdkService?.configMinimumObstacleDistance(applicationInfo.packageName, -1) ?: 0
            } catch (e: RemoteException) {
                Log.d(TAG, "get MinimumObstacleDistance error")
                0
            }
        }
        set(value) {
            try {
                val ret = sdkService?.configMinimumObstacleDistance(applicationInfo.packageName, value)
                Log.d(TAG, "set MinimumObstacleDistance ret $ret")
            } catch (e: RemoteException) {
                Log.d(TAG, "set MinimumObstacleDistance error")
            }
        }

    @Throws(RemoteException::class)
    fun showNormalNotification(notification: NormalNotification) {
        if (sdkService != null) {
            sdkService!!.showNormalNotification(notification)
        } else {
            throw RemoteException("Sdk service is null.")
        }
    }

    @Throws(RemoteException::class)
    fun showAlertNotification(
        notification: AlertNotification,
        notificationListener: NotificationListener
    ) {
        if (sdkService != null) {
            sdkService!!.showAlertNotification(notification)
            listenersMap[notification.notificationId] = notificationListener
        } else {
            throw RemoteException("Sdk service is null.")
        }
    }

    @Throws(RemoteException::class)
    fun removeAlertNotification(notification: AlertNotification) {
        if (sdkService != null) {
            sdkService!!.removeAlertNotification(notification)
        } else {
            throw RemoteException("Sdk service is null.")
        }
    }

    @UiThread
    fun addOnPrivacyModeStateChangedListener(listener: OnPrivacyModeChangedListener) {
        onPrivacyModeStateChangedListeners.add(listener)
    }

    @UiThread
    fun removeOnPrivacyModeStateChangedListener(listener: OnPrivacyModeChangedListener) {
        onPrivacyModeStateChangedListeners.remove(listener)
    }

    @UiThread
    fun addOnBatteryStatusChangedListener(listener: OnBatteryStatusChangedListener) {
        onBatteryStatusChangedListeners.add(listener)
    }

    @UiThread
    fun removeOnBatteryStatusChangedListener(listener: OnBatteryStatusChangedListener) {
        onBatteryStatusChangedListeners.remove(listener)
    }

    @UiThread
    fun addOnDisabledFeatureListUpdatedListener(listener: OnDisabledFeatureListUpdatedListener) {
        onDisabledFeatureListUpdatedListeners.add(listener)
    }

    @UiThread
    fun removeOnDisabledFeatureListUpdatedListener(listener: OnDisabledFeatureListUpdatedListener) {
        onDisabledFeatureListUpdatedListeners.remove(listener)
    }

    @UiThread
    fun addOnGreetModeStateChangedListener(listener: OnGreetModeStateChangedListener) {
        onGreetModeStateChangedListeners.add(listener)
    }

    @UiThread
    fun removeOnGreetModeStateChangedListener(listener: OnGreetModeStateChangedListener) {
        onGreetModeStateChangedListeners.remove(listener)
    }

    /*****************************************/
    /*               Kiosk Mode              */
    /*****************************************/

    /**
     * Is current skill a Kiosk Mode skill.
     *
     * @return Value of the Metadata Kiosk.
     */
    private val isMetaDataKiosk: Boolean
        get() = applicationInfo.metaData != null
                && applicationInfo.metaData.getBoolean(SdkConstants.METADATA_KIOSK, false)

    /**
     * Toggle the wakeup trigger on and off.
     *
     * @param disabled Set true to disable the wakeup or false to enable it.
     */
    fun toggleWakeup(disabled: Boolean) {
        if (!isMetaDataKiosk) {
            Log.e(TAG, "Wakeup can only be toggled in Kiosk Mode")
            sdkServiceCallback.onSdkError(SdkException.permissionDenied("Kiosk Mode"))
            return
        }
        try {
            sdkService?.toggleWakeup(disabled, applicationInfo.packageName)
        } catch (e: RemoteException) {
            Log.e(TAG, "toggleWakeup() error")
        }
    }

    val wakeupWordDisabled: Boolean
        @CheckResult
        @JvmName("isWakeupDisabled")
        get() {
            try {
                return sdkService?.isWakeupDisabled ?: false
            } catch (e: RemoteException) {
                Log.e(TAG, "isWakeupDisabled() error")
            }
            return false
        }

    /**
     * Toggle the visibility of the navigation billboard when you perform goTo commands.
     *
     * @param disabled Set true to disable the billboard or false to enable it.
     */
    fun toggleNavigationBillboard(disabled: Boolean) {
        if (!isMetaDataKiosk) {
            Log.e(TAG, "Billboard can only be toggled in Kiosk Mode")
            sdkServiceCallback.onSdkError(SdkException.permissionDenied("Kiosk Mode"))
            return
        }
        try {
            sdkService?.setGoToBillboardDisabled(applicationInfo.packageName, disabled)
        } catch (e: RemoteException) {
            Log.e(TAG, "toggleNavigationBillboard() error")
        }
    }

    @get:JvmName("isNavigationBillboardDisabled")
    val navigationBillboardDisabled: Boolean
        @CheckResult
        get() {
            try {
                return sdkService?.isGoToBillboardDisabled ?: false
            } catch (e: RemoteException) {
                Log.e(TAG, "isNavigationBillboardDisabled() error")
            }
            return false
        }

    /**
     * Request to be the selected Kiosk Mode App programmatically.
     */
    fun requestToBeKioskApp() {
        if (!isMetaDataKiosk) {
            sdkServiceCallback.onSdkError(SdkException.permissionDenied("Kiosk Mode"))
            return
        }
        try {
            sdkService?.requestToBeKioskApp(applicationInfo.packageName)
        } catch (e: RemoteException) {
            Log.e(TAG, "requestToBeKioskApp() error")
        }
    }

    /**
     * Whether this App is selected as the Kiosk Mode App.
     */
    @CheckResult
    fun isSelectedKioskApp(): Boolean {
        if (!isMetaDataKiosk) {
            sdkServiceCallback.onSdkError(SdkException.permissionDenied("Kiosk Mode"))
            return false
        }
        try {
            return sdkService?.isSelectedKioskApp(applicationInfo.packageName) ?: false
        } catch (e: RemoteException) {
            Log.e(TAG, "isSelectedKioskApp() error")
        }
        return false
    }

    /**
     * @param mode, added in 134. When turning Kiosk mode off, assign a home screen mode to be set.
     *  if target launcher doesn't support this param yet, it will set to [HomeScreenMode.DEFAULT] mode.
     *  If set to [HomeScreenMode.URL], but no URL is set, it will fallback to [HomeScreenMode.DEFAULT] mode.
     *  Cannot turn off Kiosk Mode and set to [HomeScreenMode.APPLICATION] mode, will fallback to default mode
     */
    @JvmOverloads
    fun setKioskModeOn(on: Boolean = true, mode: HomeScreenMode = HomeScreenMode.DEFAULT) {
        if (!isMetaDataKiosk) {
            sdkServiceCallback.onSdkError(SdkException.permissionDenied("Kiosk Mode"))
            return
        }
        try {
            sdkService?.setKioskModeOn(applicationInfo.packageName, on, mode.name)
        } catch (e: RemoteException) {
            Log.e(TAG, "setKioskModeOn() error")
        }
    }

    @CheckResult
    fun isKioskModeOn(): Boolean {
        return try {
            sdkService?.isKioskModeOn ?: false
        } catch (e: RemoteException) {
            Log.e(TAG, "isKioskModeOn() error")
            false
        }
    }

    /**
     * Get current home screen mode
     * @return current [HomeScreenMode]
     *         or null if failed to get the mode
     */
    @CheckResult
    fun getHomeScreenMode() : HomeScreenMode? {
        return try {
            val homeScreenMode = sdkService?.getHomeScreenMode(applicationInfo.packageName)
            return HomeScreenMode.getHomeScreenMode(homeScreenMode)
        } catch (e: RemoteException) {
            Log.e(TAG, "getHomeScreenMode() error")
            null
        }
    }

    /**
     * Set interaction state as ON, to keep greet mode under interaction state.
     *
     * In greet mode, if there is active movement/conversation/detection/sequence/telepresence/touch,
     * then greet mode will take it as user interaction and hold it in [OnGreetModeStateChangedListener.INTERACTION] state.
     * But if the selected kiosk app wants to play a video in the interaction state of greet mode even none of above conditions is met,
     * then this function will set the interaction flag as true, and keep it as interaction state, until the flag is cleared.
     *
     * This function shall be used together with [OnGreetModeStateChangedListener] to fine control greet mode state transition.
     *
     * The flag will be cleared when
     * <ul>
     *   <li> Kiosk mode is OFF.
     *   <li> Kiosk app is changed.
     *   <li> Greet mode is OFF.
     *   <li> Current kiosk app set it as false
     * </ul>
     *
     * Require active Kiosk app permission to use this method
     *
     * @param on, true to hold it as interaction state.
     *
     * @return request result
     * <ul>
     *   <li> -1 for failed to request, maybe robot is not ready
     *   <li> 0 for request succeed
     *   <li> 403 for current app is not selected Kiosk app
     * </ul>
     */
    fun setInteractionState(on: Boolean): Int {
        return try {
            sdkService?.setInteractionState(applicationInfo.packageName, on) ?: -1
        } catch (e: RemoteException) {
            Log.e(TAG, "setInteractionState($on) error")
            -1
        }
    }

    /*****************************************/
    /*            Activity Stream            */
    /*****************************************/

    fun setActivityStreamPublishListener(activityStreamPublishListener: ActivityStreamPublishListener?) {
        this.activityStreamPublishListener = activityStreamPublishListener
    }

    @Throws(RemoteException::class)
    fun shareActivityObject(activityStreamObject: ActivityStreamObject) {
        sdkService?.let {
            thread {
                ActivityStreamUtils.handleActivityStreamObject(activityStreamObject)
                try {
                    it.shareActivityStreamObject(activityStreamObject)
                } catch (e: RemoteException) {
                    Log.e(TAG, "Sdk service is null")
                }
            }
        }
    }

    /*****************************************/
    /*                 Media                 */
    /*****************************************/

    @Deprecated("No longer supported")
    fun setMediaButtonListener(mediaButtonListener: MediaButtonListener) {
        this.mediaButtonListener = mediaButtonListener
    }

    @Deprecated("No longer supported")
    fun removeMediaButtonListener() {
        mediaButtonListener = null
    }

    @Deprecated("No longer supported")
    @Throws(RemoteException::class)
    fun updateMediaBar(mediaBarData: MediaBarData) {
        mediaBarData.packageName = applicationInfo.packageName
        mediaBar.updateMediaBar(mediaBarData)
    }

    @Deprecated("No longer supported")
    @Throws(RemoteException::class)
    fun pauseMediaBar() {
        mediaBar.pauseMediaBar()
    }

    @Deprecated("No longer supported")
    @Throws(RemoteException::class)
    fun setMediaPlaying(isPlaying: Boolean) {
        mediaBar.setMediaPlaying(isPlaying, applicationInfo.packageName)
    }

    /*****************************************/
    /*             Detection Mode            */
    /*****************************************/

    @UiThread
    fun addOnUserInteractionChangedListener(listener: OnUserInteractionChangedListener) {
        onUserInteractionChangedListeners.add(listener)
    }

    @UiThread
    fun removeOnUserInteractionChangedListener(listener: OnUserInteractionChangedListener) {
        onUserInteractionChangedListeners.remove(listener)
    }

    @UiThread
    fun addOnDetectionStateChangedListener(listener: OnDetectionStateChangedListener) {
        onDetectionStateChangedListeners.add(listener)
    }

    @Deprecated(
        "Use removeOnDetectionStateChangedListener(listener) instead.",
        ReplaceWith("this.removeOnDetectionStateChangedListener(listener)"),
        DeprecationLevel.WARNING
    )
    @UiThread
    fun removeDetectionStateChangedListener(listener: OnDetectionStateChangedListener) {
        onDetectionStateChangedListeners.remove(listener)
    }

    @UiThread
    fun removeOnDetectionStateChangedListener(listener: OnDetectionStateChangedListener) {
        onDetectionStateChangedListeners.remove(listener)
    }

    @UiThread
    fun addOnDetectionDataChangedListener(listener: OnDetectionDataChangedListener) {
        onDetectionDataChangedListeners.add(listener)
    }

    @UiThread
    fun removeOnDetectionDataChangedListener(listener: OnDetectionDataChangedListener) {
        onDetectionDataChangedListeners.remove(listener)
    }

    /*****************************************/
    /*               Permission              */
    /*****************************************/

    /**
     * Check permission grant status.
     *
     * @param permission The [Permission] you want to check.
     * @return Grant status. See [com.robotemi.sdk.permission.Permission.PermissionResult].
     */
    @Permission.PermissionResult
    @CheckResult
    fun checkSelfPermission(permission: Permission): Int {
        if (permission.isKioskPermission && !isMetaDataKiosk) {
            Log.w(TAG, "Only Kiosk App may have kiosk permissions")
            sdkServiceCallback.onSdkError(SdkException.permissionDenied("Kiosk Mode"))
            return Permission.DENIED
        }
        try {
            return sdkService?.checkSelfPermission(applicationInfo.packageName, permission.value)
                ?: Permission.DENIED
        } catch (e: RemoteException) {
            Log.e(TAG, "checkSelfPermission() error")
        }
        return Permission.DENIED
    }

    /**
     * Request permissions.
     *
     * If you already had the permission, Launcher will not handle this request again.
     *
     * Add [OnRequestPermissionResultListener] to listen the request result.
     *
     * @param permissions A list holds the permissions you want to request.
     * @param requestCode Identify which request.
     */
    fun requestPermissions(permissions: List<Permission>, requestCode: Int) {
        val permissionsFromMetadata =
            applicationInfo.metaData?.getString(SdkConstants.METADATA_PERMISSIONS)
        if (permissionsFromMetadata.isNullOrBlank()) {
            Log.w(TAG, "There is no valid permission in metadata")
            return
        }

        val validPermissions = mutableListOf<String>()
        for (permission in permissions) {
            if (!permissionsFromMetadata.contains(permission.value)) {
                Log.w(TAG, "This permission $permission is not declared in AndroidManifest.xml")
                continue
            }
            validPermissions.add(permission.value)
        }

        if (validPermissions.isEmpty()) {
            Log.w(TAG, "There is no valid permission in permissions")
            return
        }

        try {
            sdkService?.requestPermissions(
                applicationInfo.packageName,
                validPermissions,
                requestCode
            )
        } catch (e: RemoteException) {
            Log.e(TAG, "requestPermissions() error")
        }
    }

    @UiThread
    fun addOnRequestPermissionResultListener(listener: OnRequestPermissionResultListener) {
        onRequestPermissionResultListeners.add(listener)
    }

    @UiThread
    fun removeOnRequestPermissionResultListener(listener: OnRequestPermissionResultListener) {
        onRequestPermissionResultListeners.remove(listener)
    }

    /*****************************************/
    /*                Sequence               */
    /*****************************************/

    /**
     * Fetch all sequences user created on the Web platform.
     *
     * @return List holds all sequences.
     */
    @WorkerThread
    @CheckResult
    @JvmOverloads
    fun getAllSequences(tags: List<String> = emptyList()): List<SequenceModel> {
        return try {
            (sdkService?.getAllSequences(applicationInfo.packageName, tags.filter { it != "" })
                ?: emptyList<SequenceModel>()).map { it.compatible() }
        } catch (e: RemoteException) {
            Log.e(TAG, "getAllSequences() error")
            emptyList()
        }
    }

    /**
     * Play sequence by sequence ID.
     *
     * @param sequenceId Sequence ID you want to play.
     * @param withPlayer Whether to play sequence with the player panel.
     * @param repeat How many times will this sequence be played after the first playing.
     */
    @JvmOverloads
    fun playSequence(sequenceId: String, withPlayer: Boolean = false, repeat: Int = 0) {
        try {
            sdkService?.playSequence(applicationInfo.packageName, sequenceId, withPlayer, repeat)
        } catch (e: RemoteException) {
            Log.e(TAG, "playSequence() error")
        }
    }

    /**
     * Control current playing sequence.
     *
     * @param sequenceCommand Operations for controlling sequence, see [SequenceCommand].
     */
    fun controlSequence(sequenceCommand: SequenceCommand) {
        try {
            sdkService?.controlSequence(applicationInfo.packageName, sequenceCommand.ordinal)
        } catch (e: RemoteException) {
            Log.e(TAG, "controlSequence() error")
        }
    }

    /*****************************************/
    /*                  Tour                 */
    /*****************************************/

    /**
     * Fetch all tours user created on the Web platform. Require [Permission.SEQUENCE]
     *
     * @return List holds all tours.
     */
    @WorkerThread
    @CheckResult
    @JvmOverloads
    fun getAllTours(tags: List<String> = emptyList()): List<TourModel> {
        return try {
            (sdkService?.getAllTours(applicationInfo.packageName, tags.filter { it != "" })
                ?: emptyList<TourModel>()).map { it.compatible() }
        } catch (e: RemoteException) {
            Log.e(TAG, "getAllTours() error")
            emptyList()
        }
    }

    /**
     * Play tour by tour ID. Require [Permission.SEQUENCE]
     *
     * @param tourId Tour ID you want to play.
     */
    fun playTour(tourId: String): Int {
        try {
            return sdkService?.playTour(applicationInfo.packageName, tourId) ?: -1
        } catch (e: RemoteException) {
            Log.e(TAG, "playTour() error")
            return -1
        }
    }

    @UiThread
    fun addOnSequencePlayStatusChangedListener(listener: OnSequencePlayStatusChangedListener) {
        onSequencePlayStatusChangedListeners.add(listener)
    }

    @UiThread
    fun removeOnSequencePlayStatusChangedListener(listener: OnSequencePlayStatusChangedListener) {
        onSequencePlayStatusChangedListeners.remove(listener)
    }

    /*****************************************/
    /*                  Map                  */
    /*****************************************/

    /**
     * Get map data.
     *
     * @return Map data.
     */
    @Nullable
    @WorkerThread
    @CheckResult
    fun getMapData(): MapDataModel? {
        val gson = Gson()
        var mapDataModel: MapDataModel? = null
        val inputStream =
            getInputStreamByMediaKey(ContentType.MAP_DATA_IMAGE, "") ?: return null
        val inputStreamReader = InputStreamReader(inputStream)

        var cursor: Cursor? = null
        try {
            val json = gson.fromJson(
                inputStreamReader,
                MapDataModel::class.java
            )
            mapDataModel = MapDataModel(
                mapImage = json.mapImage,
                mapName = json.mapName ?: ""
            )
            val uriStr = StringBuffer("content://")
                .append(SdkConstants.PROVIDER_AUTHORITY)
                .append("/").append(SdkConstants.PROVIDER_PARAMETER_MAP_DATA)
                .toString()
            cursor = context.contentResolver.query(
                Uri.parse(uriStr),
                null,
                null,
                null,
                null
            )
            if (cursor == null || !cursor.moveToFirst()) {
                return mapDataModel
            }
            val mapId = cursor.getString(cursor.getColumnIndexOrThrow(MAP_ID))
            val mapInfoJson = cursor.getString(cursor.getColumnIndexOrThrow(MAP_INFO))
            val mapElementsJson = cursor.getString(cursor.getColumnIndexOrThrow(MAP_ELEMENTS))
            val mapInfo = gson.fromJson(mapInfoJson, MapInfo::class.java)
            val mapElements = gson.fromJson<List<Layer>>(
                mapElementsJson,
                object : TypeToken<List<Layer>>() {}.type
            )

            val mapName = try {
                cursor.getString(cursor.getColumnIndexOrThrow(MAP_NAME))
            } catch (e: IllegalArgumentException) {
                ""
            }
            mapDataModel.mapId = mapId
            mapDataModel.mapInfo = mapInfo
            mapDataModel.mapName = mapName
            mapElements?.map {
                if (it.layerCategory == VIRTUAL_WALL) mapDataModel.virtualWalls.add(it)
                if (it.layerCategory == GREEN_PATH) mapDataModel.greenPaths.add(it)
                if (it.layerCategory == LOCATION) mapDataModel.locations.add(it)
                if (it.layerCategory == MAP_ERASER) mapDataModel.mapEraser.add(it)
            }
        } catch (e: JsonParseException) {
            Log.e(TAG, "getMapData() - JSON parse error: ${e.message}")
        } finally {
            cursor?.close()
            try {
                inputStream.close()
                inputStreamReader.close()
            } catch (e: IOException) {
                Log.e(TAG, "getMapData() - ${e.message}")
            }
        }
        return mapDataModel
    }

    /**
     * Get map elements, such as locations, virtual walls, green paths, etc.
     *
     * @return Map elements.
     */
    fun getMapElements(): List<Layer>? {
        if (checkSelfPermission(Permission.MAP) == Permission.DENIED) {
            Log.e(TAG, "getMapElements() - Permission denied")
            return null
        }
//        if (isMapLocked() == true) return sdkService?.getMapElements(applicationInfo.packageName)
        var cursor: Cursor? = null
        val uriStr = StringBuffer("content://")
            .append(SdkConstants.PROVIDER_AUTHORITY)
            .append("/").append(SdkConstants.PROVIDER_PARAMETER_MAP_DATA)
            .toString()
        cursor = context.contentResolver.query(
            Uri.parse(uriStr),
            arrayOf(MAP_ELEMENTS),
            null,
            null,
            null
        )
        if (cursor == null || !cursor.moveToFirst()) {
            return null
        }
        val mapElementsJson = cursor.getString(cursor.getColumnIndexOrThrow(MAP_ELEMENTS))
        cursor.close()
        return gson.fromJson<List<Layer>>(
            mapElementsJson,
            object : TypeToken<List<Layer>>() {}.type
        )
    }

    /**
     * Get map image and map info.
     *
     * @return Map data model with only map image and map info
     */
    fun getMapImage(): MapDataModel? {
        if (checkSelfPermission(Permission.MAP) == Permission.DENIED) {
            Log.e(TAG, "getMapElements() - Permission denied")
            return null
        }
//        val gson = Gson()
//        val inputStream =
//            getInputStreamByMediaKey(ContentType.MAP_DATA_IMAGE, "") ?: return null
//        val inputStreamReader = InputStreamReader(inputStream)
//        var mapDataModel: MapDataModel? = null
//        try {
//            mapDataModel = gson.fromJson(
//                inputStreamReader,
//                MapDataModel::class.java
//            )
//        } catch (e: JsonParseException) {
//            Log.e(TAG, "getMapImage() - JSON parse error: ${e.message}")
//        } finally {
//            try {
//                inputStream.close()
//                inputStreamReader.close()
//            } catch (e: IOException) {
//                Log.e(TAG, "getMapImage() - ${e.message}")
//            }
//        }
//        return mapDataModel
        var cursor: Cursor? = null
        val uriStr = StringBuffer("content://")
            .append(SdkConstants.PROVIDER_AUTHORITY)
            .append("/").append(SdkConstants.PROVIDER_PARAMETER_MAP_DATA)
            .toString()
        cursor = context.contentResolver.query(
            Uri.parse(uriStr),
            arrayOf(MAP_IMAGE),
            null,
            null,
            null
        )
        if (cursor == null || !cursor.moveToFirst()) {
            return null
        }
        val mapImageJson = cursor.getString(cursor.getColumnIndexOrThrow(MAP_IMAGE))
        val mapInfoJson = cursor.getString(cursor.getColumnIndexOrThrow(MAP_INFO))
        val mapDataBase64 = cursor.getString(cursor.getColumnIndexOrThrow(MAP_BASE64))

        cursor.close()

        val mapImage = gson.fromJson(
            mapImageJson,
            MapImage::class.java
        )
        val mapInfo = gson.fromJson(mapInfoJson, MapInfo::class.java)

        val decoded = Base64.decode(mapDataBase64, Base64.NO_WRAP)
        val unzipped =
            GZIPInputStream(decoded.inputStream()).bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
        val realData = Base64.decode(unzipped, Base64.NO_WRAP).map { it.toInt() }

        return MapDataModel(
            mapImage = mapImage.copy(data = realData),
            mapInfo = mapInfo,
        )
    }

    /**
     * Check if the map is lost.
     *
     * @return true if the map is lost, false otherwise.
     */
    fun isMapLost(): Boolean? {
        try {
            if (sdkService?.isMapLost == NOT_SET) {
                Log.e(TAG, "this launcher version doesn't support isMapLost() yet")
                return null
            }
            return sdkService?.isMapLost == TRUE
        } catch (e: RemoteException) {
            Log.e(TAG, "isMapLost() error")
            return null
        }
    }

    /**
     * Check if the map is locked.
     *
     * @return true if the map is locked, false otherwise.
     */
    fun isMapLocked(): Boolean? {
        try {
            if (sdkService?.isMapLocked == NOT_SET) {
                Log.e(TAG, "this launcher version doesn't support isMapLocked() yet")
                return null
            }
            return sdkService?.isMapLocked == TRUE
        } catch (e: RemoteException) {
            Log.e(TAG, "isMapLocked() error")
            return null
        }
    }

    /**
     * Get current map backup file
     */
    fun getCurrentMapBackupFile(withoutUI: Boolean = false): ParcelFileDescriptor? {
        return getInputStreamPipe(ContentType.MAP_BACKUP, withoutUI)
    }

    /**
     * Get map list from server.
     *
     * @return Saved map
     */
    @WorkerThread
    fun getMapList(): List<MapModel> {
        try {
            return sdkService?.getMapList(applicationInfo.packageName) ?: emptyList()
        } catch (e: RemoteException) {
            Log.e(TAG, "getMapList() error")
        }
        return emptyList()
    }

    /**
     * Load map by map ID.
     * The result is broadcast from onLoadMapStatusChanged
     *
     * @param mapId The map ID of the map to be loaded
     * @param reposeRequired If needs to repose after loading map, default as false
     * @param position The position for repose
     * @param offline Skip fetching the latest map data of target mapId, default as false.
     * @param withoutUI Load the map in the background without showing any blocking UI, default as false.
     * @return Request id. In the format of UUID, e.g. 538b44c9-fdcf-426a-9693-d72e9c0f9550. Used in onLoadMapStatusChanged callback.
     */
    @JvmOverloads
    fun loadMap(
        mapId: String,
        reposeRequired: Boolean = false,
        position: Position? = null,
        offline: Boolean = false,
        withoutUI: Boolean = false
    ): String {

        return try {
            if (position == null) {
                sdkService?.loadMap(
                    applicationInfo.packageName,
                    mapId,
                    reposeRequired,
                    offline,
                    withoutUI
                ) ?: ""
            } else {
                sdkService?.loadMapWithPosition(
                    applicationInfo.packageName,
                    mapId,
                    reposeRequired,
                    position,
                    offline,
                    withoutUI
                ) ?: ""
            }
        } catch (e: RemoteException) {
            Log.e(TAG, "loadMap() error")
            ""
        }
    }

    /**
     * Load map to cache for offline use.
     *
     * @param mapId The map ID of the map to be loaded into cache
     * @return RequestId
     */
    fun loadMapToCache(mapId: String): String {
        return try {
            sdkService?.loadMapToCache(
                applicationInfo.packageName,
                mapId
            ) ?: ""
        } catch (e: RemoteException) {
            Log.e(TAG, "loadMap() error")
            ""
        }
    }

    /**
     * Multi-floor.
     */
    fun isMultiFloorEnabled(): Boolean? {
        return sdkService?.isMultiFloorEnabled()
    }

    /**
     * Multi-floor function.
     *
     * Require [Permission.MAP] and [Permission.SETTINGS].
     *
     * @return true on success.
     */
    fun setMultiFloorEnabled(enabled: Boolean): Boolean {
        return try {
            Log.d(TAG, "package ${applicationInfo.packageName}")
            sdkService?.setMultiFloorEnabled(applicationInfo.packageName, enabled) ?: false
        } catch (e: RemoteException) {
            Log.e(TAG, "setMultiFloorEnabled() error")
            false
        }
    }

    fun getCurrentFloor(): Floor? {
        return try {
            sdkService?.getCurrentFloor(applicationInfo.packageName)
        } catch (e: RemoteException) {
            Log.e(TAG, "getCurrentFloor() error")
            null
        }
    }

    fun getAllFloors(): List<Floor> {
        return try {
            sdkService?.getAllFloors(applicationInfo.packageName) ?: emptyList()
        } catch (e: RemoteException) {
            Log.e(TAG, "getAllFloors() error")
            emptyList()
        }
    }

    fun loadFloor(floorId: Int, position: Position) {
        try {
            sdkService?.loadFloor(applicationInfo.packageName, floorId, position)
        } catch (e: RemoteException) {
            Log.e(TAG, "loadFloor() error")
        }
    }

    /**
     * Reset map.
     *      Note this method can be invoked out of home base, and will build a map without home base.
     *      This is usefully when multi-floor is enabled, it can still save home base after move temi onto home base manually.
     * Require [Permission.MAP] permission
     * Supported from 134 launcher.
     *
     * @param allFloor, true to reset maps of all floor, false to reset current map. Used in multi-floor.
     * @return 0 if the operation is not supported by current launcher
     *         200 for reset map succeed
     *         400 for invalid action
     *         403 for [Permission.MAP] permission required
     *         408 for operation timeout
     *
     */
    @WorkerThread
    fun resetMap(allFloor: Boolean): Int {
        try {
            val resp = sdkService?.resetMap(applicationInfo.packageName, allFloor)?.toIntOrNull() ?: 0
            Log.d(TAG, "resetMap $allFloor, result $resp")
            return resp
        } catch (e: RemoteException) {
            Log.e(TAG, "resetMap() error")
        }
        return 0
    }

    /**
     * Finish mapping, map will no longer update after finish mapping
     * This operation may take several seconds or even longer.
     *
     * Require [Permission.MAP] permission
     * Supported from 134 launcher.
     *
     * @param mapName, map name give to the map, optional.
     * @return 0 if the operation is not supported by current launcher
     *         200 for success
     *         304 for mapping has been already finished
     *         400 for invalid action
     *         403 for [Permission.MAP] permission required
     *         408 for operation timeout
     */
    @WorkerThread
    @JvmOverloads
    fun finishMapping(mapName: String? = null): Int {
        try {
            val resp = sdkService?.finishMapping(applicationInfo.packageName, mapName)?.toIntOrNull() ?: 0
            Log.d(TAG, "finishMapping $mapName, result $resp")
            return resp
        } catch (e: RemoteException) {
            Log.e(TAG, "finishMapping() error")
        }
        return 0
    }

    /**
     * Update current map name
     * Require [Permission.MAP] permission
     * Supported from 134 launcher.
     *
     * @param mapName, map name give to current map.
     * @return 0 if the operation is not supported by current launcher
     *         200 if the map name is set.
     *         400 for invalid action
     *         403 for [Permission.MAP] permission required
     *         429 for too many requests, wait for 2 seconds
     */
    fun updateMapName(mapName: String): Int {
        try {
            val resp = sdkService?.updateMapName(applicationInfo.packageName, mapName)?.toIntOrNull() ?: 0
            Log.d(TAG, "updateMapName $mapName, result $resp")
            return resp
        } catch (e: RemoteException) {
            Log.e(TAG, "updateMapName() error")
        }
        return 0
    }

    /**
     * Unlock the map and continue mapping.
     * Require [Permission.MAP] permission
     * Supported from 134 launcher.
     *
     * @return 0 if the operation is not supported by current launcher
     *         200 for success.
     *         304 for map already unlocked
     *         400 for invalid action
     *         403 for [Permission.MAP] permission required
     *         408 for operation timeout
     *         429 for too many requests, wait for 5 seconds
     */
    @WorkerThread
    fun continueMapping(): Int {
        try {
            val resp = sdkService?.continueMapping(applicationInfo.packageName)?.toIntOrNull() ?: 0
            Log.d(TAG, "continueMapping, result $resp")
            return resp
        } catch (e: RemoteException) {
            Log.e(TAG, "continueMapping() error")
        }
        return 0
    }

    /**
     * Update existing or insert new layer
     * If layerId exists then it will update current layer.
     * If not, then a new layer will be created based on given data.
     *
     * Require [Permission.MAP] permission
     * Supported from 134 launcher.
     *
     * @param layer, layer data to be updated or inserted. Use [Layer.upsertLayer] to create the layer
     *
     * @return 0 if the operation is not supported by current launcher
     *         200 for success
     *         400 invalid parameter
     *         403 for [Permission.MAP] permission required
     *         413 pose out of map
     *
     */
    @WorkerThread
    fun upsertMapLayer(layer: Layer): Int {
        try {
            val resp = sdkService?.upsertMapLayer(
                applicationInfo.packageName,
                gson.toJson(layer.roundByCategory())
            )?.toIntOrNull() ?: 0
            Log.d(TAG, "upsertLayer, result $resp")
            return resp
        } catch (e: RemoteException) {
            Log.e(TAG, "upsertLayer() error")
        }
        return 0
    }

    /**
     * Delete map layer, only support deleting virtual wall and path
     *
     * @param layerCategory, can only take [GREEN_PATH] and [VIRTUAL_WALL]
     *
     * @return 0 if the operation is not supported by current launcher
     *         200 for success
     *         400 invalid parameter
     *         403 for [Permission.MAP] permission required
     *         404 target map layer doesn't exist
     */
    @WorkerThread
    fun deleteMapLayer(layerId: String, layerCategory: Int): Int {
        try {
            val resp = sdkService?.deleteMapLayer(applicationInfo.packageName, layerId, layerCategory)?.toIntOrNull() ?: 0
            Log.d(TAG, "deleteMapLayer, result $resp")
            return resp
        } catch (e: RemoteException) {
            Log.e(TAG, "deleteMapLayer() error")
        }
        return 0
    }

    @UiThread
    fun addOnLoadMapStatusChangedListener(listener: OnLoadMapStatusChangedListener) {
        onLoadMapStatusChangedListeners.add(listener)
    }

    @UiThread
    fun removeOnLoadMapStatusChangedListener(listener: OnLoadMapStatusChangedListener) {
        onLoadMapStatusChangedListeners.remove(listener)
    }

    @UiThread
    fun addOnLoadFloorStatusChangedListener(listener: OnLoadFloorStatusChangedListener) {
        onLoadFloorStatusChangedListeners.add(listener)
    }

    @UiThread
    fun removeOnLoadFloorStatusChangedListener(listener: OnLoadFloorStatusChangedListener) {
        onLoadFloorStatusChangedListeners.remove(listener)
    }

    @UiThread
    fun addOnMapStatusChangedListener(listener: OnMapStatusChangedListener) {
        onMapStatusChangedListeners.add(listener)
    }

    @UiThread
    fun removeOnMapStatusChangedListener(listener: OnMapStatusChangedListener) {
        onMapStatusChangedListeners.remove(listener)
    }

    @UiThread
    fun addOnMapElementsChangedListener(listener: OnMapElementsChangedListener) {
        onMapElementsChangedListeners.add(listener)
    }

    @UiThread
    fun removeOnMapElementsChangedListener(listener: OnMapElementsChangedListener) {
        onMapElementsChangedListeners.remove(listener)
    }

    @UiThread
    fun addOnMapNameChangedListener(listener: OnMapNameChangedListener) {
        onMapNameChangedListeners.add(listener)
    }

    @UiThread
    fun removeOnMapNameChangedListener(listener: OnMapNameChangedListener) {
        onMapNameChangedListeners.remove(listener)
    }

    /*****************************************/
    /*            Face Recognition           */
    /*****************************************/

    /**
     * Start face recognition.
     *
     * @param withSdkFaces, Whether to include faces registered from your own app.
     *                      Supported from 131 version. You can register faces from SDK.
     *                      Check https://github.com/robotemi/sdk/wiki/temi-Center#face-recognition on how to do it.
     */
    fun startFaceRecognition(withSdkFaces: Boolean = false) {
        try {
            sdkService?.startFaceRecognition(applicationInfo.packageName, withSdkFaces)
        } catch (e: RemoteException) {
            Log.e(TAG, "startFaceRecognition() error")
        }
    }

    /**
     * Stop face recognition.
     */
    fun stopFaceRecognition() {
        try {
            sdkService?.stopFaceRecognition(applicationInfo.packageName)
        } catch (e: RemoteException) {
            Log.e(TAG, "stopFaceRecognition() error")
        }
    }

    @UiThread
    fun addOnFaceRecognizedListener(listener: OnFaceRecognizedListener) {
        onFaceRecognizedListeners.add(listener)
    }

    @UiThread
    fun removeOnFaceRecognizedListener(listener: OnFaceRecognizedListener) {
        onFaceRecognizedListeners.remove(listener)
    }

    @UiThread
    fun addOnContinuousFaceRecognizedListener(listener: OnContinuousFaceRecognizedListener) {
        onContinuousFaceRecognizedListeners.add(listener)
    }

    @UiThread
    fun removeOnContinuousFaceRecognizedListener(listener: OnContinuousFaceRecognizedListener) {
        onContinuousFaceRecognizedListeners.remove(listener)
    }

    /*****************************************/
    /*                 Serial                */

    /**
     * Send command to control hardware components on temi GO.
     *
     * This is the core api, which exposed the fundamental communication protocol to hardware control board.
     *
     */
    fun sendSerialCommand(command: Int, data: ByteArray): Int {
        return try {
            sdkService?.sendSerialCommand(command, data) ?: -1
        } catch (e: RemoteException) {
            Log.e(TAG, "sendSerialCommand() error")
            -1
        }
    }

    @UiThread
    fun addOnSerialRawDataListener(listener: OnSerialRawDataListener) {
        onSerialRawDataListeners.add(listener)
    }

    @UiThread
    fun removeOnSerialRawDataListener(listener: OnSerialRawDataListener) {
        onSerialRawDataListeners.remove(listener)
    }

    @UiThread
    fun addOnRobotDragStateChangedListener(listener: OnRobotDragStateChangedListener) {
        onRobotDragStateChangedListeners.add(listener)
    }

    @UiThread
    fun removeOnRobotDragStateChangedListener(listener: OnRobotDragStateChangedListener) {
        onRobotDragStateChangedListeners.remove(listener)
    }

    /*****************************************/
    /*                 Common                */
    /*****************************************/

    @Nullable
    @WorkerThread
    fun getInputStreamByMediaKey(contentType: ContentType, mediaKey: String): InputStream? {
        val uriStr = StringBuffer("content://")
            .append(SdkConstants.PROVIDER_AUTHORITY)
            .append("/").append(contentType.path)
            .append("?").append(SdkConstants.PROVIDER_PARAMETER_MEDIA_KEY)
            .append("=").append(mediaKey)
            .toString()
        try {
            return context.contentResolver.openInputStream(Uri.parse(uriStr))
        } catch (e: FileNotFoundException) {
            Log.e(TAG, e.message ?: "")
            sdkServiceCallback.onSdkError(SdkException.launcherError("No such file exists"))
        } catch (e: IllegalStateException) {
            Log.e(TAG, "getInputStreamByMediaKey error.\n ${e.message}")
        }
        return null
    }

    @Nullable
    @WorkerThread
    private fun getInputStreamPipe(contentType: ContentType, mediaKey: Boolean): ParcelFileDescriptor? {
        val uriStr = StringBuffer("content://")
            .append(SdkConstants.PROVIDER_AUTHORITY)
            .append("/").append(contentType.path)
            .append("?").append(SdkConstants.PROVIDER_PARAMETER_MEDIA_KEY)
            .append("=").append(mediaKey)
            .toString()

        val descriptor = context.contentResolver.openFileDescriptor(Uri.parse(uriStr), "r") ?: return null

        Log.d("Map-SDK", "Got descriptor, $descriptor")
        return descriptor
    }

    /**
     * Load map from a map back up file.
     * It is like [loadMap], but this one will take the map file provided by SDK clients.
     * Currently 2 types of scheme are supported, content:// and file://
     * Please take a look in MapActivity of the sample app on how to pass map file to temi launcher.
     *
     * It is safe to delete the source map file after this method completes.
     */
    fun loadMapWithBackupFile(
        uri: Uri,
        reposeRequired: Boolean = false,
        position: Position? = null,
        withoutUI: Boolean = false) {

        val contentValues = ContentValues()
        contentValues.put("reposeRequired", reposeRequired)
        contentValues.put("positionX", position?.x)
        contentValues.put("positionY", position?.y)
        contentValues.put("positionYaw", position?.yaw)
        contentValues.put("withoutUI", withoutUI)
        contentValues.put("uri", uri.toString())

        if (TemiSdkContentProvider.sdkContext == null) {
            Log.e("Robot", "loadMapWithBackupFile, context is null")
        }

        val globalVersion = instance?.launcherVersion?.contains("usa", true) == true

        val packageName = if (globalVersion) {
            "com.roboteam.teamy.usa"
        } else {
            "com.roboteam.teamy.china"
        }

        TemiSdkContentProvider.sdkContext?.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

        try {
            TemiSdkContentProvider.sdkContext?.contentResolver?.insert(Uri.parse("content://${SdkConstants.PROVIDER_AUTHORITY}/map"), contentValues)
        } catch (e: IllegalArgumentException) {
            Log.e("Robot", "Insert Exception when loadMapWithBackupFile", e)
        }

    }

    @WorkerThread
    @JvmOverloads
    fun getSignedUrlByMediaKey(
        mediaKeys: List<String>,
        width: Int = -1,
        height: Int = -1
    ): List<Pair<String, String>> {
        return try {
            val results = sdkService?.getSignedUrlByMediaKey(
                applicationInfo.packageName,
                mediaKeys.filter { it.isNotBlank() },
                width,
                height
            ) ?: emptyList()
            return results.map {
                val jsonObject = JSONObject(it)
                val mediaKey = try {
                    jsonObject.getString("mediaKey")
                } catch (e: JSONException) {
                    ""
                }
                val signedUrl = try {
                    jsonObject.getString("signedUrl")
                } catch (e: JSONException) {
                    ""
                }
                return@map Pair(mediaKey, signedUrl)
            }
        } catch (e: RemoteException) {
            Log.e(TAG, "getSignedUrlByMediaKey() error")
            emptyList()
        }
    }

    @UiThread
    fun addOnSdkExceptionListener(listener: OnSdkExceptionListener) {
        onSdkExceptionListeners.add(listener)
    }

    @UiThread
    fun removeOnSdkExceptionListener(listener: OnSdkExceptionListener) {
        onSdkExceptionListeners.remove(listener)
    }

    fun addOnButtonStatusChangedListener(listener: OnButtonStatusChangedListener) {
        onButtonStatusChangedListeners.add(listener)
    }

    fun removeOnButtonStatusChangedListener(listener: OnButtonStatusChangedListener) {
        onButtonStatusChangedListeners.remove(listener)
    }

    fun addOnButtonModeChangedListener(listener: OnButtonModeChangedListener) {
        onButtonModeChangedListeners.add(listener)
    }

    fun removeOnButtonModeChangedListener(listener: OnButtonModeChangedListener) {
        onButtonModeChangedListeners.remove(listener)
    }

    /*****************************************/
    /*               Interface               */
    /*****************************************/

    interface WakeupWordListener {
        fun onWakeupWord(wakeupWord: String, direction: Int, origin: WakeupOrigin = WakeupOrigin.UNKNOWN)
    }

    interface TtsListener {
        fun onTtsStatusChanged(ttsRequest: TtsRequest)
    }

    interface AsrListener {
        fun onAsrResult(asrResult: String, sttLanguage: SttLanguage)
    }

    interface NlpListener {
        fun onNlpCompleted(nlpResult: NlpResult)
    }

    interface ActivityStreamPublishListener {
        fun onPublish(message: ActivityStreamPublishMessage)
    }

    @Deprecated("No longer supported by temi launcher")
    interface MediaButtonListener {

        fun onPlayButtonClicked(play: Boolean)

        fun onNextButtonClicked()

        fun onBackButtonClicked()

        fun onTrackBarChanged(position: Int)
    }

    interface NotificationListener {
        fun onNotificationBtnClicked(btnNumber: Int)
    }

    interface ConversationViewAttachesListener {
        fun onConversationAttaches(isAttached: Boolean)
    }

    companion object {

        private const val TAG = "Robot"

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: Robot? = null

        @JvmStatic
        fun getInstance(): Robot {
            if (instance == null) {
                synchronized(Robot::class.java) {
                    if (instance == null) {
                        val context = TemiSdkContentProvider.sdkContext
                            ?: throw NullPointerException("context == null")
                        instance = Robot(context)
                    }
                }
            }
            return instance!!
        }
    }

    private class GsonUTCDateAdapter : JsonSerializer<Date>,
        JsonDeserializer<Date> {
        private val dateFormat: DateFormat

        init {
            dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        }

        @Synchronized
        override fun serialize(
            date: Date,
            type: Type,
            jsonSerializationContext: JsonSerializationContext
        ): JsonElement {
            return JsonPrimitive(dateFormat.format(date))
        }

        @Synchronized
        override fun deserialize(
            jsonElement: JsonElement,
            type: Type,
            jsonDeserializationContext: JsonDeserializationContext
        ): Date {
            return try {
                dateFormat.parse(jsonElement.asString)
            } catch (e: ParseException) {
                throw JsonParseException(e)
            }
        }
    }

    private inline fun <reified T: Any> decodeBase64UngzipJsonArray(json: String): List<T> {
        return try {
            val compressedData = Base64.decode(json, Base64.NO_WRAP)

            ByteArrayOutputStream().use { outputStream ->
                // Decompress the data with GZIP
                GZIPInputStream(compressedData.inputStream()).use { gzipInputStream ->
                    gzipInputStream.copyTo(outputStream)
                }
                val decompressedData = outputStream.toByteArray()

                // Convert the decompressed data to a string
                val decompressedString = String(decompressedData)

                // Convert the string to a list of Object T
                val type = object : TypeToken<List<T>>() {}.type
                gson.fromJson(decompressedString, type)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse json array", e)
            listOf()
        }
    }

    private inline fun <reified T: Any> decodeBase64UngzipJson(json: String): T? {
        return try {
            val compressedData = Base64.decode(json, Base64.NO_WRAP)

            ByteArrayOutputStream().use { outputStream ->
                // Decompress the data with GZIP
                GZIPInputStream(compressedData.inputStream()).use { gzipInputStream ->
                    gzipInputStream.copyTo(outputStream)
                }
                val decompressedData = outputStream.toByteArray()

                // Convert the decompressed data to a string
                val decompressedString = String(decompressedData)

                // Convert the string to object T
                gson.fromJson(decompressedString, T::class.java)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse json", e)
            null
        }
    }
}
