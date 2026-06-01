import 'package:flutter/material.dart';
import '../theme/app_theme.dart';
import '../widgets/gradient_background.dart';
import '../widgets/gradient_button.dart';
import '../widgets/shield_logo.dart';

class WelcomeScreen extends StatelessWidget {
  const WelcomeScreen({super.key});
  static const routeName = '/';

  void _snack(BuildContext ctx, String msg) =>
      ScaffoldMessenger.of(ctx)
        ..hideCurrentSnackBar()
        ..showSnackBar(SnackBar(
          content: Text(msg),
          duration: const Duration(seconds: 2),
          margin: const EdgeInsets.fromLTRB(16, 0, 16, 16),
        ));

  @override
  Widget build(BuildContext context) {
    final mq   = MediaQuery.of(context);
    final sw   = mq.size.width;
    final sh   = mq.size.height;

    // Logo: ~48 % of screen width, clamped to a sensible range
    final logoW = (sw * .48).clamp(180.0, 240.0);
    // Horizontal padding: ~6 % each side
    final hPad  = sw * .06;

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
                  // ── Premium top spacing ────────────────────────────────────
                  SizedBox(height: sh * .048),

                  // ─── Animated shield logo with premium effects ────────────────
                  AnimatedContainer(
                    duration: const Duration(milliseconds: 800),
                    curve: Curves.elasticOut,
                    child: ShieldLogo(size: logoW),
                  ),

                  SizedBox(height: sh * .055),

                  // ─── Animated brand text with glow ────────────────────────────
                  AnimatedOpacity(
                    duration: const Duration(milliseconds: 1000),
                    curve: Curves.easeOut,
                    opacity: 1.0,
                    child: AnimatedContainer(
                      duration: const Duration(milliseconds: 1200),
                      curve: Curves.bounceOut,
                      child: RichText(
                        text: TextSpan(children: [
                          TextSpan(text: 'ByMe', style: AppTextStyles.brandWhite),
                          TextSpan(text: 'VPN',  style: AppTextStyles.brandGreen),
                        ]),
                      ),
                    ),
                  ),

                  SizedBox(height: sh * .02),

                  // ─── Animated slogan ─────────────────────────────────────────
                  AnimatedOpacity(
                    duration: const Duration(milliseconds: 1100),
                    curve: Curves.easeOut,
                    opacity: 1.0,
                    child: AnimatedContainer(
                      duration: const Duration(milliseconds: 1000),
                      curve: Curves.easeOutBack,
                      child: Text(
                        'Speed. Anonymity. Honesty.',
                        style: AppTextStyles.slogan,
                      ),
                    ),
                  ),

                  // ─── Push remaining content to bottom ───────────────────────
                  const Spacer(),

                  // ─── Premium page indicator dots ───────────────────────────
                  const AnimatedOpacity(
                    duration: Duration(milliseconds: 1200),
                    curve: Curves.easeOut,
                    opacity: 1.0,
                    child: _Dots(total: 3, active: 1),
                  ),

                  SizedBox(height: sh * .025),

                  // ─── Premium action buttons with hover effects ───────────────
                  AnimatedOpacity(
                    duration: const Duration(milliseconds: 1300),
                    curve: Curves.easeOut,
                    opacity: 1.0,
                    child: AnimatedContainer(
                      duration: const Duration(milliseconds: 800),
                      curve: Curves.elasticOut,
                      child: Row(
                        children: [
                          Expanded(
                            child: GradientButton(
                              label: 'Sign Up',
                              gradient: AppGradients.signUp,
                              onTap: () => _snack(context, 'Transition to Sign Up'),
                            ),
                          ),
                          SizedBox(width: sw * .045),
                          Expanded(
                            child: GradientButton(
                              label: 'Log In',
                              gradient: AppGradients.logIn,
                              darkLabel: true,
                              onTap: () => _snack(context, 'Transition to Log In'),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),

                  SizedBox(height: sh * .055),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

/// Three small dots; active dot is wider and brighter.
class _Dots extends StatelessWidget {
  const _Dots({required this.total, required this.active});
  final int total;
  final int active;

  @override
  Widget build(BuildContext context) => Row(
    mainAxisSize: MainAxisSize.min,
    children: List.generate(total, (i) {
      final on = i == active;
      return Container(
        margin: const EdgeInsets.symmetric(horizontal: 3.5),
        width:  on ? 20 : 6,
        height: 6,
        decoration: BoxDecoration(
          color: on
              ? AppColors.white
              : AppColors.sloganGrey.withValues(alpha: .45),
          borderRadius: BorderRadius.circular(3),
        ),
      );
    }),
  );
}
