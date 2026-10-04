import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../app_state.dart';
import '../models.dart';
import 'common.dart';

/// Form to offer a book, calculator or other resource for donation or loan.
class OfferItemScreen extends StatefulWidget {
  const OfferItemScreen({super.key});

  @override
  State<OfferItemScreen> createState() => _OfferItemScreenState();
}

class _OfferItemScreenState extends State<OfferItemScreen> {
  final _formKey = GlobalKey<FormState>();
  final _title = TextEditingController();
  final _description = TextEditingController();
  String _category = 'BOOK';
  String _mode = 'DONATION';
  String _condition = 'GOOD';
  bool _busy = false;

  @override
  void dispose() {
    _title.dispose();
    _description.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    final state = context.read<AppState>();
    final navigator = Navigator.of(context);
    setState(() => _busy = true);
    final saved = await runAction(
      context,
      () => state.api.createItem(
        title: _title.text.trim(),
        description: _description.text.trim().isEmpty ? null : _description.text.trim(),
        category: _category,
        mode: _mode,
        condition: _condition,
      ),
      success: state.user!.isCoordinator ? 'Item added to the catalogue' : 'Thank you! The coordinator will review it.',
    );
    if (!mounted) return;
    setState(() => _busy = false);
    if (saved) navigator.pop(true);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Offer an item')),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            TextFormField(
              controller: _title,
              maxLength: 120,
              decoration: const InputDecoration(labelText: 'Title (e.g. book name, calculator model)'),
              validator: (value) => (value == null || value.trim().isEmpty) ? 'Required' : null,
            ),
            const SizedBox(height: 8),
            TextFormField(
              controller: _description,
              maxLines: 3,
              maxLength: 1000,
              decoration: const InputDecoration(labelText: 'Description (optional)'),
            ),
            const SizedBox(height: 8),
            _dropdown('Category', _category, const ['BOOK', 'CALCULATOR', 'OTHER'], (v) => _category = v),
            const SizedBox(height: 16),
            _dropdown('Type', _mode, const ['DONATION', 'LOAN'], (v) => _mode = v),
            const SizedBox(height: 16),
            _dropdown('Condition', _condition, const ['NEW', 'GOOD', 'FAIR', 'POOR'], (v) => _condition = v),
            const SizedBox(height: 24),
            FilledButton(onPressed: _busy ? null : _submit, child: Text(_busy ? 'Saving…' : 'Submit')),
          ],
        ),
      ),
    );
  }

  Widget _dropdown(String name, String value, List<String> options, void Function(String) assign) {
    return DropdownButtonFormField<String>(
      initialValue: value,
      decoration: InputDecoration(labelText: name),
      items: [for (final option in options) DropdownMenuItem(value: option, child: Text(label(option)))],
      onChanged: (selected) => setState(() => assign(selected!)),
    );
  }
}
