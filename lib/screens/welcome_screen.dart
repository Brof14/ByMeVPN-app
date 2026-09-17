import 'package:flutter/material.dart';
import '../theme/app_theme.dart';
import '../widgets/gradient_background.dart';
import '../widgets/gradient_button.dart';
import '../widgets/shield_logo.dart';
import 'sign_up_screen.dart';
import 'sign_in_screen.dart';

class WelcomeScreen extends StatelessWidget {
  const WelcomeScreen({super.key});
  static const routeName = '/';

  @override
  Widget build(BuildContext context) {
    final mq = MediaQuery.of(context);
    final sw = mq.size.width;
    final sh = mq.size.height;

    // Shield logo scaled accurately (~53% screen width)
    final logoSize = (sw * 0.53).clamp(195.0, 230.0);
    final hPad = (sw * 0.065).clamp(22.0, 28.0);

    return Scaffold(
      backgroundColor: Colors.transparent,
      body: GradientBackground(
        child: SafeArea(
          child: SizedBox.expand(
            child: Padding(
              padding: EdgeInsets.symmetric(horizontal: hPad),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.center,
                children: [
                  SizedBox(height: sh * 0.05),

                  // ByMeVPN Shield Logo
                  ShieldLogo(size: logoSize),

                  SizedBox(height: sh * 0.04),

                  // Brand Text: "ByMe" (White) + "VPN" (Emerald Green)
                  RichText(
                    textAlign: TextAlign.center,
                    text: TextSpan(
                      children: [
                        TextSpan(
                          text: 'ByMe',
                          style: AppTextStyles.brandWhite,
                        ),
                        TextSpan(
                          text: 'VPN',
                          style: AppTextStyles.brandGreen,
                        ),
                      ],
                    ),
                  ),

                  const SizedBox(height: 10),

                  // Slogan: "Speed. Anonymity. Honesty."
                  Text(
                    'Speed. Anonymity. Honesty.',
                    style: AppTextStyles.slogan,
                    textAlign: TextAlign.center,
                  ),

                  // Pushes buttons to bottom
                  const Spacer(),

                  // Action Buttons: Sign Up & Log In side-by-side
                  Row(
                    children: [
                      Expanded(
                        child: GradientButton(
                          label: 'Sign Up',
                          gradient: AppGradients.signUp,
                          onTap: () {
                            Navigator.of(context).pushNamed(SignUpScreen.routeName);
                          },
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: GradientButton(
                          label: 'Log In',
                          gradient: AppGradients.logIn,
                          onTap: () {
                            Navigator.of(context).pushNamed(SignInScreen.routeName);
                          },
                        ),
                      ),
                    ],
                  ),

                  const SizedBox(height: 20),

                  // 4 Dots: Dot 2 is active white with glow
                  const _PageDots(total: 4, activeIndex: 1),

                  SizedBox(height: sh * 0.025),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _PageDots extends StatelessWidget {
  const _PageDots({
    required this.total,
    required this.activeIndex,
  });

  final int total;
  final int activeIndex;

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      mainAxisAlignment: MainAxisAlignment.center,
      children: List.generate(total, (i) {
        final isActive = i == activeIndex;
        return Container(
          margin: const EdgeInsets.symmetric(horizontal: 4.5),
          width: isActive ? 7.5 : 5.5,
          height: isActive ? 7.5 : 5.5,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            color: isActive
                ? AppColors.white
                : const Color(0xFF19263E),
            boxShadow: isActive
                ? [
                    BoxShadow(
                      color: Colors.white.withValues(alpha: 0.5),
                      blurRadius: 4.0,
                      spreadRadius: 1.0,
                    ),
                  ]
                : null,
          ),
        );
      }),
    );
  }
}
