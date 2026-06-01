import 'package:flutter/material.dart';

abstract class AppColors {
  // Background — very dark, barely perceptible gradient
  static const Color bgTop    = Color(0xFF0A0E1F);
  static const Color bgMiddle = Color(0xFF0F1529);
  static const Color bgBottom = Color(0xFF141B3F);

  // Brand green — bright emerald green
  static const Color brandGreen = Color(0xFF4BF091);
  static const Color neonGreen  = Color(0xFF00FF88);

  // Brand blue — premium deep blue
  static const Color brandBlue  = Color(0xFF1A7FFF);
  static const Color deepBlue   = Color(0xFF1155CC);
  static const Color electricBlue = Color(0xFF007BFF);

  // Premium metallic and glass effects
  static const Color white      = Color(0xFFFFFFFF);
  static const Color glassLight = Color(0x1AFFFFFF);
  static const Color glassDark  = Color(0x0DFFFFFF);
  static const Color metallicSilver = Color(0xFFC0C0C0);
  static const Color cyanGlow   = Color(0xFF00FFFF);

  // Slogan — muted blue-grey
  static const Color sloganGrey = Color(0xFFA8B4CC);
  static const Color subtleGrey = Color(0xFF6B7280);
}

abstract class AppGradients {
  // Screen background: premium deep navy blue with subtle glow
  static const LinearGradient background = LinearGradient(
    begin: Alignment.topCenter,
    end: Alignment.bottomCenter,
    colors: [AppColors.bgTop, AppColors.bgMiddle, AppColors.bgBottom],
    stops: [0.0, 0.45, 1.0],
  );

  // Shield rim & ∞ symbol: premium metallic gradient with cyan-green accent
  static const LinearGradient shieldRim = LinearGradient(
    begin: Alignment(-0.8, -0.8),
    end: Alignment(0.8, 0.8),
    colors: [
      Color(0xFF1A7FFF),     // Electric blue
      Color(0xFF007BFF),     // Premium blue
      Color(0xFF00FFFF),     // Cyan glow
      Color(0xFF00FF88),     // Emerald green
    ],
    stops: [0.0, 0.3, 0.7, 1.0],
  );

  // Shield inner metallic gradient for 3D effect
  static const LinearGradient shieldMetallic = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [
      Color(0xFF2A4FFF),     // Light metallic blue
      Color(0xFF1A7FFF),     // Main metallic
      Color(0xFF0D50CC),     // Dark metallic
    ],
    stops: [0.0, 0.5, 1.0],
  );

  // Glassmorphism overlay for shield
  static const LinearGradient shieldGlass = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [
      AppColors.glassLight,
      AppColors.glassDark,
      AppColors.glassLight,
    ],
    stops: [0.0, 0.5, 1.0],
  );

  // Sign Up button: premium blue gradient
  static const LinearGradient signUp = LinearGradient(
    begin: Alignment.centerLeft,
    end: Alignment.centerRight,
    colors: [
      Color(0xFF1360E0),     // Deep blue
      Color(0xFF1A7FFF),     // Brand blue
      Color(0xFF2A90FF),     // Light blue
    ],
    stops: [0.0, 0.5, 1.0],
  );

  // Log In button: premium emerald green gradient
  static const LinearGradient logIn = LinearGradient(
    begin: Alignment.centerLeft,
    end: Alignment.centerRight,
    colors: [
      Color(0xFF00CC66),     // Deep emerald
      Color(0xFF2EDE82),     // Main green
      Color(0xFF4BF091),     // Bright green
    ],
    stops: [0.0, 0.5, 1.0],
  );

  // Abstract geometric glow background
  static const LinearGradient abstractGlow = LinearGradient(
    begin: Alignment(-1.0, -1.0),
    end: Alignment(1.0, 1.0),
    colors: [
      AppColors.glassLight,
      AppColors.glassDark,
      Color(0x0D00FFFF),
      Color(0x0D00FF88),
      AppColors.glassDark,
    ],
    stops: [0.0, 0.3, 0.5, 0.7, 1.0],
  );
}

abstract class AppTextStyles {
  // Premium brand typography
  static TextStyle get brandWhite => TextStyle(
    fontSize: 42,
    fontWeight: FontWeight.w900,
    color: AppColors.white,
    letterSpacing: -.6,
    height: 1.0,
    shadows: [
      Shadow(
        color: AppColors.cyanGlow.withValues(alpha: 0.3),
        offset: const Offset(0, 0),
        blurRadius: 20,
      ),
    ],
  );

  static TextStyle get brandGreen => TextStyle(
    fontSize: 42,
    fontWeight: FontWeight.w900,
    color: AppColors.brandGreen,
    letterSpacing: -.6,
    height: 1.0,
    shadows: [
      Shadow(
        color: AppColors.neonGreen.withValues(alpha: 0.4),
        offset: const Offset(0, 0),
        blurRadius: 25,
      ),
    ],
  );

  static TextStyle get slogan => TextStyle(
    fontSize: 16,
    fontWeight: FontWeight.w600,
    color: AppColors.sloganGrey,
    letterSpacing: .4,
    height: 1.3,
    shadows: [
      Shadow(
        color: Colors.black.withValues(alpha: 0.5),
        offset: const Offset(0, 1),
        blurRadius: 2,
      ),
    ],
  );

  // Premium button text
  static TextStyle get btnWhite => TextStyle(
    fontSize: 17,
    fontWeight: FontWeight.w800,
    color: AppColors.white,
    letterSpacing: .3,
    shadows: [
      Shadow(
        color: Colors.black.withValues(alpha: 0.3),
        offset: const Offset(0, 1),
        blurRadius: 2,
      ),
    ],
  );

  static TextStyle get btnDark => TextStyle(
    fontSize: 17,
    fontWeight: FontWeight.w800,
    color: const Color(0xFF082010),
    letterSpacing: .3,
    shadows: [
      Shadow(
        color: Colors.black.withValues(alpha: 0.2),
        offset: const Offset(0, 1),
        blurRadius: 2,
      ),
    ],
  );
}

class AppTheme {
  AppTheme._();

  static ThemeData get dark => ThemeData(
    brightness: Brightness.dark,
    scaffoldBackgroundColor: AppColors.bgTop,
    colorScheme: const ColorScheme.dark(
      primary: AppColors.brandGreen,
      secondary: AppColors.brandBlue,
    ),
    useMaterial3: true,
    snackBarTheme: const SnackBarThemeData(
      backgroundColor: Color(0xFF1C2750),
      contentTextStyle: TextStyle(color: AppColors.white, fontSize: 14),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.all(Radius.circular(12)),
      ),
      behavior: SnackBarBehavior.floating,
    ),
  );
}
