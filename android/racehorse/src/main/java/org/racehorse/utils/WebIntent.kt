package org.racehorse.utils

import android.content.Intent
import androidx.core.net.toUri
import java.io.Serializable

/**
 * The [Intent] representation passed from and to the web application.
 *
 * @param action The general action to be performed, such as [Intent.ACTION_VIEW].
 * @param data The URI-encoded data that intent is operating on.
 * @param flags Special flags associated with this intent, such as [Intent.FLAG_ACTIVITY_NEW_TASK].
 * @param extras A map of extended data from the intent.
 */
class WebIntent(
    var action: String? = null,
    var data: String? = null,
    var flags: Int = 0,
    var extras: Map<String, Serializable?>? = null
) {

    constructor(intent: Intent) : this(
        intent.action,
        intent.dataString,
        intent.flags,

        @Suppress("DEPRECATION")
        intent.extras?.keySet()?.associateWith(intent::getSerializableExtra)
    )

    fun toIntent() = Intent(action, data?.toUri())
        .addFlags(flags)
        .also { extras?.forEach(it::putExtra) }
}
