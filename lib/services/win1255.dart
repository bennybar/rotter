/// Windows-1255 (Hebrew) decoder.
///
/// rotter.net serves everything as cp1255 and Dart has no built-in codec for
/// it (and `enough_convert` ships 1250–1256 *except* 1255). This is a tiny,
/// dependency-free byte→Unicode decoder — the only thing the reading path needs
/// (writes go through the WebView, so no cp1255 *encoding* is required).
String decodeWin1255(List<int> bytes) {
  final out = StringBuffer();
  for (final b in bytes) {
    if (b < 0x80) {
      out.writeCharCode(b);
    } else {
      out.writeCharCode(_high[b - 0x80]);
    }
  }
  return out.toString();
}

/// Decode a cp1255 percent-encoded value (rotter encodes usernames in URLs this
/// way, e.g. `%E0%F8%E8` → `אבט`-style Hebrew), not UTF-8.
String decodeWin1255Percent(String s) {
  final bytes = <int>[];
  for (var i = 0; i < s.length;) {
    final c = s[i];
    if (c == '%' && i + 2 < s.length) {
      final h = int.tryParse(s.substring(i + 1, i + 3), radix: 16);
      if (h != null) {
        bytes.add(h);
        i += 3;
        continue;
      }
    }
    bytes.add(c == '+' ? 0x20 : s.codeUnitAt(i));
    i++;
  }
  return decodeWin1255(bytes);
}

/// Encode a string to windows-1255 bytes (Unicode → cp1255). Characters with no
/// cp1255 mapping become '?'. Needed to POST Hebrew form data to rotter the way
/// the site expects (it reads params as windows-1255, not UTF-8).
List<int> encodeWin1255(String s) {
  final out = <int>[];
  for (final rune in s.runes) {
    if (rune < 0x80) {
      out.add(rune);
    } else {
      out.add(_reverse[rune] ?? 0x3F); // '?'
    }
  }
  return out;
}

/// cp1255 `application/x-www-form-urlencoded` component encoding: spaces → '+',
/// unreserved bytes pass through, everything else → %XX over the cp1255 bytes.
String _formComponent(String s) {
  final sb = StringBuffer();
  for (final b in encodeWin1255(s)) {
    if (b == 0x20) {
      sb.write('+');
    } else if ((b >= 0x30 && b <= 0x39) ||
        (b >= 0x41 && b <= 0x5A) ||
        (b >= 0x61 && b <= 0x7A) ||
        b == 0x2D ||
        b == 0x2E ||
        b == 0x5F ||
        b == 0x7E) {
      sb.writeCharCode(b);
    } else {
      sb.write('%');
      sb.write(b.toRadixString(16).toUpperCase().padLeft(2, '0'));
    }
  }
  return sb.toString();
}

/// Build a cp1255 urlencoded request body from form fields.
String encodeWin1255Form(Map<String, String> fields) => fields.entries
    .map((e) => '${_formComponent(e.key)}=${_formComponent(e.value)}')
    .join('&');

/// Unicode → cp1255 byte, derived from [_high] (the decode table).
final Map<int, int> _reverse = {
  for (var i = 0; i < _high.length; i++)
    if (_high[i] != _r) _high[i]: i + 0x80,
};

const int _r = 0xFFFD; // replacement char for undefined slots

// Unicode code points for bytes 0x80..0xFF (index 0 == 0x80).
const List<int> _high = [
  0x20AC, _r, 0x201A, 0x0192, 0x201E, 0x2026, 0x2020, 0x2021, // 80-87
  0x02C6, 0x2030, _r, 0x2039, _r, _r, _r, _r, // 88-8F
  _r, 0x2018, 0x2019, 0x201C, 0x201D, 0x2022, 0x2013, 0x2014, // 90-97
  0x02DC, 0x2122, _r, 0x203A, _r, _r, _r, _r, // 98-9F
  0x00A0, 0x00A1, 0x00A2, 0x00A3, 0x20AA, 0x00A5, 0x00A6, 0x00A7, // A0-A7 (A4=₪)
  0x00A8, 0x00A9, 0x00D7, 0x00AB, 0x00AC, 0x00AD, 0x00AE, 0x00AF, // A8-AF (AA=×)
  0x00B0, 0x00B1, 0x00B2, 0x00B3, 0x00B4, 0x00B5, 0x00B6, 0x00B7, // B0-B7
  0x00B8, 0x00B9, 0x00F7, 0x00BB, 0x00BC, 0x00BD, 0x00BE, 0x00BF, // B8-BF (BA=÷)
  0x05B0, 0x05B1, 0x05B2, 0x05B3, 0x05B4, 0x05B5, 0x05B6, 0x05B7, // C0-C7 niqqud
  0x05B8, 0x05B9, _r, 0x05BB, 0x05BC, 0x05BD, 0x05BE, 0x05BF, // C8-CF
  0x05C0, 0x05C1, 0x05C2, 0x05C3, 0x05F0, 0x05F1, 0x05F2, 0x05F3, // D0-D7
  0x05F4, _r, _r, _r, _r, _r, _r, _r, // D8-DF
  0x05D0, 0x05D1, 0x05D2, 0x05D3, 0x05D4, 0x05D5, 0x05D6, 0x05D7, // E0-E7 alef..
  0x05D8, 0x05D9, 0x05DA, 0x05DB, 0x05DC, 0x05DD, 0x05DE, 0x05DF, // E8-EF
  0x05E0, 0x05E1, 0x05E2, 0x05E3, 0x05E4, 0x05E5, 0x05E6, 0x05E7, // F0-F7
  0x05E8, 0x05E9, 0x05EA, _r, _r, 0x200E, 0x200F, _r, // F8-FF ..tav, LRM, RLM
];
