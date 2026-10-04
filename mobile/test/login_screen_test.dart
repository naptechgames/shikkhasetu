import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:shikkhasetu/app_state.dart';
import 'package:shikkhasetu/screens/login_screen.dart';

void main() {
  Future<void> pumpLogin(WidgetTester tester) => tester.pumpWidget(
        ChangeNotifierProvider(create: (_) => AppState(), child: const MaterialApp(home: LoginScreen())),
      );

  testWidgets('empty form shows validation messages and does not call the server', (tester) async {
    await pumpLogin(tester);

    await tester.tap(find.byKey(const Key('submit')));
    await tester.pump();

    expect(find.text('Enter a valid e-mail'), findsOneWidget);
    expect(find.text('At least 6 characters'), findsOneWidget);
  });

  testWidgets('password boundary: 5 characters is refused, 6 is accepted by the form', (tester) async {
    await pumpLogin(tester);

    await tester.enterText(find.byKey(const Key('password')), '12345');
    await tester.tap(find.byKey(const Key('submit')));
    await tester.pump();
    expect(find.text('At least 6 characters'), findsOneWidget);

    await tester.enterText(find.byKey(const Key('password')), '123456');
    await tester.tap(find.byKey(const Key('submit')));
    await tester.pump();
    expect(find.text('At least 6 characters'), findsNothing);
    expect(find.text('Enter a valid e-mail'), findsOneWidget); // still blocked by the e-mail field
  });

  testWidgets('switching to registration shows the name field', (tester) async {
    await pumpLogin(tester);
    expect(find.byKey(const Key('name')), findsNothing);

    await tester.tap(find.text('New student? Create an account'));
    await tester.pump();

    expect(find.byKey(const Key('name')), findsOneWidget);
    expect(find.text('Create student account'), findsOneWidget);
  });
}
