// ISdkService.aidl
package com.robotemi.sdk;

import android.content.pm.ActivityInfo;
import com.robotemi.sdk.ISdkServiceCallback;
import com.robotemi.sdk.SttRequest;
import com.robotemi.sdk.TtsRequest;
import com.robotemi.sdk.voice.WakeupRequest;
import com.robotemi.sdk.DisplayListRequest;
import com.robotemi.sdk.activitystream.ActivityStreamObject;
import com.robotemi.sdk.notification.AlertNotification;
import com.robotemi.sdk.notification.NormalNotification;
import com.robotemi.sdk.mediabar.MediaBarData;
import com.robotemi.sdk.UserInfo;
import com.robotemi.sdk.model.RecentCallModel;
import com.robotemi.sdk.BatteryData;
import com.robotemi.sdk.sequence.SequenceModel;
import com.robotemi.sdk.telepresence.Participant;
import com.robotemi.sdk.tourguide.TourModel;
import com.robotemi.sdk.navigation.model.Position;
import com.robotemi.sdk.map.MapDataModel;
import com.robotemi.sdk.model.MemberStatusModel;
import com.robotemi.sdk.map.MapModel;
import com.robotemi.sdk.map.Floor;
import com.robotemi.sdk.voice.model.TtsVoice;
import com.robotemi.sdk.map.Layer;

interface ISdkService {

    void speak(in TtsRequest ttsRequest);

    void register(in ApplicationInfo applicationInfo, ISdkServiceCallback callback);

    void shareActivityStreamObject(in ActivityStreamObject activityStreamObject);

    /**
     * Stops currently processed TTS request and empty the queue.
     */
    void cancelAll();

    /**
     * update the media bar data (title, subtitle, icon etc..)
     *
     * @param mediaBarData an object containg the media bar data (title, subtitle, icon etc..).
     */
    void setMedia(in MediaBarData mediaBarData);

    /**
    * pauses the media
    */
    void pause();

    /**
    * Indicate media is playing for specified packageName
    */
    void setMediaPlaying(in boolean isPlaying, in String packageName);

    /**
    * show a notification on top of the screen
    *
    * @param notification - the notification object containg the notification data
    */
    void showNormalNotification(in NormalNotification notification);

    /**
    * show a notification on top of the screen
    *
    * @param notification - the notification object containg the notification data
    */
    void showAlertNotification(in AlertNotification notification);

    /**
    * removes a spesific notification from the screen
    *
    * @param notification - the notification object containg the notification data
    */
    void removeAlertNotification(in AlertNotification notification);

    void onStart(in ActivityInfo activityInfo);

    /**
     * Request to lock contexts even if skill screen is dismissed.
     * Useful for services running in the background without UI.
     *
     * @param contextsToLock - List of contexts names to lock.
     */
    void lockContexts(in List<String> contextsToLock);

    /**
     * Release previously locked contexts. See {@link #lockContexts(List)}.
     *
     * @param contextsToRelease - List of contexts names to release.
     */
    void releaseContexts(in List<String> contextsToRelease);

    /**
     * Send robot to previously saved location.
     *
     * @param location - Saved location name.
     */
    void goTo(in String location, int backwards, int noBypass, in String speedLevel, int highAccuracyArrival, int noRotationAtEnd);

    /**
     * Retrieve list of previously saved locations.
     *
     * @return List of saved locations.
     */
    List<String> getLocations();

    /**
     * Save location.
     *
     * @param - Location name.
     *
     * @return Result of a successful or failed operation.
     */
    boolean saveLocation(in String name);

    /**
     * Request robot to follow the user.
     */
    void followMe();

    /**
     * Request robot to follow the user.
     */
    void beWithMe(in String speedLevel);

    void skidJoy(in float x, in float y, in boolean smart);

    void turnBy(in int azimuth, in float speed);

    void tiltAngle(in int degrees, in float speed);

    void tiltBy(in int degrees, in float speed);

    String startTelepresence(in String displayName, in String peerId, int platform);

    UserInfo getAdminInfo();

    List<UserInfo> getAllContacts();

    void showAppList();

    List<RecentCallModel> getRecentCalls();

    void showTopBar();

    void hideTopBar();

    /*
     * Request robot to stop any current movement.
     */
    void stopMovement();

    BatteryData getBatteryData();

    String getSerialNumber();

    /**
     * Request robot to toggle the wakeup trigger
     */
     void toggleWakeup(boolean disable, in String packageName);

     /**
     * Request robot to show/hide navigation billboards
     */
     void toggleNavigationBillboard(in boolean hide);

     /**
     * Delete location.
     *
     * @param name - Location name.
     *
     * @return Result of a successful or failed operation.
     */
    boolean deleteLocation(in String name);

    void wakeup(in int[] languages, in SttRequest sttRequest, in WakeupRequest wakeupRequest);

    String getWakeupWord();

     /**
     * Request robot to turn on/off privacy mode
     */
    void togglePrivacyMode(boolean on, in String packageName);

    boolean getPrivacyModeState();

    void constraintBeWith();

    void toggleHardButtons(boolean disable, in String packageName);

    boolean isHardButtonsDisabled();

    void askQuestion(in String question, in TtsRequest ttsRequest, in SttRequest sttRequest);

    void finishConversation();

    String getLauncherVersion();

    String getRoboxVersion();

    int checkSelfPermission(in String packageName, in String permission);

    void requestPermissions(in String packageName, in List<String> permissions, int requestCode);

    void requestToBeKioskApp(in String packageName);

    boolean isSelectedKioskApp(in String packageName);

    void setTopBadgeEnabled(in String packageName, boolean enabled);

    boolean isTopBadgeEnabled();

    void setGoToBillboardDisabled(in String packageName, boolean disabled);

    boolean isGoToBillboardDisabled();

    void setDetectionModeOn(in String packageName, boolean on, float distance);

    boolean isDetectionModeOn();

    void setTrackUserOn(in String packageName, boolean on);

    boolean isTrackUserOn();

    void setAutoReturnOn(in String packageName, boolean on);

    boolean isAutoReturnOn();

    void setVolume(in String packageName, int volume);

    int getVolume();

    void setNavigationSafety(in String packageName, in String safetyLevel);

    String getNavigationSafety();

    void setGoToSpeed(in String packageName, in String speedLevel);

    String getGoToSpeed();

    void startFaceRecognition(in String packageName, boolean withSdkFaces);

    void stopFaceRecognition(in String packageName);

    List<SequenceModel> getAllSequences(in String packageName, in List<String> tags);

    void playSequence(in String packageName, in String sequenceId, boolean withPlayer, int repeat);

    void goToPosition(in Position position, int backwards, int noBypass, in String speedLevel, int highAccuracyArrival);

    MapDataModel getMapData(in String packageName);

    void startDefaultNlu(in String packageName, in String content, int language);

    boolean isWakeupDisabled();

    void repose(in Position position);

    void restart(in String packageName);

    void startPage(in String packageName, in String page);

    List<MemberStatusModel> getMembersStatus();

    List<MapModel> getMapList(in String packageName);

    String loadMap(in String packageName, in String mapId, boolean reposeRequired, boolean offline, boolean withouUI);

    void lock(in String packageName, boolean isForLocking);

    boolean isLocked();

    void muteAlexa(in String packageName);

    String loadMapWithPosition(in String packageName, in String mapId, boolean reposeRequired, in Position position, boolean offline, boolean withouUI);

    void shutdown(in String packageName);

    List<String> getSignedUrlByMediaKey(in String packageName, in List<String> mediaKeys, int width, int height);

    void setSoundMode(in String packageName, int mode);

    void setHardButtonMode(in String packageName, int type, int mode);

    int getHardButtonMode(int type);

    String getNickName(in String packageName);

    void setMode(in String packageName, int mode);

    int getMode();

    void setKioskModeOn(in String packageName, boolean on, in String targetMode);

    boolean isKioskModeOn();

    Map getSupportedLatinKeyboards();

    void enabledLatinKeyboards(in String packageName, in List<String> keyboards);

    void publishTtsStatus(in String packageName, in TtsRequest ttsRequest);

    void controlSequence(in String packageName, int command);

    void setGroundDepthCliffDetectionEnabled(in String packageName, boolean enabled);

    boolean isGroundDepthCliffDetectionEnabled();

    boolean hasCliffSensor();

    void setCliffSensorMode(in String packageName, int mode);

    int getCliffSensorMode();

    void setHeadDepthSensitivity(in String packageName, int level);

    int getHeadDepthSensitivity();

    void setFrontTOFEnabled(in String packageName, boolean enabled);

    boolean isFrontTOFEnabled();

    void setBackTOFEnabled(in String packageName, boolean enabled);

    boolean isBackTOFEnabled();

    Floor getCurrentFloor(in String packageName);

    List<Floor> getAllFloors(in String packageName);

    void loadFloor(in String packageName, int floorId, in Position position);

    String loadMapToCache(in String packageName, in String mapId);

    boolean setTtsVoice(in String packageName, in TtsVoice ttsVoice);

    TtsVoice getTtsVoice();

    boolean isStandByOn();

    int startStandBy(in String packageName);

    int stopStandBy(in String packageName, in String password);

    boolean isMultiFloorEnabled();

    boolean setMultiFloorEnabled(in String packageName, boolean enabled);

    int patrol(in List<String> locations, boolean nonstop, int times, int waiting);

    // Serial
    int sendSerialCommand(int command, in byte[] data);

    int setInteractionState(in String packageName, boolean on);

    String createLinkBasedMeeting(in String packageName, in String json);

    int stopTelepresence(String packageName);

    int enableStandBy(in String packageName, boolean enabled, in String password);

    String startMeeting(in String packageName, in List<Participant> participants, boolean firstParticipantJoinedAsHost, boolean blockRobotInteraction);

    int configMinimumObstacleDistance(in String packageName, int value);

    List<TourModel> getAllTours(in String packageName, in List<String> tags);

    int playTour(in String packageName, in String tourId);

    int setAsrLanguages(in String packageName, in int[] languages);

    Position getPosition(in String packageName);

    int setMicGainLevel(in String packageName, int level);

    String resetMap(in String packageName, boolean allFloor);

    String finishMapping(in String packageName, in String mapName);

    String updateMapName(in String packageName, in String mapName);

    String continueMapping(in String packageName);

    String upsertMapLayer(in String packageName, in String layer);

    String deleteMapLayer(in String packageName, in String layerId, int layerCategory);

    String getHardButtonStatus(in String packageName, int type);

    String setFollowSpeed(in String packageName, in String speedLevel);

    String getFollowSpeed();

    String getHomeScreenMode(in String packageName);

    int isMapLost();

    int isMapLocked();

    int getReposeStatus();
}