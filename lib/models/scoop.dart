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
}
