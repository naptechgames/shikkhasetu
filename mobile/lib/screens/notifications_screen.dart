import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../app_state.dart';
import '../models.dart';
import 'common.dart';

/// In-app notifications created by the backend observers. Tap to mark as read.
class NotificationsScreen extends StatelessWidget {
  const NotificationsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final api = context.read<AppState>().api;
    return AsyncList<AppNotification>(
      load: api.notifications,
      emptyText: 'No notifications yet.',
      itemBuilder: (context, notification, reload) => ListTile(
        leading: Icon(notification.read ? Icons.notifications_none : Icons.notifications_active,
            color: notification.read ? null : Theme.of(context).colorScheme.primary),
        title: Text(notification.message,
            style: TextStyle(fontWeight: notification.read ? FontWeight.normal : FontWeight.bold)),
        subtitle: Text(formatDate(notification.createdAt)),
        onTap: notification.read
            ? null
            : () async {
                if (await runAction(context, () => api.markNotificationRead(notification.id))) reload();
              },
      ),
    );
  }
}
