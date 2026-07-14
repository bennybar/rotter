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
}
