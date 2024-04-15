package org.racehorse

import android.Manifest
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.database.getStringOrNull
import org.greenrobot.eventbus.Subscribe
import org.racehorse.utils.askForPermission
import org.racehorse.utils.launchActivityForResult
import java.io.Serializable

class Contact(
    val name: String?,
    val photoUri: String?,
    val emails: Array<String>,
    val phoneNumbers: Array<String>
) : Serializable

class PickContactEvent : RequestEvent() {
    class ResultEvent(val contact: Contact?) : ResponseEvent()
}

open class ContactsPlugin(private val activity: ComponentActivity) {

    @Subscribe
    open fun onPickContact(event: PickContactEvent) {
        activity.askForPermission(Manifest.permission.READ_CONTACTS) { isGranted ->
            if (!isGranted) {
                event.respond(PickContactEvent.ResultEvent(null))
                return@askForPermission
            }

            val isLaunched =
                activity.launchActivityForResult(ActivityResultContracts.PickContact(), null) { contactUri ->
                    if (contactUri == null) {
                        event.respond(PickContactEvent.ResultEvent(null))
                        return@launchActivityForResult
                    }

                    event.respond {
                        val selection =
                            ContactsContract.CommonDataKinds.Email.CONTACT_ID + "=" + checkNotNull(contactUri.lastPathSegment)

                        val emails = buildList<String> {
                            activity.contentResolver.query(
                                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                                arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS),
                                selection,
                                null,
                                null
                            ).use { cursor ->
                                while (checkNotNull(cursor).moveToNext()) {
                                    add(cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS)))
                                }
                            }
                        }

                        val phoneNumbers = buildList<String> {
                            activity.contentResolver.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                arrayOf(
                                    ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER,
                                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                                ),
                                selection,
                                null,
                                null
                            ).use { cursor ->
                                while (checkNotNull(cursor).moveToNext()) {
                                    add(
                                        cursor.getStringOrNull(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER))
                                            ?: cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                                    )
                                }
                            }
                        }

                        activity.contentResolver.query(
                            contactUri,
                            arrayOf(
                                ContactsContract.Contacts.DISPLAY_NAME,
                                ContactsContract.Contacts.PHOTO_URI
                            ),
                            null,
                            null,
                            null
                        ).use { cursor ->
                            checkNotNull(cursor).moveToFirst()

                            val contact = Contact(
                                name = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)),
                                photoUri = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI)),
                                emails = emails.toTypedArray(),
                                phoneNumbers = phoneNumbers.toTypedArray()
                            )

                            PickContactEvent.ResultEvent(contact)
                        }
                    }
                }

            if (!isLaunched) {
                event.respond(PickContactEvent.ResultEvent(null))
            }
        }
    }
}
