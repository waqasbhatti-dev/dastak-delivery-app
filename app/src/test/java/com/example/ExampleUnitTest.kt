package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testSoleAdminAuthorization() {
    val soleAdmin = "mwaqasbhatti1230@gmail.com"
    val formerSecondary = "mwaqasbhatti57570@gmail.com"
    val regularUser = "customer@example.com"

    assertTrue(com.example.ui.admin.isAuthorizedAdmin(soleAdmin))
    assertTrue(com.example.ui.admin.isAuthorizedAdmin("MWAQASBHATTI1230@GMAIL.COM")) // case insensitive
    assertFalse(com.example.ui.admin.isAuthorizedAdmin(formerSecondary))
    assertFalse(com.example.ui.admin.isAuthorizedAdmin(regularUser))
    assertFalse(com.example.ui.admin.isAuthorizedAdmin(null))
  }
}
