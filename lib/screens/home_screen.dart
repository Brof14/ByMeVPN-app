import 'package:flutter/material.dart';
import '../theme/app_theme.dart';
import '../widgets/gradient_background.dart';
import '../widgets/shield_logo.dart';
import '../widgets/social_icons.dart';
import 'welcome_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({
    super.key,
    this.userEmail = 'mama.nikfjdj@gmail.com',
    this.isGoogle = true,
  });

  final String userEmail;
  final bool isGoogle;
  static const routeName = '/home';

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> with SingleTickerProviderStateMixin {
  bool _isConnected = true;
  String _selectedCountry = 'United States';
  String _selectedCity = 'New York';
  String _selectedFlag = '🇺🇸';
  int _selectedPing = 24;
  bool _showServers = false;

  late AnimationController _pulseController;

  @override
  void initState() {
    super.initState();
    _pulseController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1400),
    )..repeat(reverse: true);
  }

  @override
  void dispose() {
    _pulseController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.transparent,
      body: GradientBackground(
        child: SafeArea(
          child: SingleChildScrollView(
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 14),
            child: Column(
              children: [
                // Top Bar
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                      decoration: BoxDecoration(
                        color: const Color(0xFF0F1E38),
                        borderRadius: BorderRadius.circular(20),
                        border: Border.all(color: const Color(0xFF1E3860)),
                      ),
                      child: Row(
                        children: [
                          if (widget.isGoogle) ...[
                            const GoogleLogoIcon(size: 16),
                            const SizedBox(width: 6),
                          ],
                          Text(
                            widget.userEmail,
                            style: const TextStyle(
                              color: Colors.white,
                              fontSize: 12.5,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ],
                      ),
                    ),
                    GestureDetector(
                      onTap: () {
                        Navigator.of(context).pushNamedAndRemoveUntil(
                          WelcomeScreen.routeName,
                          (route) => false,
                        );
                      },
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                        decoration: BoxDecoration(
                          color: const Color(0xFF1A1A2E),
                          borderRadius: BorderRadius.circular(12),
                          border: Border.all(color: const Color(0xFF332F4C)),
                        ),
                        child: const Text(
                          'Log Out',
                          style: TextStyle(
                            color: Color(0xFFFF6B6B),
                            fontSize: 12.5,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                    ),
                  ],
                ),

                const SizedBox(height: 24),

                // Shield Logo
                const ShieldLogo(size: 110),

                const SizedBox(height: 18),

                // Status Badge
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                  decoration: BoxDecoration(
                    color: _isConnected
                        ? const Color(0xFF26E875).withValues(alpha: 0.15)
                        : const Color(0xFF64758E).withValues(alpha: 0.15),
                    borderRadius: BorderRadius.circular(14),
                    border: Border.all(
                      color: _isConnected
                          ? const Color(0xFF26E875).withValues(alpha: 0.4)
                          : const Color(0xFF64758E).withValues(alpha: 0.3),
                    ),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Container(
                        width: 8,
                        height: 8,
                        decoration: BoxDecoration(
                          shape: BoxShape.circle,
                          color: _isConnected ? const Color(0xFF26E875) : const Color(0xFF94A3B8),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Text(
                        _isConnected ? 'CONNECTED & PROTECTED' : 'VPN DISCONNECTED',
                        style: TextStyle(
                          color: _isConnected ? const Color(0xFF26E875) : const Color(0xFF94A3B8),
                          fontSize: 13,
                          fontWeight: FontWeight.bold,
                          letterSpacing: 0.5,
                        ),
                      ),
                    ],
                  ),
                ),

                const SizedBox(height: 30),

                // Big Power Toggle Button
                GestureDetector(
                  onTap: () => setState(() => _isConnected = !_isConnected),
                  child: AnimatedBuilder(
                    animation: _pulseController,
                    builder: (context, child) {
                      return Container(
                        width: 170,
                        height: 170,
                        decoration: BoxDecoration(
                          shape: BoxShape.circle,
                          boxShadow: _isConnected
                              ? [
                                  BoxShadow(
                                    color: const Color(0xFF26E875)
                                        .withValues(alpha: 0.25 * _pulseController.value),
                                    blurRadius: 28,
                                    spreadRadius: 8,
                                  ),
                                ]
                              : [],
                        ),
                        child: Center(
                          child: Container(
                            width: 130,
                            height: 130,
                            decoration: BoxDecoration(
                              shape: BoxShape.circle,
                              gradient: _isConnected
                                  ? const LinearGradient(
                                      colors: [Color(0xFF00C4FF), Color(0xFF26E875)],
                                    )
                                  : const LinearGradient(
                                      colors: [Color(0xFF162542), Color(0xFF0A1324)],
                                    ),
                            ),
                            child: Center(
                              child: Text(
                                _isConnected ? 'STOP' : 'START',
                                style: TextStyle(
                                  color: _isConnected ? const Color(0xFF031015) : Colors.white,
                                  fontSize: 19,
                                  fontWeight: FontWeight.w900,
                                  letterSpacing: 0.8,
                                ),
                              ),
                            ),
                          ),
                        ),
                      );
                    },
                  ),
                ),

                const SizedBox(height: 30),

                // Server Selector Card
                GestureDetector(
                  onTap: () => setState(() => _showServers = !_showServers),
                  child: Container(
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: const Color(0xFF0C172C),
                      borderRadius: BorderRadius.circular(16),
                      border: Border.all(color: const Color(0xFF1C345C), width: 1.2),
                    ),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Row(
                          children: [
                            Text(_selectedFlag, style: const TextStyle(fontSize: 28)),
                            const SizedBox(width: 14),
                            Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  '$_selectedCountry ($_selectedCity)',
                                  style: const TextStyle(
                                    color: Colors.white,
                                    fontSize: 15,
                                    fontWeight: FontWeight.bold,
                                  ),
                                ),
                                Text(
                                  'Optimal Server • $_selectedPing ms',
                                  style: const TextStyle(
                                    color: Color(0xFF94A3B8),
                                    fontSize: 12.5,
                                  ),
                                ),
                              ],
                            ),
                          ],
                        ),
                        Text(
                          _showServers ? '▲' : '▼',
                          style: const TextStyle(
                            color: Color(0xFF00D4FF),
                            fontSize: 14,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),

                if (_showServers) ...[
                  const SizedBox(height: 8),
                  Container(
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      color: const Color(0xFF091224),
                      borderRadius: BorderRadius.circular(16),
                      border: Border.all(color: const Color(0xFF182D50)),
                    ),
                    child: Column(
                      children: [
                        _buildServerRow('🇺🇸', 'United States', 'New York', 24),
                        _buildServerRow('🇩🇪', 'Germany', 'Frankfurt', 18),
                        _buildServerRow('🇳🇱', 'Netherlands', 'Amsterdam', 15),
                        _buildServerRow('🇬🇧', 'United Kingdom', 'London', 21),
                        _buildServerRow('🇯🇵', 'Japan', 'Tokyo', 85),
                      ],
                    ),
                  ),
                ],

                const SizedBox(height: 20),

                // Metrics
                Row(
                  children: [
                    Expanded(
                      child: Container(
                        padding: const EdgeInsets.all(14),
                        decoration: BoxDecoration(
                          color: const Color(0xFF0C172C),
                          borderRadius: BorderRadius.circular(14),
                          border: Border.all(color: const Color(0xFF1C345C), width: 1.2),
                        ),
                        child: Column(
                          children: [
                            const Text(
                              'DOWNLOAD',
                              style: TextStyle(
                                color: Color(0xFF94A3B8),
                                fontSize: 11,
                                fontWeight: FontWeight.bold,
                                letterSpacing: 0.5,
                              ),
                            ),
                            const SizedBox(height: 4),
                            Text(
                              _isConnected ? '78.4 Mb/s' : '0.0 Mb/s',
                              style: const TextStyle(
                                color: Color(0xFF00D4FF),
                                fontSize: 16,
                                fontWeight: FontWeight.w900,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Container(
                        padding: const EdgeInsets.all(14),
                        decoration: BoxDecoration(
                          color: const Color(0xFF0C172C),
                          borderRadius: BorderRadius.circular(14),
                          border: Border.all(color: const Color(0xFF1C345C), width: 1.2),
                        ),
                        child: Column(
                          children: [
                            const Text(
                              'UPLOAD',
                              style: TextStyle(
                                color: Color(0xFF94A3B8),
                                fontSize: 11,
                                fontWeight: FontWeight.bold,
                                letterSpacing: 0.5,
                              ),
                            ),
                            const SizedBox(height: 4),
                            Text(
                              _isConnected ? '34.2 Mb/s' : '0.0 Mb/s',
                              style: const TextStyle(
                                color: Color(0xFF26E875),
                                fontSize: 16,
                                fontWeight: FontWeight.w900,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 24),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildServerRow(String flag, String country, String city, int ping) {
    final isCurrent = _selectedCountry == country;
    return GestureDetector(
      onTap: () {
        setState(() {
          _selectedFlag = flag;
          _selectedCountry = country;
          _selectedCity = city;
          _selectedPing = ping;
          _showServers = false;
        });
      },
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
        decoration: BoxDecoration(
          color: isCurrent ? const Color(0xFF142442) : Colors.transparent,
          borderRadius: BorderRadius.circular(10),
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Row(
              children: [
                Text(flag, style: const TextStyle(fontSize: 22)),
                const SizedBox(width: 10),
                Text(
                  '$country - $city',
                  style: TextStyle(
                    color: Colors.white,
                    fontSize: 14,
                    fontWeight: isCurrent ? FontWeight.bold : FontWeight.normal,
                  ),
                ),
              ],
            ),
            Text(
              '$ping ms',
              style: const TextStyle(
                color: Color(0xFF26E875),
                fontSize: 12,
                fontWeight: FontWeight.w600,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
