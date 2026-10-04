import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:http/http.dart' as http;

import 'models.dart';

/// An error answer from the backend, or a network problem (statusCode 0).
class ApiException implements Exception {
  final int statusCode;
  final String message;

  ApiException(this.statusCode, this.message);

  @override
  String toString() => message;
}

/// All calls to the ShikkhaSetu REST API. The app has no business rules of
/// its own: the backend decides what is allowed.
class ApiClient {
  final String baseUrl;
  final String? token;
  final http.Client _http;

  /// Long enough for a free cloud server that has to wake up first.
  static const _timeout = Duration(seconds: 60);

  ApiClient({required this.baseUrl, this.token, http.Client? httpClient}) : _http = httpClient ?? http.Client();

  Future<dynamic> _send(String method, String path, {Map<String, dynamic>? body, Map<String, String>? query}) async {
    var uri = Uri.parse('$baseUrl/api$path');
    if (query != null && query.isNotEmpty) {
      uri = uri.replace(queryParameters: query);
    }
    final headers = {
      'Content-Type': 'application/json',
      if (token != null) 'Authorization': 'Bearer $token',
    };
    try {
      final http.Response response;
      if (method == 'GET') {
        response = await _http.get(uri, headers: headers).timeout(_timeout);
      } else {
        response = await _http
            .post(uri, headers: headers, body: body == null ? null : jsonEncode(body))
            .timeout(_timeout);
      }
      final text = utf8.decode(response.bodyBytes);
      final dynamic decoded;
      try {
        decoded = text.isEmpty ? null : jsonDecode(text);
      } on FormatException {
        // Not JSON, so the answer did not come from our backend.
        throw ApiException(
            0,
            response.statusCode == 404
                ? 'No ShikkhaSetu server was found at $baseUrl'
                : 'The server is starting up. Please try again in a minute.');
      }
      if (response.statusCode >= 200 && response.statusCode < 300) {
        return decoded;
      }
      final message = decoded is Map && decoded['error'] != null ? decoded['error'] as String : 'Request failed';
      throw ApiException(response.statusCode, message);
    } on SocketException {
      throw ApiException(0, 'Cannot reach the server at $baseUrl');
    } on TimeoutException {
      throw ApiException(0, 'The server did not answer in time');
    } on http.ClientException {
      throw ApiException(0, 'Cannot reach the server at $baseUrl');
    } on FormatException {
      throw ApiException(0, 'The server is starting up. Please try again in a minute.');
    }
  }

  // ---- authentication ----

  /// Returns the session token and the logged-in user.
  Future<(String, User)> login(String email, String password) async {
    final json = await _send('POST', '/auth/login', body: {'email': email, 'password': password});
    return (json['token'] as String, User.fromJson(json['user'] as Map<String, dynamic>));
  }

  Future<void> register(String name, String email, String password) =>
      _send('POST', '/auth/register', body: {'name': name, 'email': email, 'password': password});

  Future<void> logout() => _send('POST', '/auth/logout');

  // ---- items ----

  Future<List<Item>> searchItems({String? query, String? category, String? mode, String? status}) async {
    final json = await _send('GET', '/items', query: {
      if (query != null && query.trim().isNotEmpty) 'q': query.trim(),
      'category': ?category,
      'mode': ?mode,
      'status': ?status,
    });
    return (json as List).map((e) => Item.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<List<Item>> myItems() async {
    final json = await _send('GET', '/items/mine');
    return (json as List).map((e) => Item.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<Item> createItem({
    required String title,
    String? description,
    required String category,
    required String mode,
    required String condition,
  }) async {
    final json = await _send('POST', '/items', body: {
      'title': title,
      'description': description,
      'category': category,
      'mode': mode,
      'condition': condition,
    });
    return Item.fromJson(json as Map<String, dynamic>);
  }

  Future<void> reviewItem(int itemId, {required bool approve}) =>
      _send('POST', '/items/$itemId/review', body: {'approve': approve});

  Future<void> allocateFirstCome(int itemId) => _send('POST', '/items/$itemId/allocate-fcfs');

  // ---- requests ----

  Future<void> createRequest(int itemId, {int? loanDays, String? note}) =>
      _send('POST', '/requests', body: {'itemId': itemId, 'loanDays': loanDays, 'note': note});

  Future<List<ItemRequest>> myRequests() async {
    final json = await _send('GET', '/requests/mine');
    return (json as List).map((e) => ItemRequest.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<List<ItemRequest>> allRequests({String? status}) async {
    final json = await _send('GET', '/requests', query: {'status': ?status});
    return (json as List).map((e) => ItemRequest.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<void> approveRequest(int id) => _send('POST', '/requests/$id/approve');

  Future<void> rejectRequest(int id) => _send('POST', '/requests/$id/reject');

  Future<void> cancelRequest(int id) => _send('POST', '/requests/$id/cancel');

  Future<void> handOver(int id, String pickupCode) =>
      _send('POST', '/requests/$id/handover', body: {'pickupCode': pickupCode});

  Future<void> returnItem(int id, String condition) =>
      _send('POST', '/requests/$id/return', body: {'condition': condition});

  // ---- notifications and dashboard ----

  Future<List<AppNotification>> notifications() async {
    final json = await _send('GET', '/notifications');
    return (json as List).map((e) => AppNotification.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<void> markNotificationRead(int id) => _send('POST', '/notifications/$id/read');

  Future<Dashboard> dashboard() async {
    final json = await _send('GET', '/dashboard');
    return Dashboard.fromJson(json as Map<String, dynamic>);
  }
}
