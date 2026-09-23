import 'dart:ui' show PlatformDispatcher;

import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// The language a thread summary is written in — chosen independently of the
/// interface, since reading rotter in Hebrew while wanting the summary in
/// English (or the reverse) is a reasonable combination.
enum SummaryLanguage { followApp, hebrew, english }

/// Settings for the optional LLM thread summary.
///
/// The API key is a credential and lives in the Keychain/Keystore (like the
/// rotter password), not SharedPreferences. Only the enabled flag, model,
/// endpoint and language are ordinary preferences.
class AIStore extends ChangeNotifier {
  AIStore._();
  static final AIStore instance = AIStore._();

  /// Editable rather than hardcoded: model identifiers change often, and a
  /// wrong one should be a line of text to correct, not a rebuild. Gateways that
  /// namespace their catalogue want the vendor prefix — `openai/gpt-5.6-luna`.
  static const defaultModel = 'gpt-5.6-luna';

  /// The API root. Configurable because the same model is reachable through
  /// gateways that speak the OpenAI chat-completions protocol elsewhere.
  static const defaultBaseUrl = 'https://api.openai.com/v1';

  static const _enabledKey = 'ai_summaries_enabled';
  static const _modelKey = 'ai_model';
  static const _baseUrlKey = 'ai_base_url';
  static const _languageKey = 'ai_summary_language';
  static const _secKey = 'openai_api_key';
  static const _secure = FlutterSecureStorage();

  bool enabled = false;
  String model = defaultModel;
  String baseUrl = defaultBaseUrl;
  SummaryLanguage language = SummaryLanguage.followApp;

  /// Tracked rather than read from secure storage on demand, so UI can listen.
  bool hasKey = false;

  /// Summaries are only offered when there is actually something to call with.
  bool get isReady => enabled && hasKey;

  Future<void> load() async {
    final prefs = await SharedPreferences.getInstance();
    enabled = prefs.getBool(_enabledKey) ?? false;
    model = prefs.getString(_modelKey) ?? defaultModel;
    baseUrl = prefs.getString(_baseUrlKey) ?? defaultBaseUrl;
    language = SummaryLanguage.values.firstWhere(
      (l) => l.name == prefs.getString(_languageKey),
      orElse: () => SummaryLanguage.followApp,
    );
    hasKey = (await apiKey()) != null;
  }

  Future<String?> apiKey() async {
    final k = await _secure.read(key: _secKey);
    return k == null || k.isEmpty ? null : k;
  }

  Future<void> setKey(String key) async {
    final trimmed = key.trim();
    if (trimmed.isEmpty) return clearKey();
    await _secure.write(key: _secKey, value: trimmed);
    hasKey = true;
    notifyListeners();
  }

  Future<void> clearKey() async {
    await _secure.delete(key: _secKey);
    hasKey = false;
    await setEnabled(false);
  }

  Future<void> setEnabled(bool v) async {
    enabled = v;
    notifyListeners();
    await (await SharedPreferences.getInstance()).setBool(_enabledKey, v);
  }

  Future<void> setModel(String v) async {
    model = v.trim().isEmpty ? defaultModel : v.trim();
    notifyListeners();
    await (await SharedPreferences.getInstance()).setString(_modelKey, model);
  }

  Future<void> setBaseUrl(String v) async {
    baseUrl = v.trim().isEmpty ? defaultBaseUrl : v.trim();
    notifyListeners();
    await (await SharedPreferences.getInstance()).setString(_baseUrlKey, baseUrl);
  }

  Future<void> setLanguage(SummaryLanguage v) async {
    language = v;
    notifyListeners();
    await (await SharedPreferences.getInstance()).setString(_languageKey, v.name);
  }

  /// The language name given to the model (the prompt itself is English).
  /// [appLocale] is the in-app locale; null means "follow the device".
  String promptLanguage(Locale? appLocale) => switch (language) {
        SummaryLanguage.hebrew => 'Hebrew',
        SummaryLanguage.english => 'English',
        SummaryLanguage.followApp =>
          (appLocale ?? PlatformDispatcher.instance.locale).languageCode == 'he'
              ? 'Hebrew'
              : 'English',
      };
}
