import 'dart:async';
import 'package:flutter/material.dart';
import '../theme/app_theme.dart';
import '../widgets/google_sign_in_dialog.dart';
import '../widgets/gradient_background.dart';
import '../widgets/shield_logo.dart';
import '../widgets/social_icons.dart';
import 'home_screen.dart';
import 'sign_up_screen.dart';

final RegExp _emailRegex = RegExp(
  r'^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$',
);

/// Sign In / Log In Screen matching ByMeVPN visual identity and Android specifications.
class SignInScreen extends StatefulWidget {
  const SignInScreen({super.key});
  static const routeName = '/signin';

  @override
  State<SignInScreen> createState() => _SignInScreenState();
}

class _SignInScreenState extends State<SignInScreen> {
  final TextEditingController _emailController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();

  bool _obscurePassword = true;
  String? _emailError;
  String? _passwordError;
  String? _authError;

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  void _validateAndSubmit() {
    FocusScope.of(context).unfocus();
    final email = _emailController.text.trim();
    final password = _passwordController.text;

    bool valid = true;

    setState(() {
      // Validate Email
      if (email.isEmpty) {
        _emailError = 'Please enter your email';
        valid = false;
      } else if (!_emailRegex.hasMatch(email)) {
        _emailError = 'Invalid email format (e.g. name@domain.com)';
        valid = false;
      } else {
        _emailError = null;
      }

      // Validate Password
      if (password.isEmpty) {
        _passwordError = 'Please enter your password';
        valid = false;
      } else if (password.length < 8) {
        _passwordError = 'Password must be at least 8 characters';
        valid = false;
      } else {
        _passwordError = null;
      }

      if (valid) {
        _authError = null;
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Signing in to ByMeVPN...'),
            backgroundColor: Color(0xFF0F2B52),
          ),
        );
        Navigator.of(context).pushReplacement(
          MaterialPageRoute(
            builder: (_) => HomeScreen(userEmail: email, isGoogle: false),
          ),
        );
      } else {
        _authError = 'Invalid credentials. If you forgot your password, please reset it below.';
      }
    });
  }

  void _showGoogleAccountPicker() {
    showDialog(
      context: context,
      barrierColor: const Color(0xD9030610),
      builder: (ctx) => GoogleSignInDialog(
        onAccountSelected: (email, name) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text('Welcome back, $name!'),
              backgroundColor: const Color(0xFF14DE6C),
            ),
          );
          Navigator.of(context).pushReplacement(
            MaterialPageRoute(
              builder: (_) => HomeScreen(userEmail: email, isGoogle: true),
            ),
          );
        },
      ),
    );
  }

  void _showForgotPasswordDialog() {
    showDialog(
      context: context,
      barrierColor: const Color(0xCC040712),
      builder: (ctx) => _ForgotPasswordDialog(
        initialEmail: _emailController.text.trim(),
        onSuccess: (newPass, email) {
          setState(() {
            _authError = null;
            _passwordError = null;
            _emailController.text = email;
            _passwordController.text = newPass;
          });
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text('Password reset successfully! Please sign in with your new password.'),
              backgroundColor: Color(0xFF14DE6C),
            ),
          );
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final size = MediaQuery.of(context).size;
    final logoSize = (size.width * 0.42).clamp(140.0, 168.0);
    final horizontalPadding = (size.width * 0.065).clamp(20.0, 26.0);

    return Scaffold(
      backgroundColor: Colors.transparent,
      body: GradientBackground(
        child: SafeArea(
          child: Center(
            child: SingleChildScrollView(
              padding: EdgeInsets.symmetric(horizontal: horizontalPadding),
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 480),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  crossAxisAlignment: CrossAxisAlignment.center,
                  children: [
                    SizedBox(height: size.height * 0.025),

                    // Shield Logo
                    ShieldLogo(size: logoSize),

                    const SizedBox(height: 18),

                    // Brand Title: "ByMe" + "VPN"
                    RichText(
                      text: TextSpan(
                        children: [
                          TextSpan(
                            text: 'ByMe',
                            style: AppTextStyles.brandWhite.copyWith(fontSize: 34),
                          ),
                          TextSpan(
                            text: 'VPN',
                            style: AppTextStyles.brandGreen.copyWith(fontSize: 34),
                          ),
                        ],
                      ),
                    ),

                    const SizedBox(height: 6),

                    // Slogan
                    const Text(
                      'Speed. Anonymity. Honesty.',
                      style: TextStyle(
                        color: Color(0xFFCBD5E1),
                        fontSize: 14.5,
                        fontWeight: FontWeight.w500,
                        letterSpacing: 0.4,
                      ),
                    ),

                    const SizedBox(height: 28),

                    // 1. Email Field
                    _SignInTextField(
                      controller: _emailController,
                      hintText: 'Email',
                      prefixIcon: Icons.mail_outline_rounded,
                      keyboardType: TextInputType.emailAddress,
                      hasError: _emailError != null,
                      onChanged: (_) {
                        if (_emailError != null) setState(() => _emailError = null);
                        if (_authError != null) setState(() => _authError = null);
                      },
                    ),

                    if (_emailError != null)
                      Align(
                        alignment: Alignment.centerLeft,
                        child: Padding(
                          padding: const EdgeInsets.only(left: 14, top: 4),
                          child: Text(
                            _emailError!,
                            style: const TextStyle(
                              color: Color(0xFFFF5252),
                              fontSize: 12,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ),
                      ),

                    const SizedBox(height: 14),

                    // 2. Password Field
                    _SignInTextField(
                      controller: _passwordController,
                      hintText: 'Password (min 8 chars)',
                      prefixIcon: Icons.lock_outline_rounded,
                      obscureText: _obscurePassword,
                      hasError: _passwordError != null,
                      onChanged: (_) {
                        if (_passwordError != null) setState(() => _passwordError = null);
                        if (_authError != null) setState(() => _authError = null);
                      },
                      suffixIcon: IconButton(
                        icon: Icon(
                          _obscurePassword ? Icons.visibility_off_outlined : Icons.visibility_outlined,
                          color: _obscurePassword ? const Color(0xFF94A3B8) : const Color(0xFF00D4FF),
                          size: 20,
                        ),
                        onPressed: () => setState(() => _obscurePassword = !_obscurePassword),
                      ),
                    ),

                    if (_passwordError != null)
                      Align(
                        alignment: Alignment.centerLeft,
                        child: Padding(
                          padding: const EdgeInsets.only(left: 14, top: 4),
                          child: Text(
                            _passwordError!,
                            style: const TextStyle(
                              color: Color(0xFFFF5252),
                              fontSize: 12,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ),
                      ),

                    // Forgot Password Link
                    Align(
                      alignment: Alignment.centerRight,
                      child: Padding(
                        padding: const EdgeInsets.only(top: 10, right: 4),
                        child: GestureDetector(
                          onTap: _showForgotPasswordDialog,
                          child: const Text(
                            'Forgot Password?',
                            style: TextStyle(
                              color: Color(0xFF00D4FF),
                              fontSize: 13.5,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ),
                      ),
                    ),

                    if (_authError != null)
                      Container(
                        margin: const EdgeInsets.symmetric(vertical: 8),
                        padding: const EdgeInsets.all(12),
                        decoration: BoxDecoration(
                          color: const Color(0xFF2A1218),
                          borderRadius: BorderRadius.circular(10),
                          border: Border.all(color: const Color(0xFF8B2535)),
                        ),
                        child: Text(
                          _authError!,
                          style: const TextStyle(
                            color: Color(0xFFFF8A95),
                            fontSize: 12.5,
                            height: 1.35,
                          ),
                        ),
                      ),

                    const SizedBox(height: 16),

                    // 3. "Sign In" Button
                    _SignInButton(
                      onTap: _validateAndSubmit,
                    ),

                    const SizedBox(height: 22),

                    // 4. "OR" Divider
                    Row(
                      children: [
                        Expanded(
                          child: Container(
                            height: 1,
                            color: const Color(0xFF1B2A45),
                          ),
                        ),
                        const Padding(
                          padding: EdgeInsets.symmetric(horizontal: 14),
                          child: Text(
                            'OR',
                            style: TextStyle(
                              color: Color(0xFF94A3B8),
                              fontSize: 12.5,
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                        ),
                        Expanded(
                          child: Container(
                            height: 1,
                            color: const Color(0xFF1B2A45),
                          ),
                        ),
                      ],
                    ),

                    const SizedBox(height: 18),

                    // 5. Social Login: Google Sign-In ONLY
                    _GoogleFullButton(
                      onTap: _showGoogleAccountPicker,
                    ),

                    const SizedBox(height: 28),

                    // 6. Footer: "Don't have an account? Sign Up"
                    GestureDetector(
                      onTap: () {
                        Navigator.of(context).pushReplacementNamed(SignUpScreen.routeName);
                      },
                      child: Padding(
                        padding: const EdgeInsets.symmetric(vertical: 8.0),
                        child: RichText(
                          text: const TextSpan(
                            children: [
                              TextSpan(
                                text: "Don't have an account? ",
                                style: TextStyle(
                                  color: Color(0xFF94A3B8),
                                  fontSize: 14.5,
                                  fontWeight: FontWeight.w400,
                                ),
                              ),
                              TextSpan(
                                text: 'Sign Up',
                                style: TextStyle(
                                  color: Color(0xFF28E875),
                                  fontSize: 14.5,
                                  fontWeight: FontWeight.w800,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                    ),

                    const SizedBox(height: 20),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _SignInTextField extends StatelessWidget {
  const _SignInTextField({
    required this.controller,
    required this.hintText,
    required this.prefixIcon,
    this.obscureText = false,
    this.hasError = false,
    this.suffixIcon,
    this.keyboardType,
    this.onChanged,
  });

  final TextEditingController controller;
  final String hintText;
  final IconData prefixIcon;
  final bool obscureText;
  final bool hasError;
  final Widget? suffixIcon;
  final TextInputType? keyboardType;
  final ValueChanged<String>? onChanged;

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 54,
      decoration: BoxDecoration(
        color: const Color(0xFF091428),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(
          color: hasError ? const Color(0xFFFF5252) : const Color(0xFF1E3458),
          width: 1.2,
        ),
      ),
      child: Center(
        child: TextField(
          controller: controller,
          obscureText: obscureText,
          keyboardType: keyboardType,
          onChanged: onChanged,
          style: const TextStyle(
            color: Colors.white,
            fontSize: 15.5,
            fontWeight: FontWeight.w500,
          ),
          cursorColor: AppColors.brandGreen,
          decoration: InputDecoration(
            isDense: true,
            hintText: hintText,
            hintStyle: const TextStyle(
              color: Color(0xFF94A3B8),
              fontSize: 15.0,
              fontWeight: FontWeight.w400,
            ),
            prefixIcon: Icon(
              prefixIcon,
              color: const Color(0xFF94A3B8),
              size: 21,
            ),
            suffixIcon: suffixIcon,
            border: InputBorder.none,
            contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
          ),
        ),
      ),
    );
  }
}

class _SignInButton extends StatefulWidget {
  const _SignInButton({required this.onTap});
  final VoidCallback onTap;

  @override
  State<_SignInButton> createState() => _SignInButtonState();
}

class _SignInButtonState extends State<_SignInButton> {
  bool _isPressed = false;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTapDown: (_) => setState(() => _isPressed = true),
      onTapUp: (_) => setState(() => _isPressed = false),
      onTapCancel: () => setState(() => _isPressed = false),
      onTap: widget.onTap,
      child: AnimatedScale(
        scale: _isPressed ? 0.98 : 1.0,
        duration: const Duration(milliseconds: 100),
        child: Container(
          height: 54,
          width: double.infinity,
          decoration: BoxDecoration(
            gradient: const LinearGradient(
              begin: Alignment.centerLeft,
              end: Alignment.centerRight,
              colors: [
                Color(0xFF00C4FF), // Bright Cyan
                Color(0xFF28E875), // Vibrant Lime Green
              ],
            ),
            borderRadius: BorderRadius.circular(15),
          ),
          child: const Center(
            child: Text(
              'Sign In',
              style: TextStyle(
                color: Color(0xFF051515),
                fontSize: 16.5,
                fontWeight: FontWeight.w800,
                letterSpacing: 0.3,
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _GoogleFullButton extends StatefulWidget {
  const _GoogleFullButton({required this.onTap});
  final VoidCallback onTap;

  @override
  State<_GoogleFullButton> createState() => _GoogleFullButtonState();
}

class _GoogleFullButtonState extends State<_GoogleFullButton> {
  bool _isPressed = false;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTapDown: (_) => setState(() => _isPressed = true),
      onTapUp: (_) => setState(() => _isPressed = false),
      onTapCancel: () => setState(() => _isPressed = false),
      onTap: widget.onTap,
      child: AnimatedScale(
        scale: _isPressed ? 0.98 : 1.0,
        duration: const Duration(milliseconds: 100),
        child: Container(
          height: 52,
          width: double.infinity,
          decoration: BoxDecoration(
            color: const Color(0xFF0D172A),
            borderRadius: BorderRadius.circular(14),
            border: Border.all(
              color: const Color(0xFF1E3458),
              width: 1.2,
            ),
          ),
          child: const Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              GoogleLogoIcon(size: 22),
              SizedBox(width: 12),
              Text(
                'Continue with Google',
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 15.0,
                  fontWeight: FontWeight.w700,
                  letterSpacing: 0.2,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _ForgotPasswordDialog extends StatefulWidget {
  const _ForgotPasswordDialog({
    required this.initialEmail,
    required this.onSuccess,
  });

  final String initialEmail;
  final Function(String newPass, String email) onSuccess;

  @override
  State<_ForgotPasswordDialog> createState() => _ForgotPasswordDialogState();
}

class _ForgotPasswordDialogState extends State<_ForgotPasswordDialog> {
  late final TextEditingController _emailController;
  final TextEditingController _codeController = TextEditingController();
  final TextEditingController _newPassController = TextEditingController();

  bool _codeSent = false;
  String? _error;
  int _countdown = 60;
  Timer? _timer;
  final String _generatedCode = "482910";

  @override
  void initState() {
    super.initState();
    _emailController = TextEditingController(text: widget.initialEmail);
  }

  void _startTimer() {
    _countdown = 60;
    _timer?.cancel();
    _timer = Timer.periodic(const Duration(seconds: 1), (t) {
      if (_countdown > 0) {
        setState(() => _countdown--);
      } else {
        t.cancel();
      }
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    _emailController.dispose();
    _codeController.dispose();
    _newPassController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Dialog(
      backgroundColor: Colors.transparent,
      insetPadding: const EdgeInsets.all(20),
      child: Container(
        constraints: const BoxConstraints(maxWidth: 400),
        padding: const EdgeInsets.all(24),
        decoration: BoxDecoration(
          color: const Color(0xFF0F1A2F),
          borderRadius: BorderRadius.circular(20),
          border: Border.all(color: const Color(0xFF223A63), width: 1.2),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              !_codeSent ? 'Reset Password' : 'Enter Verification Code',
              textAlign: TextAlign.center,
              style: const TextStyle(
                color: Colors.white,
                fontSize: 19,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 10),
            Text(
              !_codeSent
                  ? "Enter the email associated with your account. We'll send you a 6-digit confirmation code."
                  : "We sent a 6-digit code to ${_emailController.text.trim()}. Enter the code and your new password.",
              textAlign: TextAlign.center,
              style: const TextStyle(
                color: Color(0xFF94A3B8),
                fontSize: 13,
                height: 1.35,
              ),
            ),
            const SizedBox(height: 20),

            if (!_codeSent) ...[
              _SignInTextField(
                controller: _emailController,
                hintText: 'Your email address',
                prefixIcon: Icons.mail_outline_rounded,
                keyboardType: TextInputType.emailAddress,
                onChanged: (_) => setState(() => _error = null),
              ),
            ] else ...[
              // Quick Fill chip
              Center(
                child: GestureDetector(
                  onTap: () => setState(() => _codeController.text = _generatedCode),
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: const Color(0x2200D4FF),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Text(
                      'Quick Fill: $_generatedCode',
                      style: const TextStyle(
                        color: Color(0xFF00D4FF),
                        fontSize: 12,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 10),
              _SignInTextField(
                controller: _codeController,
                hintText: '6-digit code',
                prefixIcon: Icons.pin_outlined,
                keyboardType: TextInputType.number,
                onChanged: (_) => setState(() => _error = null),
              ),
              const SizedBox(height: 12),
              _SignInTextField(
                controller: _newPassController,
                hintText: 'New Password (min 8 chars)',
                prefixIcon: Icons.lock_outline_rounded,
                obscureText: true,
                onChanged: (_) => setState(() => _error = null),
              ),
              const SizedBox(height: 8),
              Align(
                alignment: Alignment.centerRight,
                child: GestureDetector(
                  onTap: _countdown == 0 ? _startTimer : null,
                  child: Text(
                    _countdown > 0 ? 'Resend code in ${_countdown}s' : 'Resend code',
                    style: TextStyle(
                      color: _countdown > 0 ? const Color(0xFF64758E) : const Color(0xFF28E875),
                      fontSize: 12.5,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
              ),
            ],

            if (_error != null) ...[
              const SizedBox(height: 10),
              Text(
                _error!,
                textAlign: TextAlign.center,
                style: const TextStyle(
                  color: Color(0xFFFF5252),
                  fontSize: 12,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],

            const SizedBox(height: 20),

            Row(
              children: [
                Expanded(
                  child: GestureDetector(
                    onTap: () => Navigator.of(context).pop(),
                    child: Container(
                      height: 46,
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
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: GestureDetector(
                    onTap: () {
                      if (!_codeSent) {
                        final email = _emailController.text.trim();
                        if (email.isEmpty || !_emailRegex.hasMatch(email)) {
                          setState(() => _error = 'Please enter a valid email');
                        } else {
                          setState(() {
                            _codeSent = true;
                            _error = null;
                          });
                          _startTimer();
                        }
                      } else {
                        if (_codeController.text.trim() != _generatedCode) {
                          setState(() => _error = "Invalid code. Click 'Quick Fill' to test.");
                        } else if (_newPassController.text.length < 8) {
                          setState(() => _error = 'Password must be at least 8 characters');
                        } else {
                          Navigator.of(context).pop();
                          widget.onSuccess(_newPassController.text, _emailController.text.trim());
                        }
                      }
                    },
                    child: Container(
                      height: 46,
                      decoration: BoxDecoration(
                        gradient: const LinearGradient(
                          colors: [Color(0xFF00C4FF), Color(0xFF28E875)],
                        ),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Center(
                        child: Text(
                          !_codeSent ? 'Send Code' : 'Confirm',
                          style: const TextStyle(
                            color: Color(0xFF051515),
                            fontSize: 14.5,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
