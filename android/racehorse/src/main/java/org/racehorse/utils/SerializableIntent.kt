package org.racehorse.utils

import android.content.Intent
import androidx.core.net.toUri
import java.io.Serializable

/**
 * The [Intent] representation passed from and to the web application.
 *
 * @param action The general action to be performed, such as [Intent.ACTION_VIEW].
 * @param type The explicit MIME type included in the intent.
 * @param data The URI-encoded data that intent is operating on.
 * @param flags Special flags associated with this intent, such as [Intent.FLAG_ACTIVITY_NEW_TASK].
 * @param extras A map of extended data from the intent.
 */
class SerializableIntent(
    var action: String? = null,
    val type: String? = null,
    var data: String? = null,
    var flags: Int = 0,
    val selector: SerializableIntent? = null,
    var extras: Map<String, Serializable?>? = null,
    var categories: Set<String>? = null,
) : Serializable {

    constructor(intent: Intent) : this(
        action = intent.action,
        type = intent.type,
        data = intent.dataString,
        flags = intent.flags,
        selector = intent.selector?.let(::SerializableIntent),
        extras = @Suppress("DEPRECATION") intent.extras?.keySet()?.associateWith(intent::getSerializableExtra),
        categories = intent.categories,
    )

    fun toIntent(): Intent = Intent(action).also {
        it.setDataAndType(data?.toUri(), type)
        it.addFlags(flags)
        it.selector = selector?.toIntent()
        extras?.forEach(it::putExtra)
        categories?.forEach(it::addCategory)
    }
}
