import 'package:flutter/material.dart';
import 'social_icons.dart';

class GoogleSignInDialog extends StatefulWidget {
  const GoogleSignInDialog({super.key, required this.onAccountSelected});

  final Function(String email, String name) onAccountSelected;

  @override
  State<GoogleSignInDialog> createState() => _GoogleSignInDialogState();
}

class _GoogleSignInDialogState extends State<GoogleSignInDialog> {
  bool _isSigningIn = false;

  void _select(String email, String name) {
    setState(() => _isSigningIn = true);
    Future.delayed(const Duration(milliseconds: 900), () {
      if (mounted) {
        Navigator.of(context).pop();
        widget.onAccountSelected(email, name);
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Dialog(
      backgroundColor: Colors.transparent,
      insetPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 24),
      child: Container(
        constraints: const BoxConstraints(maxWidth: 400),
        padding: const EdgeInsets.all(24),
        decoration: BoxDecoration(
          color: const Color(0xFF0F1A2F),
          borderRadius: BorderRadius.circular(24),
          border: Border.all(color: const Color(0xFF223A63), width: 1.2),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const GoogleLogoIcon(size: 32),
            const SizedBox(height: 14),
            const Text(
              'Sign in with Google',
              style: TextStyle(
                color: Colors.white,
                fontSize: 20,
                fontWeight: FontWeight.bold,
                letterSpacing: 0.2,
              ),
            ),
            const SizedBox(height: 4),
            const Text(
              'Choose an account to continue to ByMeVPN',
              textAlign: TextAlign.Center,
              style: TextStyle(
                color: Color(0xFF94A3B8),
                fontSize: 13.5,
              ),
            ),
            const SizedBox(height: 18),
            Container(height: 1, color: const Color(0xFF1E3050)),
            const SizedBox(height: 14),

            if (_isSigningIn) ...[
              const Padding(
                padding: EdgeInsets.symmetric(vertical: 24),
                child: Column(
                  children: [
                    CircularProgressIndicator(
                      color: Color(0xFF4285F4),
                      strokeWidth: 3,
                    ),
                    SizedBox(height: 16),
                    Text(
                      'Connecting to Google...',
                      style: TextStyle(
                        color: Color(0xFFCBD5E1),
                        fontSize: 14.5,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ],
                ),
              ),
            ] else ...[
              _buildAccountTile(
                name: 'Mama',
                email: 'mama.nikfjdj@gmail.com',
                initial: 'M',
                color: const Color(0xFF1A73E8),
              ),
              const SizedBox(height: 10),
              _buildAccountTile(
                name: 'ByMeVPN User',
                email: 'bymevpn.user@gmail.com',
                initial: 'B',
                color: const Color(0xFF0F9D58),
              ),
              const SizedBox(height: 16),
              Container(height: 1, color: const Color(0xFF1E3050)),
              const SizedBox(height: 14),
              const Text(
                'To continue, Google will share your name, email address, and language preference with ByMeVPN.',
                textAlign: TextAlign.Center,
                style: TextStyle(
                  color: Color(0xFF7085A3),
                  fontSize: 11.5,
                  height: 1.35,
                ),
              ),
              const SizedBox(height: 18),
              GestureDetector(
                onTap: () => Navigator.of(context).pop(),
                child: Container(
                  height: 46,
                  width: double.infinity,
                  decoration: BoxDecoration(
                    color: const Color(0xFF162542),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: const Center(
                    child: Text(
                      'Cancel',
                      style: TextStyle(
                        color: Color(0xFF94A3B8),
                        fontSize: 14.5,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildAccountTile({
    required String name,
    required String email,
    required String initial,
    required Color color,
  }) {
    return GestureDetector(
      onTap: () => _select(email, name),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
        decoration: BoxDecoration(
          color: const Color(0xFF14223A),
          borderRadius: BorderRadius.circular(14),
          border: Border.all(color: const Color(0xFF1E345A)),
        ),
        child: Row(
          children: [
            Container(
              width: 38,
              height: 38,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: color,
              ),
              child: Center(
                child: Text(
                  initial,
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    name,
                    style: const TextStyle(
                      color: Colors.white,
                      fontSize: 14.5,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  Text(
                    email,
                    style: const TextStyle(
                      color: Color(0xFF94A3B8),
                      fontSize: 12.5,
                    ),
                  ),
                ],
              ),
            ),
            const GoogleLogoIcon(size: 18),
          ],
        ),
      ),
    );
  }
}
