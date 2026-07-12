/// A single message in a thread (root = num 0, comments otherwise).
class Message {
  final int num;
  final String author;
  final int? rating; // member rank, 1..5 stars (registered users), or null
  final String? profileUrl; // rotter member page (ratings/details), or null
  // Member stats shown beside the author (parsed from the thread HTML).
  final String? joinDate; // e.g. "10.11.23"
  final int? messages; // total posts on rotter (הודעות)
  final int? raters; // how many members rated them (מדרגים)
  final int? points; // reputation points (נקודות)
  final String? title;
  final String? bodyHtml; // cleaned rich HTML; null for headline-only posts
  final String? date; // gregorian DD.MM.YY as shown
  final String? time; // HH:MM
  final DateTime? timestamp; // parsed from date+time (comments only)
  final int? parent; // null on root; 0 = replies to root; else another num

  const Message({
    required this.num,
    required this.author,
    this.rating,
    this.profileUrl,
    this.joinDate,
    this.messages,
    this.raters,
    this.points,
    this.title,
    this.bodyHtml,
    this.date,
    this.time,
    this.timestamp,
    this.parent,
  });

  bool get isRoot => num == 0;
}

/// A parsed thread: its messages plus a children index for tree rendering.
class Thread {
  final String id;
  final List<Message> messages;

  Thread({required this.id, required this.messages});

  Message? get root =>
      messages.where((m) => m.isRoot).cast<Message?>().firstWhere((_) => true, orElse: () => null);

  List<Message> get comments => messages.where((m) => !m.isRoot).toList();

  /// Children of a given message num, ordered by num.
  List<Message> childrenOf(int parentNum) {
    final kids = messages.where((m) => !m.isRoot && (m.parent ?? 0) == parentNum).toList();
    kids.sort((a, b) => a.num.compareTo(b.num));
    return kids;
  }
}
