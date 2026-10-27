# Diagnosing delayed Hue activation

Launcher 1.7 and later record each On/Off request with a random correlation ID, wall-clock timestamp, monotonic timestamp, elapsed durations and network transport flags. Events distinguish TCP connect, TLS handshake, request write, status/header/body reads, Digest challenges, HTTP status, saved switch verification and native service notification. Failures record exception types, categories, cause chains and bounded stack frames. Raw exception messages are omitted because they can contain credentials or response data.

Private JSON-lines files rotate at 64 KiB, retaining up to three files (192 KiB). They survive activity closure and process restarts, but not uninstall, data clearing or a TV wipe. Android logcat receives the same events. A write failure is reported to logcat and never prevents a power request.

After a power attempt, read-only checks sample the saved native switch at 2, 10, 30 and 60 seconds. Times are target delays; slow requests can delay later samples. A newer request cancels remaining older samples. Checks continue after closing the activity while its process remains alive. Android process termination can end monitoring. These checks **do not establish actual lamp synchronization**; they help identify unexpected switch changes. These observational checks never write settings or restart services. In 1.8, the preceding explicit button command has its own bounded recovery: up to two state writes, each followed by a service notification and up to two reads on fresh TLS connections. Socket timeouts are capped by the remaining command budget (30 seconds), with a 3-second normal read timeout for GET. Certificate/authentication failures are not retried as transient failures.

The success message explicitly says that actual light response is unverified. Errors show a diagnostic ID rather than potentially sensitive exception text.

## Collect immediately after a problem

Note the approximate local time, button pressed, diagnostic ID if shown, and observed lamp behavior. Keep the app installed. From a computer with authorized ADB:

```sh
python3 tools/collect_diagnostics.py --serial TV_IP:5555
```

Use `--adb /path/to/adb` if needed. The output directory contains persistent launcher events and an allowlisted extract of native Philips logcat events (state transitions, discovery results, switch changes and exception types). Raw native logs can include Hue authentication keys, so the collector holds them only in memory and never exports complete lines. The native ring buffer is finite; collect promptly.

For a read-only API health check without changing synchronization:

```sh
adb -s TV_IP:5555 shell content call --uri content://local.tv.hue.tvapi --method diagnostic-check
```

Export and health-check operations require Android DUMP permission, available to authorized ADB shell. They are unavailable to ordinary apps. Export excludes provisioning preferences, credentials, certificate values, response bodies and authentication headers. Do not publish raw logcat or the application's shared preferences.

## Limits

The launcher cannot identify every writer of native TV settings or observe every internal Philips failure. Standard Android permissions do not let it continuously read another app's logs. A saved On switch or accepted service intent does not prove that lights received colors. Correlate launcher events with native transitions and user observations. If Philips does not log the original cause, this diagnostic version cannot reconstruct it with certainty.
