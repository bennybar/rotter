import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:intl/intl.dart' as intl;

import 'app_localizations_en.dart';
import 'app_localizations_he.dart';

// ignore_for_file: type=lint

/// Callers can lookup localized strings with an instance of L10n
/// returned by `L10n.of(context)`.
///
/// Applications need to include `L10n.delegate()` in their app's
/// `localizationDelegates` list, and the locales they support in the app's
/// `supportedLocales` list. For example:
///
/// ```dart
/// import 'l10n/app_localizations.dart';
///
/// return MaterialApp(
///   localizationsDelegates: L10n.localizationsDelegates,
///   supportedLocales: L10n.supportedLocales,
///   home: MyApplicationHome(),
/// );
/// ```
///
/// ## Update pubspec.yaml
///
/// Please make sure to update your pubspec.yaml to include the following
/// packages:
///
/// ```yaml
/// dependencies:
///   # Internationalization support.
///   flutter_localizations:
///     sdk: flutter
///   intl: any # Use the pinned version from flutter_localizations
///
///   # Rest of dependencies
/// ```
///
/// ## iOS Applications
///
/// iOS applications define key application metadata, including supported
/// locales, in an Info.plist file that is built into the application bundle.
/// To configure the locales supported by your app, you’ll need to edit this
/// file.
///
/// First, open your project’s ios/Runner.xcworkspace Xcode workspace file.
/// Then, in the Project Navigator, open the Info.plist file under the Runner
/// project’s Runner folder.
///
/// Next, select the Information Property List item, select Add Item from the
/// Editor menu, then select Localizations from the pop-up menu.
///
/// Select and expand the newly-created Localizations item then, for each
/// locale your application supports, add a new item and select the locale
/// you wish to add from the pop-up menu in the Value field. This list should
/// be consistent with the languages listed in the L10n.supportedLocales
/// property.
abstract class L10n {
  L10n(String locale)
    : localeName = intl.Intl.canonicalizedLocale(locale.toString());

  final String localeName;

  static L10n? of(BuildContext context) {
    return Localizations.of<L10n>(context, L10n);
  }

  static const LocalizationsDelegate<L10n> delegate = _L10nDelegate();

  /// A list of this localizations delegate along with the default localizations
  /// delegates.
  ///
  /// Returns a list of localizations delegates containing this delegate along with
  /// GlobalMaterialLocalizations.delegate, GlobalCupertinoLocalizations.delegate,
  /// and GlobalWidgetsLocalizations.delegate.
  ///
  /// Additional delegates can be added by appending to this list in
  /// MaterialApp. This list does not have to be used at all if a custom list
  /// of delegates is preferred or required.
  static const List<LocalizationsDelegate<dynamic>> localizationsDelegates =
      <LocalizationsDelegate<dynamic>>[
        delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
      ];

  /// A list of this localizations delegate's supported locales.
  static const List<Locale> supportedLocales = <Locale>[
    Locale('en'),
    Locale('he'),
  ];

  /// No description provided for @appTitle.
  ///
  /// In en, this message translates to:
  /// **'Rotter Scoops'**
  String get appTitle;

  /// No description provided for @tabScoops.
  ///
  /// In en, this message translates to:
  /// **'Scoops'**
  String get tabScoops;

  /// No description provided for @tabSearch.
  ///
  /// In en, this message translates to:
  /// **'Search'**
  String get tabSearch;

  /// No description provided for @tabNewMessage.
  ///
  /// In en, this message translates to:
  /// **'New message'**
  String get tabNewMessage;

  /// No description provided for @tabSettings.
  ///
  /// In en, this message translates to:
  /// **'Settings'**
  String get tabSettings;

  /// No description provided for @settingsTitle.
  ///
  /// In en, this message translates to:
  /// **'Settings'**
  String get settingsTitle;

  /// No description provided for @appearance.
  ///
  /// In en, this message translates to:
  /// **'APPEARANCE'**
  String get appearance;

  /// No description provided for @theme.
  ///
  /// In en, this message translates to:
  /// **'Theme'**
  String get theme;

  /// No description provided for @themeSystem.
  ///
  /// In en, this message translates to:
  /// **'Device'**
  String get themeSystem;

  /// No description provided for @themeLight.
  ///
  /// In en, this message translates to:
  /// **'Light'**
  String get themeLight;

  /// No description provided for @themeDark.
  ///
  /// In en, this message translates to:
  /// **'Dark'**
  String get themeDark;

  /// No description provided for @accentColor.
  ///
  /// In en, this message translates to:
  /// **'Accent color'**
  String get accentColor;

  /// No description provided for @textSize.
  ///
  /// In en, this message translates to:
  /// **'Text size'**
  String get textSize;

  /// No description provided for @threadSpacing.
  ///
  /// In en, this message translates to:
  /// **'Thread spacing'**
  String get threadSpacing;

  /// No description provided for @predictiveBack.
  ///
  /// In en, this message translates to:
  /// **'Predictive back'**
  String get predictiveBack;

  /// No description provided for @predictiveBackHint.
  ///
  /// In en, this message translates to:
  /// **'Peek at the previous screen while swiping back'**
  String get predictiveBackHint;

  /// No description provided for @language.
  ///
  /// In en, this message translates to:
  /// **'Language'**
  String get language;

  /// No description provided for @languageSystem.
  ///
  /// In en, this message translates to:
  /// **'Device'**
  String get languageSystem;

  /// No description provided for @hebrew.
  ///
  /// In en, this message translates to:
  /// **'עברית'**
  String get hebrew;

  /// No description provided for @english.
  ///
  /// In en, this message translates to:
  /// **'English'**
  String get english;

  /// No description provided for @account.
  ///
  /// In en, this message translates to:
  /// **'ACCOUNT'**
  String get account;

  /// No description provided for @signIn.
  ///
  /// In en, this message translates to:
  /// **'Sign in'**
  String get signIn;

  /// No description provided for @usernameLabel.
  ///
  /// In en, this message translates to:
  /// **'Username'**
  String get usernameLabel;

  /// No description provided for @passwordLabel.
  ///
  /// In en, this message translates to:
  /// **'Password'**
  String get passwordLabel;

  /// No description provided for @loginFailed.
  ///
  /// In en, this message translates to:
  /// **'Wrong username or password'**
  String get loginFailed;

  /// No description provided for @loginError.
  ///
  /// In en, this message translates to:
  /// **'Sign-in failed — check your connection and try again'**
  String get loginError;

  /// No description provided for @signOut.
  ///
  /// In en, this message translates to:
  /// **'Sign out'**
  String get signOut;

  /// No description provided for @signedIn.
  ///
  /// In en, this message translates to:
  /// **'Signed in'**
  String get signedIn;

  /// No description provided for @loginSubtitle.
  ///
  /// In en, this message translates to:
  /// **'Sign in to rotter.net to post'**
  String get loginSubtitle;

  /// No description provided for @newMessagePrompt.
  ///
  /// In en, this message translates to:
  /// **'Sign in to post a new message'**
  String get newMessagePrompt;

  /// No description provided for @newMessagePromptBody.
  ///
  /// In en, this message translates to:
  /// **'Posting on Rotter requires a signed-in account. Sign in once and you can open a new thread right here.'**
  String get newMessagePromptBody;

  /// No description provided for @compose.
  ///
  /// In en, this message translates to:
  /// **'New thread'**
  String get compose;

  /// No description provided for @loadingError.
  ///
  /// In en, this message translates to:
  /// **'Couldn\'t load'**
  String get loadingError;

  /// No description provided for @threadNotReady.
  ///
  /// In en, this message translates to:
  /// **'This scoop isn\'t available yet — try again in a moment'**
  String get threadNotReady;

  /// No description provided for @scoopRemoved.
  ///
  /// In en, this message translates to:
  /// **'This scoop is no longer available — it was most likely removed'**
  String get scoopRemoved;

  /// No description provided for @removedBadge.
  ///
  /// In en, this message translates to:
  /// **'Removed'**
  String get removedBadge;

  /// No description provided for @retry.
  ///
  /// In en, this message translates to:
  /// **'Retry'**
  String get retry;

  /// No description provided for @refresh.
  ///
  /// In en, this message translates to:
  /// **'Refresh'**
  String get refresh;

  /// No description provided for @openInBrowser.
  ///
  /// In en, this message translates to:
  /// **'Open in browser'**
  String get openInBrowser;

  /// No description provided for @replies.
  ///
  /// In en, this message translates to:
  /// **'{count, plural, =0{No replies} =1{1 reply} other{{count} replies}}'**
  String replies(int count);

  /// No description provided for @reply.
  ///
  /// In en, this message translates to:
  /// **'Reply'**
  String get reply;

  /// No description provided for @edit.
  ///
  /// In en, this message translates to:
  /// **'Edit'**
  String get edit;

  /// No description provided for @save.
  ///
  /// In en, this message translates to:
  /// **'Save'**
  String get save;

  /// No description provided for @send.
  ///
  /// In en, this message translates to:
  /// **'Send'**
  String get send;

  /// No description provided for @composeHint.
  ///
  /// In en, this message translates to:
  /// **'Write your reply…'**
  String get composeHint;

  /// No description provided for @subjectHint.
  ///
  /// In en, this message translates to:
  /// **'Headline'**
  String get subjectHint;

  /// No description provided for @bodyHint.
  ///
  /// In en, this message translates to:
  /// **'Write your scoop…'**
  String get bodyHint;

  /// No description provided for @postFailed.
  ///
  /// In en, this message translates to:
  /// **'Couldn\'t post — check your connection and try again'**
  String get postFailed;

  /// No description provided for @posting.
  ///
  /// In en, this message translates to:
  /// **'Posting…'**
  String get posting;

  /// No description provided for @postSuccess.
  ///
  /// In en, this message translates to:
  /// **'Posted'**
  String get postSuccess;

  /// No description provided for @markRead.
  ///
  /// In en, this message translates to:
  /// **'Read'**
  String get markRead;

  /// No description provided for @markUnread.
  ///
  /// In en, this message translates to:
  /// **'Unread'**
  String get markUnread;

  /// No description provided for @markAllRead.
  ///
  /// In en, this message translates to:
  /// **'Mark all read'**
  String get markAllRead;

  /// No description provided for @markAllReadConfirm.
  ///
  /// In en, this message translates to:
  /// **'Mark all scoops as read?'**
  String get markAllReadConfirm;

  /// No description provided for @markedAllRead.
  ///
  /// In en, this message translates to:
  /// **'All marked as read'**
  String get markedAllRead;

  /// No description provided for @cancel.
  ///
  /// In en, this message translates to:
  /// **'Cancel'**
  String get cancel;

  /// No description provided for @newComments.
  ///
  /// In en, this message translates to:
  /// **'New comments'**
  String get newComments;

  /// No description provided for @newBadge.
  ///
  /// In en, this message translates to:
  /// **'New'**
  String get newBadge;

  /// No description provided for @today.
  ///
  /// In en, this message translates to:
  /// **'Today'**
  String get today;

  /// No description provided for @yesterday.
  ///
  /// In en, this message translates to:
  /// **'Yesterday'**
  String get yesterday;

  /// No description provided for @earlier.
  ///
  /// In en, this message translates to:
  /// **'Earlier'**
  String get earlier;

  /// No description provided for @searchScoops.
  ///
  /// In en, this message translates to:
  /// **'Search scoops'**
  String get searchScoops;

  /// No description provided for @noResults.
  ///
  /// In en, this message translates to:
  /// **'No matching scoops'**
  String get noResults;

  /// No description provided for @op.
  ///
  /// In en, this message translates to:
  /// **'OP'**
  String get op;

  /// No description provided for @jumpToNewest.
  ///
  /// In en, this message translates to:
  /// **'Jump to newest'**
  String get jumpToNewest;

  /// No description provided for @nextComment.
  ///
  /// In en, this message translates to:
  /// **'Next comment'**
  String get nextComment;

  /// No description provided for @myReply.
  ///
  /// In en, this message translates to:
  /// **'Your reply'**
  String get myReply;

  /// No description provided for @justNow.
  ///
  /// In en, this message translates to:
  /// **'just now'**
  String get justNow;

  /// No description provided for @minutesAgo.
  ///
  /// In en, this message translates to:
  /// **'{n}m ago'**
  String minutesAgo(int n);

  /// No description provided for @hoursAgo.
  ///
  /// In en, this message translates to:
  /// **'{n}h ago'**
  String hoursAgo(int n);

  /// No description provided for @daysAgo.
  ///
  /// In en, this message translates to:
  /// **'{n}d ago'**
  String daysAgo(int n);

  /// No description provided for @titleOnly.
  ///
  /// In en, this message translates to:
  /// **'(headline only)'**
  String get titleOnly;

  /// No description provided for @emptyScoops.
  ///
  /// In en, this message translates to:
  /// **'No scoops yet'**
  String get emptyScoops;

  /// No description provided for @filterMine.
  ///
  /// In en, this message translates to:
  /// **'Scoops I replied to'**
  String get filterMine;

  /// No description provided for @noMyReplies.
  ///
  /// In en, this message translates to:
  /// **'You haven\'t replied to any scoops yet'**
  String get noMyReplies;

  /// No description provided for @showSearchTab.
  ///
  /// In en, this message translates to:
  /// **'Show search tab'**
  String get showSearchTab;

  /// No description provided for @sortBy.
  ///
  /// In en, this message translates to:
  /// **'Sort scoops by'**
  String get sortBy;

  /// No description provided for @sortLastComment.
  ///
  /// In en, this message translates to:
  /// **'Last comment'**
  String get sortLastComment;

  /// No description provided for @sortPostTime.
  ///
  /// In en, this message translates to:
  /// **'Post time'**
  String get sortPostTime;

  /// No description provided for @userRating.
  ///
  /// In en, this message translates to:
  /// **'Member rating'**
  String get userRating;

  /// No description provided for @userDetails.
  ///
  /// In en, this message translates to:
  /// **'Member details'**
  String get userDetails;

  /// No description provided for @memberPoints.
  ///
  /// In en, this message translates to:
  /// **'Points'**
  String get memberPoints;

  /// No description provided for @memberRaters.
  ///
  /// In en, this message translates to:
  /// **'Raters'**
  String get memberRaters;

  /// No description provided for @memberPosts.
  ///
  /// In en, this message translates to:
  /// **'Posts'**
  String get memberPosts;

  /// No description provided for @memberSince.
  ///
  /// In en, this message translates to:
  /// **'Member since'**
  String get memberSince;

  /// No description provided for @memberRank.
  ///
  /// In en, this message translates to:
  /// **'Rank'**
  String get memberRank;

  /// No description provided for @userPostsInThread.
  ///
  /// In en, this message translates to:
  /// **'{count, plural, =0{No posts in this thread} =1{1 post in this thread} other{{count} posts in this thread}}'**
  String userPostsInThread(int count);

  /// No description provided for @accentAmber.
  ///
  /// In en, this message translates to:
  /// **'Amber'**
  String get accentAmber;

  /// No description provided for @accentRed.
  ///
  /// In en, this message translates to:
  /// **'Red'**
  String get accentRed;

  /// No description provided for @accentBlue.
  ///
  /// In en, this message translates to:
  /// **'Blue'**
  String get accentBlue;

  /// No description provided for @accentGreen.
  ///
  /// In en, this message translates to:
  /// **'Green'**
  String get accentGreen;

  /// No description provided for @accentPurple.
  ///
  /// In en, this message translates to:
  /// **'Purple'**
  String get accentPurple;

  /// No description provided for @accentGraphite.
  ///
  /// In en, this message translates to:
  /// **'Graphite'**
  String get accentGraphite;

  /// No description provided for @loginInProgress.
  ///
  /// In en, this message translates to:
  /// **'Complete sign-in in the page below'**
  String get loginInProgress;

  /// No description provided for @loginSuccess.
  ///
  /// In en, this message translates to:
  /// **'Signed in'**
  String get loginSuccess;

  /// No description provided for @about.
  ///
  /// In en, this message translates to:
  /// **'ABOUT'**
  String get about;

  /// No description provided for @aboutBody.
  ///
  /// In en, this message translates to:
  /// **'An unofficial reader for rotter.net scoops.'**
  String get aboutBody;

  /// No description provided for @filterTitle.
  ///
  /// In en, this message translates to:
  /// **'Show'**
  String get filterTitle;

  /// No description provided for @filterAll.
  ///
  /// In en, this message translates to:
  /// **'All'**
  String get filterAll;

  /// No description provided for @filterUnread.
  ///
  /// In en, this message translates to:
  /// **'Unread'**
  String get filterUnread;

  /// No description provided for @filterNewComments.
  ///
  /// In en, this message translates to:
  /// **'New comments'**
  String get filterNewComments;

  /// No description provided for @filterSaved.
  ///
  /// In en, this message translates to:
  /// **'Saved'**
  String get filterSaved;

  /// No description provided for @filterFollowing.
  ///
  /// In en, this message translates to:
  /// **'Following'**
  String get filterFollowing;

  /// No description provided for @filterShowAll.
  ///
  /// In en, this message translates to:
  /// **'Show all scoops'**
  String get filterShowAll;

  /// No description provided for @emptyUnread.
  ///
  /// In en, this message translates to:
  /// **'Nothing unread'**
  String get emptyUnread;

  /// No description provided for @emptyNewComments.
  ///
  /// In en, this message translates to:
  /// **'No new comments'**
  String get emptyNewComments;

  /// No description provided for @emptySaved.
  ///
  /// In en, this message translates to:
  /// **'No saved scoops yet'**
  String get emptySaved;

  /// No description provided for @emptyFollowing.
  ///
  /// In en, this message translates to:
  /// **'No followed discussions yet'**
  String get emptyFollowing;

  /// No description provided for @refreshFailed.
  ///
  /// In en, this message translates to:
  /// **'Couldn\'t refresh — showing previous results'**
  String get refreshFailed;

  /// No description provided for @saveScoop.
  ///
  /// In en, this message translates to:
  /// **'Save scoop'**
  String get saveScoop;

  /// No description provided for @unsaveScoop.
  ///
  /// In en, this message translates to:
  /// **'Remove from saved'**
  String get unsaveScoop;

  /// No description provided for @share.
  ///
  /// In en, this message translates to:
  /// **'Share'**
  String get share;

  /// No description provided for @openOnRotter.
  ///
  /// In en, this message translates to:
  /// **'Open on rotter.net'**
  String get openOnRotter;

  /// No description provided for @feedNewScoops.
  ///
  /// In en, this message translates to:
  /// **'New scoops available'**
  String get feedNewScoops;

  /// No description provided for @feedUpdated.
  ///
  /// In en, this message translates to:
  /// **'Scoop updates available'**
  String get feedUpdated;

  /// No description provided for @follow.
  ///
  /// In en, this message translates to:
  /// **'Follow discussion'**
  String get follow;

  /// No description provided for @unfollow.
  ///
  /// In en, this message translates to:
  /// **'Unfollow discussion'**
  String get unfollow;

  /// No description provided for @commentNew.
  ///
  /// In en, this message translates to:
  /// **'New since your last visit'**
  String get commentNew;

  /// No description provided for @backToReply.
  ///
  /// In en, this message translates to:
  /// **'Back to reply'**
  String get backToReply;

  /// No description provided for @previousNew.
  ///
  /// In en, this message translates to:
  /// **'Previous new comment'**
  String get previousNew;

  /// No description provided for @nextNew.
  ///
  /// In en, this message translates to:
  /// **'Next new comment'**
  String get nextNew;

  /// No description provided for @newOfCount.
  ///
  /// In en, this message translates to:
  /// **'{index} of {count} new'**
  String newOfCount(int index, int count);

  /// No description provided for @newRepliesCount.
  ///
  /// In en, this message translates to:
  /// **'New replies: {count}'**
  String newRepliesCount(int count);

  /// No description provided for @searchThread.
  ///
  /// In en, this message translates to:
  /// **'Search discussion'**
  String get searchThread;

  /// No description provided for @searchThreadHint.
  ///
  /// In en, this message translates to:
  /// **'Text or username'**
  String get searchThreadHint;

  /// No description provided for @previousMatch.
  ///
  /// In en, this message translates to:
  /// **'Previous match'**
  String get previousMatch;

  /// No description provided for @nextMatch.
  ///
  /// In en, this message translates to:
  /// **'Next match'**
  String get nextMatch;

  /// No description provided for @matchesCount.
  ///
  /// In en, this message translates to:
  /// **'Matches: {count}'**
  String matchesCount(int count);

  /// No description provided for @collapseAll.
  ///
  /// In en, this message translates to:
  /// **'Collapse all discussions'**
  String get collapseAll;

  /// No description provided for @expandAll.
  ///
  /// In en, this message translates to:
  /// **'Expand all discussions'**
  String get expandAll;

  /// No description provided for @resumeReading.
  ///
  /// In en, this message translates to:
  /// **'Resume reading'**
  String get resumeReading;

  /// No description provided for @startTop.
  ///
  /// In en, this message translates to:
  /// **'Start at the top'**
  String get startTop;

  /// No description provided for @navigationHint.
  ///
  /// In en, this message translates to:
  /// **'Use the floating arrows to move between discussions or jump to the latest reply.'**
  String get navigationHint;

  /// No description provided for @close.
  ///
  /// In en, this message translates to:
  /// **'Close'**
  String get close;

  /// No description provided for @replyingTo.
  ///
  /// In en, this message translates to:
  /// **'Replying to {name}'**
  String replyingTo(String name);

  /// No description provided for @depthLevel.
  ///
  /// In en, this message translates to:
  /// **'Nesting level {n}'**
  String depthLevel(int n);

  /// No description provided for @hideReplies.
  ///
  /// In en, this message translates to:
  /// **'Hide replies'**
  String get hideReplies;

  /// No description provided for @showReplies.
  ///
  /// In en, this message translates to:
  /// **'{count, plural, =1{Show 1 reply} other{Show {count} replies}}'**
  String showReplies(int count);

  /// No description provided for @discardDraftTitle.
  ///
  /// In en, this message translates to:
  /// **'Discard this draft?'**
  String get discardDraftTitle;

  /// No description provided for @discard.
  ///
  /// In en, this message translates to:
  /// **'Discard'**
  String get discard;

  /// No description provided for @keepEditing.
  ///
  /// In en, this message translates to:
  /// **'Keep editing'**
  String get keepEditing;

  /// No description provided for @notSignedInError.
  ///
  /// In en, this message translates to:
  /// **'You\'re not signed in to rotter'**
  String get notSignedInError;

  /// No description provided for @blockedError.
  ///
  /// In en, this message translates to:
  /// **'rotter blocked the request — try again in a moment'**
  String get blockedError;

  /// No description provided for @done.
  ///
  /// In en, this message translates to:
  /// **'Done'**
  String get done;

  /// No description provided for @copy.
  ///
  /// In en, this message translates to:
  /// **'Copy'**
  String get copy;

  /// No description provided for @copied.
  ///
  /// In en, this message translates to:
  /// **'Copied'**
  String get copied;

  /// No description provided for @aiSection.
  ///
  /// In en, this message translates to:
  /// **'AI summary'**
  String get aiSection;

  /// No description provided for @aiEnable.
  ///
  /// In en, this message translates to:
  /// **'Summarize threads'**
  String get aiEnable;

  /// No description provided for @aiKey.
  ///
  /// In en, this message translates to:
  /// **'API key'**
  String get aiKey;

  /// No description provided for @aiKeyPlaceholder.
  ///
  /// In en, this message translates to:
  /// **'OpenAI API key'**
  String get aiKeyPlaceholder;

  /// No description provided for @aiKeyStored.
  ///
  /// In en, this message translates to:
  /// **'Key stored — enter a new one to replace'**
  String get aiKeyStored;

  /// No description provided for @aiSaveKey.
  ///
  /// In en, this message translates to:
  /// **'Save key'**
  String get aiSaveKey;

  /// No description provided for @aiRemoveKey.
  ///
  /// In en, this message translates to:
  /// **'Remove'**
  String get aiRemoveKey;

  /// No description provided for @aiModel.
  ///
  /// In en, this message translates to:
  /// **'Model'**
  String get aiModel;

  /// No description provided for @aiEndpoint.
  ///
  /// In en, this message translates to:
  /// **'Endpoint'**
  String get aiEndpoint;

  /// No description provided for @aiAdvanced.
  ///
  /// In en, this message translates to:
  /// **'Advanced'**
  String get aiAdvanced;

  /// No description provided for @aiPrivacyNote.
  ///
  /// In en, this message translates to:
  /// **'Summarizing sends the post and its comments to the endpoint below. Your key is kept in the device\'s secure storage and is never sent anywhere else.'**
  String get aiPrivacyNote;

  /// No description provided for @aiSummary.
  ///
  /// In en, this message translates to:
  /// **'Summary'**
  String get aiSummary;

  /// No description provided for @aiSummarize.
  ///
  /// In en, this message translates to:
  /// **'Summarize'**
  String get aiSummarize;

  /// No description provided for @aiSummarizing.
  ///
  /// In en, this message translates to:
  /// **'Summarizing…'**
  String get aiSummarizing;

  /// No description provided for @aiRegenerate.
  ///
  /// In en, this message translates to:
  /// **'Regenerate'**
  String get aiRegenerate;

  /// No description provided for @aiDisclaimer.
  ///
  /// In en, this message translates to:
  /// **'Written by a language model from the thread\'s own text. rotter posts are often unverified — check before relying on it.'**
  String get aiDisclaimer;

  /// No description provided for @aiErrorTitle.
  ///
  /// In en, this message translates to:
  /// **'Couldn\'t summarize'**
  String get aiErrorTitle;

  /// No description provided for @aiErrorNotConfigured.
  ///
  /// In en, this message translates to:
  /// **'Add an OpenAI API key in Settings first'**
  String get aiErrorNotConfigured;

  /// No description provided for @aiErrorEmpty.
  ///
  /// In en, this message translates to:
  /// **'The model returned nothing'**
  String get aiErrorEmpty;

  /// No description provided for @aiLanguage.
  ///
  /// In en, this message translates to:
  /// **'Summary language'**
  String get aiLanguage;

  /// No description provided for @aiLanguageFollowApp.
  ///
  /// In en, this message translates to:
  /// **'Same as app'**
  String get aiLanguageFollowApp;

  /// No description provided for @aiLanguageHebrew.
  ///
  /// In en, this message translates to:
  /// **'Hebrew'**
  String get aiLanguageHebrew;

  /// No description provided for @aiLanguageEnglish.
  ///
  /// In en, this message translates to:
  /// **'English'**
  String get aiLanguageEnglish;

  /// No description provided for @aiStatusConfigured.
  ///
  /// In en, this message translates to:
  /// **'Configured'**
  String get aiStatusConfigured;

  /// No description provided for @aiStatusOff.
  ///
  /// In en, this message translates to:
  /// **'Not configured'**
  String get aiStatusOff;
}

class _L10nDelegate extends LocalizationsDelegate<L10n> {
  const _L10nDelegate();

  @override
  Future<L10n> load(Locale locale) {
    return SynchronousFuture<L10n>(lookupL10n(locale));
  }

  @override
  bool isSupported(Locale locale) =>
      <String>['en', 'he'].contains(locale.languageCode);

  @override
  bool shouldReload(_L10nDelegate old) => false;
}

L10n lookupL10n(Locale locale) {
  // Lookup logic when only language code is specified.
  switch (locale.languageCode) {
    case 'en':
      return L10nEn();
    case 'he':
      return L10nHe();
  }

  throw FlutterError(
    'L10n.delegate failed to load unsupported locale "$locale". This is likely '
    'an issue with the localizations generation tool. Please file an issue '
    'on GitHub with a reproducible sample app and the gen-l10n configuration '
    'that was used.',
  );
}
