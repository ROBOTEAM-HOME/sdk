package com.robotemi.sdk.listeners

import com.robotemi.sdk.constants.HardButton

interface OnButtonModeChangedListener {

    fun onButtonModeChanged(buttonType: HardButton , buttonMode: HardButton.Mode)

}