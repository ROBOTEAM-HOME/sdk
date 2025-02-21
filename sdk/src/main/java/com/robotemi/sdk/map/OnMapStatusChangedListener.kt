package com.robotemi.sdk.map

interface OnMapStatusChangedListener {

    /**
     * @param isLost, true if the map is lost
     * @param isLocked, true if the map is locked
     */
    fun onMapStatusChanged(isLost: Boolean, isLocked: Boolean)
}