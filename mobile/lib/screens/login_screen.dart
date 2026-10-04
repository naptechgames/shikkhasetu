import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api_client.dart';
import '../app_state.dart';

/// Login and student registration on one screen. The server address can be
/// changed here because the backend runs on a laptop in the local network.
class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _name = TextEditingController();
  final _email = TextEditingController();
  final _password = TextEditingController();
  late final _server = TextEditingController(text: context.read<AppState>().baseUrl);

  bool _registerMode = false;
  bool _busy = false;
  String? _error;

  @override
  void dispose() {
    _name.dispose();
    _email.dispose();
    _password.dispose();
    _server.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    final state = context.read<AppState>();
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      await state.setBaseUrl(_server.text);
      if (_registerMode) {
        await state.api.register(_name.text.trim(), _email.text.trim(), _password.text);
      }
      await state.login(_email.text.trim(), _password.text);
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  String? _required(String? value) => (value == null || value.trim().isEmpty) ? 'Required' : null;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: Form(
              key: _formKey,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Icon(Icons.school, size: 64, color: theme.colorScheme.primary),
                  const SizedBox(height: 8),
                  Text('ShikkhaSetu', textAlign: TextAlign.center, style: theme.textTheme.headlineMedium),
                  Text('Share books and calculators on campus',
                      textAlign: TextAlign.center, style: theme.textTheme.bodyMedium),
                  const SizedBox(height: 24),
                  if (_registerMode) ...[
                    TextFormField(
                      key: const Key('name'),
                      controller: _name,
                      decoration: const InputDecoration(labelText: 'Full name'),
                      validator: _required,
                    ),
                    const SizedBox(height: 12),
                  ],
                  TextFormField(
                    key: const Key('email'),
                    controller: _email,
                    keyboardType: TextInputType.emailAddress,
                    decoration: const InputDecoration(labelText: 'E-mail'),
                    validator: (value) => (value == null || !value.contains('@')) ? 'Enter a valid e-mail' : null,
                  ),
                  const SizedBox(height: 12),
                  TextFormField(
                    key: const Key('password'),
                    controller: _password,
                    obscureText: true,
                    decoration: const InputDecoration(labelText: 'Password'),
                    validator: (value) => (value == null || value.length < 6) ? 'At least 6 characters' : null,
                  ),
                  const SizedBox(height: 12),
                  TextFormField(
                    key: const Key('server'),
                    controller: _server,
                    keyboardType: TextInputType.url,
                    decoration: const InputDecoration(
                      labelText: 'Server address',
                      helperText: 'Change only if your teacher or coordinator tells you to',
                    ),
                    validator: (value) =>
                        (value == null || !value.trim().startsWith('http')) ? 'Must start with http' : null,
                  ),
                  const SizedBox(height: 16),
                  if (_error != null)
                    Padding(
                      padding: const EdgeInsets.only(bottom: 12),
                      child: Text(_error!, style: TextStyle(color: theme.colorScheme.error)),
                    ),
                  FilledButton(
                    key: const Key('submit'),
                    onPressed: _busy ? null : _submit,
                    child: Text(_busy ? 'Please wait…' : (_registerMode ? 'Create student account' : 'Log in')),
                  ),
                  TextButton(
                    onPressed: _busy ? null : () => setState(() => _registerMode = !_registerMode),
                    child: Text(_registerMode ? 'I already have an account' : 'New student? Create an account'),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
