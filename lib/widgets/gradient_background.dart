import 'package:flutter/material.dart';
import '../theme/app_theme.dart';

/// Screen background featuring deep midnight navy gradient
/// with a soft, subtle radial ambient glow centered behind the shield logo.
class GradientBackground extends StatelessWidget {
  const GradientBackground({super.key, required this.child});
  final Widget child;

  @override
  Widget build(BuildContext context) {
    final size = MediaQuery.of(context).size;

    return Stack(
      children: [
        // 1. Base vertical navy gradient
        Container(
          width: double.infinity,
          height: double.infinity,
          decoration: const BoxDecoration(
            gradient: AppGradients.background,
          ),
        ),

        // 2. Soft radial ambient glow behind the shield
        Positioned(
          top: size.height * 0.08,
          left: 0,
          right: 0,
          child: Center(
            child: Container(
              width: size.width * 0.85,
              height: size.width * 0.85,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                gradient: RadialGradient(
                  colors: [
                    const Color(0xFF102855).withValues(alpha: 0.65),
                    const Color(0xFF0C1B3E).withValues(alpha: 0.25),
                    Colors.transparent,
                  ],
                  stops: const [0.0, 0.45, 1.0],
                ),
              ),
            ),
          ),
        ),

        // 3. Child content
        Positioned.fill(child: child),
      ],
    );
  }
}
