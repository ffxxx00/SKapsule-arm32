package com.skarm.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class Arm32RuntimeTest {
    @Test
    fun arm32ProcessUsesArm32Archives() {
        assertEquals("bin-arm.tar.xz", JreInstaller.archAssetName(false))
        assertEquals("lwjgl/lwjgl-3.4.1-android-natives-arm32.zip", LwjglInstaller.nativesAssetName(false))
    }

    @Test
    fun arm64ProcessKeepsArm64Archives() {
        assertEquals("bin-arm64.tar.xz", JreInstaller.archAssetName(true))
        assertEquals("lwjgl/lwjgl-3.4.1-android-natives-arm64.zip", LwjglInstaller.nativesAssetName(true))
    }

    @Test
    fun arm32HeapLeavesAddressSpaceForNativeLibraries() {
        assertEquals(1024, RamSettings.heapLimitMb(4096, false))
        assertEquals(1024, RamSettings.heapLimitMb(8192, false))
        assertEquals(2048, RamSettings.heapLimitMb(4096, true))
        assertEquals(4096, RamSettings.heapLimitMb(16384, true))
        assertEquals(896, RamSettings.heapLimitMb(1920, false))
        assertEquals(512, RamSettings.heapLimitMb(1024, false))
    }
}
