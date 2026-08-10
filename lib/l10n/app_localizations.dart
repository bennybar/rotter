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
