package com.zeynbakers.order_management_system.core.testing

import android.os.Bundle
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityChecks
import com.karumi.shot.ShotTestRunner

class UiTestRunner : ShotTestRunner() {
    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        AccessibilityChecks.enable().setRunChecksFromRootView(true)
    }
}
