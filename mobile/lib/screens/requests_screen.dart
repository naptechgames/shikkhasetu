import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../app_state.dart';
import '../models.dart';
import 'common.dart';

/// Student: own requests with pickup code and due date.
/// Coordinator: all requests with approve / reject / hand over / return.
class RequestsScreen extends StatefulWidget {
  const RequestsScreen({super.key});

  @override
  State<RequestsScreen> createState() => _RequestsScreenState();
}

class _RequestsScreenState extends State<RequestsScreen> {
  static const _statuses = ['PENDING', 'APPROVED', 'HANDED_OVER', 'RETURNED', 'REJECTED', 'CANCELLED'];

  String? _status = 'PENDING'; // coordinator filter
  int _reloadCounter = 0;

  void _reload() => setState(() => _reloadCounter++);

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppState>();
    final user = state.user!;

    return Column(
      children: [
        if (user.isCoordinator)
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
            child: Row(
              children: [
                for (final status in <String?>[null, ..._statuses])
                  Padding(
                    padding: const EdgeInsets.only(right: 6),
                    child: ChoiceChip(
                      label: Text(status == null ? 'All' : label(status)),
                      selected: _status == status,
                      onSelected: (_) {
                        _status = status;
                        _reload();
                      },
                    ),
                  ),
              ],
            ),
          ),
        Expanded(
          child: AsyncList<ItemRequest>(
            key: ValueKey(_reloadCounter),
            load: () => user.isCoordinator ? state.api.allRequests(status: _status) : state.api.myRequests(),
            emptyText: user.isCoordinator ? 'No requests with this status.' : 'You have not requested anything yet.',
            itemBuilder: (context, request, reload) => _RequestCard(request: request, user: user, onChanged: _reload),
          ),
        ),
      ],
    );
  }
}

class _RequestCard extends StatelessWidget {
  final ItemRequest request;
  final User user;
  final VoidCallback onChanged;

  const _RequestCard({required this.request, required this.user, required this.onChanged});

  @override
  Widget build(BuildContext context) {
    final api = context.read<AppState>().api;
    final item = request.item;
    final theme = Theme.of(context);

    Future<void> act(Future<void> Function() action, String success) async {
      if (await runAction(context, action, success: success)) onChanged();
    }

    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 5),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Icon(categoryIcon(item.category)),
                const SizedBox(width: 10),
                Expanded(child: Text(item.title, style: theme.textTheme.titleMedium)),
                StatusChip(request.overdue ? 'Overdue' : label(request.status),
                    color: request.overdue ? Colors.red.shade700 : statusColor(request.status)),
              ],
            ),
            const SizedBox(height: 6),
            Text('${item.itemCode} · ${item.isLoan ? 'Loan for ${request.loanDays} days' : 'Donation'}'
                ' · Requested ${formatDate(request.createdAt)}'),
            if (user.isCoordinator) Text('Student: ${request.requesterName}'),
            if (request.note != null && request.note!.isNotEmpty) Text('Note: ${request.note}'),
            if (request.dueDate != null && request.status == 'HANDED_OVER')
              Text('Return by ${formatDate(request.dueDate!)}',
                  style: TextStyle(fontWeight: FontWeight.bold, color: request.overdue ? Colors.red.shade700 : null)),
            if (request.returnCondition != null) Text('Returned in condition: ${label(request.returnCondition!)}'),
            if (request.pickupCode != null)
              Container(
                margin: const EdgeInsets.only(top: 8),
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: theme.colorScheme.primaryContainer,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Row(
                  children: [
                    const Icon(Icons.key),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text('Pickup code: ${request.pickupCode}\nTell this code to the coordinator.',
                          style: const TextStyle(fontWeight: FontWeight.bold)),
                    ),
                  ],
                ),
              ),
            const SizedBox(height: 4),
            Wrap(
              spacing: 8,
              children: [
                if (user.isCoordinator && request.status == 'PENDING') ...[
                  FilledButton.tonal(
                    onPressed: () => act(() => api.approveRequest(request.id), 'Request approved, item reserved'),
                    child: const Text('Approve'),
                  ),
                  OutlinedButton(
                    onPressed: () => act(() => api.rejectRequest(request.id), 'Request rejected'),
                    child: const Text('Reject'),
                  ),
                ],
                if (user.isCoordinator && request.status == 'APPROVED')
                  FilledButton.tonal(onPressed: () => _handOver(context), child: const Text('Hand over')),
                if (user.isCoordinator && request.status == 'HANDED_OVER' && item.isLoan)
                  FilledButton.tonal(onPressed: () => _return(context), child: const Text('Record return')),
                if (request.status == 'PENDING' || request.status == 'APPROVED')
                  TextButton(
                    onPressed: () => act(() => api.cancelRequest(request.id), 'Request cancelled'),
                    child: Text(user.isCoordinator ? 'Cancel request' : 'Cancel my request'),
                  ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  /// The coordinator types the pickup code that the student shows.
  Future<void> _handOver(BuildContext context) async {
    final api = context.read<AppState>().api;
    final code = TextEditingController();
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Hand over item'),
        content: TextField(
          controller: code,
          keyboardType: TextInputType.number,
          maxLength: 6,
          decoration: InputDecoration(labelText: 'Pickup code of ${request.requesterName}'),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(dialogContext, false), child: const Text('Cancel')),
          FilledButton(onPressed: () => Navigator.pop(dialogContext, true), child: const Text('Confirm handover')),
        ],
      ),
    );
    if (confirmed != true || !context.mounted) return;
    if (await runAction(context, () => api.handOver(request.id, code.text.trim()), success: 'Handover recorded')) {
      onChanged();
    }
  }

  /// The coordinator records the condition in which the item came back.
  Future<void> _return(BuildContext context) async {
    final api = context.read<AppState>().api;
    final condition = await showDialog<String>(
      context: context,
      builder: (dialogContext) => SimpleDialog(
        title: const Text('Condition of the returned item'),
        children: [
          for (final option in const ['NEW', 'GOOD', 'FAIR', 'POOR'])
            SimpleDialogOption(onPressed: () => Navigator.pop(dialogContext, option), child: Text(label(option))),
        ],
      ),
    );
    if (condition == null || !context.mounted) return;
    if (await runAction(context, () => api.returnItem(request.id, condition), success: 'Return recorded')) {
      onChanged();
    }
  }
}
