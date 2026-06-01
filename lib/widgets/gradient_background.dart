import 'package:flutter/material.dart';
import '../theme/app_theme.dart';

class GradientBackground extends StatelessWidget {
  const GradientBackground({super.key, required this.child});
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Stack(
      children: [
        // Base gradient background
        DecoratedBox(
          decoration: const BoxDecoration(gradient: AppGradients.background),
          child: child,
        ),

        // Abstract geometric glow overlay
        Positioned.fill(
          child: CustomPaint(
            painter: _AbstractGlowPainter(),
          ),
        ),
      ],
    );
  }
}

/// Custom painter for abstract geometric glow effects
class _AbstractGlowPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final W = size.width;
    final H = size.height;

    // Subtle geometric abstract glow
    final glowPaint = Paint()
      ..shader = AppGradients.abstractGlow.createShader(Rect.fromLTWH(0, 0, W, H))
      ..style = PaintingStyle.fill
      ..blendMode = BlendMode.softLight
      ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 30.0);

    // Create subtle geometric shapes for glow effect
    const shapeCount = 5;
    for (int i = 0; i < shapeCount; i++) {
      final centerX = W * (0.2 + (i * 0.15));
      final centerY = H * (0.3 + (i * 0.1));
      final radius = W * (0.1 + (i * 0.02));

      canvas.drawCircle(
        Offset(centerX, centerY),
        radius,
        glowPaint..color = AppColors.glassLight.withValues(alpha: 0.3 - (i * 0.05)),
      );
    }

    // Additional diagonal glow lines
    for (int i = 0; i < 3; i++) {
      final startX = W * (i * 0.3);
      final startY = H * (0.1 + (i * 0.2));
      final endX = W * (0.8 - (i * 0.2));
      final endY = H * (0.9 - (i * 0.2));

      final paint = glowPaint..color = AppColors.cyanGlow.withValues(alpha: 0.1 - (i * 0.03));
      paint.strokeWidth = 2.0 + (i * 2.0);
      canvas.drawLine(
        Offset(startX, startY),
        Offset(endX, endY),
        paint,
      );
    }
  }

  @override
  bool shouldRepaint(covariant CustomPainter old) => false;
}
