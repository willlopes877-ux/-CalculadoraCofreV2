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
        getWindow().setStatusBarColor(Color.rgb(8,10,18));
        getWindow().setNavigationBarColor(Color.rgb(8,10,18));
        setContentView(new ArenaView(this));
    }

    static class ArenaView extends View {
        final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        final Random rnd=new Random();
        final ArrayList<Particle> particles=new ArrayList<>();
        float d,w,h,playerX,enemyX,enemyY,playerHp=100,enemyHp=100,xp=0,shake=0,skillCharge=0;
        int level=1,enemyLevel=1,damage=18,defense=2,kills=0,combo=0;
        long lastAttack=0,dodgeUntil=0,enemyAttackAt=0,enemyTelegraphUntil=0,attackUntil=0,lastFrame=0;
        boolean dodging=false,enemyWarning=false,attacking=false,paused=false,gameOver=false,playerHit=false;

        ArenaView(Context c){super(c);d=getResources().getDisplayMetrics().density;setFocusable(true);}
        float dp(float v){return v*d;}

        @Override protected void onSizeChanged(int sw,int sh,int ow,int oh){w=sw;h=sh;playerX=w*.30f;enemyX=w*.72f;enemyY=h*.48f;}

        @Override protected void onDraw(Canvas c){
            super.onDraw(c); long now=System.currentTimeMillis();
            if(lastFrame==0)lastFrame=now;
            float dt=Math.min(.04f,(now-lastFrame)/1000f); lastFrame=now;
            if(!paused&&!gameOver)update(dt,now);
            c.save();
            if(shake>0){float s=dp(6)*shake;c.translate((rnd.nextFloat()*2-1)*s,(rnd.nextFloat()*2-1)*s);shake=Math.max(0,shake-dt*4);}
            drawBackground(c);drawArena(c);drawEnemy(c);drawPlayer(c);drawParticles(c);drawHud(c);drawControls(c);
            if(paused)drawPause(c);
            if(gameOver)drawGameOver(c);
            c.restore();postInvalidateDelayed(16);
        }

        void update(float dt,long now){
            if(now>attackUntil)attacking=false;
            if(now>dodgeUntil)dodging=false;
            enemyWarning=enemyTelegraphUntil>now;
            skillCharge=Math.min(100,skillCharge+dt*(7+level*.7f));
            float dist=playerX-enemyX;
            if(Math.abs(dist)>dp(125))enemyX+=Math.signum(dist)*dp(38)*dt;
            if(Math.abs(dist)<=dp(155)&&now>enemyAttackAt){enemyAttackAt=now+1450-Math.min(350,enemyLevel*25);enemyTelegraphUntil=now+520;}
            if(enemyTelegraphUntil>0&&enemyTelegraphUntil<=now){
                enemyTelegraphUntil=0;
                if(!dodging){playerHp-=Math.max(5,12+enemyLevel*2-defense);playerHit=true;burst(playerX,h*.48f,20);shake=.55f;if(playerHp<=0){playerHp=0;gameOver=true;}}
            }
            for(Particle q:particles){q.x+=q.vx*dt;q.y+=q.vy*dt;q.life-=dt;q.vy+=dp(180)*dt;}
            particles.removeIf(q->q.life<=0);
            if(playerHit&&rnd.nextInt(4)==0)playerHit=false;
        }

        void defeatEnemy(long now){
            kills++;xp+=42+enemyLevel*10;burst(enemyX,enemyY,42);enemyLevel++;
            enemyHp=100+enemyLevel*18;enemyX=w*.72f;combo=0;
            if(xp>=level*100){xp-=level*100;level++;damage+=6;defense+=2;skillCharge=Math.min(100,skillCharge+35);burst(playerX,h*.45f,55);}
        }

        void drawBackground(Canvas c){
            p.setShader(new LinearGradient(0,0,0,h,Color.rgb(8,14,30),Color.rgb(46,18,55),Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);
            p.setColor(Color.argb(45,100,160,255));for(int i=0;i<24;i++)c.drawCircle((i*97)%w,(i*137)%h,dp(2+i%3),p);
        }
        void drawArena(Canvas c){
            float ground=h*.64f;
            p.setShader(new LinearGradient(0,ground,0,h,Color.rgb(34,38,58),Color.rgb(8,10,18),Shader.TileMode.CLAMP));c.drawRect(0,ground,w,h,p);p.setShader(null);
            p.setColor(Color.argb(75,170,130,220));c.drawOval(w*.06f,ground-dp(18),w*.94f,ground+dp(90),p);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2));p.setColor(Color.argb(100,220,210,255));c.drawOval(w*.10f,ground-dp(4),w*.90f,ground+dp(62),p);p.setStyle(Paint.Style.FILL);
        }

        void drawPlayer(Canvas c){
            float now=System.currentTimeMillis(),y=h*.48f,bob=(float)Math.sin(now/120.0)*dp(3),x=playerX;
            if(dodging)bob-=dp(12);
            p.setColor(Color.argb(90,0,0,0));c.drawOval(x-dp(43),y+dp(48),x+dp(43),y+dp(62),p);
            p.setColor(playerHit?Color.rgb(255,70,70):Color.rgb(55,125,255));c.drawRoundRect(x-dp(27),y-dp(8)+bob,x+dp(27),y+dp(48)+bob,dp(16),dp(16),p);
            p.setColor(Color.rgb(245,197,150));c.drawCircle(x,y-dp(32)+bob,dp(24),p);
            p.setColor(Color.rgb(35,35,45));c.drawArc(x-dp(24),y-dp(55)+bob,x+dp(24),y-dp(18)+bob,180,180,true,p);
            p.setColor(Color.WHITE);c.drawCircle(x-dp(8),y-dp(34)+bob,dp(3),p);c.drawCircle(x+dp(8),y-dp(34)+bob,dp(3),p);
            float legKick=attacking?(float)Math.sin((now%230)/230.0*Math.PI)*dp(34):0;
            p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeWidth(dp(11));p.setColor(Color.rgb(48,75,145));
            if(attacking){c.drawLine(x-dp(10),y+dp(42),x+dp(18)+legKick,y+dp(32),p);c.drawLine(x+dp(8),y+dp(42),x+dp(48)+legKick,y+dp(20),p);}
            else{c.drawLine(x-dp(10),y+dp(42),x-dp(18),y+dp(63),p);c.drawLine(x+dp(10),y+dp(42),x+dp(18),y+dp(63),p);}
            p.setStrokeWidth(dp(7));p.setColor(Color.rgb(25,25,35));
            if(attacking)c.drawLine(x+dp(48)+legKick,y+dp(20),x+dp(70)+legKick,y+dp(20),p);
            else{c.drawLine(x-dp(18),y+dp(63),x-dp(4),y+dp(63),p);c.drawLine(x+dp(18),y+dp(63),x+dp(32),y+dp(63),p);}
            if(attacking){p.setColor(Color.argb(210,90,210,255));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(8));c.drawArc(x+dp(5),y-dp(42),x+dp(130),y+dp(55),-70,125,false,p);p.setStyle(Paint.Style.FILL);}
            p.setStrokeCap(Paint.Cap.BUTT);
        }

        void drawEnemy(Canvas c){
            float y=enemyY,x=enemyX;
            p.setColor(Color.argb(80,0,0,0));c.drawOval(x-dp(45),y+dp(45),x+dp(45),y+dp(60),p);
            p.setColor(Color.rgb(180,55,70));c.drawRoundRect(x-dp(28),y-dp(5),x+dp(28),y+dp(48),dp(14),dp(14),p);
            p.setColor(Color.rgb(90,35,50));c.drawCircle(x,y-dp(30),dp(25),p);
            p.setColor(Color.rgb(255,215,70));c.drawCircle(x-dp(9),y-dp(32),dp(4),p);c.drawCircle(x+dp(9),y-dp(32),dp(4),p);
            p.setColor(Color.rgb(55,15,25));c.drawRect(x-dp(18),y-dp(52),x+dp(18),y-dp(35),p);
            if(enemyWarning){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(5));p.setColor(Color.rgb(255,75,70));c.drawCircle(x,y,dp(62),p);p.setStyle(Paint.Style.FILL);text(c,"!",x,y-dp(70),dp(24),Color.rgb(255,90,80),true,Paint.Align.CENTER);}
            bar(c,x-dp(50),y-dp(80),x+dp(50),y-dp(69),enemyHp,100+enemyLevel*18,Color.rgb(245,75,90));
            text(c,"LV "+enemyLevel,x,y-dp(91),dp(12),Color.WHITE,true,Paint.Align.CENTER);
        }

        void drawParticles(Canvas c){for(Particle q:particles){int a=(int)Math.max(0,Math.min(255,q.life/.85f*255));p.setColor(Color.argb(a,q.kind==1?255:100,q.kind==1?170:210,q.kind==1?50:255));c.drawCircle(q.x,q.y,q.r,p);}}
        void drawHud(Canvas c){
            text(c,"ARENA HERO",dp(18),dp(32),dp(20),Color.WHITE,true,Paint.Align.LEFT);
            text(c,"NÍVEL "+level,dp(18),dp(57),dp(13),Color.rgb(150,210,255),true,Paint.Align.LEFT);
            bar(c,dp(18),dp(67),w*.52f,dp(80),xp,level*100,Color.rgb(80,190,255));
            text(c,(int)xp+" / "+level*100+" XP",dp(25),dp(78),dp(10),Color.WHITE,false,Paint.Align.LEFT);
            bar(c,w*.57f,dp(67),w-dp(18),dp(80),playerHp,100,Color.rgb(65,220,130));
            text(c,"HP "+(int)playerHp,w-dp(25),dp(78),dp(10),Color.WHITE,true,Paint.Align.RIGHT);
            text(c,"ATQ "+damage+"  DEF "+defense+"  VITÓRIAS "+kills,w/2,dp(105),dp(11),Color.WHITE,true,Paint.Align.CENTER);
            if(combo>1)text(c,"COMBO x"+combo,w/2,dp(127),dp(15),Color.rgb(255,210,80),true,Paint.Align.CENTER);
            float l=w*.60f,r=w-dp(18),t=dp(137),b=dp(151);
            bar(c,l,t,r,b,skillCharge,100,skillCharge>=100?Color.rgb(170,80,255):Color.rgb(100,75,190));
            text(c,skillCharge>=100?"HABILIDADE PRONTA":"HABILIDADE "+(int)skillCharge+"%",l,t-dp(5),dp(9),Color.WHITE,true,Paint.Align.LEFT);
        }

        void drawControls(Canvas c){
            float y=h-dp(82);
            button(c,dp(16),y,dp(78),y+dp(58),"◀",Color.rgb(48,59,83));
            button(c,dp(82),y,dp(144),y+dp(58),"▶",Color.rgb(48,59,83));
            button(c,dp(148),y,dp(220),y+dp(58),"⚡",skillCharge>=100?Color.rgb(125,55,230):Color.rgb(55,55,70));
            button(c,w-dp(150),y,w-dp(82),y+dp(58),"↗",Color.rgb(45,135,180));
            button(c,w-dp(74),y,w-dp(16),y+dp(58),"⚔",Color.rgb(210,62,78));
            button(c,w-dp(150),dp(18),w-dp(18),dp(62),paused?"▶":"Ⅱ",paused?Color.rgb(50,150,100):Color.rgb(65,75,100));
            text(c,skillCharge>=100?"ESPECIAL":"CARREGANDO",dp(184),y-dp(7),dp(8),Color.LTGRAY,true,Paint.Align.CENTER);
            text(c,"ESQUIVA",w-dp(116),y-dp(7),dp(9),Color.LTGRAY,false,Paint.Align.CENTER);
        }

        void drawPause(Canvas c){p.setColor(Color.argb(190,0,0,0));c.drawRect(0,0,w,h,p);text(c,"PAUSADO",w/2,h*.43f,dp(30),Color.WHITE,true,Paint.Align.CENTER);text(c,"Toque em ▶ para continuar",w/2,h*.50f,dp(15),Color.rgb(190,205,220),false,Paint.Align.CENTER);}
        void drawGameOver(Canvas c){p.setColor(Color.argb(190,0,0,0));c.drawRect(0,0,w,h,p);text(c,"VOCÊ FOI DERROTADO",w/2,h*.40f,dp(27),Color.WHITE,true,Paint.Align.CENTER);text(c,"Toque para recomeçar",w/2,h*.47f,dp(15),Color.rgb(180,200,220),false,Paint.Align.CENTER);}
        void button(Canvas c,float l,float t,float r,float b,String s,int color){p.setColor(Color.argb(65,0,0,0));c.drawRoundRect(l+dp(2),t+dp(4),r+dp(2),b+dp(4),dp(18),dp(18),p);p.setColor(color);c.drawRoundRect(l,t,r,b,dp(18),dp(18),p);text(c,s,(l+r)/2,(t+b)/2+dp(7),s.length()>3?dp(11):dp(25),Color.WHITE,true,Paint.Align.CENTER);}
        void bar(Canvas c,float l,float t,float r,float b,float value,float max,int color){p.setColor(Color.argb(100,0,0,0));c.drawRoundRect(l,t,r,b,dp(8),dp(8),p);float f=Math.max(0,Math.min(1,value/max));p.setColor(color);c.drawRoundRect(l,t,l+(r-l)*f,b,dp(8),dp(8),p);}
        void text(Canvas c,String s,float x,float y,float size,int color,boolean bold,Paint.Align align){p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(color);p.setTextSize(size);p.setTextAlign(align);p.setTypeface(Typeface.create("sans",bold?Typeface.BOLD:Typeface.NORMAL));c.drawText(s,x,y,p);}

        void burst(float x,float y,int n){for(int i=0;i<n;i++){double a=rnd.nextDouble()*Math.PI*2;float v=dp(60+rnd.nextInt(180));Particle q=new Particle();q.x=x;q.y=y;q.vx=(float)Math.cos(a)*v;q.vy=(float)Math.sin(a)*v-dp(70);q.life=.35f+rnd.nextFloat()*.55f;q.r=dp(2+rnd.nextInt(4));q.kind=rnd.nextBoolean()?1:0;particles.add(q);}}
        
        @Override public boolean onTouchEvent(MotionEvent e){
            if(e.getAction()!=MotionEvent.ACTION_DOWN)return true;
            float x=e.getX(),y=e.getY();
            if(gameOver){reset();return true;}
            if(x>w-dp(150)&&y<dp(75)){paused=!paused;return true;}
            if(paused)return true;
            float by=h-dp(82);
            if(y>=by){
                if(x<dp(82))move(-1);
                else if(x<dp(148))move(1);
                else if(x<dp(225))useSkill();
                else if(x>w-dp(150)&&x<w-dp(74))dodge();
                else if(x>=w-dp(74))attack();
            }
            return true;
        }

        void attack(){
            long now=System.currentTimeMillis();if(now-lastAttack<250||dodging)return;
            combo=(now-lastAttack<900)?Math.min(4,combo+1):1;lastAttack=now;attacking=true;attackUntil=now+230;
            if(Math.abs(playerX-enemyX)<dp(200)){
                int hit=damage+(combo-1)*5;boolean crit=combo==4&&rnd.nextFloat()<.35f;if(crit)hit*=2;
                enemyHp-=hit;burst(enemyX,enemyY,crit?34:18);shake=crit?.9f:.5f;
                if(enemyHp<=0)defeatEnemy(now);
            }
        }
        void dodge(){long now=System.currentTimeMillis();if(now<dodgeUntil)return;dodging=true;dodgeUntil=now+520;playerX=Math.min(w*.55f,playerX+dp(55));burst(playerX,h*.48f,18);}
        void move(float dir){if(gameOver||dodging)return;playerX=Math.max(dp(65),Math.min(w*.55f,playerX+dir*dp(55)));}
        void useSkill(){if(skillCharge<100)return;skillCharge=0;enemyHp-=damage*3.5f;burst(enemyX,enemyY,65);shake=1f;if(enemyHp<=0)defeatEnemy(System.currentTimeMillis());}
        void reset(){playerHp=100;xp=0;skillCharge=0;level=1;enemyLevel=1;damage=18;defense=2;kills=0;combo=0;lastAttack=0;dodgeUntil=0;enemyAttackAt=System.currentTimeMillis()+900;enemyTelegraphUntil=0;enemyHp=100;playerX=w*.30f;enemyX=w*.72f;gameOver=false;paused=false;particles.clear();}

        static class Particle{float x,y,vx,vy,life,r;int kind;}
    }
}