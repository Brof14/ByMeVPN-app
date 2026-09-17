import 'package:flutter/material.dart';
import '../theme/app_theme.dart';

class GradientButton extends StatefulWidget {
  const GradientButton({
    super.key,
    required this.label,
    required this.gradient,
    required this.onTap,
  });

  final String label;
  final LinearGradient gradient;
  final VoidCallback onTap;

  @override
  State<GradientButton> createState() => _GradientButtonState();
}

class _GradientButtonState extends State<GradientButton> {
  bool _isHovered = false;
  bool _isPressed = false;

  @override
  Widget build(BuildContext context) {
    const radius = BorderRadius.all(Radius.circular(14));

    return MouseRegion(
      onEnter: (_) => setState(() => _isHovered = true),
      onExit: (_) => setState(() => _isHovered = false),
      child: GestureDetector(
        onTapDown: (_) => setState(() => _isPressed = true),
        onTapUp: (_) => setState(() => _isPressed = false),
        onTapCancel: () => setState(() => _isPressed = false),
        onTap: widget.onTap,
        child: AnimatedScale(
          scale: _isPressed ? 0.97 : (_isHovered ? 1.01 : 1.0),
          duration: const Duration(milliseconds: 120),
          child: Container(
            height: 52,
            decoration: BoxDecoration(
              gradient: widget.gradient,
              borderRadius: radius,
              boxShadow: [
                BoxShadow(
                  color: widget.gradient.colors.first.withValues(alpha: 0.35),
                  offset: const Offset(0, 4),
                  blurRadius: _isHovered ? 12 : 8,
                  spreadRadius: 0,
                ),
                BoxShadow(
                  color: widget.gradient.colors.last.withValues(alpha: 0.25),
                  offset: const Offset(0, 2),
                  blurRadius: 6,
                  spreadRadius: 0,
                ),
              ],
            ),
            child: Center(
              child: Text(
                widget.label,
                style: AppTextStyles.buttonText,
              ),
            ),
          ),
        ),
      ),
    );
  }
}
