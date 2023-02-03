package org.racehorse.evergreen

/**
 * Describes the latest update available for download.
 *
 * @param version The latest version.
 * @param url The URL of the bundle archive to download.
 * @param mandatory If `true` then the app cannot be started before update is downloaded and applied, otherwise the
 * update is downloaded in the background and applied during the next app restart.
 */
class UpdateDescriptor(val version: String, val url: String, val mandatory: Boolean)
