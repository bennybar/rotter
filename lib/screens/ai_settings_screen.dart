import 'package:flutter/material.dart';

import '../l10n/app_localizations.dart';
import '../services/ai_store.dart';
import '../theme.dart';

/// Configuration for the optional thread summary, on its own screen — an API
/// key, a model id and an endpoint are technical fields that made the main
/// Settings read as a developer page. Settings keeps a one-line status.
class AISettingsScreen extends StatefulWidget {
  const AISettingsScreen({super.key});

  @override
  State<AISettingsScreen> createState() => _AISettingsScreenState();
}

class _AISettingsScreenState extends State<AISettingsScreen> {
  final ai = AIStore.instance;
  // Held only until written to secure storage, then cleared.
  final _key = TextEditingController();
  late final _model = TextEditingController(text: ai.model);
  late final _endpoint = TextEditingController(text: ai.baseUrl);
  bool _advanced = false;

  @override
  void dispose() {
    _key.dispose();
    _model.dispose();
    _endpoint.dispose();
    super.dispose();
  }

  Future<void> _saveKey() async {
    if (_key.text.trim().isEmpty) return;
    await ai.setKey(_key.text);
    _key.clear();
    if (mounted) FocusScope.of(context).unfocus();
  }

  @override
  Widget build(BuildContext context) {
    final l = L10n.of(context)!;
    final accent = Theme.of(context).colorScheme.primary;
    return Scaffold(
      appBar: AppBar(title: Text(l.aiSection)),
      body: ListenableBuilder(
        listenable: ai,
        builder: (context, _) => ListView(
          padding: EdgeInsets.fromLTRB(18, 8, 18, 28 + MediaQuery.paddingOf(context).bottom),
          children: [
            _card(Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              SwitchListTile.adaptive(
                contentPadding: EdgeInsets.zero,
                title: Text(l.aiEnable,
                    style: TextStyle(fontWeight: FontWeight.w700, color: cInk(context))),
                value: ai.enabled,
                activeTrackColor: accent,
                // Nothing to call with until a key is stored.
                onChanged: ai.hasKey ? ai.setEnabled : null,
              ),
              // Stated plainly: this ships the thread's text off the device to a
              // third party, which nothing else in the app does.
              Text(l.aiPrivacyNote,
                  style: TextStyle(fontSize: 12.5, height: 1.45, color: cMuted(context))),
            ])),
            const SizedBox(height: 14),
            _card(Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(l.aiKey, style: TextStyle(fontWeight: FontWeight.w700, color: cInk(context))),
              const SizedBox(height: 10),
              TextField(
                controller: _key,
                obscureText: true,
                autocorrect: false,
                enableSuggestions: false,
                textDirection: TextDirection.ltr,
                onSubmitted: (_) => _saveKey(),
                decoration: InputDecoration(
                  hintText: ai.hasKey ? l.aiKeyStored : l.aiKeyPlaceholder,
                  border: const OutlineInputBorder(),
                  isDense: true,
                ),
              ),
              const SizedBox(height: 8),
              Row(children: [
                ValueListenableBuilder(
                  valueListenable: _key,
                  builder: (context, v, _) => FilledButton.tonal(
                    onPressed: v.text.trim().isEmpty ? null : _saveKey,
                    child: Text(l.aiSaveKey),
                  ),
                ),
                const Spacer(),
                if (ai.hasKey)
                  TextButton(
                    style: TextButton.styleFrom(foregroundColor: Colors.red.shade400),
                    onPressed: () {
                      _key.clear();
                      ai.clearKey();
                    },
                    child: Text(l.aiRemoveKey),
                  ),
              ]),
            ])),
            const SizedBox(height: 14),
            _card(Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(l.aiLanguage,
                  style: TextStyle(fontWeight: FontWeight.w700, color: cInk(context))),
              const SizedBox(height: 10),
              SegmentedButton<SummaryLanguage>(
                showSelectedIcon: false,
                segments: [
                  ButtonSegment(value: SummaryLanguage.followApp, label: Text(l.aiLanguageFollowApp)),
                  ButtonSegment(value: SummaryLanguage.hebrew, label: Text(l.aiLanguageHebrew)),
                  ButtonSegment(value: SummaryLanguage.english, label: Text(l.aiLanguageEnglish)),
                ],
                selected: {ai.language},
                onSelectionChanged: (s) => ai.setLanguage(s.first),
              ),
            ])),
            const SizedBox(height: 14),
            _card(Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              InkWell(
                onTap: () => setState(() => _advanced = !_advanced),
                child: Row(children: [
                  Text(l.aiAdvanced,
                      style: TextStyle(fontWeight: FontWeight.w700, color: cInk(context))),
                  const Spacer(),
                  Icon(_advanced ? Icons.expand_less_rounded : Icons.expand_more_rounded,
                      color: cMuted(context)),
                ]),
              ),
              if (_advanced) ...[
                const SizedBox(height: 12),
                // Editable: model ids change often, and a wrong one should be a
                // line of text to fix, not a rebuild.
                _field(l.aiModel, _model, AIStore.defaultModel, ai.setModel),
                const SizedBox(height: 12),
                _field(l.aiEndpoint, _endpoint, AIStore.defaultBaseUrl, ai.setBaseUrl,
                    keyboard: TextInputType.url),
              ],
            ])),
          ],
        ),
      ),
    );
  }

  Widget _field(String label, TextEditingController c, String hint, ValueChanged<String> onChanged,
          {TextInputType? keyboard}) =>
      TextField(
        controller: c,
        autocorrect: false,
        enableSuggestions: false,
        keyboardType: keyboard,
        textDirection: TextDirection.ltr,
        onChanged: onChanged,
        decoration: InputDecoration(
          labelText: label,
          hintText: hint,
          border: const OutlineInputBorder(),
          isDense: true,
        ),
      );

  Widget _card(Widget child) => Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: cSurface(context),
          borderRadius: BorderRadius.circular(20),
        ),
        child: child,
      );
}
