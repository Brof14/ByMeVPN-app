import 'package:flutter/material.dart';

abstract class AppColors {
  // Deep midnight navy background
  static const Color bgTop = Color(0xFF060A17);
  static const Color bgMiddle = Color(0xFF0A1326);
  static const Color bgBottom = Color(0xFF060B18);
  static const Color shieldInnerTop = Color(0xFF0C1B38);
  static const Color shieldInnerBottom = Color(0xFF050B18);

  // Brand colors
  static const Color brandGreen = Color(0xFF24E872);
  static const Color neonGreen = Color(0xFF00FF85);
  static const Color electricBlue = Color(0xFF0062FF);
  static const Color cyan = Color(0xFF00C8FF);

  // Neutral / text colors
  static const Color white = Color(0xFFFFFFFF);
  static const Color sloganGrey = Color(0xFF98ABC2);
  static const Color dotInactive = Color(0xFF18253C);

  // Button gradients
  static const Color blueBtnStart = Color(0xFF0055F6);
  static const Color blueBtnEnd = Color(0xFF0084FF);
  static const Color greenBtnStart = Color(0xFF14DF6C);
  static const Color greenBtnEnd = Color(0xFF43F38B);
}

abstract class AppGradients {
  // Screen background
  static const LinearGradient background = LinearGradient(
    begin: Alignment.topCenter,
    end: Alignment.bottomCenter,
    colors: [
      AppColors.bgTop,
      AppColors.bgMiddle,
      AppColors.bgBottom,
    ],
    stops: [0.0, 0.45, 1.0],
  );

  // Shield rim gradient: Blue (bottom-left) to Neon Green (top-right)
  static const LinearGradient shield = LinearGradient(
    begin: Alignment.bottomLeft,
    end: Alignment.topRight,
    colors: [
      Color(0xFF0062FF),
      Color(0xFF00A6FF),
      Color(0xFF00FFA2),
      Color(0xFF24E872),
    ],
    stops: [0.0, 0.32, 0.68, 1.0],
  );

  // Crossover Ribbon gradient: Cyan/Blue (left) to Neon Lime/Green (right)
  static const LinearGradient ribbon = LinearGradient(
    begin: Alignment.centerLeft,
    end: Alignment.centerRight,
    colors: [
      Color(0xFF0099FF),
      Color(0xFF00D4FF),
      Color(0xFF00FF88),
      Color(0xFF2AE678),
    ],
    stops: [0.0, 0.30, 0.70, 1.0],
  );

  // Sign Up button
  static const LinearGradient signUp = LinearGradient(
    begin: Alignment.centerLeft,
    end: Alignment.centerRight,
    colors: [
      AppColors.blueBtnStart,
      AppColors.blueBtnEnd,
    ],
  );

  // Log In button
  static const LinearGradient logIn = LinearGradient(
    begin: Alignment.centerLeft,
    end: Alignment.centerRight,
    colors: [
      AppColors.greenBtnStart,
      AppColors.greenBtnEnd,
    ],
  );
}

abstract class AppTextStyles {
  // Brand title: "ByMe" in white
  static TextStyle get brandWhite => const TextStyle(
    fontSize: 38,
    fontWeight: FontWeight.w800,
    color: AppColors.white,
    letterSpacing: 0.0,
    height: 1.15,
  );

  // Brand title: "VPN" in bright neon green
  static TextStyle get brandGreen => const TextStyle(
    fontSize: 38,
    fontWeight: FontWeight.w800,
    color: AppColors.brandGreen,
    letterSpacing: 0.0,
    height: 1.15,
  );

  // Slogan: "Speed. Anonymity. Honesty."
  static TextStyle get slogan => const TextStyle(
    fontSize: 15.5,
    fontWeight: FontWeight.w500,
    color: AppColors.sloganGrey,
    letterSpacing: 0.3,
    height: 1.3,
  );

  // Button text: Pure white
  static TextStyle get buttonText => const TextStyle(
    fontSize: 16.5,
    fontWeight: FontWeight.w700,
    color: AppColors.white,
    letterSpacing: 0.2,
  );
}

class AppTheme {
  AppTheme._();

  static ThemeData get dark => ThemeData(
    brightness: Brightness.dark,
    scaffoldBackgroundColor: AppColors.bgTop,
    colorScheme: const ColorScheme.dark(
      primary: AppColors.brandGreen,
      secondary: AppColors.electricBlue,
    ),
    useMaterial3: true,
  );
}
