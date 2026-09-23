/// A scoop thread as it appears in the RSS list.
class Scoop {
  final String id;
  final String title;
  final String url;
  final DateTime? published;

  const Scoop({
    required this.id,
    required this.title,
    required this.url,
    this.published,
  });

  // Persisted so the list paints from disk at launch, and so saved/followed
  // threads outlive the rolling RSS feed.
  Map<String, dynamic> toJson() => {
        'id': id,
        'title': title,
        'url': url,
        if (published != null) 'published': published!.millisecondsSinceEpoch,
      };

  factory Scoop.fromJson(Map<String, dynamic> j) => Scoop(
        id: j['id'] as String,
        title: j['title'] as String,
        url: j['url'] as String,
        published: j['published'] == null
            ? null
            : DateTime.fromMillisecondsSinceEpoch((j['published'] as num).toInt()),
      );

  @override
  bool operator ==(Object other) =>
      other is Scoop &&
      other.id == id &&
      other.title == title &&
      other.url == url &&
      other.published == published;

  @override
  int get hashCode => Object.hash(id, title, url, published);
}
