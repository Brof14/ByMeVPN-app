import 'package:flutter/material.dart';
import '../theme/app_theme.dart';

class GradientButton extends StatefulWidget {
  const GradientButton({
    super.key,
    required this.label,
    required this.gradient,
    required this.onTap,
    this.darkLabel = false,
  });

  final String label;
  final LinearGradient gradient;
  final VoidCallback onTap;
  final bool darkLabel;

  @override
  State<GradientButton> createState() => _GradientButtonState();
}

class _GradientButtonState extends State<GradientButton> {
  bool _isHovered = false;
  bool _isPressed = false;

  @override
  Widget build(BuildContext context) {
    const radius = BorderRadius.all(Radius.circular(16));

    return MouseRegion(
      onEnter: (_) => setState(() => _isHovered = true),
      onExit: (_) => setState(() => _isHovered = false),
      child: GestureDetector(
        onTapDown: (_) => setState(() => _isPressed = true),
        onTapUp: (_) => setState(() => _isPressed = false),
        onTapCancel: () => setState(() => _isPressed = false),
        onTap: widget.onTap,
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 200),
          curve: Curves.easeOut,
          margin: EdgeInsets.symmetric(
            horizontal: _isHovered ? 2.0 : 0.0,
            vertical: _isPressed ? 1.0 : 0.0,
          ),
          decoration: BoxDecoration(
            gradient: widget.gradient,
            borderRadius: radius,
            boxShadow: [
              BoxShadow(
                color: widget.darkLabel
                    ? Colors.transparent
                    : widget.gradient.colors[0].withValues(alpha: 0.3),
                offset: const Offset(0, 4),
                blurRadius: _isHovered ? 12 : 8,
                spreadRadius: _isPressed ? 1 : 0,
              ),
              BoxShadow(
                color: widget.darkLabel
                    ? Colors.transparent
                    : widget.gradient.colors.last.withValues(alpha: 0.2),
                offset: const Offset(0, 2),
                blurRadius: _isHovered ? 8 : 4,
                spreadRadius: _isPressed ? 0 : 0,
              ),
            ],
          ),
          child: SizedBox(
            height: 56,
            child: Center(
              child: Text(
                widget.label,
                style: widget.darkLabel ? AppTextStyles.btnDark : AppTextStyles.btnWhite,
              ),
            ),
          ),
        ),
      ),
    );
  }
}
