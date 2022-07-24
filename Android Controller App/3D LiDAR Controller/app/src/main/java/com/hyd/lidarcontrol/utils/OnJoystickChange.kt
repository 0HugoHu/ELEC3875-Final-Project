package com.hyd.lidarcontrol.utils

// functional interface for joystick movement
fun interface OnJoystickChange {
    fun invoke(horizontal : Float, vertical : Float)
}