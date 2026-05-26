package com.zeynbakers.order_management_system.core.testing

import android.os.Bundle
import com.karumi.shot.ShotTestRunner

class UiTestRunner : ShotTestRunner() {
    override fun onCreate(arguments: Bundle) {
        super.onCreate(arguments)
        // AccessibilityChecks dependency removed - re-add if needed in build.gradle
    }
}
