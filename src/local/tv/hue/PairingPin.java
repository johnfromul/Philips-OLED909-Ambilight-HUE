package local.tv.hue;

import java.util.regex.*;

/** The firmware puts localized words after the fixed app name in some languages. */
final class PairingPin {
 private static final Pattern PROMPT=Pattern.compile(Pattern.quote("Ambilight Hue control")+"(?![\\p{L}\\p{N}_])[^:]{0,160}:\\s*([0-9]{1,4})(?![0-9])");
 static String extract(CharSequence text){if(text==null)return null;Matcher match=PROMPT.matcher(text);return match.find()?match.group(1):null;}
}
