package local.tv.hue;
import android.content.Context;import android.graphics.Color;import android.text.*;import android.text.style.ForegroundColorSpan;
final class UiColors {
 static final int YELLOW=Color.rgb(255,217,92),GREEN=Color.rgb(100,225,140),RED=Color.rgb(255,112,112);
 static CharSequence marks(String value){SpannableString text=new SpannableString(value);for(int i=0;i<value.length();i++){char ch=value.charAt(i);if(ch=='✓'||ch=='✕')text.setSpan(new ForegroundColorSpan(ch=='✓'?GREEN:RED),i,i+1,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);}return text;}
 static CharSequence summary(Context c,String value){SpannableString text=new SpannableString(marks(value));int end=value.indexOf('\n');if(end<0)end=value.length();text.setSpan(new ForegroundColorSpan(YELLOW),0,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);String prefix=FeatureStrings.get(c,"switch")+": ";int from=value.indexOf(prefix);if(from>=0){int start=from+prefix.length(),to=value.indexOf('\n',start);if(to<0)to=value.length();String state=value.substring(start,to);if(state.equals(FeatureStrings.get(c,"state_on"))||state.equals(FeatureStrings.get(c,"state_off")))text.setSpan(new ForegroundColorSpan(state.equals(FeatureStrings.get(c,"state_on"))?GREEN:RED),from,to,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);}return text;}
}
