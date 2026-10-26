# ByMeVPN Production ProGuard & R8 Optimization Rules

# 1. Native Xray Core / libv2ray bindings
-keep class libv2ray.** { *; }
-keep class go.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

# 2. Credential Manager & Google Play Identity
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# 3. AndroidX Security Crypto & Keystore
-keep class androidx.security.crypto.** { *; }

# 4. JSON Models & API Data Structures
-keepclassmembers class com.example.bymevpn.data.** {
    <fields>;
    <methods>;
}

# 5. VPN Service & Core Engine
-keep class com.example.bymevpn.vpn.** { *; }
-keep class com.example.bymevpn.vpn.xray.** { *; }

# 6. Compose Runtime & UI
-keep class androidx.compose.** { *; }
