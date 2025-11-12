import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_inappwebview/flutter_inappwebview.dart';
import 'core/constants.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const ReforcusApp());
}

class ReforcusApp extends StatelessWidget {
  const ReforcusApp({super.key});
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'reforcus',
      theme: ThemeData(useMaterial3: true),
      home: const ViewerPage(),
    );
  }
}

class ViewerPage extends StatefulWidget {
  const ViewerPage({super.key});
  @override
  State<ViewerPage> createState() => _ViewerPageState();
}

class _ViewerPageState extends State<ViewerPage> {
  final GlobalKey webViewKey = GlobalKey();
  InAppWebViewController? controller;

  late DateTime _startAt;
  Timer? _timer;
  Duration _elapsed = Duration.zero;
  int _viewCount = 0;

  String _currentUrl = AppUrls.youtubeShorts; // 初期は YouTube Shorts

  @override
  void initState() {
    super.initState();
    _startAt = DateTime.now();
    _timer = Timer.periodic(const Duration(seconds: 1), (_) {
      setState(() {
        _elapsed = DateTime.now().difference(_startAt);
      });
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  String two(int n) => n.toString().padLeft(2, '0');

  String get _elapsedLabel {
    final s = _elapsed.inSeconds;
    final hh = two(s ~/ 3600);
    final mm = two((s % 3600) ~/ 60);
    final ss = two(s % 60);
    return '$hh:$mm:$ss';
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('reforcus'),
        actions: [
          PopupMenuButton<String>(
            onSelected: (v) {
              setState(() {
                if (v == 'yt') _currentUrl = AppUrls.youtubeShorts;
                if (v == 'ig') _currentUrl = AppUrls.instagramReels;
              });
              controller?.loadUrl(
                urlRequest: URLRequest(url: WebUri(_currentUrl)),
              );
              _resetCounters();
            },
            itemBuilder: (_) => const [
              PopupMenuItem(value: 'yt', child: Text('YouTube Shorts')),
              PopupMenuItem(value: 'ig', child: Text('Instagram Reels')),
            ],
          ),
        ],
      ),
      body: Stack(children: [_buildWebView(), _buildOverlay()]),
    );
  }

  Widget _buildWebView() {
    return InAppWebView(
      key: webViewKey,
      initialUrlRequest: URLRequest(url: WebUri(_currentUrl)),
      initialSettings: InAppWebViewSettings(
        javaScriptEnabled: true,
        mediaPlaybackRequiresUserGesture: false,
        useShouldOverrideUrlLoading: true,
      ),
      onWebViewCreated: (ctrl) async {
        controller = ctrl;

        // JS → Flutter の通信ハンドラ
        ctrl.addJavaScriptHandler(
          handlerName: 'metrics',
          callback: (args) {
            if (args.isNotEmpty && args.first is Map) {
              final m = args.first as Map;
              if (m['type'] == 'view') {
                setState(() {
                  _viewCount = (m['count'] as num?)?.toInt() ?? _viewCount + 1;
                });
              }
            }
            return {'ok': true};
          },
        );
      },
      shouldOverrideUrlLoading: (controller, action) async {
        final host = action.request.url?.host ?? '';
        // 許可ドメインのみ通す（安全性と体験の一貫性のため）
        if (!AllowedHosts.set.contains(host)) {
          return NavigationActionPolicy.CANCEL;
        }
        return NavigationActionPolicy.ALLOW;
      },
      onLoadStop: (ctrl, url) async {
        // DOM 監視・URL変化監視の JS を注入
        await _injectObserver(ctrl);
      },
    );
  }

  Widget _buildOverlay() {
    return IgnorePointer(
      ignoring: true, // タップを WebView へ透過
      child: Align(
        alignment: Alignment.topRight,
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: DecoratedBox(
            decoration: BoxDecoration(
              color: Colors.black.withOpacity(0.6),
              borderRadius: BorderRadius.circular(10),
            ),
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
              child: DefaultTextStyle(
                style: const TextStyle(color: Colors.white, fontSize: 16),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(_elapsedLabel),
                    const SizedBox(width: 8),
                    Text('• $_viewCount'),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }

  void _resetCounters() {
    setState(() {
      _startAt = DateTime.now();
      _elapsed = Duration.zero;
      _viewCount = 0;
    });
  }

  Future<void> _injectObserver(InAppWebViewController ctrl) async {
    const js = r"""
(function(){
  if (window.__reforcusInjected) return;
  window.__reforcusInjected = true;

  let count = 0;
  let lastNotifyAt = 0;

  function notifyView() {
    const now = Date.now();
    // 連続通知の暴発を抑制（1秒以内は無視）
    if (now - lastNotifyAt < 1000) return;
    lastNotifyAt = now;
    count++;
    try {
      window.flutter_inappwebview.callHandler('metrics', {type:'view', count});
    } catch (e) {}
  }

  function handleUrlChange(){
    // YouTube Shorts: /shorts/<id>
    if (location.hostname.includes('youtube.com') && location.pathname.startsWith('/shorts/')) {
      notifyView();
    }
    // Instagram Reels: /reels/ or /reel/<id>
    if (location.hostname.includes('instagram.com') && (location.pathname.startsWith('/reels') || location.pathname.startsWith('/reel/'))) {
      notifyView();
    }
  }

  // History API をフック（SPA の画面遷移に対応）
  (function(history){
    const push = history.pushState, replace = history.replaceState;
    history.pushState = function(){ push.apply(this, arguments); setTimeout(handleUrlChange, 0); }
    history.replaceState = function(){ replace.apply(this, arguments); setTimeout(handleUrlChange, 0); }
    window.addEventListener('popstate', handleUrlChange);
  })(window.history);

  // 画面に完全に入った video を検知
  const obs = new IntersectionObserver(entries => {
    for (const e of entries) {
      if (e.isIntersecting && e.intersectionRatio > 0.9) {
        notifyView();
      }
    }
  }, {threshold:[0.9]});

  function hookCandidates(){
    const cands = [];
    // Shorts（暫定セレクタ）
    cands.push(...document.querySelectorAll('ytd-reel-video-renderer video, #shorts-container video'));
    // Reels（暫定セレクタ）
    cands.push(...document.querySelectorAll('article video, [data-testid="current-reel-video"] video'));
    cands.forEach(v=>obs.observe(v));
  }

  new MutationObserver(hookCandidates).observe(document.documentElement, {childList:true, subtree:true});
  hookCandidates();

  // 初回
  handleUrlChange();
})();
""";
    await ctrl.evaluateJavascript(source: js);
  }
}
