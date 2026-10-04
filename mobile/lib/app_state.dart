import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'api_client.dart';
import 'models.dart';

/// The login session: server address, token and current user. It is saved on
/// the phone so the user stays logged in after closing the app.
class AppState extends ChangeNotifier {
  /// The cloud backend. It can still be changed on the login screen, e.g. to
  /// http://192.168.x.x:8080 for a backend on a laptop in the same Wi-Fi.
  static const defaultBaseUrl = 'https://shikkhasetu.onrender.com';

  static const _oldDefaults = {'http://192.168.0.100:8080', 'https://naptechgames-shikkhasetu.hf.space'};

  String baseUrl = defaultBaseUrl;
  String? token;
  User? user;
  bool loaded = false;

  bool get isLoggedIn => token != null && user != null;

  ApiClient get api => ApiClient(baseUrl: baseUrl, token: token);

  Future<void> load() async {
    final prefs = await SharedPreferences.getInstance();
    final savedUrl = prefs.getString('baseUrl');
    // Addresses that earlier versions of the app saved as their default are
    // ignored, so an updated app moves to the current server by itself.
    baseUrl = (savedUrl == null || _oldDefaults.contains(savedUrl)) ? defaultBaseUrl : savedUrl;
    token = prefs.getString('token');
    final savedUser = prefs.getString('user');
    user = savedUser == null ? null : User.fromJson(jsonDecode(savedUser) as Map<String, dynamic>);
    loaded = true;
    notifyListeners();
  }

  Future<void> setBaseUrl(String url) async {
    baseUrl = url.trim().replaceAll(RegExp(r'/+$'), '');
    final prefs = await SharedPreferences.getInstance();
    if (baseUrl == defaultBaseUrl) {
      await prefs.remove('baseUrl'); // only an address the user changed is remembered
    } else {
      await prefs.setString('baseUrl', baseUrl);
    }
    notifyListeners();
  }

  Future<void> login(String email, String password) async {
    final (newToken, newUser) = await api.login(email, password);
    token = newToken;
    user = newUser;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('token', newToken);
    await prefs.setString('user', jsonEncode(newUser.toJson()));
    notifyListeners();
  }

  Future<void> logout() async {
    try {
      await api.logout();
    } on ApiException {
      // The server may be unreachable; the local session is removed anyway.
    }
    await clearSession();
  }

  /// Also called when the server answers 401 (session no longer valid).
  Future<void> clearSession() async {
    token = null;
    user = null;
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove('token');
    await prefs.remove('user');
    notifyListeners();
  }
}
