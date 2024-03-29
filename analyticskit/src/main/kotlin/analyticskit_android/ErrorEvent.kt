/*
 * Copyright 2016 - 2022 busybusy, Inc.
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
package com.busybusy.analyticskit_android

/**
 * Defines information that is needed to distribute an "Error" event to the registered analytics providers.
 *
 * @param eventName the name of the `ErrorEvent`
 *
 * @author John Hunt on 4/6/16.
 */
class ErrorEvent(eventName: String = CommonEvents.ERROR) : AnalyticsEvent(eventName) {
    val ERROR_MESSAGE = "error_message"
    val EXCEPTION_OBJECT = "exception_object"
    val ERROR_OBJECT = "error_object"

    /**
     * Sets an error message on the `ErrorEvent`.
     *
     * @param errorMessage the message to set
     * @return the `ErrorEvent` instance (for builder-style convenience)
     */
    fun setMessage(errorMessage: String): ErrorEvent {
        putAttribute(ERROR_MESSAGE, errorMessage)
        return this
    }

    /**
     * Access the error message.
     *
     * @return the error message set on this event. Returns `null` if the message was not set.
     */
    fun message(): String? = attributes?.get(ERROR_MESSAGE)?.toString()

    /**
     * Sets an `Exception` object to associate with this event.
     *
     * @param exception the Exception object to store
     * @return the `ErrorEvent` instance (for builder-style convenience)
     */
    fun setException(exception: Exception): ErrorEvent {
        putAttribute(EXCEPTION_OBJECT, exception)
        return this
    }

    /**
     * Access the [Exception] object.
     *
     * @return the `Exception` set on this event. Returns `null` if the exception was not set.
     */
    fun exception(): Exception? = attributes?.get(EXCEPTION_OBJECT) as? Exception

    /**
     * Sets an `Error` object to associate with this event.
     *
     * @param error the Error object to store
     * @return the `ErrorEvent` instance (for builder-style convenience)
     */
    fun setError(error: Error): ErrorEvent {
        putAttribute(ERROR_OBJECT, error)
        return this
    }

    /**
     * Access the [Error] object.
     *
     * @return the `Error` set on this event. Returns `null` if the error was not set.
     */
    fun error(): Error? = attributes?.get(ERROR_OBJECT) as? Error
}
