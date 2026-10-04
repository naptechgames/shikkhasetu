import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import 'app_state.dart';
import 'screens/home_screen.dart';
import 'screens/login_screen.dart';

void main() {
  runApp(
    ChangeNotifierProvider(
      create: (_) => AppState()..load(),
      child: const ShikkhaSetuApp(),
    ),
  );
}

class ShikkhaSetuApp extends StatelessWidget {
  const ShikkhaSetuApp({super.key});

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppState>();
    return MaterialApp(
      title: 'ShikkhaSetu',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF173B59)),
        inputDecorationTheme: const InputDecorationTheme(border: OutlineInputBorder()),
      ),
      home: !state.loaded
          ? const Scaffold(body: Center(child: CircularProgressIndicator()))
          : state.isLoggedIn
              ? const HomeScreen()
              : const LoginScreen(),
    );
  }
}
