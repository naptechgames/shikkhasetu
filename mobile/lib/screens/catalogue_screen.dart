import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../app_state.dart';
import '../models.dart';
import 'common.dart';
import 'offer_item_screen.dart';

/// Search and filter the catalogue. A student can request an available item;
/// a coordinator can review listings and allocate by "first come, first served".
class CatalogueScreen extends StatefulWidget {
  const CatalogueScreen({super.key});

  @override
  State<CatalogueScreen> createState() => _CatalogueScreenState();
}

class _CatalogueScreenState extends State<CatalogueScreen> {
  final _search = TextEditingController();
  String? _category;
  String? _mode;
  String? _status;
  bool _mineOnly = false;
  int _reloadCounter = 0;

  void _reload() => setState(() => _reloadCounter++);

  @override
  void dispose() {
    _search.dispose();
    super.dispose();
  }

  Future<List<Item>> _load() {
    final api = context.read<AppState>().api;
    if (_mineOnly) return api.myItems();
    return api.searchItems(query: _search.text, category: _category, mode: _mode, status: _status);
  }

  @override
  Widget build(BuildContext context) {
    final user = context.watch<AppState>().user!;
    final statuses = user.isCoordinator
        ? const ['PENDING_REVIEW', 'AVAILABLE', 'RESERVED', 'ON_LOAN', 'DONATED', 'REJECTED']
        : const ['AVAILABLE', 'RESERVED', 'ON_LOAN', 'DONATED'];

    return Scaffold(
      floatingActionButton: FloatingActionButton.extended(
        icon: const Icon(Icons.add),
        label: Text(user.isCoordinator ? 'Add item' : 'Offer item'),
        onPressed: () async {
          final created = await Navigator.of(context)
              .push<bool>(MaterialPageRoute(builder: (_) => const OfferItemScreen()));
          if (created == true) _reload();
        },
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(12, 12, 12, 4),
            child: TextField(
              controller: _search,
              textInputAction: TextInputAction.search,
              onSubmitted: (_) => _reload(),
              decoration: InputDecoration(
                hintText: 'Search title, description or item code',
                prefixIcon: const Icon(Icons.search),
                suffixIcon: IconButton(
                  icon: const Icon(Icons.clear),
                  onPressed: () {
                    _search.clear();
                    _reload();
                  },
                ),
                isDense: true,
              ),
            ),
          ),
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.symmetric(horizontal: 12),
            child: Row(
              children: [
                _filter('Category', _category, const ['BOOK', 'CALCULATOR', 'OTHER'], (v) => _category = v),
                _filter('Type', _mode, const ['LOAN', 'DONATION'], (v) => _mode = v),
                _filter('Status', _status, statuses, (v) => _status = v),
                FilterChip(
                  label: const Text('My listings'),
                  selected: _mineOnly,
                  onSelected: (selected) {
                    _mineOnly = selected;
                    _reload();
                  },
                ),
              ],
            ),
          ),
          Expanded(
            child: AsyncList<Item>(
              key: ValueKey(_reloadCounter),
              load: _load,
              emptyText: 'No items match your search.',
              itemBuilder: (context, item, reload) => _ItemCard(item: item, user: user, onChanged: _reload),
            ),
          ),
        ],
      ),
    );
  }

  /// A drop-down filter with an "All" choice.
  Widget _filter(String name, String? value, List<String> options, void Function(String?) assign) {
    return Padding(
      padding: const EdgeInsets.only(right: 8),
      child: DropdownButton<String?>(
        value: value,
        hint: Text(name),
        underline: const SizedBox.shrink(),
        items: [
          DropdownMenuItem<String?>(value: null, child: Text('$name: all')),
          for (final option in options) DropdownMenuItem<String?>(value: option, child: Text(label(option))),
        ],
        onChanged: (selected) {
          assign(selected);
          _reload();
        },
      ),
    );
  }
}

class _ItemCard extends StatelessWidget {
  final Item item;
  final User user;
  final VoidCallback onChanged;

  const _ItemCard({required this.item, required this.user, required this.onChanged});

  @override
  Widget build(BuildContext context) {
    final api = context.read<AppState>().api;
    final isOwner = item.ownerId == user.id;

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
                Expanded(child: Text(item.title, style: Theme.of(context).textTheme.titleMedium)),
                StatusChip(label(item.status), color: statusColor(item.status)),
              ],
            ),
            const SizedBox(height: 6),
            Text('${item.itemCode} · ${item.isLoan ? 'Loan' : 'Donation'} · Condition: ${label(item.condition)}'
                ' · From: ${item.ownerName}'),
            if (item.description != null && item.description!.isNotEmpty)
              Padding(padding: const EdgeInsets.only(top: 4), child: Text(item.description!)),
            const SizedBox(height: 4),
            Wrap(
              spacing: 8,
              children: [
                if (!user.isCoordinator && item.isAvailable && !isOwner)
                  FilledButton.tonal(
                    onPressed: () => _request(context),
                    child: Text(item.isLoan ? 'Request to borrow' : 'Request donation'),
                  ),
                if (user.isCoordinator && item.status == 'PENDING_REVIEW') ...[
                  FilledButton.tonal(
                    onPressed: () async {
                      if (await runAction(context, () => api.reviewItem(item.id, approve: true),
                          success: 'Listing approved')) {
                        onChanged();
                      }
                    },
                    child: const Text('Approve listing'),
                  ),
                  OutlinedButton(
                    onPressed: () async {
                      if (await runAction(context, () => api.reviewItem(item.id, approve: false),
                          success: 'Listing rejected')) {
                        onChanged();
                      }
                    },
                    child: const Text('Reject'),
                  ),
                ],
                if (user.isCoordinator && item.isAvailable)
                  OutlinedButton(
                    onPressed: () async {
                      if (await runAction(context, () => api.allocateFirstCome(item.id),
                          success: 'Oldest pending request approved')) {
                        onChanged();
                      }
                    },
                    child: const Text('Approve first-come request'),
                  ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  /// Asks for the loan period (loans only) and an optional note, then sends the request.
  Future<void> _request(BuildContext context) async {
    final api = context.read<AppState>().api;
    final days = TextEditingController(text: '7');
    final note = TextEditingController();
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: Text(item.isLoan ? 'Borrow "${item.title}"' : 'Request "${item.title}"'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (item.isLoan)
              TextField(
                controller: days,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(labelText: 'Loan period in days (1–30)'),
              ),
            const SizedBox(height: 12),
            TextField(controller: note, decoration: const InputDecoration(labelText: 'Note (optional)')),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(dialogContext, false), child: const Text('Cancel')),
          FilledButton(onPressed: () => Navigator.pop(dialogContext, true), child: const Text('Send request')),
        ],
      ),
    );
    if (confirmed != true || !context.mounted) return;
    final sent = await runAction(
      context,
      () => api.createRequest(item.id,
          loanDays: item.isLoan ? int.tryParse(days.text.trim()) : null,
          note: note.text.trim().isEmpty ? null : note.text.trim()),
      success: 'Request sent. The coordinator will review it.',
    );
    if (sent) onChanged();
  }
}
