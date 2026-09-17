import 'dart:math' as math;
import 'package:flutter/material.dart';

/// Authentic Google 'G' logo drawn using Canvas
class GoogleLogoWidget extends StatelessWidget {
  const GoogleLogoWidget({super.key, this.size = 24});
  final double size;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: size,
      height: size,
      child: CustomPaint(
        painter: _GoogleLogoPainter(),
      ),
    );
  }
}

class _GoogleLogoPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final w = size.width;
    final h = size.height;
    final center = Offset(w / 2, h / 2);
    final radius = w / 2;
    final strokeW = w * 0.22;

    final rect = Rect.fromCircle(center: center, radius: radius - strokeW / 2);

    final bluePaint = Paint()
      ..color = const Color(0xFF4285F4)
      ..style = PaintingStyle.stroke
      ..strokeWidth = strokeW
      ..strokeCap = StrokeCap.butt;

    final greenPaint = Paint()
      ..color = const Color(0xFF34A853)
      ..style = PaintingStyle.stroke
      ..strokeWidth = strokeW
      ..strokeCap = StrokeCap.butt;

    final yellowPaint = Paint()
      ..color = const Color(0xFFFBBC05)
      ..style = PaintingStyle.stroke
      ..strokeWidth = strokeW
      ..strokeCap = StrokeCap.butt;

    final redPaint = Paint()
      ..color = const Color(0xFFEA4335)
      ..style = PaintingStyle.stroke
      ..strokeWidth = strokeW
      ..strokeCap = StrokeCap.butt;

    // Blue arc (right and middle bar)
    canvas.drawArc(rect, -0.75, 1.25, false, bluePaint);
    // Green arc (bottom right to bottom left)
    canvas.drawArc(rect, 0.5, 1.35, false, greenPaint);
    // Yellow arc (bottom left to top left)
    canvas.drawArc(rect, 1.85, 1.5, false, yellowPaint);
    // Red arc (top left to top right)
    canvas.drawArc(rect, 3.35, 1.45, false, redPaint);

    // Blue horizontal bar in the middle
    final barPaint = Paint()
      ..color = const Color(0xFF4285F4)
      ..style = PaintingStyle.fill;

    final barRect = Rect.fromLTWH(
      center.dx,
      center.dy - strokeW / 2,
      radius,
      strokeW,
    );
    canvas.drawRect(barRect, barPaint);
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}

/// Authentic Apple silhouette logo drawn using Canvas
class AppleLogoWidget extends StatelessWidget {
  const AppleLogoWidget({super.key, this.size = 24, this.color = Colors.white});
  final double size;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: size,
      height: size,
      child: CustomPaint(
        painter: _AppleLogoPainter(color: color),
      ),
    );
  }
}

class _AppleLogoPainter extends CustomPainter {
  _AppleLogoPainter({required this.color});
  final Color color;

  @override
  void paint(Canvas canvas, Size size) {
    final scale = size.width / 170.0;
    canvas.save();
    canvas.scale(scale, scale);

    final paint = Paint()
      ..color = color
      ..style = PaintingStyle.fill;

    // Leaf
    final leaf = Path();
    leaf.moveTo(105.4, 38.6);
    leaf.cubicTo(114.7, 27.2, 121.0, 11.5, 119.3, -4.0);
    leaf.cubicTo(105.9, -3.4, 89.6, 5.0, 80.0, 16.2);
    leaf.cubicTo(71.5, 26.1, 64.0, 42.1, 66.0, 57.2);
    leaf.cubicTo(80.9, 58.3, 96.1, 49.9, 105.4, 38.6);
    leaf.close();
    canvas.drawPath(leaf, paint);

    // Apple body with bite on the right
    final body = Path();
    body.moveTo(125.6, 67.2);
    body.cubicTo(102.1, 68.6, 85.0, 54.0, 68.6, 54.0);
    body.cubicTo(51.3, 54.0, 31.7, 69.1, 19.3, 69.1);
    body.cubicTo(-6.6, 69.1, -38.0, 41.6, -38.0, 3.5);
    body.cubicTo(-38.0, -32.6, -17.5, -50.9, 1.2, -50.9);
    body.cubicTo(16.5, -50.9, 29.8, -40.0, 41.9, -40.0);
    body.cubicTo(53.2, -40.0, 68.6, -51.8, 85.9, -51.8);
    body.cubicTo(93.4, -51.8, 119.6, -49.0, 136.0, -25.0);
    body.cubicTo(103.5, -6.0, 109.5, 38.0, 140.0, 49.7);
    body.cubicTo(133.0, 70.0, 116.0, 100.0, 98.0, 126.0);
    body.close();

    // Standard simplified apple body centered
    final simpleBody = Path();
    simpleBody.moveTo(85, 38);
    simpleBody.cubicTo(68, 38, 54, 46, 42, 46);
    simpleBody.cubicTo(30, 46, 17, 36, 0, 36);
    simpleBody.cubicTo(-28, 36, -55, 60, -55, 104);
    simpleBody.cubicTo(-55, 140, -36, 174, -18, 200);
    simpleBody.cubicTo(-6, 218, 5, 236, 19, 236);
    simpleBody.cubicTo(32, 236, 40, 224, 56, 224);
    simpleBody.cubicTo(72, 224, 80, 236, 94, 236);
    simpleBody.cubicTo(108, 236, 118, 219, 130, 200);
    simpleBody.cubicTo(144, 179, 150, 168, 155, 156);
    simpleBody.cubicTo(117, 141, 113, 86, 152, 69);
    simpleBody.cubicTo(137, 47, 112, 38, 85, 38);
    simpleBody.close();

    // Draw standard smooth apple
    canvas.restore();

    // Direct path in 0..size coordinates
    final p = Path();
    final sw = size.width;
    final sh = size.height;

    // Leaf
    p.moveTo(sw * 0.54, sh * 0.05);
    p.cubicTo(sw * 0.60, sh * 0.05, sw * 0.68, sh * 0.12, sw * 0.66, sh * 0.22);
    p.cubicTo(sw * 0.58, sh * 0.22, sw * 0.50, sh * 0.15, sw * 0.54, sh * 0.05);
    p.close();

    // Body
    p.moveTo(sw * 0.65, sh * 0.26);
    p.cubicTo(sw * 0.56, sh * 0.26, sw * 0.49, sh * 0.31, sw * 0.42, sh * 0.31);
    p.cubicTo(sw * 0.35, sh * 0.31, sw * 0.27, sh * 0.26, sw * 0.18, sh * 0.26);
    p.cubicTo(sw * 0.05, sh * 0.26, 0.0, sh * 0.41, 0.0, sh * 0.60);
    p.cubicTo(0.0, sh * 0.77, sw * 0.11, sh * 0.95, sw * 0.21, sh * 0.95);
    p.cubicTo(sw * 0.28, sh * 0.95, sw * 0.32, sh * 0.89, sw * 0.42, sh * 0.89);
    p.cubicTo(sw * 0.52, sh * 0.89, sw * 0.56, sh * 0.95, sw * 0.63, sh * 0.95);
    p.cubicTo(sw * 0.73, sh * 0.95, sw * 0.82, sh * 0.79, sw * 0.86, sh * 0.72);
    // Bite
    p.cubicTo(sw * 0.73, sh * 0.67, sw * 0.73, sh * 0.49, sw * 0.86, sh * 0.43);
    p.cubicTo(sw * 0.80, sh * 0.33, sw * 0.71, sh * 0.26, sw * 0.65, sh * 0.26);
    p.close();

    canvas.drawPath(p, paint);
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}
