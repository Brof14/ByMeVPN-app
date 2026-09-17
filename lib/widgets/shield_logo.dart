import 'dart:math' as math;
import 'package:flutter/material.dart';
import '../theme/app_theme.dart';

/// ByMeVPN Shield logo matching the reference screenshot 1:1.
/// Features a precision-engineered arched gradient shield outline,
/// deep interior contrast, and a 3D crossover ribbon (X) in the center.
class ShieldLogo extends StatelessWidget {
  const ShieldLogo({super.key, this.size = 212});
  final double size;

  @override
  Widget build(BuildContext context) {
    final h = size * 1.18;
    return SizedBox(
      width: size,
      height: h,
      child: CustomPaint(
        painter: _ShieldPainter(),
      ),
    );
  }
}

class _ShieldPainter extends CustomPainter {
  Path _createShieldPath(double w, double h, double inset) {
    final effW = w - inset * 2.0;
    final effH = h - inset * 2.0;
    final crestH = effH * 0.045; // Arched crest height
    final cr = effW * 0.16; // Corner radius

    final p = Path();
    // Top-center arched crest
    p.moveTo(inset + effW * 0.50, inset);

    // Arch to top-right corner
    p.quadraticBezierTo(
      inset + effW * 0.78,
      inset + crestH * 0.35,
      inset + effW - cr,
      inset + crestH,
    );
    // Rounded top-right corner
    p.quadraticBezierTo(
      inset + effW,
      inset + crestH,
      inset + effW,
      inset + crestH + cr,
    );
    // Right side slightly bowed outwards
    p.lineTo(inset + effW, inset + effH * 0.38);
    // Right curve tapering to bottom tip
    p.cubicTo(
      inset + effW,
      inset + effH * 0.70,
      inset + effW * 0.80,
      inset + effH * 0.88,
      inset + effW * 0.50,
      inset + effH,
    );
    // Left curve tapering up from bottom tip
    p.cubicTo(
      inset + effW * 0.20,
      inset + effH * 0.88,
      inset,
      inset + effH * 0.70,
      inset,
      inset + effH * 0.38,
    );
    // Left side
    p.lineTo(inset, inset + crestH + cr);
    // Rounded top-left corner
    p.quadraticBezierTo(
      inset,
      inset + crestH,
      inset + cr,
      inset + crestH,
    );
    // Arch to top-center crest
    p.quadraticBezierTo(
      inset + effW * 0.22,
      inset + crestH * 0.35,
      inset + effW * 0.50,
      inset,
    );
    p.close();

    return p;
  }

  @override
  void paint(Canvas canvas, Size size) {
    final w = size.width;
    final h = size.height;
    final shieldRect = Rect.fromLTWH(0, 0, w, h);

    final shieldStrokeW = w * 0.118;
    final halfStroke = shieldStrokeW / 2.0;

    final shieldPath = _createShieldPath(w, h, halfStroke);

    // 0. Dark inner fill inside shield for rich contrast and depth
    final innerPath = _createShieldPath(w, h, halfStroke + 2.0);
    final innerPaint = Paint()
      ..shader = const LinearGradient(
        begin: Alignment.topCenter,
        end: Alignment.bottomCenter,
        colors: [
          Color(0xFF0C1B38),
          Color(0xFF050B18),
        ],
      ).createShader(shieldRect)
      ..style = PaintingStyle.fill;
    canvas.drawPath(innerPath, innerPaint);

    // 1. Ambient soft neon glow behind the shield rim
    final glowPaint = Paint()
      ..shader = AppGradients.shield.createShader(shieldRect)
      ..style = PaintingStyle.stroke
      ..strokeWidth = shieldStrokeW * 1.45
      ..strokeCap = StrokeCap.round
      ..strokeJoin = StrokeJoin.round
      ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 13.0)
      ..color = Colors.white.withValues(alpha: 0.40);

    canvas.drawPath(shieldPath, glowPaint);

    // 2. Crisp main shield rim
    final shieldPaint = Paint()
      ..shader = AppGradients.shield.createShader(shieldRect)
      ..style = PaintingStyle.stroke
      ..strokeWidth = shieldStrokeW
      ..strokeCap = StrokeCap.round
      ..strokeJoin = StrokeJoin.round;

    canvas.drawPath(shieldPath, shieldPaint);

    // 3. Central 3D Crossover Ribbon (X)
    _drawCrossoverRibbon(canvas, w, h);
  }

  void _drawCrossoverRibbon(Canvas canvas, double w, double h) {
    final cx = w * 0.50;
    final cy = h * 0.44;

    final x0 = w * 0.215;
    final y0 = h * 0.122;
    final k = 0.65; // Bézier control coefficient for perfect 45° S-curve
    final strokeW = w * 0.115;

    final ribbonRect = Rect.fromCenter(
      center: Offset(cx, cy),
      width: x0 * 2.8,
      height: y0 * 2.8,
    );

    // Strand 1: Top-Left (-x0, -y0) to Bottom-Right (+x0, +y0)
    final strand1 = Path();
    strand1.moveTo(cx - x0, cy - y0);
    strand1.cubicTo(
      cx - x0 + k * x0,
      cy - y0,
      cx + x0 - k * x0,
      cy + y0,
      cx + x0,
      cy + y0,
    );

    // Strand 2: Bottom-Left (-x0, +y0) to Top-Right (+x0, -y0)
    final strand2 = Path();
    strand2.moveTo(cx - x0, cy + y0);
    strand2.cubicTo(
      cx - x0 + k * x0,
      cy + y0,
      cx + x0 - k * x0,
      cy - y0,
      cx + x0,
      cy - y0,
    );

    final ribbonShader = AppGradients.ribbon.createShader(ribbonRect);

    // Ambient glow behind the ribbons
    final glowPaint = Paint()
      ..shader = ribbonShader
      ..style = PaintingStyle.stroke
      ..strokeWidth = strokeW * 1.35
      ..strokeCap = StrokeCap.round
      ..strokeJoin = StrokeJoin.round
      ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 9.0)
      ..color = Colors.white.withValues(alpha: 0.38);

    canvas.drawPath(strand1, glowPaint);
    canvas.drawPath(strand2, glowPaint);

    // Sharp main ribbon stroke
    final mainPaint = Paint()
      ..shader = ribbonShader
      ..style = PaintingStyle.stroke
      ..strokeWidth = strokeW
      ..strokeCap = StrokeCap.round
      ..strokeJoin = StrokeJoin.round;

    // Draw Strand 1 (underneath)
    canvas.drawPath(strand1, mainPaint);

    // Realistic drop shadow under Strand 2 where it crosses Strand 1
    // Short path segment around the center intersection (t ~ 0.35 to 0.65)
    final shadowPath = Path();
    shadowPath.moveTo(cx - x0 * 0.35, cy + y0 * 0.35);
    shadowPath.cubicTo(
      cx - x0 * 0.12,
      cy + y0 * 0.12,
      cx + x0 * 0.12,
      cy - y0 * 0.12,
      cx + x0 * 0.35,
      cy - y0 * 0.35,
    );

    final shadowPaint = Paint()
      ..color = const Color(0xB0020612)
      ..style = PaintingStyle.stroke
      ..strokeWidth = strokeW * 1.25
      ..strokeCap = StrokeCap.round
      ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 3.5);

    canvas.drawPath(shadowPath, shadowPaint);

    // Draw Strand 2 (on top)
    canvas.drawPath(strand2, mainPaint);
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}
