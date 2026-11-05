package local.tv.hue;

import android.app.*;import android.content.Context;import android.graphics.*;import android.view.*;import android.widget.*;

/** Original drawing with the native ten-zone ordering; no Philips artwork bundled. */
final class PositionPicker {
 interface Selected{void accept(int angle);}
 static final float[][] POINTS={{.32f,.83f},{.22f,.62f},{.22f,.38f},{.32f,.17f},{.50f,.08f},{.68f,.17f},{.78f,.38f},{.78f,.62f},{.68f,.83f},{.50f,.92f}};
 static void show(Activity activity,String name,int angle,Selected selected){
  Dialog dialog=new Dialog(activity);LinearLayout panel=new LinearLayout(activity);panel.setOrientation(1);panel.setPadding(32,20,32,20);panel.setBackgroundColor(Color.rgb(25,38,65));
  TextView title=new TextView(activity);title.setText(name);title.setTextSize(24);title.setTextColor(Color.WHITE);panel.addView(title);
  TextView hint=new TextView(activity);hint.setText(SetupStrings.get(activity,"position_hint"));hint.setTextColor(Color.LTGRAY);hint.setTextSize(17);panel.addView(hint);
  TextView label=new TextView(activity);label.setTextSize(23);label.setTextColor(Color.WHITE);label.setGravity(Gravity.CENTER);
  ZoneView zones=new ZoneView(activity,angle,label);panel.addView(zones,new LinearLayout.LayoutParams(-1,(int)(activity.getResources().getDisplayMetrics().heightPixels*.50)));panel.addView(label);
  LinearLayout buttons=new LinearLayout(activity);buttons.setGravity(Gravity.CENTER);Button confirm=new Button(activity);confirm.setText(Strings.get(activity,"save"));confirm.setAllCaps(false);buttons.addView(confirm);Button cancel=new Button(activity);cancel.setText(Strings.get(activity,"cancel"));cancel.setAllCaps(false);buttons.addView(cancel);panel.addView(buttons);
  Runnable save=()->{selected.accept(zones.current+1);dialog.dismiss();};zones.confirm=save;confirm.setOnClickListener(v->save.run());cancel.setOnClickListener(v->dialog.dismiss());dialog.setContentView(panel);dialog.show();dialog.getWindow().setLayout((int)(activity.getResources().getDisplayMetrics().widthPixels*.85),-2);zones.requestFocus();
 }
 static final class ZoneView extends View {
  int current;final Paint paint=new Paint(3);final TextView label;final String[] names;Runnable confirm;
  ZoneView(Context c,int angle,TextView l){super(c);current=Math.max(0,Math.min(9,angle-1));label=l;names=SetupStrings.positions(c);setFocusable(true);setFocusableInTouchMode(true);update();}
  void update(){label.setText(names[current]);setContentDescription(names[current]);invalidate();}
  protected void onDraw(Canvas canvas){super.onDraw(canvas);float w=getWidth(),h=getHeight();float r=Math.min(w*.027f,h*.05f);
   paint.setColor(Color.rgb(12,23,37));paint.setStyle(Paint.Style.FILL);canvas.drawRoundRect(w*.35f,h*.34f,w*.65f,h*.67f,12,12,paint);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(3);paint.setColor(Color.rgb(143,159,178));canvas.drawRoundRect(w*.35f,h*.34f,w*.65f,h*.67f,12,12,paint);paint.setStyle(Paint.Style.FILL);paint.setColor(Color.WHITE);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(h*.085f);canvas.drawText("TV",w*.5f,h*.54f,paint);
   for(int i=0;i<10;i++){float x=POINTS[i][0]*w,y=POINTS[i][1]*h;paint.setColor(i==current?Color.rgb(255,217,92):Color.rgb(139,156,178));canvas.drawCircle(x,y,r*(i==current?1.18f:1),paint);if(i==current){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(3);canvas.drawCircle(x,y,r*1.50f,paint);paint.setStyle(Paint.Style.FILL);}paint.setColor(Color.rgb(14,24,38));paint.setTextSize(r*.85f);String number=i<4?"L"+(i+1):i==4?"T":i<9?"R"+(9-i):"B";canvas.drawText(number,x,y+r*.30f,paint);}
  }
  public boolean onKeyDown(int key,KeyEvent event){int dx=0,dy=0;if(key==KeyEvent.KEYCODE_DPAD_LEFT)dx=-1;else if(key==KeyEvent.KEYCODE_DPAD_RIGHT)dx=1;else if(key==KeyEvent.KEYCODE_DPAD_UP)dy=-1;else if(key==KeyEvent.KEYCODE_DPAD_DOWN)dy=1;else if(key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_ENTER){if(event.getRepeatCount()==0&&confirm!=null)confirm.run();return true;}else return super.onKeyDown(key,event);
   float best=Float.MAX_VALUE;int next=current;for(int i=0;i<10;i++){float x=POINTS[i][0]-POINTS[current][0],y=POINTS[i][1]-POINTS[current][1];float along=x*dx+y*dy,across=Math.abs(x*dy-y*dx);if(along>.01f){float score=along+across*2;if(score<best){best=score;next=i;}}}current=next;update();return true;
  }
  public boolean onTouchEvent(MotionEvent e){if(e.getAction()!=MotionEvent.ACTION_UP)return true;float best=Float.MAX_VALUE;int next=current;for(int i=0;i<10;i++){float dx=e.getX()-POINTS[i][0]*getWidth(),dy=e.getY()-POINTS[i][1]*getHeight(),d=dx*dx+dy*dy;if(d<best){best=d;next=i;}}current=next;update();performClick();return true;}
  public boolean performClick(){super.performClick();return true;}
 }
}
