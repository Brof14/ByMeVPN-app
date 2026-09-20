package com.example.bymevpn

import com.example.bymevpn.data.api.ApiError
import com.example.bymevpn.data.api.ApiJsonParsers
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AccountCredentialsTest {

    @Test
    fun testFastApiDetailError() {
        val json = """{"detail":{"error":{"code":"INVALID_CREDENTIALS","message":"Invalid email or password"}}}"""
        val error = ApiError.fromJson(json)
        assertEquals("INVALID_CREDENTIALS", error.code)
        assertEquals("Invalid email or password", error.message)
    }

    @Test
    fun testFastApiValidationError() {
        val json = """{"detail":[{"type":"string_too_short","loc":["body","password"],"msg":"String should have at least 8 characters"}]}"""
        val error = ApiError.fromJson(json)
        assertEquals("VALIDATION_ERROR", error.code)
        assertEquals("String should have at least 8 characters", error.message)
    }

    @Test
    fun testParseUserProfile_backendFormat() {
        val json = JSONObject("""{"user":{"user_id":-13,"email":"winchik@gmail.com","name":"Admin Winchik","email_verified":false,"created_at":"2026-09-20T16:45:36"}}""")
        val user = ApiJsonParsers.parseUserProfile(json)
        assertEquals("-13", user.id)
        assertEquals("winchik@gmail.com", user.email)
        assertEquals("Admin Winchik", user.name)
    }
}
