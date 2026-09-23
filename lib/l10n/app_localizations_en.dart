// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for English (`en`).
class L10nEn extends L10n {
  L10nEn([String locale = 'en']) : super(locale);

  @override
  String get appTitle => 'Rotter Scoops';

  @override
  String get tabScoops => 'Scoops';

  @override
  String get tabSearch => 'Search';

  @override
  String get tabNewMessage => 'New message';

  @override
  String get tabSettings => 'Settings';

  @override
  String get settingsTitle => 'Settings';

  @override
  String get appearance => 'APPEARANCE';

  @override
  String get theme => 'Theme';

  @override
  String get themeSystem => 'Device';

  @override
  String get themeLight => 'Light';

  @override
  String get themeDark => 'Dark';

  @override
  String get accentColor => 'Accent color';

  @override
  String get textSize => 'Text size';

  @override
  String get threadSpacing => 'Thread spacing';

  @override
  String get predictiveBack => 'Predictive back';

  @override
  String get predictiveBackHint =>
      'Peek at the previous screen while swiping back';

  @override
  String get language => 'Language';

  @override
  String get languageSystem => 'Device';

  @override
  String get hebrew => 'עברית';

  @override
  String get english => 'English';

  @override
  String get account => 'ACCOUNT';

  @override
  String get signIn => 'Sign in';

  @override
  String get usernameLabel => 'Username';

  @override
  String get passwordLabel => 'Password';

  @override
  String get loginFailed => 'Wrong username or password';

  @override
  String get loginError =>
      'Sign-in failed — check your connection and try again';

  @override
  String get signOut => 'Sign out';

  @override
  String get signedIn => 'Signed in';

  @override
  String get loginSubtitle => 'Sign in to rotter.net to post';

  @override
  String get newMessagePrompt => 'Sign in to post a new message';

  @override
  String get newMessagePromptBody =>
      'Posting on Rotter requires a signed-in account. Sign in once and you can open a new thread right here.';

  @override
  String get compose => 'New thread';

  @override
  String get loadingError => 'Couldn\'t load';

  @override
  String get threadNotReady =>
      'This scoop isn\'t available yet — try again in a moment';

  @override
  String get scoopRemoved =>
      'This scoop is no longer available — it was most likely removed';

  @override
  String get removedBadge => 'Removed';

  @override
  String get retry => 'Retry';

  @override
  String get refresh => 'Refresh';

  @override
  String get openInBrowser => 'Open in browser';

  @override
  String replies(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '$count replies',
      one: '1 reply',
      zero: 'No replies',
    );
    return '$_temp0';
  }

  @override
  String get reply => 'Reply';

  @override
  String get edit => 'Edit';

  @override
  String get save => 'Save';

  @override
  String get send => 'Send';

  @override
  String get composeHint => 'Write your reply…';

  @override
  String get subjectHint => 'Headline';

  @override
  String get bodyHint => 'Write your scoop…';

  @override
  String get postFailed =>
      'Couldn\'t post — check your connection and try again';

  @override
  String get posting => 'Posting…';

  @override
  String get postSuccess => 'Posted';

  @override
  String get markRead => 'Read';

  @override
  String get markUnread => 'Unread';

  @override
  String get markAllRead => 'Mark all read';

  @override
  String get markAllReadConfirm => 'Mark all scoops as read?';

  @override
  String get markedAllRead => 'All marked as read';

  @override
  String get cancel => 'Cancel';

  @override
  String get newComments => 'New comments';

  @override
  String get newBadge => 'New';

  @override
  String get today => 'Today';

  @override
  String get yesterday => 'Yesterday';

  @override
  String get earlier => 'Earlier';

  @override
  String get searchScoops => 'Search scoops';

  @override
  String get noResults => 'No matching scoops';

  @override
  String get op => 'OP';

  @override
  String get jumpToNewest => 'Jump to newest';

  @override
  String get nextComment => 'Next comment';

  @override
  String get myReply => 'Your reply';

  @override
  String get justNow => 'just now';

  @override
  String minutesAgo(int n) {
    return '${n}m ago';
  }

  @override
  String hoursAgo(int n) {
    return '${n}h ago';
  }

  @override
  String daysAgo(int n) {
    return '${n}d ago';
  }

  @override
  String get titleOnly => '(headline only)';

  @override
  String get emptyScoops => 'No scoops yet';

  @override
  String get filterMine => 'Scoops I replied to';

  @override
  String get noMyReplies => 'You haven\'t replied to any scoops yet';

  @override
  String get showSearchTab => 'Show search tab';

  @override
  String get sortBy => 'Sort scoops by';

  @override
  String get sortLastComment => 'Last comment';

  @override
  String get sortPostTime => 'Post time';

  @override
  String get userRating => 'Member rating';

  @override
  String get userDetails => 'Member details';

  @override
  String get memberPoints => 'Points';

  @override
  String get memberRaters => 'Raters';

  @override
  String get memberPosts => 'Posts';

  @override
  String get memberSince => 'Member since';

  @override
  String get memberRank => 'Rank';

  @override
  String userPostsInThread(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '$count posts in this thread',
      one: '1 post in this thread',
      zero: 'No posts in this thread',
    );
    return '$_temp0';
  }

  @override
  String get accentAmber => 'Amber';

  @override
  String get accentRed => 'Red';

  @override
  String get accentBlue => 'Blue';

  @override
  String get accentGreen => 'Green';

  @override
  String get accentPurple => 'Purple';

  @override
  String get accentGraphite => 'Graphite';

  @override
  String get loginInProgress => 'Complete sign-in in the page below';

  @override
  String get loginSuccess => 'Signed in';

  @override
  String get about => 'ABOUT';

  @override
  String get aboutBody => 'An unofficial reader for rotter.net scoops.';

  @override
  String get filterTitle => 'Show';

  @override
  String get filterAll => 'All';

  @override
  String get filterUnread => 'Unread';

  @override
  String get filterNewComments => 'New comments';

  @override
  String get filterSaved => 'Saved';

  @override
  String get filterFollowing => 'Following';

  @override
  String get filterShowAll => 'Show all scoops';

  @override
  String get emptyUnread => 'Nothing unread';

  @override
  String get emptyNewComments => 'No new comments';

  @override
  String get emptySaved => 'No saved scoops yet';

  @override
  String get emptyFollowing => 'No followed discussions yet';

  @override
  String get refreshFailed => 'Couldn\'t refresh — showing previous results';

  @override
  String get saveScoop => 'Save scoop';

  @override
  String get unsaveScoop => 'Remove from saved';

  @override
  String get share => 'Share';

  @override
  String get openOnRotter => 'Open on rotter.net';

  @override
  String get feedNewScoops => 'New scoops available';

  @override
  String get feedUpdated => 'Scoop updates available';

  @override
  String get follow => 'Follow discussion';

  @override
  String get unfollow => 'Unfollow discussion';

  @override
  String get commentNew => 'New since your last visit';

  @override
  String get backToReply => 'Back to reply';

  @override
  String get previousNew => 'Previous new comment';

  @override
  String get nextNew => 'Next new comment';

  @override
  String newOfCount(int index, int count) {
    return '$index of $count new';
  }

  @override
  String newRepliesCount(int count) {
    return 'New replies: $count';
  }

  @override
  String get searchThread => 'Search discussion';

  @override
  String get searchThreadHint => 'Text or username';

  @override
  String get previousMatch => 'Previous match';

  @override
  String get nextMatch => 'Next match';

  @override
  String matchesCount(int count) {
    return 'Matches: $count';
  }

  @override
  String get collapseAll => 'Collapse all discussions';

  @override
  String get expandAll => 'Expand all discussions';

  @override
  String get resumeReading => 'Resume reading';

  @override
  String get startTop => 'Start at the top';

  @override
  String get navigationHint =>
      'Use the floating arrows to move between discussions or jump to the latest reply.';

  @override
  String get close => 'Close';

  @override
  String replyingTo(String name) {
    return 'Replying to $name';
  }

  @override
  String depthLevel(int n) {
    return 'Nesting level $n';
  }

  @override
  String get hideReplies => 'Hide replies';

  @override
  String showReplies(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: 'Show $count replies',
      one: 'Show 1 reply',
    );
    return '$_temp0';
  }

  @override
  String get discardDraftTitle => 'Discard this draft?';

  @override
  String get discard => 'Discard';

  @override
  String get keepEditing => 'Keep editing';

  @override
  String get notSignedInError => 'You\'re not signed in to rotter';

  @override
  String get blockedError =>
      'rotter blocked the request — try again in a moment';

  @override
  String get done => 'Done';

  @override
  String get copy => 'Copy';

  @override
  String get copied => 'Copied';

  @override
  String get aiSection => 'AI summary';

  @override
  String get aiEnable => 'Summarize threads';

  @override
  String get aiKey => 'API key';

  @override
  String get aiKeyPlaceholder => 'OpenAI API key';

  @override
  String get aiKeyStored => 'Key stored — enter a new one to replace';

  @override
  String get aiSaveKey => 'Save key';

  @override
  String get aiRemoveKey => 'Remove';

  @override
  String get aiModel => 'Model';

  @override
  String get aiEndpoint => 'Endpoint';

  @override
  String get aiAdvanced => 'Advanced';

  @override
  String get aiPrivacyNote =>
      'Summarizing sends the post and its comments to the endpoint below. Your key is kept in the device\'s secure storage and is never sent anywhere else.';

  @override
  String get aiSummary => 'Summary';

  @override
  String get aiSummarize => 'Summarize';

  @override
  String get aiSummarizing => 'Summarizing…';

  @override
  String get aiRegenerate => 'Regenerate';

  @override
  String get aiDisclaimer =>
      'Written by a language model from the thread\'s own text. rotter posts are often unverified — check before relying on it.';

  @override
  String get aiErrorTitle => 'Couldn\'t summarize';

  @override
  String get aiErrorNotConfigured => 'Add an OpenAI API key in Settings first';

  @override
  String get aiErrorEmpty => 'The model returned nothing';

  @override
  String get aiLanguage => 'Summary language';

  @override
  String get aiLanguageFollowApp => 'Same as app';

  @override
  String get aiLanguageHebrew => 'Hebrew';

  @override
  String get aiLanguageEnglish => 'English';

  @override
  String get aiStatusConfigured => 'Configured';

  @override
  String get aiStatusOff => 'Not configured';
}
