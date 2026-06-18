package com.kavach.upi.service

sealed class UICommand {
    object DeployOverlay : UICommand()
    object DismissOverlay : UICommand()
    object StartAlarm : UICommand()
    object StopAlarm : UICommand()
    object FullTeardown : UICommand()
}
