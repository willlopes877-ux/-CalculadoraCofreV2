package com.example.gamev2;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import android.content.Context;
import java.util.*;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(7,9,16));
        getWindow().setNavigationBarColor(Color.rgb(7,9,16));
        setContentView(new ArenaView(this));
    }

    static class ArenaView extends View {
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Random rnd = new Random();
        final ArrayList<Particle> particles = new ArrayList<>();
        float d,w,h,playerX,enemyX,playerHp=100,enemyHp=100,xp,shake,skillCharge;\n        int aiMood=0, aiAggression=1;\n        long aiThinkAt, aiDodgeUntil;\n        String aiLine="IA: analisando você...";
        int level=1,enemyLevel=1,damage=20,defense=3,kills,combo;
        long lastAttack,dodgeUntil,enemyAttackAt,enemyTelegraphUntil,attackUntil,skillFlashUntil,lastFrame;
        boolean dodging,enemyWarning,attacking,paused,gameOver,playerHit;

        ArenaView(Context c){super(c);d=getResources().getDisplayMetrics().density;setFocusable(true);}
        float dp(float v){return v*d;}
        @Override protected void onSizeChanged(int sw,int sh,int ow,int oh){w=sw;h=sh;playerX=w*.28f;enemyX=w*.72f;}

        @Override protected void onDraw(Canvas c){
            long now=System.currentTimeMillis();
            if(lastFrame==0)lastFrame=now;
            float dt=Math.min(.033f,(now-lastFrame)/1000f); lastFrame=now;
            if(!paused&&!gameOver) update(dt,now);
            c.save();
            if(shake>0){float s=dp(7)*shake;c.translate((rnd.nextFloat()*2-1)*s,(rnd.nextFloat()*2-1)*s);shake=Math.max(0,shake-dt*5);}
            drawBackground(c); drawArena(c); drawEnemy(c); drawPlayer(c); drawParticles(c); drawHud(c); drawControls(c);
            if(paused)drawPause(c); if(gameOver)drawGameOver(c);
            c.restore(); postInvalidateDelayed(16);
        }

        void update(float dt,long now){
            if(now>attackUntil)attacking=false;
            if(now>dodgeUntil)dodging=false;
            enemyWarning=enemyTelegraphUntil>now;
            skillCharge=Math.min(100,skillCharge+dt*(8+level*.8f));
            float dist=playerX-enemyX;
            if(Math.abs(dist)>dp(135))enemyX+=Math.signum(dist)*dp(42)*dt;
            if(Math.abs(dist)<dp(175)&&now>enemyAttackAt){
                enemyAttackAt=now+1250-Math.min(300,enemyLevel*25);
                enemyTelegraphUntil=now+500;\n                aiThink(now);
            }
            if(enemyTelegraphUntil>0&&enemyTelegraphUntil<=now){
                enemyTelegraphUntil=0;
                if(!dodging){playerHp-=Math.max(5,13+enemyLevel*2-defense);playerHit=true;burst(playerX,h*.48f,24);shake=.65f;if(playerHp<=0){playerHp=0;gameOver=true;}}
            }
            for(Particle q:particles){q.x+=q.vx*dt;q.y+=q.vy*dt;q.vy+=dp(190)*dt;q.life-=dt;}
            particles.removeIf(q->q.life<=0);
            if(playerHit&&rnd.nextInt(5)==0)playerHit=false;
        }

        void aiThink(long now){\n            if(now<aiThinkAt)return;\n            aiThinkAt=now+650-Math.min(220,enemyLevel*18);\n            float hpRatio=playerHp/100f;\n            float enemyRatio=enemyHp/(100f+enemyLevel*20f);\n            if(combo>=3){ aiMood=2; aiAggression=3; aiLine="IA: quebrando seu combo!"; }\n            else if(hpRatio<.35f){ aiMood=1; aiAggression=2; aiLine="IA: vou finalizar!"; }\n            else if(enemyRatio<.35f){ aiMood=3; aiAggression=1; aiLine="IA: recuando..."; }\n            else { aiMood=0; aiAggression=1+rnd.nextInt(2); aiLine=rnd.nextBoolean()?"IA: analisando...":"IA: minha vez!"; }\n            if(aiMood==3 && rnd.nextFloat()<.45f) enemyX=Math.max(w*.60f,enemyX-dp(35));\n            if(aiMood==2 && rnd.nextFloat()<.35f) enemyX=Math.min(w*.82f,enemyX+dp(25));\n        }\n\n        void defeatEnemy(long now){
            kills++; xp+=45+enemyLevel*12; burst(enemyX,h*.48f,48);
            enemyLevel++; enemyHp=100+enemyLevel*20; enemyX=w*.72f; combo=0;
            enemyAttackAt=now+850;
            if(xp>=level*100){xp-=level*100;level++;damage+=7;defense+=2;skillCharge=Math.min(100,skillCharge+40);burst(playerX,h*.45f,65);}
        }

        void drawBackground(Canvas c){
            p.setShader(new LinearGradient(0,0,0,h,Color.rgb(6,12,27),Color.rgb(45,16,48),Shader.TileMode.CLAMP));
            c.drawRect(0,0,w,h,p); p.setShader(null);
            p.setColor(Color.argb(70,120,170,255));
            for(int i=0;i<30;i++){float sx=(i*83)%w,sy=(i*151)%Math.max(1,(int)(h*.58f));c.drawCircle(sx,sy,dp(1+i%3),p);}
            p.setColor(Color.argb(55,255,180,80));c.drawCircle(w*.82f,h*.20f,dp(48),p);
        }

        void drawArena(Canvas c){
            float ground=h*.65f;
            p.setShader(new LinearGradient(0,ground,0,h,Color.rgb(48,52,72),Color.rgb(9,11,18),Shader.TileMode.CLAMP));
            c.drawRect(0,ground,w,h,p);p.setShader(null);
            p.setColor(Color.argb(80,180,140,230));c.drawOval(w*.05f,ground-dp(18),w*.95f,ground+dp(105),p);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2));p.setColor(Color.argb(120,220,220,255));
            c.drawOval(w*.10f,ground-dp(2),w*.90f,ground+dp(65),p);p.setStyle(Paint.Style.FILL);
            p.setColor(Color.argb(45,255,255,255));
            for(int i=0;i<7;i++)c.drawRect(w*(.12f+i*.12f),ground,w*(.14f+i*.12f),h,p);
        }

        void drawPlayer(Canvas c){
            float now=System.currentTimeMillis(),y=h*.48f,bob=(float)Math.sin(now/105.0)*dp(3),x=playerX;
            if(dodging)bob-=dp(12);
            p.setColor(Color.argb(90,0,0,0));c.drawOval(x-dp(46),y+dp(47),x+dp(46),y+dp(64),p);
            p.setColor(playerHit?Color.rgb(255,70,70):Color.rgb(48,125,255));
            c.drawRoundRect(x-dp(29),y-dp(8)+bob,x+dp(29),y+dp(49)+bob,dp(15),dp(15),p);
            p.setColor(Color.rgb(245,198,154));c.drawCircle(x,y-dp(34)+bob,dp(25),p);
            p.setColor(Color.rgb(28,28,38));c.drawArc(x-dp(25),y-dp(59)+bob,x+dp(25),y-dp(18)+bob,180,180,true,p);
            p.setColor(Color.WHITE);c.drawCircle(x-dp(9),y-dp(36)+bob,dp(3),p);c.drawCircle(x+dp(9),y-dp(36)+bob,dp(3),p);
            p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeWidth(dp(11));p.setColor(Color.rgb(42,73,145));
            if(attacking){
                float phase=(System.currentTimeMillis()%240)/240f;
                float kick=(float)Math.sin(phase*Math.PI)*dp(48);
                c.drawLine(x-dp(11),y+dp(42),x+dp(20)+kick,y+dp(30),p);
                c.drawLine(x+dp(9),y+dp(42),x+dp(48)+kick,y+dp(18),p);
                p.setStrokeWidth(dp(7));p.setColor(Color.rgb(20,20,30));c.drawLine(x+dp(48)+kick,y+dp(18),x+dp(76)+kick,y+dp(18),p);
            }else{
                c.drawLine(x-dp(11),y+dp(42),x-dp(20),y+dp(64),p);c.drawLine(x+dp(11),y+dp(42),x+dp(20),y+dp(64),p);
                p.setStrokeWidth(dp(7));p.setColor(Color.rgb(20,20,30));c.drawLine(x-dp(20),y+dp(64),x-dp(4),y+dp(64),p);c.drawLine(x+dp(20),y+dp(64),x+dp(36),y+dp(64),p);
            }
            p.setStrokeCap(Paint.Cap.BUTT);
            if(attacking){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(8));p.setColor(Color.argb(220,100,220,255));c.drawArc(x+dp(5),y-dp(44),x+dp(135),y+dp(55),-70,125,false,p);p.setStyle(Paint.Style.FILL);}
        }

        void drawEnemy(Canvas c){
            float y=h*.48f,x=enemyX;
            p.setColor(Color.argb(85,0,0,0));c.drawOval(x-dp(46),y+dp(47),x+dp(46),y+dp(64),p);
            p.setColor(aiMood==2?Color.rgb(235,45,55):aiMood==3?Color.rgb(145,55,180):Color.rgb(190,55,70));c.drawRoundRect(x-dp(29),y-dp(6),x+dp(29),y+dp(49),dp(15),dp(15),p);
            p.setColor(Color.rgb(91,34,48));c.drawCircle(x,y-dp(31),dp(26),p);
            p.setColor(Color.rgb(255,215,70));c.drawCircle(x-dp(9),y-dp(33),dp(4),p);c.drawCircle(x+dp(9),y-dp(33),dp(4),p);
            p.setColor(Color.rgb(55,15,25));c.drawRect(x-dp(19),y-dp(54),x+dp(19),y-dp(36),p);
            if(enemyWarning){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(5));p.setColor(Color.rgb(255,75,70));c.drawCircle(x,y,dp(65),p);p.setStyle(Paint.Style.FILL);text(c,"!",x,y-dp(73),dp(26),Color.rgb(255,90,80),true,Paint.Align.CENTER);}
            bar(c,x-dp(52),y-dp(82),x+dp(52),y-dp(70),enemyHp,100+enemyLevel*20,Color.rgb(245,70,90));
            text(c,"VILÃO IA  LV "+enemyLevel,x,y-dp(93),dp(10),Color.WHITE,true,Paint.Align.CENTER);\n            if(System.currentTimeMillis()<aiThinkAt+900) text(c,aiLine,x,y-dp(110),dp(9),Color.rgb(255,220,130),true,Paint.Align.CENTER);
        }

        void drawParticles(Canvas c){for(Particle q:particles){int a=(int)Math.max(0,Math.min(255,q.life/.9f*255));p.setColor(Color.argb(a,q.kind==1?255:90,q.kind==1?175:220,q.kind==1?45:255));c.drawCircle(q.x,q.y,q.r,p);}}

        void drawHud(Canvas c){
            text(c,"ARENA HERO",dp(18),dp(32),dp(20),Color.WHITE,true,Paint.Align.LEFT);
            text(c,"LV "+level+"  •  ATQ "+damage+"  •  DEF "+defense,dp(18),dp(53),dp(11),Color.rgb(165,215,255),true,Paint.Align.LEFT);
            bar(c,dp(18),dp(63),w*.52f,dp(76),xp,level*100,Color.rgb(70,190,255));
            text(c,(int)xp+"/"+level*100+" XP",dp(24),dp(74),dp(9),Color.WHITE,false,Paint.Align.LEFT);
            bar(c,w*.57f,dp(63),w-dp(18),dp(76),playerHp,100,Color.rgb(60,220,130));
            text(c,"HP "+(int)playerHp,w-dp(24),dp(74),dp(9),Color.WHITE,true,Paint.Align.RIGHT);
            if(combo>1)text(c,"COMBO x"+combo,w/2,dp(105),dp(17),Color.rgb(255,215,75),true,Paint.Align.CENTER);
            float l=w*.60f,r=w-dp(18),t=dp(119),b=dp(134);
            bar(c,l,t,r,b,skillCharge,100,skillCharge>=100?Color.rgb(175,70,255):Color.rgb(90,75,190));
            text(c,skillCharge>=100?"ESPECIAL PRONTO":"ESPECIAL "+(int)skillCharge+"%",l,t-dp(5),dp(9),Color.WHITE,true,Paint.Align.LEFT);
            text(c,"VITÓRIAS "+kills,w-dp(20),dp(98),dp(10),Color.WHITE,true,Paint.Align.RIGHT);
        }

        void drawControls(Canvas c){
            float y=h-dp(82);
            button(c,dp(14),y,dp(76),y+dp(58),"◀",Color.rgb(45,58,85));
            button(c,dp(82),y,dp(144),y+dp(58),"▶",Color.rgb(45,58,85));
            button(c,dp(150),y,dp(222),y+dp(58),"⚡",skillCharge>=100?Color.rgb(130,55,235):Color.rgb(55,55,70));
            button(c,w-dp(150),y,w-dp(82),y+dp(58),"↗",Color.rgb(45,140,185));
            button(c,w-dp(74),y,w-dp(14),y+dp(58),"⚔",Color.rgb(210,60,75));
            button(c,w-dp(150),dp(16),w-dp(16),dp(61),paused?"▶":"Ⅱ",paused?Color.rgb(50,150,100):Color.rgb(65,75,100));
            text(c,"ESPECIAL",dp(186),y-dp(7),dp(8),Color.LTGRAY,true,Paint.Align.CENTER);
            text(c,"ESQUIVA",w-dp(116),y-dp(7),dp(9),Color.LTGRAY,false,Paint.Align.CENTER);
        }

        void drawPause(Canvas c){p.setColor(Color.argb(195,0,0,0));c.drawRect(0,0,w,h,p);text(c,"PAUSADO",w/2,h*.42f,dp(30),Color.WHITE,true,Paint.Align.CENTER);text(c,"Toque em ▶ para continuar",w/2,h*.49f,dp(15),Color.LTGRAY,false,Paint.Align.CENTER);}
        void drawGameOver(Canvas c){p.setColor(Color.argb(195,0,0,0));c.drawRect(0,0,w,h,p);text(c,"VOCÊ FOI DERROTADO",w/2,h*.40f,dp(27),Color.WHITE,true,Paint.Align.CENTER);text(c,"Toque na tela para recomeçar",w/2,h*.47f,dp(15),Color.LTGRAY,false,Paint.Align.CENTER);}
        void button(Canvas c,float l,float t,float r,float b,String s,int color){p.setColor(Color.argb(75,0,0,0));c.drawRoundRect(l+dp(2),t+dp(4),r+dp(2),b+dp(4),dp(18),dp(18),p);p.setColor(color);c.drawRoundRect(l,t,r,b,dp(18),dp(18),p);text(c,s,(l+r)/2,(t+b)/2+dp(8),s.length()>3?dp(11):dp(25),Color.WHITE,true,Paint.Align.CENTER);}
        void bar(Canvas c,float l,float t,float r,float b,float value,float max,int color){p.setColor(Color.argb(100,0,0,0));c.drawRoundRect(l,t,r,b,dp(8),dp(8),p);float f=max<=0?0:Math.max(0,Math.min(1,value/max));p.setColor(color);c.drawRoundRect(l,t,l+(r-l)*f,b,dp(8),dp(8),p);}
        void text(Canvas c,String s,float x,float y,float size,int color,boolean bold,Paint.Align align){p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(color);p.setTextSize(size);p.setTextAlign(align);p.setTypeface(Typeface.create("sans",bold?Typeface.BOLD:Typeface.NORMAL));c.drawText(s,x,y,p);}

        void burst(float x,float y,int n){for(int i=0;i<n;i++){double a=rnd.nextDouble()*Math.PI*2;float v=dp(70+rnd.nextInt(190));Particle q=new Particle();q.x=x;q.y=y;q.vx=(float)Math.cos(a)*v;q.vy=(float)Math.sin(a)*v-dp(80);q.life=.4f+rnd.nextFloat()*.55f;q.r=dp(2+rnd.nextInt(4));q.kind=rnd.nextBoolean()?1:0;particles.add(q);}}
        
        @Override public boolean onTouchEvent(MotionEvent e){
            if(e.getAction()!=MotionEvent.ACTION_DOWN)return true;
            float x=e.getX(),y=e.getY();
            if(gameOver){reset();return true;}
            if(x>w-dp(150)&&y<dp(75)){paused=!paused;return true;}
            if(paused)return true;
            float by=h-dp(82);
            if(y>=by){
                if(x<dp(82))move(-1);
                else if(x<dp(150))move(1);
                else if(x<dp(225))useSkill();
                else if(x>w-dp(150)&&x<w-dp(74))dodge();
                else if(x>=w-dp(74))attack();
            }
            return true;
        }

        void attack(){
            long now=System.currentTimeMillis();
            if(now-lastAttack<250||dodging)return;
            combo=(now-lastAttack<900)?Math.min(4,combo+1):1;lastAttack=now;attacking=true;attackUntil=now+240;
            if(Math.abs(playerX-enemyX)<dp(205)){
                int hit=damage+(combo-1)*6;boolean crit=combo==4&&rnd.nextFloat()<.4f;if(crit)hit*=2;
                enemyHp-=hit;burst(enemyX,h*.48f,crit?40:20);shake=crit?.95f:.55f;
                if(enemyHp<=0)defeatEnemy(now);
            }
        }
        void dodge(){long now=System.currentTimeMillis();if(now<dodgeUntil)return;dodging=true;dodgeUntil=now+520;playerX=Math.min(w*.56f,playerX+dp(60));burst(playerX,h*.48f,20);}
        void move(float dir){if(gameOver||dodging)return;playerX=Math.max(dp(65),Math.min(w*.56f,playerX+dir*dp(55)));}
        void useSkill(){if(skillCharge<100)return;skillCharge=0; aiLine="IA: especial detectado!";skillFlashUntil=System.currentTimeMillis()+350;enemyHp-=damage*4.5f;burst(enemyX,h*.48f,90);shake=1.2f;if(enemyHp<=0)defeatEnemy(System.currentTimeMillis());}
        void reset(){playerHp=100;xp=0;skillCharge=0;level=1;enemyLevel=1;damage=20;defense=3;kills=0;combo=0;lastAttack=0;dodgeUntil=0;enemyAttackAt=System.currentTimeMillis()+900;enemyTelegraphUntil=0;enemyHp=100;aiMood=0;aiAggression=1;aiLine="IA: analisando você...";aiThinkAt=System.currentTimeMillis()+700;aiDodgeUntil=0;playerX=w*.28f;enemyX=w*.72f;gameOver=false;paused=false;particles.clear();}

        static class Particle{float x,y,vx,vy,life,r;int kind;}
    }
}