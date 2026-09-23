// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Hebrew (`he`).
class L10nHe extends L10n {
  L10nHe([String locale = 'he']) : super(locale);

  @override
  String get appTitle => 'רוטר סקופים';

  @override
  String get tabScoops => 'סקופים';

  @override
  String get tabSearch => 'חיפוש';

  @override
  String get tabNewMessage => 'הודעה חדשה';

  @override
  String get tabSettings => 'הגדרות';

  @override
  String get settingsTitle => 'הגדרות';

  @override
  String get appearance => 'מראה';

  @override
  String get theme => 'ערכת נושא';

  @override
  String get themeSystem => 'מערכת';

  @override
  String get themeLight => 'בהיר';

  @override
  String get themeDark => 'כהה';

  @override
  String get accentColor => 'צבע הדגשה';

  @override
  String get textSize => 'גודל טקסט';

  @override
  String get threadSpacing => 'ריווח באשכול';

  @override
  String get predictiveBack => 'חזרה עם הצצה';

  @override
  String get predictiveBackHint => 'הצצה למסך הקודם בזמן החלקה לאחור';

  @override
  String get language => 'שפה';

  @override
  String get languageSystem => 'מערכת';

  @override
  String get hebrew => 'עברית';

  @override
  String get english => 'English';

  @override
  String get account => 'חשבון';

  @override
  String get signIn => 'התחברות';

  @override
  String get usernameLabel => 'שם משתמש';

  @override
  String get passwordLabel => 'סיסמה';

  @override
  String get loginFailed => 'שם משתמש או סיסמה שגויים';

  @override
  String get loginError => 'ההתחברות נכשלה — בדקו את החיבור ונסו שוב';

  @override
  String get signOut => 'התנתקות';

  @override
  String get signedIn => 'מחובר/ת';

  @override
  String get loginSubtitle => 'התחברות ל-rotter.net כדי לפרסם';

  @override
  String get newMessagePrompt => 'התחבר/י כדי לפרסם הודעה חדשה';

  @override
  String get newMessagePromptBody =>
      'פרסום ברוטר מחייב חשבון מחובר. התחבר/י פעם אחת, ותוכל/י לפתוח אשכול חדש כאן.';

  @override
  String get compose => 'אשכול חדש';

  @override
  String get loadingError => 'הטעינה נכשלה';

  @override
  String get threadNotReady => 'הסקופ עדיין לא זמין — נסה שוב בעוד רגע';

  @override
  String get scoopRemoved => 'הסקופ אינו זמין יותר — ככל הנראה הוסר';

  @override
  String get removedBadge => 'הוסר';

  @override
  String get retry => 'נסה שוב';

  @override
  String get refresh => 'רענון';

  @override
  String get openInBrowser => 'פתח בדפדפן';

  @override
  String replies(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '$count תגובות',
      one: 'תגובה אחת',
      zero: 'אין תגובות',
    );
    return '$_temp0';
  }

  @override
  String get reply => 'תגובה';

  @override
  String get edit => 'עריכה';

  @override
  String get save => 'שמור';

  @override
  String get send => 'שלח';

  @override
  String get composeHint => 'כתוב/כתבי תגובה…';

  @override
  String get subjectHint => 'כותרת';

  @override
  String get bodyHint => 'כתוב/כתבי את הסקופ…';

  @override
  String get postFailed => 'הפרסום נכשל — בדקו את החיבור ונסו שוב';

  @override
  String get posting => 'מפרסם…';

  @override
  String get postSuccess => 'פורסם';

  @override
  String get markRead => 'נקרא';

  @override
  String get markUnread => 'לא נקרא';

  @override
  String get markAllRead => 'סמן הכל כנקרא';

  @override
  String get markAllReadConfirm => 'לסמן את כל הסקופים כנקראו?';

  @override
  String get markedAllRead => 'הכל סומן כנקרא';

  @override
  String get cancel => 'ביטול';

  @override
  String get newComments => 'תגובות חדשות';

  @override
  String get newBadge => 'חדש';

  @override
  String get today => 'היום';

  @override
  String get yesterday => 'אתמול';

  @override
  String get earlier => 'מוקדם יותר';

  @override
  String get searchScoops => 'חיפוש בסקופים';

  @override
  String get noResults => 'לא נמצאו סקופים';

  @override
  String get op => 'כותב';

  @override
  String get jumpToNewest => 'לתגובה האחרונה';

  @override
  String get nextComment => 'לתגובה הבאה';

  @override
  String get myReply => 'התגובה שלך';

  @override
  String get justNow => 'כעת';

  @override
  String minutesAgo(int n) {
    return 'לפני $n דק׳';
  }

  @override
  String hoursAgo(int n) {
    return 'לפני $n שע׳';
  }

  @override
  String daysAgo(int n) {
    return 'לפני $n ימ׳';
  }

  @override
  String get titleOnly => '(כותרת בלבד)';

  @override
  String get emptyScoops => 'אין סקופים עדיין';

  @override
  String get filterMine => 'סקופים שהגבתי בהם';

  @override
  String get noMyReplies => 'עדיין לא הגבת לאף סקופ';

  @override
  String get showSearchTab => 'הצג לשונית חיפוש';

  @override
  String get sortBy => 'מיון סקופים לפי';

  @override
  String get sortLastComment => 'תגובה אחרונה';

  @override
  String get sortPostTime => 'זמן פרסום';

  @override
  String get userRating => 'דירוג חבר';

  @override
  String get userDetails => 'פרטי חבר';

  @override
  String get memberPoints => 'נקודות';

  @override
  String get memberRaters => 'מדרגים';

  @override
  String get memberPosts => 'הודעות';

  @override
  String get memberSince => 'חבר מתאריך';

  @override
  String get memberRank => 'דירוג';

  @override
  String userPostsInThread(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '$count הודעות באשכול',
      one: 'הודעה אחת באשכול',
      zero: 'אין הודעות באשכול',
    );
    return '$_temp0';
  }

  @override
  String get accentAmber => 'ענבר';

  @override
  String get accentRed => 'אדום';

  @override
  String get accentBlue => 'כחול';

  @override
  String get accentGreen => 'ירוק';

  @override
  String get accentPurple => 'סגול';

  @override
  String get accentGraphite => 'גרפיט';

  @override
  String get loginInProgress => 'השלימו את ההתחברות בעמוד שמתחת';

  @override
  String get loginSuccess => 'מחובר/ת';

  @override
  String get about => 'אודות';

  @override
  String get aboutBody => 'קורא לא רשמי לסקופים של rotter.net.';

  @override
  String get filterTitle => 'הצג';

  @override
  String get filterAll => 'הכל';

  @override
  String get filterUnread => 'לא נקראו';

  @override
  String get filterNewComments => 'תגובות חדשות';

  @override
  String get filterSaved => 'שמורים';

  @override
  String get filterFollowing => 'במעקב';

  @override
  String get filterShowAll => 'הצג את כל הסקופים';

  @override
  String get emptyUnread => 'הכל נקרא';

  @override
  String get emptyNewComments => 'אין תגובות חדשות';

  @override
  String get emptySaved => 'אין עדיין סקופים שמורים';

  @override
  String get emptyFollowing => 'אין עדיין דיונים במעקב';

  @override
  String get refreshFailed => 'הרענון נכשל — מוצגות התוצאות הקודמות';

  @override
  String get saveScoop => 'שמירת סקופ';

  @override
  String get unsaveScoop => 'הסרה מהשמורים';

  @override
  String get share => 'שיתוף';

  @override
  String get openOnRotter => 'פתיחה ב-rotter.net';

  @override
  String get feedNewScoops => 'סקופים חדשים זמינים';

  @override
  String get feedUpdated => 'עדכונים לסקופים זמינים';

  @override
  String get follow => 'עקוב אחר הדיון';

  @override
  String get unfollow => 'הפסק לעקוב';

  @override
  String get commentNew => 'חדש מאז הביקור האחרון';

  @override
  String get backToReply => 'חזרה לתגובה';

  @override
  String get previousNew => 'התגובה החדשה הקודמת';

  @override
  String get nextNew => 'התגובה החדשה הבאה';

  @override
  String newOfCount(int index, int count) {
    return 'חדשות: $index מתוך $count';
  }

  @override
  String newRepliesCount(int count) {
    return 'תגובות חדשות: $count';
  }

  @override
  String get searchThread => 'חיפוש בדיון';

  @override
  String get searchThreadHint => 'טקסט או שם משתמש';

  @override
  String get previousMatch => 'התוצאה הקודמת';

  @override
  String get nextMatch => 'התוצאה הבאה';

  @override
  String matchesCount(int count) {
    return 'תוצאות: $count';
  }

  @override
  String get collapseAll => 'כווץ את כל הדיונים';

  @override
  String get expandAll => 'הרחב את כל הדיונים';

  @override
  String get resumeReading => 'המשך מהמקום שבו עצרת';

  @override
  String get startTop => 'התחל מלמעלה';

  @override
  String get navigationHint =>
      'החצים הצפים מאפשרים לעבור בין דיונים או לדלג לתגובה החדשה ביותר.';

  @override
  String get close => 'סגור';

  @override
  String replyingTo(String name) {
    return 'בתגובה ל$name';
  }

  @override
  String depthLevel(int n) {
    return 'עומק $n';
  }

  @override
  String get hideReplies => 'הסתר תגובות';

  @override
  String showReplies(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: 'הצג $count תגובות',
      one: 'הצג תגובה אחת',
    );
    return '$_temp0';
  }

  @override
  String get discardDraftTitle => 'למחוק את הטיוטה?';

  @override
  String get discard => 'מחיקה';

  @override
  String get keepEditing => 'המשך עריכה';

  @override
  String get notSignedInError => 'אינך מחובר/ת לרוטר';

  @override
  String get blockedError => 'רוטר חסם את הבקשה — נסו שוב בעוד רגע';

  @override
  String get done => 'סיום';

  @override
  String get copy => 'העתקה';

  @override
  String get copied => 'הועתק';

  @override
  String get aiSection => 'סיכום AI';

  @override
  String get aiEnable => 'סיכום אשכולות';

  @override
  String get aiKey => 'מפתח API';

  @override
  String get aiKeyPlaceholder => 'מפתח API של OpenAI';

  @override
  String get aiKeyStored => 'מפתח שמור — הזינו חדש כדי להחליף';

  @override
  String get aiSaveKey => 'שמירת מפתח';

  @override
  String get aiRemoveKey => 'הסרה';

  @override
  String get aiModel => 'מודל';

  @override
  String get aiEndpoint => 'כתובת ה-API';

  @override
  String get aiAdvanced => 'מתקדם';

  @override
  String get aiPrivacyNote =>
      'סיכום שולח את ההודעה והתגובות לכתובת שלמטה. המפתח נשמר באחסון המאובטח של המכשיר ואינו נשלח לשום מקום אחר.';

  @override
  String get aiSummary => 'סיכום';

  @override
  String get aiSummarize => 'סיכום';

  @override
  String get aiSummarizing => 'מסכם…';

  @override
  String get aiRegenerate => 'יצירה מחדש';

  @override
  String get aiDisclaimer =>
      'נכתב על ידי מודל שפה מתוך טקסט האשכול. פרסומים ברוטר לרוב אינם מאומתים — בדקו לפני שתסתמכו על זה.';

  @override
  String get aiErrorTitle => 'הסיכום נכשל';

  @override
  String get aiErrorNotConfigured => 'הוסיפו מפתח API של OpenAI בהגדרות';

  @override
  String get aiErrorEmpty => 'המודל לא החזיר תשובה';

  @override
  String get aiLanguage => 'שפת הסיכום';

  @override
  String get aiLanguageFollowApp => 'כמו האפליקציה';

  @override
  String get aiLanguageHebrew => 'עברית';

  @override
  String get aiLanguageEnglish => 'אנגלית';

  @override
  String get aiStatusConfigured => 'מוגדר';

  @override
  String get aiStatusOff => 'לא מוגדר';
}
