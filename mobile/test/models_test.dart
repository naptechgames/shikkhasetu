import 'package:flutter_test/flutter_test.dart';
import 'package:shikkhasetu/models.dart';

void main() {
  const itemJson = {
    'id': 3,
    'itemCode': 'SS-00003',
    'title': 'Casio fx-991ES',
    'description': null,
    'category': 'CALCULATOR',
    'mode': 'LOAN',
    'condition': 'GOOD',
    'status': 'AVAILABLE',
    'ownerId': 1,
    'ownerName': 'Club Coordinator',
  };

  test('Item.fromJson reads all fields', () {
    final item = Item.fromJson(itemJson);
    expect(item.itemCode, 'SS-00003');
    expect(item.isLoan, isTrue);
    expect(item.isAvailable, isTrue);
    expect(item.description, isNull);
  });

  test('ItemRequest.fromJson reads dates, pickup code and overdue flag', () {
    final request = ItemRequest.fromJson({
      'id': 9,
      'item': itemJson,
      'requesterId': 2,
      'requesterName': 'Demo Student',
      'status': 'HANDED_OVER',
      'loanDays': 7,
      'note': null,
      'createdAt': '2026-10-01T09:30:00',
      'pickupCode': null,
      'handedOverAt': '2026-10-02T10:00:00',
      'dueDate': '2026-10-09',
      'returnedAt': null,
      'returnCondition': null,
      'overdue': true,
    });
    expect(request.dueDate, DateTime(2026, 10, 9));
    expect(request.overdue, isTrue);
    expect(request.pickupCode, isNull);
    expect(request.item.title, 'Casio fx-991ES');
  });

  test('User role decides isCoordinator and survives toJson/fromJson', () {
    const user = User(id: 1, name: 'C', email: 'c@test.local', role: 'COORDINATOR');
    final copy = User.fromJson(user.toJson());
    expect(copy.isCoordinator, isTrue);
    expect(const User(id: 2, name: 'S', email: 's@test.local', role: 'STUDENT').isCoordinator, isFalse);
  });

  test('label and formatDate make values readable', () {
    expect(label('ON_LOAN'), 'On loan');
    expect(label('PENDING_REVIEW'), 'Pending review');
    expect(formatDate(DateTime(2026, 3, 5)), '2026-03-05');
  });
}
