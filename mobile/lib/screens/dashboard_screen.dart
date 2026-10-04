import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../app_state.dart';
import '../models.dart';

/// Impact dashboard: counts of confirmed handovers, loans and recipients.
class DashboardScreen extends StatefulWidget {
  const DashboardScreen({super.key});

  @override
  State<DashboardScreen> createState() => _DashboardScreenState();
}

class _DashboardScreenState extends State<DashboardScreen> {
  late Future<Dashboard> _future = context.read<AppState>().api.dashboard();

  void _reload() => setState(() => _future = context.read<AppState>().api.dashboard());

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<Dashboard>(
      future: _future,
      builder: (context, snapshot) {
        if (snapshot.connectionState != ConnectionState.done) {
          return const Center(child: CircularProgressIndicator());
        }
        if (snapshot.hasError) {
          return Center(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text('${snapshot.error}'),
                OutlinedButton(onPressed: _reload, child: const Text('Refresh')),
              ],
            ),
          );
        }
        final d = snapshot.data!;
        return RefreshIndicator(
          onRefresh: () async => _reload(),
          child: GridView.count(
            crossAxisCount: 2,
            padding: const EdgeInsets.all(12),
            childAspectRatio: 1.5,
            children: [
              _tile(context, 'Completed donations', d.completedDonations, Icons.volunteer_activism),
              _tile(context, 'Completed loans', d.completedLoans, Icons.assignment_turned_in),
              _tile(context, 'Unique recipients', d.uniqueRecipients, Icons.people),
              _tile(context, 'Active loans', d.activeLoans, Icons.swap_horiz),
              _tile(context, 'Overdue loans', d.overdueLoans, Icons.warning_amber),
              _tile(context, 'Available items', d.availableItems, Icons.inventory_2),
              _tile(context, 'Pending requests', d.pendingRequests, Icons.hourglass_bottom),
            ],
          ),
        );
      },
    );
  }

  Widget _tile(BuildContext context, String name, int value, IconData icon) {
    final theme = Theme.of(context);
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Icon(icon, color: theme.colorScheme.primary),
            Text('$value', style: theme.textTheme.headlineMedium),
            Text(name, style: theme.textTheme.bodySmall),
          ],
        ),
      ),
    );
  }
}
