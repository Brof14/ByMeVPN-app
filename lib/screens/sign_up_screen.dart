import 'package:flutter/material.dart';
import '../theme/app_theme.dart';
import '../widgets/google_sign_in_dialog.dart';
import '../widgets/gradient_background.dart';
import '../widgets/shield_logo.dart';
import '../widgets/social_icons.dart';
import 'home_screen.dart';
import 'sign_in_screen.dart';

final RegExp _emailRegex = RegExp(
  r'^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$',
);
final RegExp _phoneRegex = RegExp(
  r'^\+?[0-9\s\-()]{7,16}$',
);

/// Sign Up screen matching ByMeVPN visual identity and layout.
class SignUpScreen extends StatefulWidget {
  const SignUpScreen({super.key});
  static const routeName = '/signup';

  @override
  State<SignUpScreen> createState() => _SignUpScreenState();
}

class _SignUpScreenState extends State<SignUpScreen> {
  final TextEditingController _accountController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();
  bool _obscurePassword = true;

  String? _accountError;
  String? _passwordError;

  @override
  void dispose() {
    _accountController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  void _validateAndSubmit() {
    FocusScope.of(context).unfocus();
    final account = _accountController.text.trim();
    final password = _passwordController.text;

    bool valid = true;

    setState(() {
      if (account.isEmpty) {
        _accountError = 'Please enter your email or phone';
        valid = false;
      } else if (!_emailRegex.hasMatch(account) && !_phoneRegex.hasMatch(account)) {
        _accountError = 'Invalid email or phone number format';
        valid = false;
      } else {
        _accountError = null;
      }

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
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Account created! Welcome to ByMeVPN.'),
            backgroundColor: Color(0xFF14DE6C),
          ),
        );
        Navigator.of(context).pushReplacement(
          MaterialPageRoute(
            builder: (_) => HomeScreen(userEmail: account, isGoogle: false),
          ),
        );
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
              content: Text('Welcome to ByMeVPN, $name!'),
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

  @override
  Widget build(BuildContext context) {
    final mq = MediaQuery.of(context);
    final sw = mq.size.width;
    final sh = mq.size.height;

    final logoSize = (sw * 0.42).clamp(140.0, 168.0);
    final hPad = (sw * 0.065).clamp(20.0, 26.0);

    return Scaffold(
      backgroundColor: Colors.transparent,
      body: GradientBackground(
        child: SafeArea(
          child: SizedBox.expand(
            child: SingleChildScrollView(
              physics: const BouncingScrollPhysics(),
              padding: EdgeInsets.symmetric(horizontal: hPad),
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 480),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.center,
                  children: [
                    SizedBox(height: (sh * 0.035).clamp(16.0, 28.0)),

                    // Shield Logo
                    ShieldLogo(size: logoSize),

                    const SizedBox(height: 18),

                    // Brand Text: "ByMe" (White) + "VPN" (Emerald Green)
                    RichText(
                      textAlign: TextAlign.center,
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

                    // Slogan: "Speed. Anonymity. Honesty."
                    const Text(
                      'Speed. Anonymity. Honesty.',
                      style: TextStyle(
                        color: Color(0xFFCBD5E1),
                        fontSize: 14.5,
                        fontWeight: FontWeight.w500,
                        letterSpacing: 0.4,
                      ),
                      textAlign: TextAlign.center,
                    ),

                    const SizedBox(height: 28),

                    // 1. Input: Email or phone number
                    _InputField(
                      controller: _accountController,
                      hintText: 'Email or phone number',
                      prefixIcon: Icons.mail_outline_rounded,
                      keyboardType: TextInputType.emailAddress,
                      hasError: _accountError != null,
                      onChanged: (_) {
                        if (_accountError != null) setState(() => _accountError = null);
                      },
                    ),

                    if (_accountError != null)
                      Align(
                        alignment: Alignment.centerLeft,
                        child: Padding(
                          padding: const EdgeInsets.only(left: 14, top: 4),
                          child: Text(
                            _accountError!,
                            style: const TextStyle(
                              color: Color(0xFFFF5252),
                              fontSize: 12,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ),
                      ),

                    const SizedBox(height: 14),

                    // 2. Input: Password
                    _InputField(
                      controller: _passwordController,
                      hintText: 'Password (min 8 chars)',
                      prefixIcon: Icons.lock_outline_rounded,
                      obscureText: _obscurePassword,
                      hasError: _passwordError != null,
                      onChanged: (_) {
                        if (_passwordError != null) setState(() => _passwordError = null);
                      },
                      suffixIcon: IconButton(
                        icon: Icon(
                          _obscurePassword
                              ? Icons.visibility_off_outlined
                              : Icons.visibility_outlined,
                          color: _obscurePassword ? const Color(0xFF94A3B8) : const Color(0xFF00D4FF),
                          size: 20,
                        ),
                        onPressed: () {
                          setState(() {
                            _obscurePassword = !_obscurePassword;
                          });
                        },
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

                    const SizedBox(height: 22),

                    // 3. Button: "Create Account"
                    _CreateAccountButton(
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

                    // 5. Social Login Buttons: Google & Apple
                    Row(
                      children: [
                        Expanded(
                          child: _SocialButton(
                            onTap: _showGoogleAccountPicker,
                            child: const GoogleLogoWidget(size: 24),
                          ),
                        ),
                        const SizedBox(width: 14),
                        Expanded(
                          child: _SocialButton(
                            onTap: () {
                              ScaffoldMessenger.of(context).showSnackBar(
                                const SnackBar(
                                  content: Text('Apple ID is available on iOS devices. Try Google Sign-In!'),
                                  backgroundColor: Color(0xFF0F2B52),
                                ),
                              );
                            },
                            child: const AppleLogoWidget(size: 24),
                          ),
                        ),
                      ],
                    ),

                    const SizedBox(height: 28),

                    // 6. Footer: "Already have an account? Sign in"
                    GestureDetector(
                      onTap: () {
                        Navigator.of(context).pushReplacementNamed(SignInScreen.routeName);
                      },
                      child: Padding(
                        padding: const EdgeInsets.symmetric(vertical: 8.0),
                        child: RichText(
                          textAlign: TextAlign.center,
                          text: const TextSpan(
                            children: [
                              TextSpan(
                                text: 'Already have an account? ',
                                style: TextStyle(
                                  color: Color(0xFF94A3B8),
                                  fontSize: 14.5,
                                  fontWeight: FontWeight.w400,
                                ),
                              ),
                              TextSpan(
                                text: 'Sign in',
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

class _InputField extends StatelessWidget {
  const _InputField({
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

class _CreateAccountButton extends StatefulWidget {
  const _CreateAccountButton({required this.onTap});
  final VoidCallback onTap;

  @override
  State<_CreateAccountButton> createState() => _CreateAccountButtonState();
}

class _CreateAccountButtonState extends State<_CreateAccountButton> {
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
              'Create Account',
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

class _SocialButton extends StatefulWidget {
  const _SocialButton({required this.onTap, required this.child});
  final VoidCallback onTap;
  final Widget child;

  @override
  State<_SocialButton> createState() => _SocialButtonState();
}

class _SocialButtonState extends State<_SocialButton> {
  bool _isPressed = false;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTapDown: (_) => setState(() => _isPressed = true),
      onTapUp: (_) => setState(() => _isPressed = false),
      onTapCancel: () => setState(() => _isPressed = false),
      onTap: widget.onTap,
      child: AnimatedScale(
        scale: _isPressed ? 0.97 : 1.0,
        duration: const Duration(milliseconds: 100),
        child: Container(
          height: 54,
          decoration: BoxDecoration(
            color: const Color(0xFF0D172A),
            borderRadius: BorderRadius.circular(14),
            border: Border.all(
              color: const Color(0xFF1E3458),
              width: 1.2,
            ),
          ),
          child: Center(child: child),
        ),
      ),
    );
  }
}
