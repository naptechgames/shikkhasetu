import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:shikkhasetu/api_client.dart';

// The real backend is replaced by a MockClient, so these tests check only
// what the app sends and how it reads the answers.
void main() {
  const base = 'http://server.test:8080';

  test('login sends the credentials and returns token and user', () async {
    late http.Request seen;
    final api = ApiClient(
      baseUrl: base,
      httpClient: MockClient((request) async {
        seen = request;
        return http.Response(
            jsonEncode({
              'token': 'abc',
              'user': {'id': 2, 'name': 'Demo', 'email': 'd@test.local', 'role': 'STUDENT'},
            }),
            200);
      }),
    );

    final (token, user) = await api.login('d@test.local', 'secret123');

    expect(seen.url.toString(), '$base/api/auth/login');
    expect(jsonDecode(seen.body), {'email': 'd@test.local', 'password': 'secret123'});
    expect(seen.headers.containsKey('Authorization'), isFalse);
    expect(token, 'abc');
    expect(user.role, 'STUDENT');
  });

  test('search sends the token and only the filters that are set', () async {
    late http.Request seen;
    final api = ApiClient(
      baseUrl: base,
      token: 'tok-1',
      httpClient: MockClient((request) async {
        seen = request;
        return http.Response('[]', 200);
      }),
    );

    final items = await api.searchItems(query: ' casio ', category: 'CALCULATOR');

    expect(items, isEmpty);
    expect(seen.headers['Authorization'], 'Bearer tok-1');
    expect(seen.url.path, '/api/items');
    expect(seen.url.queryParameters, {'q': 'casio', 'category': 'CALCULATOR'});
  });

  test('a loan request sends item id and loan days', () async {
    late http.Request seen;
    final api = ApiClient(
      baseUrl: base,
      token: 't',
      httpClient: MockClient((request) async {
        seen = request;
        return http.Response('{}', 201);
      }),
    );

    await api.createRequest(5, loanDays: 14);

    expect(seen.method, 'POST');
    expect(seen.url.path, '/api/requests');
    expect(jsonDecode(seen.body), {'itemId': 5, 'loanDays': 14, 'note': null});
  });

  test('an error answer becomes an ApiException with the server message', () async {
    final api = ApiClient(
      baseUrl: base,
      token: 't',
      httpClient: MockClient(
          (request) async => http.Response(jsonEncode({'error': 'This item is already allocated'}), 409)),
    );

    expect(
      () => api.approveRequest(1),
      throwsA(isA<ApiException>()
          .having((e) => e.statusCode, 'statusCode', 409)
          .having((e) => e.message, 'message', 'This item is already allocated')),
    );
  });

  test('a network failure becomes an ApiException with status 0', () async {
    final api = ApiClient(
      baseUrl: base,
      httpClient: MockClient((request) async => throw http.ClientException('connection refused')),
    );

    expect(() => api.dashboard(), throwsA(isA<ApiException>().having((e) => e.statusCode, 'statusCode', 0)));
  });
}
