package com.example.bymevpn

import com.example.bymevpn.data.AccountRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountCredentialsTest {

    @Test
    fun testAdminCredentials_valid() {
        assertTrue(AccountRepository.isAdminCredentials("winchik@gmail.com", "WeMMR7X3"))
        assertTrue(AccountRepository.isAdminCredentials("  winchik@gmail.com  ", "WeMMR7X3"))
        assertTrue(AccountRepository.isAdminCredentials("WINCHIK@GMAIL.COM", "WeMMR7X3"))
    }

    @Test
    fun testAdminCredentials_invalid() {
        assertFalse(AccountRepository.isAdminCredentials("other@gmail.com", "WeMMR7X3"))
        assertFalse(AccountRepository.isAdminCredentials("winchik@gmail.com", "wrong_pass"))
        assertFalse(AccountRepository.isAdminCredentials("", ""))
    }
}
