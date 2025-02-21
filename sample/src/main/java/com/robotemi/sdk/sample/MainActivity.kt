package com.robotemi.sdk.sample

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Environment
import android.os.RemoteException
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import androidx.annotation.CheckResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.gson.Gson
import com.robotemi.sdk.*
import com.robotemi.sdk.Robot.*
import com.robotemi.sdk.Robot.Companion.getInstance
import com.robotemi.sdk.TtsRequest.Companion.create
import com.robotemi.sdk.activitystream.ActivityStreamObject
import com.robotemi.sdk.activitystream.ActivityStreamPublishMessage
import com.robotemi.sdk.constants.*
import com.robotemi.sdk.exception.OnSdkExceptionListener
import com.robotemi.sdk.exception.SdkException
import com.robotemi.sdk.face.ContactModel
import com.robotemi.sdk.face.OnContinuousFaceRecognizedListener
import com.robotemi.sdk.face.OnFaceRecognizedListener
import com.robotemi.sdk.listeners.*
import com.robotemi.sdk.map.Floor
import com.robotemi.sdk.map.LayerPose
import com.robotemi.sdk.map.MapModel
import com.robotemi.sdk.map.OnLoadFloorStatusChangedListener
import com.robotemi.sdk.map.OnLoadMapStatusChangedListener
import com.robotemi.sdk.map.OnMapStatusChangedListener
import com.robotemi.sdk.model.CallEventModel
import com.robotemi.sdk.model.DetectionData
import com.robotemi.sdk.navigation.listener.OnCurrentPositionChangedListener
import com.robotemi.sdk.navigation.listener.OnDistanceToDestinationChangedListener
import com.robotemi.sdk.navigation.listener.OnDistanceToLocationChangedListener
import com.robotemi.sdk.navigation.listener.OnReposeStatusChangedListener
import com.robotemi.sdk.navigation.model.Position
import com.robotemi.sdk.navigation.model.SafetyLevel
import com.robotemi.sdk.navigation.model.SpeedLevel
import com.robotemi.sdk.permission.OnRequestPermissionResultListener
import com.robotemi.sdk.permission.Permission
import com.robotemi.sdk.sequence.OnSequencePlayStatusChangedListener
import com.robotemi.sdk.sequence.SequenceModel
import com.robotemi.sdk.telepresence.CallState
import com.robotemi.sdk.telepresence.LinkBasedMeeting
import com.robotemi.sdk.telepresence.Participant
import com.robotemi.sdk.tourguide.TourModel
import com.robotemi.sdk.voice.ITtsService
import com.robotemi.sdk.voice.WakeupOrigin
import com.robotemi.sdk.voice.model.TtsVoice
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.group_app_and_permission.*
import kotlinx.android.synthetic.main.group_buttons.*
import kotlinx.android.synthetic.main.group_map_and_movement.*
import kotlinx.android.synthetic.main.group_resources.*
import kotlinx.android.synthetic.main.group_settings_and_status.*
import kotlinx.android.synthetic.main.group_settings_and_status.view.btnBatteryInfo
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.Executors
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity(), NlpListener, OnRobotReadyListener,
    ConversationViewAttachesListener, WakeupWordListener, ActivityStreamPublishListener,
    TtsListener, OnBeWithMeStatusChangedListener, OnGoToLocationStatusChangedListener,
    OnLocationsUpdatedListener, OnConstraintBeWithStatusChangedListener,
    OnDetectionStateChangedListener, AsrListener, OnTelepresenceEventChangedListener,
    OnRequestPermissionResultListener, OnDistanceToLocationChangedListener,
    OnCurrentPositionChangedListener, OnSequencePlayStatusChangedListener, OnRobotLiftedListener,
    OnDetectionDataChangedListener, OnUserInteractionChangedListener, OnFaceRecognizedListener,
    OnConversationStatusChangedListener, OnTtsVisualizerWaveFormDataChangedListener,
    OnTtsVisualizerFftDataChangedListener, OnReposeStatusChangedListener,
    OnLoadMapStatusChangedListener, OnDisabledFeatureListUpdatedListener,
    OnMovementVelocityChangedListener, OnMovementStatusChangedListener,
    OnContinuousFaceRecognizedListener, ITtsService, OnGreetModeStateChangedListener,
    TextToSpeech.OnInitListener, OnLoadFloorStatusChangedListener,
    OnDistanceToDestinationChangedListener, OnSdkExceptionListener, OnRobotDragStateChangedListener, OnMapStatusChangedListener,
    OnButtonStatusChangedListener {

    private lateinit var robot: Robot

    private val executorService = Executors.newSingleThreadExecutor()

    private var tts: TextToSpeech? = null

    private var debugReceiver: TemiBroadcastReceiver? = null

    private val assistantReceiver = AssistantChangeReceiver()

    private val telepresenceStatusChangedListener: OnTelepresenceStatusChangedListener by lazy {
        object : OnTelepresenceStatusChangedListener("") {
            override fun onTelepresenceStatusChanged(callState: CallState) {
                printLog("CallState $callState, ${callState.lowLightMode}")
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        verifyStoragePermissions(this)
        robot = getInstance()
        initOnClickListener()
        tvLog.movementMethod = ScrollingMovementMethod.getInstance()
        robot.addOnRequestPermissionResultListener(this)
        robot.addOnTelepresenceEventChangedListener(this)
        robot.addOnFaceRecognizedListener(this)
        robot.addOnContinuousFaceRecognizedListener(this)
        robot.addOnLoadMapStatusChangedListener(this)
        robot.addOnDisabledFeatureListUpdatedListener(this)
        robot.addOnSdkExceptionListener(this)
        robot.addOnMovementStatusChangedListener(this)
        robot.addOnGreetModeStateChangedListener(this)
        robot.addOnLoadFloorStatusChangedListener(this)
        robot.addOnTelepresenceStatusChangedListener(telepresenceStatusChangedListener)
        val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        if (appInfo.metaData != null
            && appInfo.metaData.getBoolean(SdkConstants.METADATA_OVERRIDE_TTS, false)
        ) {
            printLog("Override tts")
            tts = TextToSpeech(this, this)
            robot.setTtsService(this)
        }
        val greetModeState = intent.extras?.getInt(SdkConstants.INTENT_ACTION_GREET_MODE_STATE)
        if (greetModeState != null) {
            tvGreetMode.text =
                "Greet Mode -> ${OnGreetModeStateChangedListener.State.fromValue(greetModeState)}"
        }
        debugReceiver = TemiBroadcastReceiver()
        registerReceiver(debugReceiver, IntentFilter(TemiBroadcastReceiver.ACTION_DEBUG))

        registerReceiver(assistantReceiver, IntentFilter(AssistantChangeReceiver.ACTION_ASSISTANT_SELECTION))
    }

    /**
     * Setting up all the event listeners
     */
    override fun onStart() {
        super.onStart()
        robot.addOnButtonStatusChangedListener(this)
        robot.addOnRobotReadyListener(this)
        robot.addNlpListener(this)
        robot.addOnBeWithMeStatusChangedListener(this)
        robot.addOnGoToLocationStatusChangedListener(this)
        robot.addConversationViewAttachesListener(this)
        robot.addWakeupWordListener(this)
        robot.addTtsListener(this)
        robot.addOnLocationsUpdatedListener(this)
        robot.addOnConstraintBeWithStatusChangedListener(this)
        robot.addOnDetectionStateChangedListener(this)
        robot.addAsrListener(this)
        robot.addOnDistanceToLocationChangedListener(this)
        robot.addOnCurrentPositionChangedListener(this)
        robot.addOnSequencePlayStatusChangedListener(this)
        robot.addOnRobotLiftedListener(this)
        robot.addOnDetectionDataChangedListener(this)
        robot.addOnUserInteractionChangedListener(this)
        robot.addOnConversationStatusChangedListener(this)
        robot.addOnTtsVisualizerWaveFormDataChangedListener(this)
        robot.addOnTtsVisualizerFftDataChangedListener(this)
        robot.addOnReposeStatusChangedListener(this)
        robot.addOnMovementVelocityChangedListener(this)
        robot.setActivityStreamPublishListener(this)
        robot.addOnDistanceToDestinationChangedListener(this)
        robot.addOnRobotDragStateChangedListener(this)
        robot.addOnMapStatusChangedListener(this)
        robot.showTopBar()
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    /**
     * Removing the event listeners upon leaving the app.
     */
    override fun onStop() {
        robot.removeOnButtonStatusChangedListener(this)
        robot.removeOnRobotReadyListener(this)
        robot.removeNlpListener(this)
        robot.removeOnBeWithMeStatusChangedListener(this)
        robot.removeOnGoToLocationStatusChangedListener(this)
        robot.removeConversationViewAttachesListener(this)
        robot.removeWakeupWordListener(this)
        robot.removeTtsListener(this)
        robot.removeOnLocationsUpdateListener(this)
        robot.removeOnDetectionStateChangedListener(this)
        robot.removeAsrListener(this)
        robot.removeOnDistanceToLocationChangedListener(this)
        robot.removeOnCurrentPositionChangedListener(this)
        robot.removeOnSequencePlayStatusChangedListener(this)
        robot.removeOnRobotLiftedListener(this)
        robot.removeOnDetectionDataChangedListener(this)
        robot.addOnUserInteractionChangedListener(this)
        robot.stopMovement()
        if (robot.checkSelfPermission(Permission.FACE_RECOGNITION) == Permission.GRANTED) {
            robot.stopFaceRecognition()
        }
        robot.removeOnConversationStatusChangedListener(this)
        robot.removeOnTtsVisualizerWaveFormDataChangedListener(this)
        robot.removeOnTtsVisualizerFftDataChangedListener(this)
        robot.removeOnReposeStatusChangedListener(this)
        robot.removeOnMovementVelocityChangedListener(this)
        robot.setActivityStreamPublishListener(null)
        robot.removeOnDistanceToDestinationChangedListener(this)
        robot.removeOnRobotDragStateChangedListener(this)
        super.onStop()
    }

    override fun onDestroy() {
        robot.removeOnMapStatusChangedListener(this)
        robot.removeOnRequestPermissionResultListener(this)
        robot.removeOnTelepresenceEventChangedListener(this)
        robot.removeOnFaceRecognizedListener(this)
        robot.removeOnContinuousFaceRecognizedListener(this)
        robot.removeOnSdkExceptionListener(this)
        robot.removeOnLoadMapStatusChangedListener(this)
        robot.removeOnDisabledFeatureListUpdatedListener(this)
        robot.removeOnMovementStatusChangedListener(this)
        robot.removeOnGreetModeStateChangedListener(this)
        robot.removeOnLoadFloorStatusChangedListener(this)
        robot.removeOnTelepresenceStatusChangedListener(telepresenceStatusChangedListener)
        if (!executorService.isShutdown) {
            executorService.shutdownNow()
        }
        tts?.shutdown()
        val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        if (appInfo.metaData != null
            && appInfo.metaData.getBoolean(SdkConstants.METADATA_OVERRIDE_TTS, false)
        ) {
            printLog("Unbind TTS service")
            tts = null
            robot.setTtsService(null)
        }
        if (debugReceiver != null) {
            unregisterReceiver(debugReceiver)
        }

        unregisterReceiver(assistantReceiver)
        super.onDestroy()
    }

    private fun initOnClickListener() {
        btnGroupSystem.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                group_settings_and_status.visibility = View.VISIBLE
                btnGroupSystem.isEnabled = false
                btnGroupNavigation.isChecked = false
                btnGroupPermission.isChecked = false
                btnGroupResources.isChecked = false
            } else {
                group_settings_and_status.visibility = View.GONE
                btnGroupSystem.isEnabled = true
            }
        }
        btnGroupNavigation.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                group_map_and_movement.visibility = View.VISIBLE
                btnGroupSystem.isChecked = false
                btnGroupNavigation.isEnabled = false
                btnGroupPermission.isChecked = false
                btnGroupResources.isChecked = false
            } else {
                group_map_and_movement.visibility = View.GONE
                btnGroupNavigation.isEnabled = true
            }
        }
        btnGroupPermission.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                group_app_and_permission.visibility = View.VISIBLE
                btnGroupSystem.isChecked = false
                btnGroupNavigation.isChecked = false
                btnGroupPermission.isEnabled = false
                btnGroupResources.isChecked = false
            } else {
                group_app_and_permission.visibility = View.GONE
                btnGroupPermission.isEnabled = true
            }
        }
        btnGroupResources.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                group_resources.visibility = View.VISIBLE
                btnGroupSystem.isChecked = false
                btnGroupNavigation.isChecked = false
                btnGroupPermission.isChecked = false
                btnGroupResources.isEnabled = false
            } else {
                group_resources.visibility = View.GONE
                btnGroupResources.isEnabled = true
            }
        }

        val mediaPlayer = MediaPlayer()

        btnGroupSystem.isChecked = true

        btnSpeak.setOnClickListener { speak() }
        btnSpeak.setOnLongClickListener {
            speak(true)
            true
        }
        btnSaveLocation.setOnClickListener { saveLocation() }
        btnGoTo.setOnClickListener { goTo() }
        btnGetPosition.setOnClickListener { getPosition() }

        btnStopMovement.setOnClickListener { stopMovement() }
        btnFollow.setOnClickListener { followMe() }
        btnFollow.setOnLongClickListener {
            followMe(SpeedLevel.HIGH)
            true
        }
        btnskidJoy.setOnClickListener { skidJoy() }
        btnskidJoyDialog.setOnClickListener {
            val alert = AlertDialog.Builder(it.context)
                .setTitle("Skid Joy control by WSAD")
                .setMessage("Control temi with keyboard WSAD, hold ctrl to move non-smartly, Z, X, C to break")
                .setPositiveButton("OK", null)
                .show()
            alert.setOnKeyListener { _, keyCode, event ->
                var x = 0f
                var y = 0f

                if (listOf(
                        KeyEvent.KEYCODE_S,
                        KeyEvent.KEYCODE_W,
                        KeyEvent.KEYCODE_A,
                        KeyEvent.KEYCODE_D,
                        KeyEvent.KEYCODE_X,
                        KeyEvent.KEYCODE_Z,
                        KeyEvent.KEYCODE_C,
                    ).contains(keyCode)
                ) {
                    // Use W, A, S, D to move the robot smartly
                    // If Ctrl is hold, then do non smart move.
                    if (listOf(
                            KeyEvent.KEYCODE_X,
                            KeyEvent.KEYCODE_Z,
                            KeyEvent.KEYCODE_C
                        ).contains(keyCode)
                    ) {
                        x = 0f
                        y = 0f
                        robot.stopMovement()
                        return@setOnKeyListener false
                    }
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        when (keyCode) {
                            KeyEvent.KEYCODE_S -> x = if (y != 0f) -0.4f else -0.6f
                            KeyEvent.KEYCODE_W -> x = if (y != 0f) 0.5f else 0.6f
                            KeyEvent.KEYCODE_A -> y = if (x < 0) -1f else 1f
                            KeyEvent.KEYCODE_D -> y = if (x < 0) 1f else -1f
                        }
                        robot.skidJoy(x, y, !event.isCtrlPressed)
                    } else if (event.action == KeyEvent.ACTION_UP) {
                        when (keyCode) {
                            KeyEvent.KEYCODE_S -> x = 0f
                            KeyEvent.KEYCODE_W -> x = 0f
                            KeyEvent.KEYCODE_A -> y = 0f
                            KeyEvent.KEYCODE_D -> y = 0f
                        }
                    }
                }
                false

            }
        }
        btnTiltAngle.setOnClickListener { tiltAngle() }
        btnTiltBy.setOnClickListener { tiltBy() }
        btnTurnBy.setOnClickListener { turnBy() }

        val navPathListener = object : OnGoToNavPathChangedListener {
            override fun onGoToNavPathChanged(path: List<LayerPose>) {
                printLog("Nav Path $path")
            }
        }
        btnNavPath.setOnClickListener { view ->
            if (view.tag == true) {
                robot.removeOnGoToNavPathChangedListener(navPathListener)
                view.tag = false
                printLog("Nav Path Listener removed")
            } else {
                robot.addOnGoToNavPathChangedListener(navPathListener)
                view.tag = true
                printLog("Nav Path Listener added")
            }
        }

        btnIsMapLost.setOnClickListener { printLog("Is Map Lost: ${robot.isMapLost()}") }
        btnIsMapLocked.setOnClickListener { printLog("Is Map Locked: ${robot.isMapLocked()}") }

        val mapStatusListener = object : OnMapStatusChangedListener {
            override fun onMapStatusChanged(isMapLost: Boolean, isLocked: Boolean) {
                printLog("Map Status: isMapLost $isMapLost, isLocked $isLocked")
            }
        }
        btnMapStatus.setOnClickListener { view ->
            if (view.tag == true) {
                robot.removeOnMapStatusChangedListener(mapStatusListener)
                view.tag = false
                printLog("Map Status Listener removed")
            } else {
                robot.addOnMapStatusChangedListener(mapStatusListener)
                view.tag = true
                printLog("Map Status Listener added")
            }
        }

        btnGetMapElements.setOnClickListener {
            if (robot.checkSelfPermission(Permission.MAP) == Permission.GRANTED) {
                printLog("map elements: ${robot.getMapElements()}")
            }   else {
                printLog("Map permission not granted")
            }
        }
        btnGetMapImage.setOnClickListener {
            if (robot.checkSelfPermission(Permission.MAP) == Permission.GRANTED) {
                printLog("map image: ${robot.getMapImage()}")
            }   else {
                printLog("Map permission not granted")
            }

        }

        btnBatteryInfo.setOnClickListener { getBatteryData() }
        btnSavedLocations.setOnClickListener { savedLocationsDialog() }
        btnCallOwner.setOnClickListener { callOwner() }
        btnStopCall.setOnClickListener {
            if (!requestPermissionIfNeeded(Permission.MEETINGS, REQUEST_CODE_NORMAL)) {
                stopCall()
            }
        }
        btnPublish.setOnClickListener { publishToActivityStream() }
        btnHideTopBar.setOnClickListener { hideTopBar() }
        btnShowTopBar.setOnClickListener { showTopBar() }
        btnWakeup.setOnClickListener { wakeup() }
        btnWakeup.setOnLongClickListener {
            Toast.makeText(this@MainActivity, robot.wakeupWord, Toast.LENGTH_SHORT).show()
            true
        }
        btnWakeupCustomLanguages.setOnClickListener { wakeupCustomLanguages() }
        btnSetAsrLanguages.setOnClickListener { setAsrLanguages() }
        btnDisableWakeup.setOnClickListener { disableWakeup() }
        btnEnableWakeup.setOnClickListener { enableWakeup() }
        btnToggleNavBillboard.setOnClickListener { toggleNavBillboard() }
        btnTogglePrivacyModeOn.setOnClickListener { privacyModeOn() }
        btnTogglePrivacyModeOff.setOnClickListener { privacyModeOff() }
        btnGetPrivacyMode.setOnClickListener { getPrivacyModeState() }
        btnEnableHardButtons.setOnClickListener { enableHardButtons() }
        btnDisableHardButtons.setOnClickListener { disableHardButtons() }
        btnIsHardButtonsDisabled.setOnClickListener { isHardButtonsEnabled() }
        btnGetOSVersion.setOnClickListener { getOSVersion() }
        btnCheckFace.setOnClickListener { requestFace() }
        btnCheckMap.setOnClickListener { requestMap() }
        btnCheckSettings.setOnClickListener { requestSettings() }
        btnCheckSequence.setOnClickListener { requestSequence() }
        btnCheckMeetings.setOnClickListener { requestMeetings() }
        btnCheckAllPermission.setOnClickListener { requestAll() }
        btnStartFaceRecognition.setOnClickListener { startFaceRecognition() }
        btnStopFaceRecognition.setOnClickListener { stopFaceRecognition() }
        btnTestFaceRecognition.setOnClickListener { testFaceRecognition() }
        btnSetUserInteractionON.setOnClickListener {
            val ret = robot.setInteractionState(true)
            Log.d("MainActivity", "Set user interaction $ret")
            mediaPlayer.setVolume(1f, 1f)
            mediaPlayer.isLooping = false
            mediaPlayer.setOnCompletionListener {
                robot.setInteractionState(false)
            }
            if (!mediaPlayer.isPlaying) {
                val descriptor: AssetFileDescriptor = assets.openFd("Lorem-ipsum.mp3")
                mediaPlayer.setDataSource(
                    descriptor.fileDescriptor,
                    descriptor.startOffset,
                    descriptor.length
                )
                descriptor.close()
                mediaPlayer.prepare()
                mediaPlayer.start()
            }
        }
        btnSetUserInteractionOFF.setOnClickListener {
            robot.setInteractionState(false)
            mediaPlayer.stop()
            mediaPlayer.reset()
        }
        btnSetGoToSpeed.setOnClickListener { setGoToSpeed() }
        btnSetFollowSpeed.setOnClickListener { setFollowSpeed() }
        btnSetGoToSafety.setOnClickListener { setGoToSafety() }
        btnToggleTopBadge.setOnClickListener { toggleTopBadge() }
        btnToggleDetectionMode.setOnClickListener { toggleDetectionMode() }
        btnToggleAutoReturn.setOnClickListener { toggleAutoReturn() }
        btnTrackUser.setOnClickListener { toggleTrackUser() }
        btnGetVolume.setOnClickListener { getVolume() }
        btnSetVolume.setOnClickListener { setVolume() }
        btnSetMicGainLevel.setOnClickListener { setMicGainLevel() }
        btnRequestToBeKioskApp.setOnClickListener { requestToBeKioskApp() }
        btnStartDetectionModeWithDistance.setOnClickListener { startDetectionWithDistance() }
        btnFetchSequence.setOnClickListener { getAllSequences() }
        btnFetchTour.setOnClickListener { getAllTours() }
        btnPlayFirstSequence.setOnClickListener { playFirstSequence() }
        btnPlayFirstTour.setOnClickListener { playFirstTour() }
        btnPlayFirstSequenceWithoutPlayer.setOnClickListener { playFirstSequenceWithoutPlayer() }
        btnFetchMap.setOnClickListener { getMap() }
        btnClearLog.setOnClickListener { clearLog() }
        btnNlu.setOnClickListener { startNlu() }
        btnGetAllContacts.setOnClickListener { getAllContacts() }
        btnGoToPosition.setOnClickListener { goToPosition() }
        btnStartTelepresenceToCenter.setOnClickListener { startTelepresenceToCenter() }
        btnStartMeeting.setOnClickListener { startMeeting() }
        btnCreateLinkBasedMeeting.setOnClickListener {
            if (requestPermissionIfNeeded(Permission.MEETINGS, REQUEST_CODE_NORMAL)) {
                // Permission not granted yet.
            } else {
                val request = LinkBasedMeeting(
                    topic = "temi Demo Meeting",
                    availability = LinkBasedMeeting.Availability(
                        start = Date(),
                        end = Date(Date().time + 86400000),
                        always = false,
                    ),
                    limit = LinkBasedMeeting.Limit(
                        callDuration = LinkBasedMeeting.CallDuration.MINUTE_10,
                        usageLimit = LinkBasedMeeting.UsageLimit.NO_LIMIT,
                    ),
                    permission = LinkBasedMeeting.Permission.DEFAULT,
                    security = LinkBasedMeeting.Security(
                        password = "1122334455", // Should use a 1 to 10-digits password.
                        hasPassword = false
                    )
                )
                thread {
                    val (code, linkUrl) = robot.createLinkBasedMeeting(request)
                    printLog("Link create request, response code $code, link $linkUrl")
                }
            }
        }
        btnCreateLinkBasedMeeting.setOnLongClickListener {
            if (requestPermissionIfNeeded(Permission.MEETINGS, REQUEST_CODE_NORMAL)) {
                // Permission not granted yet.
            } else {
                val request = LinkBasedMeeting(
                    topic = "temi Demo Meeting",
                    availability = LinkBasedMeeting.Availability(
                        start = Date(),
                        end = Date(Date().time + 86400000),
                        always = false,
                    ),
                    limit = LinkBasedMeeting.Limit(
                        callDuration = LinkBasedMeeting.CallDuration.MINUTE_10,
                        usageLimit = LinkBasedMeeting.UsageLimit.NO_LIMIT,
                    ),
                    permission = LinkBasedMeeting.Permission.DISABLE_ROBOT_INTERACTION,
                    security = LinkBasedMeeting.Security(
                        password = "1122334455", // Should use a 1 to 10-digits password.
                        hasPassword = false
                    )
                )
                thread {
                    val (code, linkUrl) = robot.createLinkBasedMeeting(request)
                    printLog("Link create request, response code $code, link $linkUrl")
                }
            }
            true
        }
        btnStartPage.setOnClickListener { startPage() }
        btnRestart.setOnClickListener { restartTemi() }
        btnGetMembersStatus.setOnClickListener { getMembersStatus() }
        btnRepose.setOnClickListener { repose() }
        btnGetMapList.setOnClickListener { getMapListBtn() }
        btnLoadMap.setOnClickListener { loadMap() }
        btnLoadMapToCache.setOnClickListener { loadMapToCache() }
        btnLoadMapOffline.setOnClickListener { loadMap(false, null, true) }
        btnLoadMapWithoutUI.setOnClickListener {
            loadMap(
                false,
                null,
                offline = false,
                withoutUI = true
            )
        }
        btnLock.setOnClickListener { lock() }
        btnUnlock.setOnClickListener { unlock() }
        btnMuteAlexa.setOnClickListener { muteAlexa() }
        btnShutdown.setOnClickListener { shutdown() }
        btnLoadMapWithPosition.setOnClickListener { loadMapWithPosition() }
        btnLoadMapWithReposePosition.setOnClickListener { loadMapWithReposePosition() }
        btnLoadMapWithRepose.setOnClickListener { loadMapWithRepose() }
        btnSetSoundMode.setOnClickListener { setSoundMode() }
        btnSetHardBtnMainMode.setOnClickListener { setHardBtnMainMode() }
        btnToggleHardBtnPower.setOnClickListener { toggleHardBtnPower() }
        btnToggleHardBtnVolume.setOnClickListener { toggleHardBtnVolume() }
        btnGetNickName.setOnClickListener { getNickName() }
        btnSetMode.setOnClickListener { setMode() }
        btnGetMode.setOnClickListener { getMode() }
        btnToggleKioskMode.setOnClickListener { toggleKiosk() }
        btnToggleKioskMode.setOnLongClickListener {
//            robot.setKioskModeOn(false, HomeScreenMode.DEFAULT)
//            robot.setKioskModeOn(false, HomeScreenMode.CLEAR)
            robot.setKioskModeOn(false, HomeScreenMode.CUSTOM_SCREEN)
//            robot.setKioskModeOn(false, HomeScreenMode.URL)
            true
        }
        btnIsKioskModeOn.setOnClickListener { isKioskModeOn() }
        btnCurrentHomeScreenMode.setOnClickListener { currentHomeScreenMode() }
        btnEnabledLatinKeyboards.setOnClickListener { enabledLatinKeyboards() }
        btnGetSupportedKeyboard.setOnClickListener { getSupportedLatinKeyboards() }
        btnToggleGroundDepthCliff.setOnClickListener { toggleGroundDepthCliff() }
        btnIsGroundDepthCliff.setOnClickListener { isGroundDepthCliffEnabled() }
        btnHasCliffSensor.setOnClickListener { hasCliffSensor() }
        btnSetCliffSensorMode.setOnClickListener { setCliffSensorMode() }
        btnGetCliffSensorMode.setOnClickListener { getCliffSensorMode() }
        btnSetHeadDepthSensitivity.setOnClickListener { setHeadDepthSensitivity() }
        btnGetHeadDepthSensitivity.setOnClickListener { getHeadDepthSensitivity() }
        btnToggleFrontTOF.setOnClickListener { toggleFrontTOF() }
        btnIsFrontTOFEnabled.setOnClickListener { isFrontTOFEnabled() }
        btnToggleBackTOF.setOnClickListener { toggleBackTOF() }
        btnIsBackTOFEnabled.setOnClickListener { isBackTOFEnabled() }
        btnMinimumObstacleDistance.setOnClickListener {
            if (requestPermissionIfNeeded(Permission.SETTINGS, REQUEST_CODE_NORMAL)) {
                return@setOnClickListener
            }
            if (robot.minimumObstacleDistance == -1) {
                Toast.makeText(this, "Minimum Obstacle Distance settings is not supported on your robot.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (groupMinimumObstacleDistance.visibility == View.GONE) {
                 groupMinimumObstacleDistance.visibility = View.VISIBLE
            }
            val distance = robot.minimumObstacleDistance
            textMinimumObstacleDistance.text = "$distance"
            seekbarMinimumObstacleDistance.progress = distance.coerceIn(0, 100)
            seekbarMinimumObstacleDistance.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    seekBar ?: return
                    // Round to 5x
                    val value = seekBar.progress / 5 * 5
                    seekBar.progress = value
                    textMinimumObstacleDistance.text = "$value"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    // nothing
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    seekBar ?: return
                    // Round to 5x
                    val value = seekBar.progress / 5 * 5
                    robot.minimumObstacleDistance = value
                }

            })
        }
        btnGetAllFloors.setOnClickListener { getAllFloors() }
        btnLoadFloorAtElevator.setOnClickListener { loadFloorAtElevator() }
        btnGetCurrentFloor.setOnClickListener {
            getCurrentFloor()
        }
        btnGetTts.setOnClickListener { getTts() }
        btnSetTts.setOnClickListener { setTts() }
        btnSerial.setOnClickListener { startActivity(Intent(this, SerialActivity::class.java)) }
        btnWebpage.setOnClickListener {
            val intent =
                Intent().setClassName("com.robotemi.browser", "com.robotemi.browser.MainActivity")
            intent.putExtra("url", "https://github.com")
            intent.putExtra("source", "intent")
            intent.putExtra("navBar", "SHOW")
            intent.putExtra("reset", "OFF")
            try {
                startActivity(intent)
            } catch (e: Exception) {
                printLog("Cannot launch browser, probably temi browser app not installed.")
            }
        }
        btnEmergencyStop.setOnClickListener {
            val status = robot.getButtonStatus(HardButton.EMERGENCY_STOP)
            printLog("Emergency Stop button status $status")
        }
        val eStopListener = object : OnButtonStatusChangedListener {
            override fun onButtonStatusChanged(hardButton: HardButton, status: HardButton.Status) {
                if (hardButton == HardButton.EMERGENCY_STOP) {
                    printLog("Emergency Stop button status changed: $status")
                }
            }
        }

        btnEmergencyStop.setOnLongClickListener { view ->
            if (view.tag == true) {
                robot.removeOnButtonStatusChangedListener(eStopListener)
                view.tag = false
                printLog("Emergency Stop button Listener removed")
            } else {
                robot.addOnButtonStatusChangedListener(eStopListener)
                view.tag = true
                printLog("Emergency Stop button Listener added")
            }
            true
        }
    }

    private fun getPosition() {
        printLog(robot.getPosition().toString())
    }

    private fun getCurrentFloor() {
        printLog(robot.getCurrentFloor()?.toString() ?: "Get current floor failed")
    }

    private fun loadFloorAtElevator() {
        if (floorList.isEmpty()) {
            getAllFloors()
        }
        if (robot.checkSelfPermission(Permission.MAP) != Permission.GRANTED) {
            return
        }
        val floorListString: MutableList<String> = ArrayList()
        floorList.forEach {
            floorListString.add(it.name)
        }
        val floorListAdapter =
            ArrayAdapter(this, R.layout.item_dialog_row, R.id.name, floorListString)
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Click item to load specific floor")
        builder.setAdapter(floorListAdapter, null)
        val dialog = builder.create()
        dialog.listView.onItemClickListener =
            OnItemClickListener { _: AdapterView<*>?, _: View?, pos: Int, _: Long ->
                val targetFloor = floorList[pos]
                printLog("Loading floor: " + targetFloor.name)
                val elevator = targetFloor.locations.find { it.name == "elevator" }
                if (elevator == null) {
                    printLog("No location elevator exists")
                    return@OnItemClickListener
                }
                robot.loadFloor(targetFloor.id, Position(elevator.x, elevator.y, elevator.yaw))
                dialog.dismiss()
            }
        dialog.show()
    }

    private var floorList = emptyList<Floor>()

    private fun getAllFloors() {
        if (requestPermissionIfNeeded(Permission.MAP, REQUEST_CODE_GET_ALL_FLOORS)) {
            return
        }
        floorList = robot.getAllFloors()
        Log.d("MainActivity", "floor list size: ${floorList.size}")
        floorList.forEach {
            printLog(it.toString())
        }
    }

    private fun isBackTOFEnabled() {
        printLog("Back TOF enabled: ${robot.backTOFEnabled}")
    }

    private fun toggleBackTOF() {
        robot.backTOFEnabled = !robot.backTOFEnabled
        isBackTOFEnabled()
    }

    private fun isFrontTOFEnabled() {
        printLog("Front TOF enabled: ${robot.frontTOFEnabled}")
    }

    private fun toggleFrontTOF() {
        robot.frontTOFEnabled = !robot.frontTOFEnabled
        isFrontTOFEnabled()
    }

    private fun getHeadDepthSensitivity() {
        printLog("Head depth sensitivity: ${robot.headDepthSensitivity}")
    }

    private fun setHeadDepthSensitivity() {
        val adapter = ArrayAdapter(
            this,
            R.layout.item_dialog_row,
            R.id.name,
            listOf(SensitivityLevel.HIGH, SensitivityLevel.LOW)
        )
        val dialog = AlertDialog.Builder(this)
            .setTitle("Select Head Depth Sensitivity")
            .setAdapter(adapter, null)
            .create()
        dialog.listView.onItemClickListener =
            OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
                robot.headDepthSensitivity = adapter.getItem(position)!!
                printLog("Set Head Depth Sensitivity to: ${adapter.getItem(position)}")
                dialog.dismiss()
            }
        dialog.show()
    }

    private fun getCliffSensorMode() {
        printLog("Cliff sensor mode: ${robot.cliffSensorMode.name}")
    }

    private fun setCliffSensorMode() {
        if (!robot.hasCliffSensor()) {
            printLog("No cliff sensor, invalid operation.")
            return
        }
        val adapter = ArrayAdapter(
            this,
            R.layout.item_dialog_row,
            R.id.name,
            listOf(
                CliffSensorMode.HIGH_SENSITIVITY,
                CliffSensorMode.LOW_SENSITIVITY,
                CliffSensorMode.OFF
            )
        )
        val dialog = AlertDialog.Builder(this)
            .setTitle("Select Cliff Sensor Mode")
            .setAdapter(adapter, null)
            .create()
        dialog.listView.onItemClickListener =
            OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
                robot.cliffSensorMode = adapter.getItem(position)!!
                printLog("Set Cliff Sensor Mode to: ${adapter.getItem(position)}")
                dialog.dismiss()
            }
        dialog.show()
    }

    private fun hasCliffSensor() {
        printLog("Has cliff sensor: ${robot.hasCliffSensor()}")
    }

    private fun isGroundDepthCliffEnabled() {
        printLog("Ground depth cliff enabled: ${robot.groundDepthCliffDetectionEnabled}")
    }

    private fun toggleGroundDepthCliff() {
        robot.groundDepthCliffDetectionEnabled = !robot.groundDepthCliffDetectionEnabled
        isGroundDepthCliffEnabled()
    }

    private fun getSupportedLatinKeyboards() {
        val supportedLatinKeyboards = robot.getSupportedLatinKeyboards()
        var count = 0
        supportedLatinKeyboards.iterator()
            .forEach {
                printLog("No.${++count} Latin keyboard: ${it.key}, enabled: ${it.value}")
            }
    }

    private fun enabledLatinKeyboards() {
        if (requestPermissionIfNeeded(Permission.SETTINGS, REQUEST_CODE_NORMAL)) {
            return
        }
        /**
         * list should be got from the keys of the map via method [com.robotemi.sdk.Robot.getSupportedLatinKeyboards]
         */
        robot.enabledLatinKeyboards(robot.getSupportedLatinKeyboards().keys.toList().subList(0, 5))
    }

    private fun isKioskModeOn() {
        printLog("Is kiosk mode on: ${robot.isKioskModeOn()}")
    }

    private fun currentHomeScreenMode() {
        printLog("Current home screen mode: ${robot.getHomeScreenMode()}")
    }

    private fun toggleKiosk() {
        robot.setKioskModeOn(!robot.isKioskModeOn())
    }

    private fun getMode() {
        printLog("System mode: ${robot.getMode()}")
    }

    private fun setMode() {
        if (requestPermissionIfNeeded(Permission.SETTINGS, REQUEST_CODE_NORMAL)) {
            return
        }
        val modes: MutableList<Mode> = ArrayList()
        modes.add(Mode.DEFAULT)
        modes.add(Mode.GREET)
        modes.add(Mode.PRIVACY)
        val adapter = ArrayAdapter(this, R.layout.item_dialog_row, R.id.name, modes)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Select Mode")
            .setAdapter(adapter, null)
            .create()
        dialog.listView.onItemClickListener =
            OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
                robot.setMode(adapter.getItem(position)!!)
                printLog("Set Mode to: ${adapter.getItem(position)}")
                dialog.dismiss()
            }
        dialog.show()
    }

    private fun getNickName() {
        printLog("temi's nick name: ${robot.getNickName()}")
    }

    private fun toggleHardBtnVolume() {
        if (requestPermissionIfNeeded(Permission.SETTINGS, REQUEST_CODE_NORMAL)) {
            return
        }
        val currentMode = robot.getHardButtonMode(HardButton.VOLUME)
        robot.setHardButtonMode(
            HardButton.VOLUME,
            if (currentMode == HardButton.Mode.ENABLED) HardButton.Mode.DISABLED
            else HardButton.Mode.ENABLED
        )
        printLog("Set hard button volume: ${robot.getHardButtonMode(HardButton.VOLUME)}")
    }

    private fun toggleHardBtnPower() {
        if (requestPermissionIfNeeded(Permission.SETTINGS, REQUEST_CODE_NORMAL)) {
            return
        }
        val currentMode = robot.getHardButtonMode(HardButton.POWER)
        robot.setHardButtonMode(
            HardButton.POWER,
            if (currentMode == HardButton.Mode.ENABLED) HardButton.Mode.DISABLED
            else HardButton.Mode.ENABLED
        )
        printLog("Set hard button power: ${robot.getHardButtonMode(HardButton.POWER)}")
    }

    private fun setHardBtnMainMode() {
        if (requestPermissionIfNeeded(Permission.SETTINGS, REQUEST_CODE_NORMAL)) {
            return
        }
        val modes: MutableList<HardButton.Mode> = ArrayList()
        modes.add(HardButton.Mode.ENABLED)
        modes.add(HardButton.Mode.DISABLED)
        modes.add(HardButton.Mode.MAIN_BLOCK_FOLLOW)
        val adapter = ArrayAdapter(this, R.layout.item_dialog_row, R.id.name, modes)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Select Main Hard Button Mode")
            .setAdapter(adapter, null)
            .create()
        dialog.listView.onItemClickListener =
            OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
                robot.setHardButtonMode(HardButton.MAIN, adapter.getItem(position)!!)
                printLog("Set Main Hard Button Mode to: ${adapter.getItem(position)}")
                dialog.dismiss()
            }
        dialog.show()
    }

    private fun setSoundMode() {
        if (requestPermissionIfNeeded(Permission.SETTINGS, REQUEST_CODE_NORMAL)) {
            return
        }
        val soundModes: MutableList<SoundMode> = ArrayList()
        soundModes.add(SoundMode.NORMAL)
        soundModes.add(SoundMode.VIDEO_CALL)
        val adapter = ArrayAdapter(this, R.layout.item_dialog_row, R.id.name, soundModes)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Select Sound Mode")
            .setAdapter(adapter, null)
            .create()
        dialog.listView.onItemClickListener =
            OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
                robot.setSoundMode(adapter.getItem(position)!!)
                printLog("Set Sound Mode to: ${adapter.getItem(position)}")
                dialog.dismiss()
            }
        dialog.show()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        var view = currentFocus
        if (view == null) {
            view = View(this)
        }
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    /**
     * Places this application in the top bar for a quick access shortcut.
     */
    override fun onRobotReady(isReady: Boolean) {
        if (isReady) {
            try {
                val activityInfo =
                    packageManager.getActivityInfo(componentName, PackageManager.GET_META_DATA)
                // Robot.getInstance().onStart() method may change the visibility of top bar.
                robot.onStart(activityInfo)
            } catch (e: PackageManager.NameNotFoundException) {
                throw RuntimeException(e)
            }
        }
    }

    /**
     * Have the robot speak while displaying what is being said.
     */
    private fun speak(askQuestion : Boolean = false) {
        val text = etSpeak.text.toString()
        val languages = ArrayList<TtsRequest.Language>()
        TtsRequest.Language.values().forEach {
            language ->  languages.add(language)
        }
        val adapter = ArrayAdapter(this, R.layout.item_dialog_row, R.id.name, languages)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Select Speaking Language")
            .setAdapter(adapter, null)
            .create()
        dialog.listView.onItemClickListener =
            OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
                val ttsRequest =
                    create(text, language = adapter.getItem(position)!!, showAnimationOnly = true)
                if (text == "queue") {
                    // A demonstration of using TTS queue

                    val request = TtsRequest.create("白日依山尽\n", language = TtsRequest.Language.ZH_CN)
                    with(TtsRequest.Language.ZH_CN) {
                        val request1 = request.copy(speech = "黄河入海流\n")
                        val request2 = request.copy(speech = "欲穷千里目\n")
                        val request3 = request.copy(speech = "更上一层楼\n")
                        robot.speak(request)
                        robot.speak(request1)
                        robot.speak(request2)
                        robot.speak(request3)
                    }

                    with(TtsRequest.Language.JA_JP) {
                        val request1 = request.copy(speech = "古池や\n", language = TtsRequest.Language.JA_JP.value)
                        val request2 = request1.copy(speech = "蛙飛び込む\n")
                        val request3 = request1.copy(speech = "水の音\n")
                        robot.speak(request1)
                        robot.speak(request2)
                        robot.speak(request3)
                    }

                    with(TtsRequest.Language.EN_US) {
                        val request1 = request.copy(speech = "It is just as I feared!\n", language = TtsRequest.Language.EN_US.value)
                        val request2 = request1.copy(speech = "Two Owls and a Hen\n")
                        val request3 = request1.copy(speech = "Four Larks and a Wren\n")
                        val request4 = request1.copy(speech = "Have all built their nests in my beard.\n")
                        robot.speak(request1)
                        robot.speak(request2)
                        robot.speak(request3)
                        robot.speak(request4)
                    }


                } else if (askQuestion) {
                    robot.askQuestion(text)
                } else {
                    robot.speak(ttsRequest)
                }
                printLog("Speak: ${adapter.getItem(position)}")
                dialog.dismiss()
            }
        dialog.show()
        hideKeyboard()
    }

    /**
     * This is an example of saving locations.
     */
    private fun saveLocation() {
        val location =
            etSaveLocation.text.toString().lowercase().trim { it <= ' ' }
        val result = robot.saveLocation(location)
        if (result) {
            robot.speak(create("I've successfully saved the $location location.", true))
        } else {
            robot.speak(create("Saved the $location location failed.", true))
        }
        hideKeyboard()
    }

    /**
     * goTo checks that the location sent is saved then goes to that location.
     */
    private fun goTo() {
        for (location in robot.locations) {
            if (location == etGoTo.text.toString().lowercase()
                    .trim { it <= ' ' }
            ) {
                robot.goTo(
                    etGoTo.text.toString().lowercase().trim { it <= ' ' },
                    backwards = false,
                    noBypass = false,
                    speedLevel = SpeedLevel.HIGH
                )
                hideKeyboard()
            }
        }
    }

    /**
     * stopMovement() is used whenever you want the robot to stop any movement
     * it is currently doing.
     */
    private fun stopMovement() {
        robot.stopMovement()
        robot.speak(create("And so I have stopped", true))
    }

    /**
     * Simple follow me example.
     */
    private fun followMe(speedLevel: SpeedLevel? = null) {
        robot.beWithMe(speedLevel)
        hideKeyboard()
    }

    /**
     * Manually navigate the robot with skidJoy, tiltAngle, turnBy and tiltBy.
     * skidJoy moves the robot exactly forward for about a second. It controls both
     * the linear and angular velocity. Float numbers must be between -1.0 and 1.0
     */
    private fun skidJoy() {
        val t = System.currentTimeMillis()
        val end = t + 500
        val speedX = try {
            etX.text.toString().toFloat()
        } catch (e: Exception) {
            1f
        }
        val speedY = try {
            etY.text.toString().toFloat()
        } catch (e: Exception) {
            0f
        }
        printLog("speedX: $speedX, speedY: $speedY")
        while (System.currentTimeMillis() < end) {
            robot.skidJoy(speedX, speedY)
        }
    }

    /**
     * tiltAngle controls temi's head by specifying which angle you want
     * to tilt to and at which speed.
     */
    private fun tiltAngle() {
        val speed = try {
            etDistance.text.toString().toFloat()
        } catch (e: Exception) {
            1f
        }
        robot.tiltAngle(23, speed)
    }

    /**
     * turnBy allows for turning the robot around in place. You can specify
     * the amount of degrees to turn by and at which speed.
     */
    private fun turnBy() {
        val speed = try {
            etDistance.text.toString().toFloat()
        } catch (e: Exception) {
            1f
        }
        robot.turnBy(90, speed)
    }

    /**
     * tiltBy is used to tilt temi's head from its current position.
     */
    private fun tiltBy() {
        val speed = try {
            etDistance.text.toString().toFloat()
        } catch (e: Exception) {
            1f
        }
        robot.tiltBy(70, speed)
    }

    /**
     * getBatteryData can be used to return the current battery status.
     */
    private fun getBatteryData() {
        val batteryData = robot.batteryData
        if (batteryData == null) {
            printLog("getBatteryData()", "batteryData is null")
            return
        }
        if (batteryData.isCharging) {
            val ttsRequest =
                create(batteryData.level.toString() + " percent battery and charging.", true)
            robot.speak(ttsRequest)
        } else {
            val ttsRequest =
                create(batteryData.level.toString() + " percent battery and not charging.", true)
            robot.speak(ttsRequest)
        }
    }

    /**
     * Display the saved locations in a dialog
     */
    private fun savedLocationsDialog() {
        hideKeyboard()
        val locations = robot.locations.toMutableList()
        val locationAdapter = ArrayAdapter(this, R.layout.item_dialog_row, R.id.name, locations)
        val versionsDialog = AlertDialog.Builder(this@MainActivity)
        versionsDialog.setTitle("Saved Locations: (Click to delete the location)")
        versionsDialog.setPositiveButton("OK", null)
        versionsDialog.setAdapter(locationAdapter, null)
        val dialog = versionsDialog.create()
        dialog.listView.onItemClickListener =
            OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
                val builder = AlertDialog.Builder(this@MainActivity)
                builder.setMessage("Delete location \"" + locationAdapter.getItem(position) + "\" ?")
                builder.setPositiveButton("No thanks") { _: DialogInterface?, _: Int -> }
                builder.setNegativeButton("Yes") { _: DialogInterface?, _: Int ->
                    val location = locationAdapter.getItem(position) ?: return@setNegativeButton
                    val result = robot.deleteLocation(location)
                    if (result) {
                        locations.removeAt(position)
                        robot.speak(create(location + "delete successfully!", false))
                        locationAdapter.notifyDataSetChanged()
                    } else {
                        robot.speak(create(location + "delete failed!", false))
                    }
                }
                val deleteDialog: Dialog = builder.create()
                deleteDialog.show()
            }
        dialog.show()
    }

    /**
     * When adding the Nlp Listener to your project you need to implement this method
     * which will listen for specific intents and allow you to respond accordingly.
     *
     *
     * See AndroidManifest.xml for reference on adding each intent.
     */
    override fun onNlpCompleted(nlpResult: NlpResult) {
        //do something with nlp result. Base the action specified in the AndroidManifest.xml
        Toast.makeText(this@MainActivity, nlpResult.action, Toast.LENGTH_SHORT).show()
        printLog("NlpCompleted: $nlpResult" )
        when (nlpResult.action) {
            ACTION_HOME_WELCOME -> robot.tiltAngle(23)
            ACTION_HOME_DANCE -> {
                val t = System.currentTimeMillis()
                val end = t + 5000
                while (System.currentTimeMillis() < end) {
                    robot.skidJoy(0f, 1f)
                }
            }
            ACTION_HOME_SLEEP -> robot.goTo(HOME_BASE_LOCATION)
        }
    }

    /**
     * callOwner is an example of how to use telepresence to call an individual.
     */
    private fun callOwner() {
        val admin = robot.adminInfo
        if (admin == null) {
            printLog("callOwner()", "adminInfo is null.")
            return
        }
        robot.startTelepresence(admin.name, admin.userId)
    }

    /**
     * stopCall is an example of how to stop call an individual.
     */
    private fun stopCall() {
        val ret = robot.stopTelepresence()
        printLog("stopTelepresence()", "Result $ret.")
    }

    /**
     * publishToActivityStream takes an image stored in the resources folder
     * and uploads it to the mobile application under the Activities tab.
     */
    private fun publishToActivityStream() {
        executorService.execute {
            val activityStreamObject: ActivityStreamObject
            val fileName = "puppy.png"
            val bm = BitmapFactory.decodeResource(resources, R.drawable.puppy)
            val puppiesFile = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath,
                fileName
            )
            val fileOutputStream: FileOutputStream
            try {
                fileOutputStream = FileOutputStream(puppiesFile)
                bm.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
                fileOutputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            activityStreamObject = ActivityStreamObject.builder()
                .activityType(ActivityStreamObject.ActivityType.PHOTO)
                .media(MediaObject.create(MediaObject.MimeType.IMAGE, puppiesFile))
                .title("Puppy")
                .source(SourceObject.create("", ""))
                .build()
            try {
                printLog(Gson().toJson(activityStreamObject), false)
                robot.shareActivityObject(activityStreamObject)
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        }
    }

    private fun hideTopBar() {
        robot.hideTopBar()
    }

    private fun showTopBar() {
        robot.showTopBar()
    }

    override fun onWakeupWord(wakeupWord: String, direction: Int, origin: WakeupOrigin) {
        // Do anything on wakeup. Follow, go to location, or even try creating dance moves.
        printLog("onWakeupWord", "$wakeupWord, $direction, $origin")
    }

    override fun onTtsStatusChanged(ttsRequest: TtsRequest) {
        // Do whatever you like upon the status changing. after the robot finishes speaking
        printLog("onTtsStatusChanged: $ttsRequest")
    }

    override fun onBeWithMeStatusChanged(status: String) {
        //  When status changes to "lock" the robot recognizes the user and begin to follow.
        printLog("BeWithMeStatus: $status")
    }

    override fun onGoToLocationStatusChanged(
        location: String,
        status: String,
        descriptionId: Int,
        description: String
    ) {
        printLog("GoToStatusChanged: status=$status, descriptionId=$descriptionId, description=$description")
        robot.speak(create(status, false))
        if (description.isNotBlank()) {
            robot.speak(create(description, false))
        }
    }

    override fun onConversationAttaches(isAttached: Boolean) {
        //Do something as soon as the conversation is displayed.
        printLog("onConversationAttaches", "isAttached:$isAttached")
    }

    override fun onPublish(message: ActivityStreamPublishMessage) {
        //After the activity stream finished publishing (photo or otherwise).
        //Do what you want based on the message returned.
        printLog("onActivityPublish - $message")
    }

    override fun onLocationsUpdated(locations: List<String>) {
        //Saving or deleting a location will update the list.
        printLog("Locations updated :\n$locations")
    }

    private fun wakeup() {
        robot.wakeup()
    }

    private fun wakeupCustomLanguages() {
        robot.wakeup(listOf(SttLanguage.SYSTEM, SttLanguage.ZH_HK, SttLanguage.KO_KR))
    }

    private fun setAsrLanguages() {
        if (!robot.isSelectedKioskApp()) {
            return
        }
        val ret = robot.setAsrLanguages(listOf(SttLanguage.SYSTEM, SttLanguage.ZH_HK, SttLanguage.KO_KR))
        printLog("setAsrLanguages: $ret")
    }

    private fun disableWakeup() {
        if (requestPermissionIfNeeded(Permission.SETTINGS, REQUEST_CODE_NORMAL)) {
            return
        }
        robot.toggleWakeup(true)
    }

    private fun enableWakeup() {
        if (requestPermissionIfNeeded(Permission.SETTINGS, REQUEST_CODE_NORMAL)) {
            return
        }
        robot.toggleWakeup(false)
    }

    private fun toggleNavBillboard() {
        if (requestPermissionIfNeeded(Permission.SETTINGS, REQUEST_CODE_NORMAL)) {
            return
        }
        robot.toggleNavigationBillboard(!robot.navigationBillboardDisabled)
    }

    override fun onConstraintBeWithStatusChanged(isConstraint: Boolean) {
        printLog("onConstraintBeWith", "ConstraintBeWith = $isConstraint")
    }

    override fun onDetectionStateChanged(state: Int) {
        tvDetectionState.text =
            "Detect State -> ${OnDetectionStateChangedListener.DetectionStatus.fromValue(state)}"
    }

    /**
     * If you want to cover the voice flow in Launcher OS,
     * please add following meta-data to AndroidManifest.xml.
     * <pre>
     * <meta-data android:name="com.robotemi.sdk.metadata.KIOSK" android:value="true"></meta-data>
     *
     * <meta-data android:name="com.robotemi.sdk.metadata.OVERRIDE_NLU" android:value="true"></meta-data>
     * <pre>
     * And also need to select this App as the Kiosk Mode App in Settings > App > Kiosk.
     *
     * @param asrResult The result of the ASR after waking up temi.
     * @param sttLanguage The detected language of the ASR result, default is SYSTEM
    </pre></pre> */
    override fun onAsrResult(asrResult: String, sttLanguage: SttLanguage) {
        printLog("onAsrResult", "asrResult = $asrResult, sttLanguage = $sttLanguage")
        try {
            val metadata = packageManager
                .getApplicationInfo(packageName, PackageManager.GET_META_DATA).metaData
                ?: return
            if (!robot.isSelectedKioskApp()) return
            if (!metadata.getBoolean(SdkConstants.METADATA_OVERRIDE_NLU)) return
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            return
        }
        when {
            asrResult.equals("Hello", ignoreCase = true) -> {
                robot.askQuestion("Hello, I'm temi, what can I do for you?")
            }
            asrResult.equals("Play music", ignoreCase = true) -> {
                robot.finishConversation()
                robot.speak(create("Okay, please enjoy.", false))
                playMusic()
            }
            asrResult.equals("Play movie", ignoreCase = true) -> {
                robot.finishConversation()
                robot.speak(create("Okay, please enjoy.", false))
                playMovie()
            }
            asrResult.lowercase().contains("follow me") -> {
                robot.finishConversation()
                robot.beWithMe()
            }
            asrResult.lowercase().contains("go to home base") -> {
                robot.finishConversation()
                robot.goTo("home base")
            }
            else -> {
                robot.askQuestion("Sorry I can't understand you, could you please ask something else?")
            }
        }
    }

    private fun playMovie() {
        // Play movie...
        printLog("onAsrResult", "Play movie...")
    }

    private fun playMusic() {
        // Play music...
        printLog("onAsrResult", "Play music...")
    }

    private fun privacyModeOn() {
        robot.privacyMode = true
        printLog("Privacy mode: ${robot.privacyMode}")
    }

    private fun privacyModeOff() {
        robot.privacyMode = false
        printLog("Privacy mode: ${robot.privacyMode}")
    }

    private fun getPrivacyModeState() {
        printLog("Privacy mode: ${robot.privacyMode}")
    }

    private fun isHardButtonsEnabled() {
        printLog("Hard buttons disabled: ${robot.isHardButtonsDisabled}")
    }

    private fun disableHardButtons() {
        robot.isHardButtonsDisabled = true
        printLog("Hard buttons disabled: ${robot.isHardButtonsDisabled}")
    }

    private fun enableHardButtons() {
        robot.isHardButtonsDisabled = false
        printLog("Hard buttons disabled: ${robot.isHardButtonsDisabled}")
    }

    private fun getOSVersion() {
        printLog("LauncherOs: ${robot.launcherVersion}, RoboxVersion: ${robot.roboxVersion}")
    }

    override fun onTelepresenceEventChanged(callEventModel: CallEventModel) {
        printLog("onTelepresenceEvent", callEventModel.toString())
    }

    override fun onRequestPermissionResult(
        permission: Permission,
        grantResult: Int,
        requestCode: Int
    ) {
        val log = String.format("Permission: %s, grantResult: %d", permission.value, grantResult)
        printLog("onRequestPermission", log)
        if (grantResult == Permission.DENIED) {
            return
        }
        when (permission) {
            Permission.FACE_RECOGNITION -> if (requestCode == REQUEST_CODE_FACE_START) {
                robot.startFaceRecognition()
            } else if (requestCode == REQUEST_CODE_FACE_STOP) {
                robot.stopFaceRecognition()
            }
            Permission.SEQUENCE -> when (requestCode) {
                REQUEST_CODE_SEQUENCE_FETCH_ALL -> {
                    getAllSequences()
                }
                REQUEST_CODE_SEQUENCE_PLAY -> {
                    playFirstSequence(true)
                }
                REQUEST_CODE_SEQUENCE_PLAY_WITHOUT_PLAYER -> {
                    playFirstSequence(false)
                }
            }
            Permission.MAP -> when (requestCode) {
                REQUEST_CODE_MAP -> {
                    getMap()
                }
                REQUEST_CODE_GET_MAP_LIST -> {
                    getMapList()
                }
                REQUEST_CODE_GET_ALL_FLOORS -> {
                    getAllFloors()
                }
            }
            Permission.SETTINGS -> if (requestCode == REQUEST_CODE_START_DETECTION_WITH_DISTANCE) {
                startDetectionWithDistance()
            }
            else -> {
                // no-op
            }
        }
    }

    private fun requestFace() {
        if (robot.checkSelfPermission(Permission.FACE_RECOGNITION) == Permission.GRANTED) {
            printLog("You already had FACE_RECOGNITION permission.")
            return
        }
        val permissions: MutableList<Permission> = ArrayList()
        permissions.add(Permission.FACE_RECOGNITION)
        robot.requestPermissions(permissions, REQUEST_CODE_NORMAL)
    }

    private fun requestMap() {
        if (robot.checkSelfPermission(Permission.MAP) == Permission.GRANTED) {
            printLog("You already had MAP permission.")
            return
        }
        val permissions: MutableList<Permission> = ArrayList()
        permissions.add(Permission.MAP)
        robot.requestPermissions(permissions, REQUEST_CODE_NORMAL)
    }

    private fun requestSettings() {
        if (robot.checkSelfPermission(Permission.SETTINGS) == Permission.GRANTED) {
            printLog("You already had SETTINGS permission.")
            return
        }
        val permissions: MutableList<Permission> = ArrayList()
        permissions.add(Permission.SETTINGS)
        robot.requestPermissions(permissions, REQUEST_CODE_NORMAL)
    }

    private fun requestSequence() {
        if (robot.checkSelfPermission(Permission.SEQUENCE) == Permission.GRANTED) {
            printLog("You already had SEQUENCE permission.")
            return
        }
        val permissions: MutableList<Permission> = ArrayList()
        permissions.add(Permission.SEQUENCE)
        robot.requestPermissions(permissions, REQUEST_CODE_NORMAL)
    }

    private fun requestMeetings() {
        if (robot.checkSelfPermission(Permission.MEETINGS) == Permission.GRANTED) {
            printLog("You already had MEETINGS permission.")
            return
        }
        val permissions: MutableList<Permission> = ArrayList()
        permissions.add(Permission.MEETINGS)
        robot.requestPermissions(permissions, REQUEST_CODE_NORMAL)
    }

    private fun requestAll() {
        val permissions: MutableList<Permission> = ArrayList()
        for (permission in Permission.values()) {
            if (robot.checkSelfPermission(permission) == Permission.GRANTED) {
                printLog("You already had $permission permission.")
                continue
            }
            permissions.add(permission)
        }
        robot.requestPermissions(permissions, REQUEST_CODE_NORMAL)
    }

    private fun startFaceRecognition() {
        if (requestPermissionIfNeeded(Permission.FACE_RECOGNITION, REQUEST_CODE_FACE_START)) {
            return
        }
        robot.startFaceRecognition()
    }

    private fun stopFaceRecognition() {
        robot.stopFaceRecognition()
    }

    private fun testFaceRecognition() {
        startActivity(Intent(this, FaceActivity::class.java))
    }

    private fun setGoToSpeed() {
        if (requestPermissionIfNeeded(Permission.SETTINGS, REQUEST_CODE_NORMAL)) {
            return
        }
        printLog("Current go to speed ${robot.goToSpeed}")
        val speedLevels: MutableList<String> = ArrayList()
        speedLevels.add(SpeedLevel.HIGH.value)
        speedLevels.add(SpeedLevel.MEDIUM.value)
        speedLevels.add(SpeedLevel.SLOW.value)
        val adapter = ArrayAdapter(this, R.layout.item_dialog_row, R.id.name, speedLevels)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Select Go To Speed Level")
            .setAdapter(adapter, null)
            .create()
        dialog.listView.onItemClickListener =
            OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
                robot.goToSpeed = SpeedLevel.valueToEnum(adapter.getItem(position)!!)
                printLog("Set go to speed to: ${adapter.getItem(position)}")
                dialog.dismiss()
            }
        dialog.show()
    }

    private fun setFollowSpeed() {
        if (requestPermissionIfNeeded(Permission.SETTINGS, REQUEST_CODE_NORMAL)) {
            return
        }
        printLog("Current follow speed ${robot.getFollowSpeed()}")
        val speedLevels: MutableList<String> = ArrayList()
        speedLevels.add(SpeedLevel.HIGH.value)
        speedLevels.add(SpeedLevel.MEDIUM.value)
        speedLevels.add(SpeedLevel.SLOW.value)
        val adapter = ArrayAdapter(this, R.layout.item_dialog_row, R.id.name, speedLevels)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Select Follow Speed Level")
            .setAdapter(adapter, null)
            .create()
        dialog.listView.onItemClickListener =
            OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
                val resp = robot.setFollowSpeed(SpeedLevel.valueToEnum(adapter.getItem(position)!!))
                printLog("Set follow speed to: ${adapter.getItem(position)}, response $resp")
                dialog.dismiss()
            }
        dialog.show()
    }

    private fun setGoToSafety() {
        if (requestPermissionIfNeeded(Permission.SETTINGS, REQUEST_CODE_NORMAL)) {
            return
        }
        printLog("Current navigation safety ${robot.navigationSafety}")
        val safetyLevel: MutableList<String> = ArrayList()
        safetyLevel.add(SafetyLevel.HIGH.value)
        safetyLevel.add(SafetyLevel.MEDIUM.value)
        val adapter = ArrayAdapter(this, R.layout.item_dialog_row, R.id.name, safetyLevel)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Select Go To Safety Level")
            .setAdapter(adapter, null)
            .create()
        dialog.listView.onItemClickListener =
            OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
                robot.navigationSafety = SafetyLevel.valueToEnum(adapter.getItem(position)!!)
                printLog("Set go to safety level to: ${adapter.getItem(position)}")
                dialog.dismiss()
            }
        dialog.show()
    }

    private fun toggleTopBadge() {
        if (requestPermissionIfNeeded(Permission.SETTINGS, REQUEST_CODE_NORMAL)) {
            return
        }
        robot.topBadgeEnabled = !robot.topBadgeEnabled
    }

    private fun toggleDetectionMode() {
        if (requestPermissionIfNeeded(Permission.SETTINGS, REQUEST_CODE_NORMAL)) {
            return
        }
        robot.detectionModeOn = !robot.detectionModeOn
    }

    private fun toggleAutoReturn() {
        if (requestPermissionIfNeeded(Permission.SETTINGS, REQUEST_CODE_NORMAL)) {
            return
        }
        robot.autoReturnOn = !robot.autoReturnOn
    }

    private fun toggleTrackUser() {
        if (requestPermissionIfNeeded(Permission.SETTINGS, REQUEST_CODE_NORMAL)) {
            return
        }
        robot.trackUserOn = !robot.trackUserOn
    }

    private fun getVolume() {
        printLog("Current volume is: " + robot.volume)
    }

    private fun setVolume() {
        if (requestPermissionIfNeeded(Permission.SETTINGS, REQUEST_CODE_NORMAL)) {
            return
        }
        val volumeList: List<String> =
            ArrayList(listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10"))
        val adapter = ArrayAdapter(this, R.layout.item_dialog_row, R.id.name, volumeList)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Set Volume")
            .setAdapter(adapter, null)
            .create()
        dialog.listView.onItemClickListener =
            OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
                robot.volume = adapter.getItem(position)!!.toInt()
                printLog("Set volume to ${adapter.getItem(position)}")
                dialog.dismiss()
            }
        dialog.show()
    }

    private fun setMicGainLevel() {
        if (requestPermissionIfNeeded(Permission.SETTINGS, REQUEST_CODE_NORMAL)) {
            return
        }
        val micGainLevelList: List<String> =
            ArrayList(listOf("1", "2", "3", "4"))
        val adapter = ArrayAdapter(this, R.layout.item_dialog_row, R.id.name, micGainLevelList)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Set Microphone Gain Level to X1-X4")
            .setAdapter(adapter, null)
            .create()
        dialog.listView.onItemClickListener =
            OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
                val result = robot.setMicGainLevel(adapter.getItem(position)!!.toInt())
                printLog("Set Microphone Gain Level to X${adapter.getItem(position)!!.toInt()} with result $result")
                dialog.dismiss()
            }
        dialog.show()
    }

    private fun requestToBeKioskApp() {
        if (robot.isSelectedKioskApp()) {
            printLog("${getString(R.string.app_name)} was the selected Kiosk App.")
            return
        }
        robot.requestToBeKioskApp()
    }

    private fun startDetectionWithDistance() {
        hideKeyboard()
        if (requestPermissionIfNeeded(
                Permission.SETTINGS,
                REQUEST_CODE_START_DETECTION_WITH_DISTANCE
            )
        ) {
            return
        }
        var distanceStr = etDistance.text.toString()
        if (distanceStr.isEmpty()) distanceStr = "0"
        try {
            val distance = distanceStr.toFloat()
            robot.setDetectionModeOn(true, distance)
            printLog("Start detection mode with distance: $distance")
        } catch (e: Exception) {
            printLog("startDetectionModeWithDistance", e.message ?: "")
        }
    }

    override fun onDistanceToLocationChanged(distances: Map<String, Float>) {
        var text = "Distance:\n"
        for (location in distances.keys) {
            text +=
                " -> $location :: ${distances[location]}\n"
        }
        tvDistance.text = text
    }

    @SuppressLint("SetTextI18n")
    override fun onCurrentPositionChanged(position: Position) {
        tvPosition.text =
            "Position -> {${position.x}, ${position.y}, ${position.yaw}}, tilt: ${position.tiltAngle}"
    }

    override fun onSequencePlayStatusChanged(status: Int) {
        printLog(String.format("onSequencePlayStatus status:%d", status))
        if (status == OnSequencePlayStatusChangedListener.ERROR
            || status == OnSequencePlayStatusChangedListener.IDLE
        ) {
            robot.showTopBar()
        }
    }

    override fun onRobotLifted(isLifted: Boolean, reason: String) {
        printLog("onRobotLifted: isLifted: $isLifted, reason: $reason")
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        hideKeyboard()
        return super.dispatchTouchEvent(ev)
    }

    @CheckResult
    private fun requestPermissionIfNeeded(permission: Permission, requestCode: Int): Boolean {
        if (robot.checkSelfPermission(permission) == Permission.GRANTED) {
            return false
        }
        robot.requestPermissions(listOf(permission), requestCode)
        return true
    }

    override fun onDetectionDataChanged(detectionData: DetectionData) {
        tvDetection.text = if (detectionData.isDetected) {
            "Detect -> angle ${detectionData.angle}, dist ${detectionData.distance}"
        } else {
            "No detection"
        }
    }

    override fun onUserInteraction(isInteracting: Boolean) {
        printLog("onUserInteraction", "isInteracting:$isInteracting")
    }

    @Volatile
    private var allSequences: List<SequenceModel> = emptyList()

    private fun getAllSequences() {
        if (requestPermissionIfNeeded(Permission.SEQUENCE, REQUEST_CODE_SEQUENCE_FETCH_ALL)) {
            return
        }
        Thread {
            allSequences = robot.getAllSequences(etNlu.text?.split(",") ?: emptyList())
            printLog("allSequences: ${allSequences.size}", false)
            val imageKeys: MutableList<String> = ArrayList()
            for ((_, _, _, imageKey) in allSequences) {
                if (imageKey.isEmpty()) continue
                imageKeys.add(imageKey)
            }
            val pairs = if (imageKeys.isEmpty()) {
                emptyList()
            } else {
                robot.getSignedUrlByMediaKey(imageKeys)
            }
            runOnUiThread {
                for (sequenceModel in allSequences) {
                    printLog(sequenceModel.toString())
                }
                for (pair in pairs) {
                    printLog(pair.component2(), false)
                }
            }
        }.start()
    }

    private fun playFirstSequence() {
        if (requestPermissionIfNeeded(Permission.SEQUENCE, REQUEST_CODE_SEQUENCE_PLAY)) {
            return
        }
        playFirstSequence(true)
    }

    private fun playFirstSequenceWithoutPlayer() {
        if (requestPermissionIfNeeded(
                Permission.SEQUENCE,
                REQUEST_CODE_SEQUENCE_PLAY_WITHOUT_PLAYER
            )
        ) {
            return
        }
        playFirstSequence(false)
    }

    private fun playFirstSequence(withPlayer: Boolean) {
        if (!allSequences.isNullOrEmpty()) {
            robot.playSequence(allSequences[0].id, withPlayer)
        }
    }

    @Volatile
    private var allTours: List<TourModel> = emptyList()

    private fun getAllTours() {
        if (requestPermissionIfNeeded(Permission.SEQUENCE, REQUEST_CODE_SEQUENCE_FETCH_ALL)) {
            return
        }
        Thread {
            allTours = robot.getAllTours()
            printLog("allTours: ${allTours.size}", false)
            runOnUiThread {
                for (tourGuide in allTours) {
                    printLog(tourGuide.toString())
                }
            }
        }.start()
    }

    private fun playFirstTour() {
        if (requestPermissionIfNeeded(Permission.SEQUENCE, REQUEST_CODE_SEQUENCE_PLAY)) {
            return
        }
        if (allTours.isNotEmpty()) {
            val ret = robot.playTour(allTours[0].id)
            printLog("playTour: $ret")
        }
    }

    private fun getMap() {
        if (requestPermissionIfNeeded(Permission.MAP, REQUEST_CODE_MAP)) {
            return
        }
        startActivity(Intent(this, MapActivity::class.java))
    }

    override fun onFaceRecognized(contactModelList: List<ContactModel>) {
        if (contactModelList.isEmpty()) {
            printLog("onFaceRecognized: User left")
            imageViewFace.visibility = View.INVISIBLE
            return
        }

        val imageKey = contactModelList.find { it.imageKey.isNotBlank() }?.imageKey
        if (!imageKey.isNullOrBlank()) {
            showFaceRecognitionImage(imageKey)
        } else {
            imageViewFace.setImageResource(R.drawable.app_icon)
            imageViewFace.visibility = View.VISIBLE
        }

        for (contactModel in contactModelList) {
            when (contactModel.userType) {
                0, 1 -> {
                    printLog("onFaceRecognized: ${contactModel.firstName} ${contactModel.lastName}")
                }
                2 -> {
                    Log.d(
                        "SAMPLE_DEBUG",
                        "VISITOR - onFaceRecognized ${contactModel.userId}, similarity ${contactModel.similarity}, age ${contactModel.age}, gender ${contactModel.gender}, faceRect ${contactModel.faceRect}"
                    )
                    printLog("onFaceRecognized: VISITOR ${contactModel.userId} ${contactModel.similarity}")
                }
                3 -> {
                    Log.d(
                        "SAMPLE_DEBUG",
                        "SDK Face - onFaceRecognized ${contactModel.userId}, ${contactModel.firstName}, similarity ${contactModel.similarity}, age ${contactModel.age}, gender ${contactModel.gender}, faceRect ${contactModel.faceRect}"
                    )
                    printLog("onFaceRecognized: SDK Face ${contactModel.userId}, ${contactModel.firstName}, similarity ${contactModel.similarity}, age ${contactModel.age}, gender ${contactModel.gender}, faceRect ${contactModel.faceRect}")
                }
                -1 -> {
                    printLog("onFaceRecognized: Unknown face, faceId ${contactModel.userId}, age ${contactModel.age}, gender ${contactModel.gender}, faceRect ${contactModel.faceRect}")
                }
            }
        }
    }

    override fun onContinuousFaceRecognized(contactModelList: List<ContactModel>) {
        var text = ""
        if (contactModelList.isEmpty()) {
            text = "onContinuousFaceRecognized: User left"
            imageViewFace.visibility = View.INVISIBLE
            tvContinuousFace.text = text
            return
        }

        val imageKey = contactModelList.find { it.imageKey.isNotBlank() }?.imageKey
        if (!imageKey.isNullOrBlank()) {
            showFaceRecognitionImage(imageKey)
        } else {
            imageViewFace.setImageResource(R.drawable.app_icon)
            imageViewFace.visibility = View.VISIBLE
        }

        text = "onContinuousFaceRecognized:\n"
        val blinker = listOf("\\", "|", "/", "-").random()
        for (contactModel in contactModelList) {
            Log.d("SAMPLE_DEBUG", "Contact $contactModel")
            text += when (contactModel.userType) {
                0, 1 -> {
                    "$blinker ${contactModel.firstName} ${contactModel.lastName}\n"
                }
                2 -> {
                    Log.d(
                        "SAMPLE_DEBUG",
                        "VISITOR - onContinuousFaceRecognized ${contactModel.userId}, similarity ${contactModel.similarity}, age ${contactModel.age}, gender ${contactModel.gender}, faceRect ${contactModel.faceRect}"
                    )
                    "$blinker  VISITOR ${contactModel.userId} similarity ${contactModel.similarity}\n"
                }
                3 -> {
                    Log.d(
                        "SAMPLE_DEBUG",
                        "SDK Face - onContinuousFaceRecognized ${contactModel.userId}, similarity ${contactModel.similarity}, age ${contactModel.age}, gender ${contactModel.gender}, faceRect ${contactModel.faceRect}"
                    )
                    "$blinker  SDK Face ${contactModel.userId} -> ${contactModel.firstName}, similarity ${contactModel.similarity}\n"
                }
                else -> {
                    "$blinker Unknown face, faceId ${contactModel.userId}, age ${contactModel.age}, gender ${contactModel.gender}, faceRect ${contactModel.faceRect}\n"
                }
            }
        }

        tvContinuousFace.text = text
    }

    private fun showFaceRecognitionImage(mediaKey: String) {
        if (mediaKey.isEmpty()) {
            imageViewFace.setImageResource(R.drawable.app_icon)
            imageViewFace.visibility = View.INVISIBLE
            return
        }
        executorService.execute {
            val inputStream =
                robot.getInputStreamByMediaKey(ContentType.FACE_RECOGNITION_IMAGE, mediaKey)
                    ?: return@execute
            runOnUiThread {
                imageViewFace.visibility = View.VISIBLE
                imageViewFace.setImageBitmap(BitmapFactory.decodeStream(inputStream))
                try {
                    inputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun printLog(msg: String, show: Boolean = true) {
        printLog("", msg, show)
    }

    private fun printLog(tag: String, msg: String, show: Boolean = true) {
        Log.d(tag.ifEmpty { "MainActivity" }, msg)
        if (!show) return
        runOnUiThread {
            tvLog.gravity = Gravity.BOTTOM
            tvLog.append("· $msg \n")
        }
    }

    private fun clearLog() {
        tvLog.text = ""
        tvContinuousFace.text = ""
        imageViewFace.visibility = View.GONE
    }

    private fun startNlu() {
        robot.startDefaultNlu(etNlu.text.toString())
    }

    override fun onSdkError(sdkException: SdkException) {
        printLog("onSdkError: $sdkException")
    }

    private fun getAllContacts() {
        val allContacts = robot.allContact
        val recentCalls = robot.recentCalls
        for (userInfo in allContacts) {
            printLog("UserInfo: $userInfo")
        }
        for (recentCall in recentCalls) {
            printLog("RecentCall: $recentCall")
        }
    }

    private fun goToPosition() {
        try {
            val x = etX.text.toString().toFloat()
            val y = etY.text.toString().toFloat()
            val yaw = etYaw.text.toString().toFloat()
            robot.goToPosition(Position(x, y, yaw, 0), backwards = false, noBypass = false)
        } catch (e: Exception) {
            e.printStackTrace()
            printLog(e.message ?: "")
        }
    }

    override fun onConversationStatusChanged(status: Int, text: String) {
        printLog("Conversation", "status=$status, text=$text")
    }

    override fun onTtsVisualizerWaveFormDataChanged(waveForm: ByteArray) {
        visualizerView.visibility = View.VISIBLE
        visualizerView.updateVisualizer(waveForm)
    }

    override fun onTtsVisualizerFftDataChanged(fft: ByteArray) {
//        Log.d("TtsVisualizer", fft.contentToString())
//        ttsVisualizerView.updateVisualizer(fft);
    }

    private fun startTelepresenceToCenter() {
        val target = robot.adminInfo
        if (target == null) {
            printLog("target is null.")
            return
        }
        robot.startTelepresence(target.name, target.userId, Platform.TEMI_CENTER)
    }

    private fun startMeeting() {
        val target = robot.adminInfo
        if (target == null) {
            printLog("target is null.")
            return
        }
        val resp = robot.startMeeting(listOf(
            Participant(target.userId, Platform.MOBILE),
            Participant(target.userId, Platform.TEMI_CENTER),
        ), firstParticipantJoinedAsHost = true)
        Log.d("MainActivity", "startMeeting result $resp")
    }

    private fun startPage() {
        val systemPages: MutableList<String> = ArrayList()
        for (page in Page.values()) {
            systemPages.add(page.toString())
        }
        val arrayAdapter = ArrayAdapter(this, R.layout.item_dialog_row, R.id.name, systemPages)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Select System Page")
            .setAdapter(arrayAdapter, null)
            .create()
        dialog.listView.onItemClickListener =
            OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
                robot.startPage(
                    Page.values()[position]
                )
                dialog.dismiss()
            }
        dialog.show()
    }

    private fun restartTemi() {
        robot.restart()
    }

    private fun getMembersStatus() {
        val memberStatusModels = robot.membersStatus
        for (memberStatusModel in memberStatusModels) {
            printLog(memberStatusModel.toString())
        }
    }

    private fun repose() {
//        Repose with position
//        robot.repose(Position(1.516081f, 3.1614602f, 3.6307523f))
//        robot.repose(Position(0f,0f,0f))

//        Repose without position
        robot.repose()
    }

    override fun onReposeStatusChanged(status: Int, description: String) {
        printLog("repose status: $status, description: $description")
    }

    override fun onLoadMapStatusChanged(status: Int, requestId: String) {
        printLog("load map status: $status, requestId: $requestId")
    }

    private var mapList: List<MapModel> = ArrayList()
    private fun getMapList() {
        if (requestPermissionIfNeeded(Permission.MAP, REQUEST_CODE_GET_MAP_LIST)) {
            return
        }
        mapList = robot.getMapList()
    }

    private fun loadMap(
        reposeRequired: Boolean,
        position: Position?,
        offline: Boolean = false,
        withoutUI: Boolean = false
    ) {
        if (mapList.isEmpty()) {
            getMapList()
        }
        if (robot.checkSelfPermission(Permission.MAP) != Permission.GRANTED) {
            return
        }
        val mapListString: MutableList<String> = ArrayList()
        for (i in mapList.indices) {
            mapListString.add(mapList[i].name)
        }
        val mapListAdapter = ArrayAdapter(this, R.layout.item_dialog_row, R.id.name, mapListString)
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Click item to load specific map")
        builder.setAdapter(mapListAdapter, null)
        val dialog = builder.create()
        dialog.listView.onItemClickListener =
            OnItemClickListener { _, _, pos: Int, _ ->
                val requestId =
                    robot.loadMap(
                        mapList[pos].id,
                        reposeRequired,
                        position,
                        offline = offline,
                        withoutUI = withoutUI
                    )
                printLog("Loading map: ${mapList[pos]}, request id $requestId, reposeRequired $reposeRequired, position $position, offline $offline, withoutUI $withoutUI")
                dialog.dismiss()
            }
        dialog.show()
    }

    private fun loadMapToCache() {
        if (mapList.isEmpty()) {
            getMapList()
        }
        if (robot.checkSelfPermission(Permission.MAP) != Permission.GRANTED) {
            return
        }
        val mapListString: MutableList<String> = ArrayList()
        for (i in mapList.indices) {
            mapListString.add(mapList[i].name)
        }
        val mapListAdapter = ArrayAdapter(this, R.layout.item_dialog_row, R.id.name, mapListString)
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Click item to load specific map")
        builder.setAdapter(mapListAdapter, null)
        val dialog = builder.create()
        dialog.listView.onItemClickListener =
            OnItemClickListener { _, _, pos: Int, _ ->
                val requestId = robot.loadMapToCache(mapList[pos].id)
                printLog("Loading map to cache: " + mapList[pos] + " request id " + requestId)
                dialog.dismiss()
            }
        dialog.show()
    }

    private fun getMapListBtn() {
        getMapList()
        for (mapModel in mapList) {
            printLog("Map: $mapModel")
        }
    }

    private fun loadMap() {
        loadMap(false, null)
    }

    override fun onDisabledFeatureListUpdated(disabledFeatureList: List<String>) {
        printLog("Disabled features: $disabledFeatureList")
    }

    private fun lock() {
        robot.locked = true
        printLog("Is temi locked: " + robot.locked)
    }

    private fun unlock() {
        robot.locked = false
        printLog("Is temi locked: " + robot.locked)
    }

    private fun muteAlexa() {
        if (robot.launcherVersion.contains("usa")) {
            printLog("Mute Alexa")
            robot.muteAlexa()
            return
        }
        printLog("muteAlexa() is useful only for Global version")
    }

    private fun shutdown() {
        if (!robot.isSelectedKioskApp()) {
            return
        }
        val builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("Shutdown temi?").create()
        builder.setPositiveButton("Yes") { _: DialogInterface?, _: Int ->
            printLog("shutdown")
            robot.shutdown()
        }
        builder.setNegativeButton("No") { _: DialogInterface?, _: Int -> }
        builder.create().show()
    }

    override fun onMovementVelocityChanged(velocity: Float) {
        printLog("Movement velocity: " + velocity + "m/s")
    }

    private fun loadMapWithPosition() {
        loadMapWithPosition(false)
    }

    private fun loadMapWithReposePosition() {
        loadMapWithPosition(true)
    }

    private fun loadMapWithRepose() {
        loadMap(true, null)
    }

    private fun loadMapWithPosition(reposeRequired: Boolean) {
        try {
            val x = etX.text.toString().toFloat()
            val y = etY.text.toString().toFloat()
            val yaw = etYaw.text.toString().toFloat()
            loadMap(true, Position(x, y, yaw, 0))
            val position = Position(x, y, yaw, 0)
            loadMap(reposeRequired, position)
        } catch (e: Exception) {
            e.printStackTrace()
            printLog(e.message ?: "")
        }
    }

    private fun getTts() {
        printLog("Get TTS Voice result ${robot.getTtsVoice()}")
    }

    private fun setTts() {
        val result = robot.setTtsVoice(TtsVoice(gender = Gender.MALE, speed = 0.5f, pitch = -2))
        printLog("Set TTS Voice result $result")
    }

    override fun onMovementStatusChanged(type: String, status: String) {
        printLog("Movement response - $type status: $status")
    }

    companion object {
        const val ACTION_HOME_WELCOME = "home.welcome"
        const val ACTION_HOME_DANCE = "home.dance"
        const val ACTION_HOME_SLEEP = "home.sleep"
        const val HOME_BASE_LOCATION = "home base"

        // Storage Permissions
        private const val REQUEST_EXTERNAL_STORAGE = 1
        private const val REQUEST_CODE_NORMAL = 0
        private const val REQUEST_CODE_FACE_START = 1
        private const val REQUEST_CODE_FACE_STOP = 2
        private const val REQUEST_CODE_MAP = 3
        private const val REQUEST_CODE_SEQUENCE_FETCH_ALL = 4
        private const val REQUEST_CODE_SEQUENCE_PLAY = 5
        private const val REQUEST_CODE_START_DETECTION_WITH_DISTANCE = 6
        private const val REQUEST_CODE_SEQUENCE_PLAY_WITHOUT_PLAYER = 7
        private const val REQUEST_CODE_GET_MAP_LIST = 8
        private const val REQUEST_CODE_GET_ALL_FLOORS = 9
        private val PERMISSIONS_STORAGE = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        /**
         * Checks if the app has permission to write to device storage
         * If the app does not has permission then the user will be prompted to grant permissions
         */
        fun verifyStoragePermissions(activity: Activity?) {
            // Check if we have write permission
            val permission = ActivityCompat.checkSelfPermission(
                activity!!,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // We don't have permission so prompt the user
                ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
                )
            }
        }
    }

    private var mTtsRequest: TtsRequest? = null

    override fun speak(ttsRequest: TtsRequest) {
        printLog("custom tts speak --> ttsRequest=$ttsRequest")
        mTtsRequest = ttsRequest
        val language = when (TtsRequest.Language.valueToEnum(ttsRequest.language)) {
            TtsRequest.Language.ZH_CN -> Locale.SIMPLIFIED_CHINESE
            TtsRequest.Language.EN_US -> Locale.US
            TtsRequest.Language.ZH_HK -> Locale("zh", "HK")
            TtsRequest.Language.ZH_TW -> Locale("zh", "Taiwan")
            TtsRequest.Language.TH_TH -> Locale("th", "TH")
            TtsRequest.Language.HE_IL -> Locale("iw", "IL")
            TtsRequest.Language.KO_KR -> Locale("ko", "ka")
            TtsRequest.Language.JA_JP -> Locale("ja", "JP")
            TtsRequest.Language.IN_ID -> Locale("in", "ID")
            TtsRequest.Language.ID_ID -> Locale("in", "ID")
            TtsRequest.Language.DE_DE -> Locale("de", "DE")
            TtsRequest.Language.FR_FR -> Locale("fr", "FR")
            TtsRequest.Language.FR_CA -> Locale("fr", "CA")
            TtsRequest.Language.PT_BR -> Locale("pt", "BR")
            TtsRequest.Language.AR_EG -> Locale("ar", "EG")
            TtsRequest.Language.AR_AE -> Locale("ar", "AE")
            TtsRequest.Language.AR_XA -> Locale("ar", "XA")
            TtsRequest.Language.RU_RU -> Locale("ru", "RU")
            TtsRequest.Language.IT_IT -> Locale("it", "IT")
            TtsRequest.Language.PL_PL -> Locale("pl", "PL")
            TtsRequest.Language.ES_ES -> Locale("es", "ES")
            TtsRequest.Language.CA_ES -> Locale("ca", "ES")
            TtsRequest.Language.HI_IN -> Locale("hi", "IN")
            TtsRequest.Language.ET_EE -> Locale("et", "EE")
            TtsRequest.Language.TR_TR -> Locale("tr", "TR")
            TtsRequest.Language.EN_IN -> Locale("en", "IN")
            TtsRequest.Language.MS_MY -> Locale("ms", "MY")
            TtsRequest.Language.VI_VN -> Locale("vi", "VN")
            TtsRequest.Language.EL_GR -> Locale("el", "GR")
            else -> if (robot.launcherVersion.contains("china")) {
                Locale.SIMPLIFIED_CHINESE
            } else {
                Locale.US
            }
        }
        tts?.language = language
        tts?.speak(ttsRequest.speech, TextToSpeech.QUEUE_ADD, Bundle(), ttsRequest.id.toString())
    }

    override fun cancel() {
        printLog("custom tts cancel")
        tts?.stop()
    }

    override fun pause() {
        printLog("custom tts pause")
    }

    override fun resume() {
        printLog("custom tts resume")
    }

    override fun onInit(status: Int) {
        printLog("TTS init status: $status")
        tts?.setPitch(0.5f)
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                printLog("custom tts start")
                if (mTtsRequest == null || utteranceId == null) {
                    return
                }
                robot.publishTtsStatus(
                    mTtsRequest!!.copy(
                        id = UUID.fromString(utteranceId),
                        status = TtsRequest.Status.STARTED
                    )
                )
            }

            override fun onDone(utteranceId: String?) {
                printLog("custom tts done")
                if (mTtsRequest == null || utteranceId == null) {
                    return
                }
                robot.publishTtsStatus(
                    mTtsRequest!!.copy(
                        id = UUID.fromString(utteranceId),
                        status = TtsRequest.Status.COMPLETED
                    )
                )
            }

            override fun onError(utteranceId: String?) {
                printLog("custom tts error")
                if (mTtsRequest == null || utteranceId == null) {
                    return
                }
                robot.publishTtsStatus(
                    mTtsRequest!!.copy(
                        id = UUID.fromString(utteranceId),
                        status = TtsRequest.Status.ERROR
                    )
                )
            }

            override fun onAudioAvailable(utteranceId: String?, audio: ByteArray?) {
                super.onAudioAvailable(utteranceId, audio)
                printLog("custom tts error")
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                super.onStop(utteranceId, interrupted)
                printLog("custom tts error")
                if (mTtsRequest == null || utteranceId == null) {
                    return
                }
                robot.publishTtsStatus(
                    mTtsRequest!!.copy(
                        id = UUID.fromString(utteranceId),
                        status = TtsRequest.Status.CANCELED
                    )
                )
            }

            override fun onBeginSynthesis(
                utteranceId: String?,
                sampleRateInHz: Int,
                audioFormat: Int,
                channelCount: Int
            ) {
                super.onBeginSynthesis(utteranceId, sampleRateInHz, audioFormat, channelCount)
                printLog("custom tts onBeginSynthesis")
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                super.onError(utteranceId, errorCode)
                printLog("custom tts onError")
                if (mTtsRequest == null || utteranceId == null) {
                    return
                }
                robot.publishTtsStatus(
                    mTtsRequest!!.copy(
                        id = UUID.fromString(utteranceId),
                        status = TtsRequest.Status.ERROR
                    )
                )
            }

            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                super.onRangeStart(utteranceId, start, end, frame)
                printLog("custom tts onRangeStart")
            }

        })
    }

    @SuppressLint("SetTextI18n")
    override fun onGreetModeStateChanged(state: Int) {
        tvGreetMode.text = "Greet Mode -> ${OnGreetModeStateChangedListener.State.fromValue(state)}"
    }

    override fun onLoadFloorStatusChanged(status: Int) {
        printLog("onLoadFloorStatusChanged: $status")
    }

    override fun onDistanceToDestinationChanged(location: String, distance: Float) {
        printLog("distance to destination: destination=$location, distance=$distance")
    }

    override fun onRobotDragStateChanged(isDragged: Boolean) {
        printLog("onRobotDragStateChanged $isDragged")
    }

    override fun onMapStatusChanged(isLost: Boolean, isLocked: Boolean) {
        Log.d("onMapStatusChanged", "isLost: $isLost, isLocked: $isLocked")
    }

    override fun onButtonStatusChanged(hardButton: HardButton, status: HardButton.Status) {
        Log.d("onButtonStatusChanged", "hardButton: $hardButton, status: $status")
    }
}