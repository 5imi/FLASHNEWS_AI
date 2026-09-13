package com.example.baseredy.flashnews.core.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TestNetworkMonitor : NetworkMonitor {
    val onlineFlow = MutableStateFlow(true)
    override val isOnline = onlineFlow
}

class NetworkMonitorTest {

    @Test
    fun testNetworkMonitor_emitsConnectivityChanges() = runTest {
        val monitor = TestNetworkMonitor()
        assertTrue(monitor.isOnline.first())

        monitor.onlineFlow.value = false
        assertFalse(monitor.isOnline.first())

        monitor.onlineFlow.value = true
        assertTrue(monitor.isOnline.first())
    }
}
