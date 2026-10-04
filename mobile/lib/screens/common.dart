import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api_client.dart';
import '../app_state.dart';

/// Runs a backend call and shows the result as a snack bar. Returns true on
/// success. A 401 answer ends the session and brings back the login screen.
Future<bool> runAction(BuildContext context, Future<void> Function() action, {String? success}) async {
  final messenger = ScaffoldMessenger.of(context);
  final state = context.read<AppState>();
  try {
    await action();
    if (success != null) {
      messenger.showSnackBar(SnackBar(content: Text(success)));
    }
    return true;
  } on ApiException catch (e) {
    messenger.showSnackBar(SnackBar(content: Text(e.message)));
    if (e.statusCode == 401) {
      await state.clearSession();
    }
    return false;
  }
}

/// Loads a list from the backend and shows loading / error / empty / data.
/// Pull down to refresh. Give it a new [key] to force a reload.
class AsyncList<T> extends StatefulWidget {
  final Future<List<T>> Function() load;
  final Widget Function(BuildContext context, T value, VoidCallback reload) itemBuilder;
  final String emptyText;

  const AsyncList({super.key, required this.load, required this.itemBuilder, required this.emptyText});

  @override
  State<AsyncList<T>> createState() => _AsyncListState<T>();
}

class _AsyncListState<T> extends State<AsyncList<T>> {
  late Future<List<T>> _future = widget.load();

  void _reload() => setState(() => _future = widget.load());

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<List<T>>(
      future: _future,
      builder: (context, snapshot) {
        if (snapshot.connectionState != ConnectionState.done) {
          return const Center(child: CircularProgressIndicator());
        }
        if (snapshot.hasError) {
          return _message(context, '${snapshot.error}', retry: true);
        }
        final values = snapshot.data!;
        if (values.isEmpty) {
          return _message(context, widget.emptyText, retry: true);
        }
        return RefreshIndicator(
          onRefresh: () async => _reload(),
          child: ListView.builder(
            padding: const EdgeInsets.only(bottom: 88),
            itemCount: values.length,
            itemBuilder: (context, index) => widget.itemBuilder(context, values[index], _reload),
          ),
        );
      },
    );
  }

  Widget _message(BuildContext context, String text, {required bool retry}) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(text, textAlign: TextAlign.center),
            const SizedBox(height: 12),
            if (retry) OutlinedButton.icon(onPressed: _reload, icon: const Icon(Icons.refresh), label: const Text('Refresh')),
          ],
        ),
      ),
    );
  }
}

/// Small coloured label for a status such as "Available" or "On loan".
class StatusChip extends StatelessWidget {
  final String text;
  final Color color;

  const StatusChip(this.text, {super.key, required this.color});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(color: color.withValues(alpha: 0.15), borderRadius: BorderRadius.circular(12)),
      child: Text(text, style: TextStyle(color: color, fontSize: 12, fontWeight: FontWeight.w600)),
    );
  }
}

Color statusColor(String status) {
  switch (status) {
    case 'AVAILABLE':
    case 'APPROVED':
    case 'RETURNED':
      return Colors.green.shade700;
    case 'PENDING':
    case 'PENDING_REVIEW':
    case 'RESERVED':
      return Colors.orange.shade800;
    case 'ON_LOAN':
    case 'HANDED_OVER':
    case 'DONATED':
      return Colors.blue.shade700;
    default:
      return Colors.red.shade700; // REJECTED, CANCELLED
  }
}

IconData categoryIcon(String category) {
  switch (category) {
    case 'BOOK':
      return Icons.menu_book;
    case 'CALCULATOR':
      return Icons.calculate;
    default:
      return Icons.category;
  }
}
