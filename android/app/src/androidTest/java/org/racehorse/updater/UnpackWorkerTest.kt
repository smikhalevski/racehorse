package org.racehorse.updater

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class UnpackWorkerTest {
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val archiveFile = File(appContext.filesDir, "test.zip")
    private val targetDir = File(appContext.filesDir, "test")

    @Before
    fun before() {
        archiveFile.delete()
        targetDir.deleteRecursively()
    }

    @Test
    fun testUnzipsArchive() {
        val zipOutputStream = ZipOutputStream(FileOutputStream(archiveFile))

        val buffer1 = "111".toByteArray()
        zipOutputStream.putNextEntry(ZipEntry("aaa.txt"))
        zipOutputStream.write(buffer1, 0, buffer1.size)
        zipOutputStream.closeEntry()

        val buffer2 = "222".toByteArray()
        zipOutputStream.putNextEntry(ZipEntry("bbb.txt"))
        zipOutputStream.write(buffer2, 0, buffer2.size)
        zipOutputStream.closeEntry()

        zipOutputStream.close()

        val worker = TestListenableWorkerBuilder<UnpackWorker>(appContext)
            .setInputData(
                workDataOf(
                    UnpackWorker.ARCHIVE_PATHNAME to archiveFile.absolutePath,
                    UnpackWorker.TARGET_DIR to targetDir.absolutePath,
                )
            )
            .build()

        val result = runBlocking {
            worker.doWork()
        }

        Assert.assertTrue(result is ListenableWorker.Result.Success)
        Assert.assertEquals("111", File(targetDir.absolutePath + File.separator + "aaa.txt").readText())
        Assert.assertEquals("222", File(targetDir.absolutePath + File.separator + "bbb.txt").readText())
    }

    @Test(expected = SecurityException::class)
    fun testThrowsSecurityException() {
        val zipOutputStream = ZipOutputStream(FileOutputStream(archiveFile))

        val buffer = "111".toByteArray()
        zipOutputStream.putNextEntry(ZipEntry("../aaa.txt"))
        zipOutputStream.write(buffer, 0, buffer.size)
        zipOutputStream.closeEntry()

        zipOutputStream.close()

        val worker = TestListenableWorkerBuilder<UnpackWorker>(appContext)
            .setInputData(
                workDataOf(
                    UnpackWorker.ARCHIVE_PATHNAME to archiveFile.absolutePath,
                    UnpackWorker.TARGET_DIR to targetDir.absolutePath,
                )
            )
            .build()

        runBlocking {
            worker.doWork()
        }
    }
}
