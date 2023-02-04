package org.racehorse.evergreen

import org.racehorse.webview.AlertEvent
import java.io.File

class BundleReadyEvent(val appDir: File)

class UpdateFailedEvent(val mandatory: Boolean) : AlertEvent

class UpdateProgressEvent(val contentLength: Int, val readLength: Long) : AlertEvent

class UpdateReadyEvent : AlertEvent

class UpdateStartedEvent(val mandatory: Boolean) : AlertEvent
