import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'theme/app_theme.dart';
import 'screens/welcome_screen.dart';
import 'screens/sign_up_screen.dart';
import 'screens/sign_in_screen.dart';
import 'screens/home_screen.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  SystemChrome.setSystemUIOverlayStyle(const SystemUiOverlayStyle(
    statusBarColor: Colors.transparent,
    statusBarIconBrightness: Brightness.light,
  ));
  SystemChrome.setPreferredOrientations(
    [DeviceOrientation.portraitUp, DeviceOrientation.portraitDown],
  ).then((_) => runApp(const App()));
}

class App extends StatelessWidget {
  const App({super.key});

  @override
  Widget build(BuildContext context) => MaterialApp(
    title: 'ByMeVPN',
    debugShowCheckedModeBanner: false,
    theme: AppTheme.dark,
    themeMode: ThemeMode.dark,
    initialRoute: WelcomeScreen.routeName,
    routes: {
      WelcomeScreen.routeName: (_) => const WelcomeScreen(),
      SignUpScreen.routeName: (_) => const SignUpScreen(),
      SignInScreen.routeName: (_) => const SignInScreen(),
      HomeScreen.routeName: (_) => const HomeScreen(),
    },
  );
}
