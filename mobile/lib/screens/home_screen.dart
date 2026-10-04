import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../app_state.dart';
import 'catalogue_screen.dart';
import 'dashboard_screen.dart';
import 'notifications_screen.dart';
import 'requests_screen.dart';

/// The main screen after login: four tabs. The same tabs are used for both
/// roles; each tab shows the actions that the role is allowed to use.
class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int _tab = 0;

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppState>();
    final user = state.user!;
    final titles = ['Catalogue', user.isCoordinator ? 'All requests' : 'My requests', 'Notifications', 'Impact'];

    return Scaffold(
      appBar: AppBar(
        title: Text(titles[_tab]),
        actions: [
          Center(
            child: Padding(
              padding: const EdgeInsets.only(right: 4),
              child: Text('${user.name}\n${user.isCoordinator ? 'Coordinator' : 'Student'}',
                  textAlign: TextAlign.right, style: Theme.of(context).textTheme.labelSmall),
            ),
          ),
          IconButton(tooltip: 'Log out', icon: const Icon(Icons.logout), onPressed: state.logout),
        ],
      ),
      // Each tab is created again when it is opened, so it always loads fresh data.
      body: switch (_tab) {
        0 => const CatalogueScreen(),
        1 => const RequestsScreen(),
        2 => const NotificationsScreen(),
        _ => const DashboardScreen(),
      },
      bottomNavigationBar: NavigationBar(
        selectedIndex: _tab,
        onDestinationSelected: (index) => setState(() => _tab = index),
        destinations: const [
          NavigationDestination(icon: Icon(Icons.menu_book), label: 'Catalogue'),
          NavigationDestination(icon: Icon(Icons.assignment), label: 'Requests'),
          NavigationDestination(icon: Icon(Icons.notifications), label: 'Alerts'),
          NavigationDestination(icon: Icon(Icons.insights), label: 'Impact'),
        ],
      ),
    );
  }
}
