import 'package:flutter/material.dart';
import '../theme/app_theme.dart';

/// Shield logo drawn entirely with CustomPaint.
/// size = widget width; height is automatically 1.18× width.
class ShieldLogo extends StatelessWidget {
  const ShieldLogo({super.key, this.size = 210});
  final double size;

  @override
  Widget build(BuildContext context) => SizedBox(
    width: size,
    height: size * 1.18,
    child: CustomPaint(painter: _ShieldPainter()),
  );
}

class _ShieldPainter extends CustomPainter {
  // ─── helpers ────────────────────────────────────────────────────────────────
  Shader _shader(Rect r) => AppGradients.shieldRim.createShader(r);
  Shader _metallicShader(Rect r) => AppGradients.shieldMetallic.createShader(r);
  Shader _glassShader(Rect r) => AppGradients.shieldGlass.createShader(r);

  /// Builds the shield outline path inside a [w]×[h] rectangle.
  /// The shield has rounded top corners and a pointed bottom.
  Path _shield(double w, double h) {
    const cr = 0.13; // corner-radius fraction of width
    final p = Path();
    // top-left arc start → top-right arc start
    p.moveTo(w * cr, 0);
    p.lineTo(w * (1 - cr), 0);
    // top-right rounded corner
    p.quadraticBezierTo(w, 0, w, h * cr);
    // right side → bottom-right curve
    p.lineTo(w, h * .44);
    p.cubicTo(w, h * .72, w * .78, h * .88, w * .5, h * .985);
    // bottom-left curve → left side
    p.cubicTo(w * .22, h * .88, 0, h * .72, 0, h * .44);
    p.lineTo(0, h * cr);
    // top-left rounded corner
    p.quadraticBezierTo(0, 0, w * cr, 0);
    p.close();
    return p;
  }



  @override
  void paint(Canvas canvas, Size size) {
    final W = size.width;
    final H = size.height;
    final fullRect = Offset.zero & size;

    // ── 1. Outer glowing rim with premium gradient ─────────────────────────────
    canvas.drawPath(
      _shield(W, H),
      Paint()
        ..shader = _shader(fullRect)
        ..style = PaintingStyle.fill
        ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 8.0), // Soft glow
    );

    // ── 2. Premium metallic 3D body ──────────────────────────────────────────
    const inset = 0.12;
    final iW = W * (1 - inset * 2);
    final iH = H * (1 - inset * 2.2);

    // Metallic gradient fill
    canvas.save();
    canvas.translate(W * inset, H * inset);
    canvas.drawPath(
      _shield(iW, iH),
      Paint()
        ..shader = _metallicShader(Rect.fromLTWH(0, 0, iW, iH))
        ..style = PaintingStyle.fill,
    );
    canvas.restore();

    // ── 3. Premium glassmorphism overlay ─────────────────────────────────────
    const glassInset = 0.08;
    final gW = W * (1 - glassInset * 2);
    final gH = H * (1 - glassInset * 2);

    canvas.save();
    canvas.translate(W * glassInset, H * glassInset);
    canvas.drawPath(
      _shield(gW, gH),
      Paint()
        ..shader = _glassShader(Rect.fromLTWH(0, 0, gW, gH))
        ..style = PaintingStyle.fill
        ..blendMode = BlendMode.softLight,
    );
    canvas.restore();

    // ── 4. Sharp infinity symbol (∞) ──────────────────────────────────────────
    _drawInfinity(canvas, W, H);

    // ── 5. Additional glow effect ─────────────────────────────────────────────
    _drawGlowEffect(canvas, W, H);
  }

  /// Additional premium glow effect around the shield
  void _drawGlowEffect(Canvas canvas, double W, double H) {
    const glowInset = 0.18;
    final glowW = W * (1 - glowInset * 2);
    final glowH = H * (1 - glowInset * 2);

    final glowPaint = Paint()
      ..shader = _shader(Rect.fromLTWH(0, 0, glowW, glowH))
      ..style = PaintingStyle.stroke
      ..strokeWidth = W * 0.06
      ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 12.0)
      ..colorFilter = ColorFilter.mode(
          AppColors.cyanGlow.withValues(alpha: 0.2), BlendMode.srcIn);

    canvas.save();
    canvas.translate(W * glowInset, H * glowInset);
    canvas.drawPath(_shield(glowW, glowH), glowPaint);
    canvas.restore();
  }



  void _drawInfinity(Canvas canvas, double W, double H) {
    // Center of the symbol — vertically centred inside the shield body (~45% down)
    final cx = W * .50;
    final cy = H * .465;

    // Loop dimensions - adjusted to match reference
    final lx = W * .22;   // half-width of one loop's oval
    final ly = H * .11;   // half-height of one loop

    final strokeW = W * .085;

    final paint = Paint()
      ..shader = _shader(Rect.fromCenter(
          center: Offset(cx, cy), width: W * .8, height: H * .35))
      ..style = PaintingStyle.stroke
      ..strokeWidth = strokeW
      ..strokeCap = StrokeCap.round
      ..strokeJoin = StrokeJoin.round;

    // Left loop — drawn as two cubic beziers meeting at the centre cross-point
    final left = Path();
    left.moveTo(cx, cy);
    left.cubicTo(
      cx - lx * .5, cy - ly * 2.2,
      cx - lx * 2.0, cy - ly * 2.2,
      cx - lx * 2.0, cy,
    );
    left.cubicTo(
      cx - lx * 2.0, cy + ly * 2.2,
      cx - lx * .5, cy + ly * 2.2,
      cx, cy,
    );

    // Right loop
    final right = Path();
    right.moveTo(cx, cy);
    right.cubicTo(
      cx + lx * .5, cy - ly * 2.2,
      cx + lx * 2.0, cy - ly * 2.2,
      cx + lx * 2.0, cy,
    );
    right.cubicTo(
      cx + lx * 2.0, cy + ly * 2.2,
      cx + lx * .5, cy + ly * 2.2,
      cx, cy,
    );

    canvas.drawPath(left, paint);
    canvas.drawPath(right, paint);
  }

  @override
  bool shouldRepaint(covariant CustomPainter old) => false;
}
