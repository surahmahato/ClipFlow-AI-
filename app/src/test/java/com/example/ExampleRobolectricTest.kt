package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ClipFlow AI", appName)
  }

  @Test
  fun `model create maps omit id and clamp bounds`() {
    val profileMap = com.example.data.model.UserProfile(
      id = "doc_id_ignored",
      userId = "uid_1",
      displayName = "Rinku",
      email = null
    ).toCreateMap()
    assertEquals(false, profileMap.containsKey("id"))
    assertEquals(false, profileMap.containsKey("email"))
    assertEquals("uid_1", profileMap["userId"])

    val clipMap = com.example.data.model.GeneratedClip(
      id = "clip_doc_ignored",
      userId = "uid_1",
      projectId = "proj_1",
      startMs = 5000L,
      endMs = 5000L,
      viralScore = 150L
    ).toCreateMap()
    assertEquals(false, clipMap.containsKey("id"))
    assertEquals(100L, clipMap["viralScore"])
    assertEquals(6000L, clipMap["endMs"])
  }
}
