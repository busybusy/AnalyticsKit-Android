/*
 * Copyright 2020 - 2023 busybusy, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.busybusy.analyticskit.kissmetrics_provider

import com.busybusy.analyticskit_android.AnalyticsEvent
import com.busybusy.analyticskit_android.AnalyticsKitProvider.PriorityFilter
import com.busybusy.analyticskit_android.ErrorEvent
import com.kissmetrics.sdk.KISSmetricsAPI
import com.kissmetrics.sdk.KISSmetricsAPI.RecordCondition
import com.kissmetrics.sdk.KISSmetricsAPI.RecordCondition.RECORD_ONCE_PER_IDENTITY
import com.kissmetrics.sdk.KISSmetricsAPI.RecordCondition.RECORD_ONCE_PER_INSTALL
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyMap
import org.mockito.ArgumentMatchers.anyString
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import java.util.*
import kotlin.properties.Delegates.notNull

/**
 * Tests the [KissMetricsProvider] class
 *
 * @author John Hunt on 2020-04-18.
 */
class KissMetricsProviderTest {
    private lateinit var kissMetrics: KISSmetricsAPI
    private lateinit var mocked: MockedStatic<KISSmetricsAPI>
    private lateinit var provider: KissMetricsProvider

    private lateinit var testEventName: String
    private lateinit var testEventPropertiesMap: Map<String, String>
    private var testRecordCondition: RecordCondition? = null
    private var sendCalled: Boolean by notNull()


    @Before
    fun setup() {
        kissMetrics = mock()
        provider = KissMetricsProvider(kissMetrics)
        sendCalled = false
        testEventPropertiesMap = mutableMapOf()
        testRecordCondition = null
        prepareMocks()
    }

    @After
    fun tearDown() {
        mocked.close()
    }

    @Test
    fun test_priorityFiltering_default() {
        val event = AnalyticsEvent("Forecast: Event Flurries")
                .setPriority(10)
                .send()
        assertThat(provider.getPriorityFilter().shouldLog(event.priority)).isEqualTo(true)
        event.setPriority(-9)
                .send()
        assertThat(provider.getPriorityFilter().shouldLog(event.priority)).isEqualTo(true)
    }

    @Test
    fun `recordCondition adds object to tracking map`() {
        val event = AnalyticsEvent("Forecast: Event Flurries")
                .recordCondition(RECORD_ONCE_PER_INSTALL)

        assertThat(event.attributes).contains(entry("record_condition", RECORD_ONCE_PER_INSTALL))
    }

    @Test
    fun test_priorityFiltering_custom() {
        val filter = PriorityFilter { priorityLevel -> priorityLevel < 10 }
        val testProvider = KissMetricsProvider(kissMetrics = kissMetrics, priorityFilter = filter)
        val event = AnalyticsEvent("Forecast: Event Flurries")
                .setPriority(10)
                .send()
        assertThat(testProvider.getPriorityFilter().shouldLog(event.priority)).isEqualTo(false)
        event.setPriority(9)
                .send()
        assertThat(testProvider.getPriorityFilter().shouldLog(event.priority)).isEqualTo(true)
    }

    @Test
    fun `stringifyAttributes turns Any into String`() {
        val eventParams = mutableMapOf<String, Any>("favorite_color" to "Blue", "favorite_number" to 42, "favorite_decimal" to 98.6)
        val stringAttributes = eventParams.stringifyAttributes()
        assertThat(stringAttributes).isNotNull

        assertThat(stringAttributes).containsExactlyInAnyOrderEntriesOf(
                mutableMapOf(
                        "favorite_color" to "Blue",
                        "favorite_number" to "42",
                        "favorite_decimal" to "98.6"
                )
        )
    }

    @Test
    fun `untimed event with empty attributes records only event name`() {

        val event = AnalyticsEvent("KissMetrics Test Run")
        provider.sendEvent(event)
        assertThat(sendCalled).isEqualTo(true)
        assertThat(testEventName).isEqualTo("KissMetrics Test Run")
        assertThat(testEventPropertiesMap).isEmpty()
        assertThat(testRecordCondition).isNull()
    }

    @Test
    fun `untimed event with attributes but no RecordCondition`() {

        val event = AnalyticsEvent("KissMetrics Event With Params Run")
                .putAttribute("some_param", "yes")
                .putAttribute("another_param", "yes again")
        provider.sendEvent(event)
        assertThat(sendCalled).isEqualTo(true)
        assertThat(testEventPropertiesMap).containsExactlyInAnyOrderEntriesOf(
                mutableMapOf("some_param" to "yes", "another_param" to "yes again")
        )
        assertThat(testRecordCondition).isNull()
    }

    @Test
    fun `untimed event where only attribute is RecordCondition`() {

        val event = AnalyticsEvent("KissMetrics Event With Params Run")
                .recordCondition(RECORD_ONCE_PER_INSTALL)
        provider.sendEvent(event)
        assertThat(sendCalled).isEqualTo(true)
        assertThat(testEventPropertiesMap).isEmpty()
        assertThat(testRecordCondition).isEqualTo(RECORD_ONCE_PER_INSTALL)
    }

    @Test
    fun `untimed event with both attributes and RecordCondition`() {

        val event = AnalyticsEvent("KissMetrics Event With Params Run")
                .putAttribute("some_param", "yes")
                .putAttribute("another_param", "yes again")
                .recordCondition(RECORD_ONCE_PER_IDENTITY)
        provider.sendEvent(event)
        assertThat(sendCalled).isEqualTo(true)
        assertThat(testEventPropertiesMap).isNotNull
        assertThat(testEventPropertiesMap).containsExactlyInAnyOrderEntriesOf(
                mutableMapOf("some_param" to "yes", "another_param" to "yes again")
        )
        assertThat(testRecordCondition).isEqualTo(RECORD_ONCE_PER_IDENTITY)
    }

    @Test
    fun `log error event`() {

        val event = ErrorEvent()
                .setMessage("something bad happened")
                .setException(EmptyStackException())
        provider.sendEvent(event)
        assertThat(sendCalled).isEqualTo(true)
        assertThat(testEventPropertiesMap).isNotNull
        assertThat(testEventPropertiesMap.size).isEqualTo(2)
        assertThat(testEventPropertiesMap).containsExactlyInAnyOrderEntriesOf(
                mutableMapOf(
                        "error_message" to "something bad happened",
                        "exception_object" to "java.util.EmptyStackException"
                )
        )
    }

    @Test
    fun `send timed event with no attributes`() {

        val event = AnalyticsEvent("KissMetrics Timed Event")
                .setTimed(true)
        provider.sendEvent(event)
        assertThat(sendCalled).isEqualTo(false)
    }

    @Test
    fun `send timed event with attributes`() {

        val event = AnalyticsEvent("KissMetrics Timed Event With Parameters")
                .setTimed(true)
                .putAttribute("some_param", "yes")
                .putAttribute("another_param", "yes again")
        provider.sendEvent(event)
        assertThat(sendCalled).isEqualTo(false)
    }

    @Test
    fun testEndTimedEvent_Valid() {

        val event = AnalyticsEvent("KissMetrics Timed Event With Parameters")
                .setTimed(true)
                .putAttribute("some_param", "yes")
                .putAttribute("another_param", "yes again")
        provider.sendEvent(event)
        assertThat(sendCalled).isEqualTo(false)
        try {
            Thread.sleep(50)  // TODO use coroutines to clean this up
        } catch (e: InterruptedException) {
            // don't do anything, this is just a test that needs some delay
        }
        provider.endTimedEvent(event)

        assertThat(sendCalled).isEqualTo(true)
        assertThat(testEventPropertiesMap).isNotNull
        assertThat(testEventPropertiesMap.size).isEqualTo(3)
        val timeString = testEventPropertiesMap[DURATION].toString()
        assertThat(testEventPropertiesMap).containsAllEntriesOf(
                mutableMapOf("some_param" to "yes", "another_param" to "yes again")
        )

        val elapsedTime = timeString.toDouble()
        assertThat(elapsedTime).isGreaterThanOrEqualTo(0.020)
    }

    @Test
    fun test_endTimedEvent_WillThrow() {

        var didThrow = false
        val event = AnalyticsEvent("KissMetrics Timed Event With Parameters")
                .setTimed(true)
        try {
            provider.endTimedEvent(event) // attempting to end a timed event that was not started should throw an exception
        } catch (e: IllegalStateException) {
            didThrow = true
        }
        assertThat(didThrow).isEqualTo(true)
    }

    @Suppress("UNCHECKED_CAST")
    private fun prepareMocks() {
        doAnswer { invocation ->
            val args = invocation.arguments
            testEventName = args[0] as String
            sendCalled = true
            null
        }.`when`(kissMetrics).record(anyString())

        doAnswer { invocation ->
            val args = invocation.arguments
            testEventName = args[0] as String
            testRecordCondition = args[1] as RecordCondition
            sendCalled = true
            null
        }.`when`(kissMetrics).record(anyString(), any<RecordCondition>())

        doAnswer { invocation ->
            val args = invocation.arguments
            testEventName = args[0] as String
            testEventPropertiesMap = args[1] as Map<String, String>
            sendCalled = true
            null
        }.`when`(kissMetrics).record(anyString(), anyMap())

        doAnswer { invocation ->
            val args = invocation.arguments
            testEventName = args[0] as String
            testEventPropertiesMap = args[1] as Map<String, String>
            testRecordCondition = args[2] as RecordCondition
            sendCalled = true
            null
        }.`when`(kissMetrics).record(anyString(), anyMap(), any())

        mocked = mockStatic(KISSmetricsAPI::class.java) {
            kissMetrics
        }
    }
}
