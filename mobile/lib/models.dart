// Plain data classes that mirror the JSON sent by the backend.

class User {
  final int id;
  final String name;
  final String email;
  final String role; // STUDENT or COORDINATOR

  const User({required this.id, required this.name, required this.email, required this.role});

  bool get isCoordinator => role == 'COORDINATOR';

  factory User.fromJson(Map<String, dynamic> json) => User(
        id: json['id'] as int,
        name: json['name'] as String,
        email: json['email'] as String,
        role: json['role'] as String,
      );

  Map<String, dynamic> toJson() => {'id': id, 'name': name, 'email': email, 'role': role};
}

class Item {
  final int id;
  final String itemCode;
  final String title;
  final String? description;
  final String category; // BOOK, CALCULATOR, OTHER
  final String mode; // DONATION, LOAN
  final String condition; // NEW, GOOD, FAIR, POOR
  final String status;
  final int ownerId;
  final String ownerName;

  const Item({
    required this.id,
    required this.itemCode,
    required this.title,
    required this.description,
    required this.category,
    required this.mode,
    required this.condition,
    required this.status,
    required this.ownerId,
    required this.ownerName,
  });

  bool get isLoan => mode == 'LOAN';
  bool get isAvailable => status == 'AVAILABLE';

  factory Item.fromJson(Map<String, dynamic> json) => Item(
        id: json['id'] as int,
        itemCode: (json['itemCode'] ?? '') as String,
        title: json['title'] as String,
        description: json['description'] as String?,
        category: json['category'] as String,
        mode: json['mode'] as String,
        condition: json['condition'] as String,
        status: json['status'] as String,
        ownerId: json['ownerId'] as int,
        ownerName: json['ownerName'] as String,
      );
}

class ItemRequest {
  final int id;
  final Item item;
  final int requesterId;
  final String requesterName;
  final String status; // PENDING, APPROVED, REJECTED, CANCELLED, HANDED_OVER, RETURNED
  final int? loanDays;
  final String? note;
  final DateTime createdAt;
  final String? pickupCode; // only sent to the requester
  final DateTime? dueDate;
  final String? returnCondition;
  final bool overdue;

  const ItemRequest({
    required this.id,
    required this.item,
    required this.requesterId,
    required this.requesterName,
    required this.status,
    required this.loanDays,
    required this.note,
    required this.createdAt,
    required this.pickupCode,
    required this.dueDate,
    required this.returnCondition,
    required this.overdue,
  });

  factory ItemRequest.fromJson(Map<String, dynamic> json) => ItemRequest(
        id: json['id'] as int,
        item: Item.fromJson(json['item'] as Map<String, dynamic>),
        requesterId: json['requesterId'] as int,
        requesterName: json['requesterName'] as String,
        status: json['status'] as String,
        loanDays: json['loanDays'] as int?,
        note: json['note'] as String?,
        createdAt: DateTime.parse(json['createdAt'] as String),
        pickupCode: json['pickupCode'] as String?,
        dueDate: json['dueDate'] == null ? null : DateTime.parse(json['dueDate'] as String),
        returnCondition: json['returnCondition'] as String?,
        overdue: (json['overdue'] ?? false) as bool,
      );
}

class AppNotification {
  final int id;
  final String message;
  final bool read;
  final DateTime createdAt;

  const AppNotification({required this.id, required this.message, required this.read, required this.createdAt});

  factory AppNotification.fromJson(Map<String, dynamic> json) => AppNotification(
        id: json['id'] as int,
        message: json['message'] as String,
        read: json['read'] as bool,
        createdAt: DateTime.parse(json['createdAt'] as String),
      );
}

class Dashboard {
  final int availableItems;
  final int pendingRequests;
  final int completedDonations;
  final int activeLoans;
  final int completedLoans;
  final int overdueLoans;
  final int uniqueRecipients;

  const Dashboard({
    required this.availableItems,
    required this.pendingRequests,
    required this.completedDonations,
    required this.activeLoans,
    required this.completedLoans,
    required this.overdueLoans,
    required this.uniqueRecipients,
  });

  factory Dashboard.fromJson(Map<String, dynamic> json) => Dashboard(
        availableItems: json['availableItems'] as int,
        pendingRequests: json['pendingRequests'] as int,
        completedDonations: json['completedDonations'] as int,
        activeLoans: json['activeLoans'] as int,
        completedLoans: json['completedLoans'] as int,
        overdueLoans: json['overdueLoans'] as int,
        uniqueRecipients: json['uniqueRecipients'] as int,
      );
}

/// "ON_LOAN" -> "On loan". Used for every enum value shown on screen.
String label(String enumValue) {
  final words = enumValue.toLowerCase().replaceAll('_', ' ');
  return words[0].toUpperCase() + words.substring(1);
}

/// 2026-10-05
String formatDate(DateTime date) =>
    '${date.year}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';
