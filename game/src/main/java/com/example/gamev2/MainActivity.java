package com.example.gamev2;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.content.Context;
import java.util.*;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(8, 10, 18));
        getWindow().setNavigationBarColor(Color.rgb(8, 10, 18));
        setContentView(new ArenaView(this));
    }

    static class ArenaView extends View {
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Random rnd = new Random();
        final ArrayList<Particle> particles = new ArrayList<>();
        float d, w, h, playerX, enemyX, enemyY;
        float playerHp=100, enemyHp=100, xp=0, shake=0;
        int level=1, enemyLevel=1, damage=18, defense=0, kills=0;
        boolean attacking=false, skillReady=false, gameOver=false;
        long attackUntil=0, lastFrame=0, enemyHitAt=0, skillUntil=0;
        int flash=0;

        ArenaView(Context c) {
            super(c);
            d=getResources().getDisplayMetrics().density;
            p.setTypeface(Typeface.create("sans", Typeface.NORMAL));
            setFocusable(true);
        }

        float dp(float v){ return v*d; }

        @Override protected void onSizeChanged(int sw,int sh,int ow,int oh){
            w=sw; h=sh; playerX=w*.30f; enemyX=w*.72f; enemyY=h*.48f;
        }

        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            long now=System.currentTimeMillis();
            if(lastFrame==0) lastFrame=now;
            float dt=Math.min(.04f,(now-lastFrame)/1000f);
            lastFrame=now;
            update(dt, now);

            c.save();
            if(shake>0){
                float s=dp(5)*shake;
                c.translate((rnd.nextFloat()*2-1)*s,(rnd.nextFloat()*2-1)*s);
                shake=Math.max(0,shake-dt*5);
            }
            drawBackground(c);
            drawArena(c);
            drawEnemy(c);
            drawPlayer(c);
            drawParticles(c);
            drawHud(c);
            drawControls(c);
            if(gameOver) drawGameOver(c);
            c.restore();
            postInvalidateDelayed(16);
        }

        void update(float dt,long now){
            if(gameOver) return;

            if(now>attackUntil) attacking=false;
            if(now>skillUntil) skillReady = level>=2;

            float dist=playerX-enemyX;
            if(Math.abs(dist)>dp(105)) enemyX += Math.signum(dist)*dp(42)*dt;
            if(Math.abs(dist)<=dp(110) && now-enemyHitAt>1150){
                enemyHitAt=now;
                playerHp-=Math.max(5,12+enemyLevel*2-defense);
                flash=10; shake=.6f; burst(playerX,h*.48f,8);
                if(playerHp<=0){ playerHp=0; gameOver=true; }
            }
            if(attacking && now+40>=attackUntil){
                float range=Math.abs(playerX-enemyX);
                if(range<dp(190) && now-enemyHitAt>0){
                    enemyHp-=damage;
                    enemyHitAt=now+350;
                    burst(enemyX,enemyY,12);
                    shake=.45f;
                    if(enemyHp<=0) defeatEnemy(now);
                }
            }
            for(Particle q:particles){ q.x+=q.vx*dt; q.y+=q.vy*dt; q.life-=dt; q.vy+=dp(180)*dt; }
            particles.removeIf(q->q.life<=0);
            if(flash>0) flash--;
        }

        void defeatEnemy(long now){
            kills++;
            float gain=35+enemyLevel*8;
            xp+=gain;
            burst(enemyX,enemyY,28);
            enemyLevel++;
            enemyHp=100+enemyLevel*18;
            enemyX=w*.72f;
            if(xp>=level*100){
                xp-=level*100; level++;
                damage+=6; defense+=2;
                if(level>=2) skillReady=true;
                burst(playerX,h*.45f,40);
            }
        }

        void drawBackground(Canvas c){
            LinearGradient g=new LinearGradient(0,0,0,h,
                Color.rgb(12,16,32),Color.rgb(35,20,48),Shader.TileMode.CLAMP);
            p.setShader(g); c.drawRect(0,0,w,h,p); p.setShader(null);
            p.setColor(Color.argb(35,120,150,255));
            for(int i=0;i<18;i++){
                float x=(i*97)%w, y=(i*137)%h;
                c.drawCircle(x,y,dp(2+i%3),p);
            }
        }

        void drawArena(Canvas c){
            float ground=h*.64f;
            p.setShader(new LinearGradient(0,ground,0,h,Color.rgb(25,29,45),Color.rgb(8,10,18),Shader.TileMode.CLAMP));
            c.drawRect(0,ground,w,h,p); p.setShader(null);
            p.setColor(Color.argb(70,150,120,220));
            c.drawOval(w*.08f,ground-dp(20),w*.92f,ground+dp(85),p);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(2));
            p.setColor(Color.argb(80,220,200,255));
            c.drawOval(w*.12f,ground-dp(5),w*.88f,ground+dp(60),p);
            p.setStyle(Paint.Style.FILL);
        }

        void drawPlayer(Canvas c){
            float y=h*.48f, bob=(float)Math.sin(System.currentTimeMillis()/150.0)*dp(3);
            float x=playerX;
            p.setColor(Color.argb(90,0,0,0)); c.drawOval(x-dp(42),y+dp(48),x+dp(42),y+dp(62),p);
            p.setColor(Color.rgb(60,115,245)); c.drawRoundRect(x-dp(26),y-dp(8)+bob,x+dp(26),y+dp(48)+bob,dp(16),dp(16),p);
            p.setColor(Color.rgb(245,197,150)); c.drawCircle(x,y-dp(32)+bob,dp(24),p);
            p.setColor(Color.rgb(35,35,45)); c.drawArc(x-dp(24),y-dp(55)+bob,x+dp(24),y-dp(18)+bob,180,180,true,p);
            p.setColor(Color.WHITE); c.drawCircle(x-dp(8),y-dp(34)+bob,dp(3),p); c.drawCircle(x+dp(8),y-dp(34)+bob,dp(3),p);
            if(attacking){
                p.setColor(Color.argb(180,100,210,255));
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(9));
                c.drawArc(x+dp(5),y-dp(42),x+dp(125),y+dp(45),-65,120,false,p);
                p.setStyle(Paint.Style.FILL);
            }
        }

        void drawEnemy(Canvas c){
            float y=enemyY, x=enemyX;
            p.setColor(Color.argb(80,0,0,0)); c.drawOval(x-dp(45),y+dp(45),x+dp(45),y+dp(60),p);
            p.setColor(Color.rgb(180,55,70)); c.drawRoundRect(x-dp(28),y-dp(5),x+dp(28),y+dp(48),dp(14),dp(14),p);
            p.setColor(Color.rgb(90,35,50)); c.drawCircle(x,y-dp(30),dp(25),p);
            p.setColor(Color.rgb(255,215,70)); c.drawCircle(x-dp(9),y-dp(32),dp(4),p); c.drawCircle(x+dp(9),y-dp(32),dp(4),p);
            p.setColor(Color.rgb(55,15,25)); c.drawRect(x-dp(18),y-dp(52),x+dp(18),y-dp(35),p);
            bar(c,x-dp(48),y-dp(78),x+dp(48),y-dp(69),enemyHp,(100+enemyLevel*18),Color.rgb(245,75,90));
            text(c,"LV "+enemyLevel,x,y-dp(88),dp(12),Color.WHITE,true,Paint.Align.CENTER);
        }

        void drawHud(Canvas c){
            text(c,"ARENA HERO",dp(18),dp(32),dp(20),Color.WHITE,true,Paint.Align.LEFT);
            text(c,"NÍVEL "+level,dp(18),dp(57),dp(13),Color.rgb(150,210,255),true,Paint.Align.LEFT);
            bar(c,dp(18),dp(67),w*.52f,dp(80),xp,level*100,Color.rgb(80,190,255));
            text(c,(int)xp+" / "+level*100+" XP",dp(25),dp(78),dp(10),Color.WHITE,false,Paint.Align.LEFT);

            bar(c,w*.57f,dp(67),w-dp(18),dp(80),playerHp,100,Color.rgb(65,220,130));
            text(c,"HP "+(int)playerHp,w-dp(25),dp(78),dp(10),Color.WHITE,true,Paint.Align.RIGHT);
            text(c,"ATQ "+damage+"   DEF "+defense+"   VITÓRIAS "+kills,w/2,dp(105),dp(11),Color.WHITE,true,Paint.Align.CENTER);
        }

        void drawControls(Canvas c){
            float y=h-dp(82);
            button(c,dp(28),y,dp(88),y+dp(58),"◀",Color.rgb(50,60,85));
            button(c,dp(126),y,dp(186),y+dp(58),"⚡",skillReady?Color.rgb(100,55,210):Color.rgb(55,55,70));
            button(c,w-dp(116),y,w-dp(28),y+dp(58),"ATACAR",Color.rgb(210,65,80));
            text(c,level>=2?"HABILIDADE PRONTA":"DESBLOQUEIA NO LV 2",dp(156),y-dp(8),dp(9),Color.LTGRAY,false,Paint.Align.CENTER);
        }

        void drawGameOver(Canvas c){
            p.setColor(Color.argb(190,0,0,0)); c.drawRect(0,0,w,h,p);
            text(c,"VOCÊ FOI DERROTADO",w/2,h*.40f,dp(27),Color.WHITE,true,Paint.Align.CENTER);
            text(c,"Toque para recomeçar",w/2,h*.47f,dp(15),Color.rgb(180,200,220),false,Paint.Align.CENTER);
        }

        void button(Canvas c,float l,float t,float r,float b,String s,int color){
            p.setColor(Color.argb(65,0,0,0)); c.drawRoundRect(l+dp(2),t+dp(4),r+dp(2),b+dp(4),dp(18),dp(18),p);
            p.setColor(color); c.drawRoundRect(l,t,r,b,dp(18),dp(18),p);
            text(c,s,(l+r)/2,(t+b)/2+dp(7),s.length()>3?dp(12):dp(25),Color.WHITE,true,Paint.Align.CENTER);
        }

        void bar(Canvas c,float l,float t,float r,float b,float value,float max,int color){
            p.setColor(Color.argb(100,0,0,0)); c.drawRoundRect(l,t,r,b,dp(8),dp(8),p);
            float f=Math.max(0,Math.min(1,value/max));
            p.setColor(color); c.drawRoundRect(l,t,l+(r-l)*f,b,dp(8),dp(8),p);
        }

        void text(Canvas c,String s,float x,float y,float size,int color,boolean bold,Paint.Align align){
            p.setShader(null); p.setStyle(Paint.Style.FILL); p.setColor(color); p.setTextSize(size);
            p.setTextAlign(align); p.setTypeface(Typeface.create("sans",bold?Typeface.BOLD:Typeface.NORMAL));
            c.drawText(s,x,y,p);
        }

        void burst(float x,float y,int n){
            for(int i=0;i<n;i++){
                double a=rnd.nextDouble()*Math.PI*2;
                float v=dp(60+rnd.nextInt(160));
                Particle q=new Particle();
                q.x=x; q.y=y; q.vx=(float)Math.cos(a)*v; q.vy=(float)Math.sin(a)*v-dp(60);
                q.life=.35f+rnd.nextFloat()*.5f; q.r=dp(2+rnd.nextInt(4));
                particles.add(q);
            }
        }

        @Override public boolean onTouchEvent(android.view.MotionEvent e){
            if(e.getAction()!=MotionEvent.ACTION_DOWN && e.getAction()!=MotionEvent.ACTION_UP) return true;
            if(e.getAction()==MotionEvent.ACTION_DOWN){
                if(gameOver){ reset(); return true; }
                float x=e.getX(), y=e.getY();
                float by=h-dp(82);
                if(y>=by){
                    if(x<dp(120)){ playerX=Math.max(dp(70),playerX-dp(55)); }
                    else if(x<dp(320) && level>=2){ useSkill(); }
                    else if(x>w-dp(135)){ attack(); }
                }
            }
            return true;
        }

        void attack(){
            attacking=true; attackUntil=System.currentTimeMillis()+220;
            if(Math.abs(playerX-enemyX)<dp(190)){
                enemyHp-=damage;
                burst(enemyX,enemyY,10); shake=.35f;
                if(enemyHp<=0) defeatEnemy(System.currentTimeMillis());
            }
        }

        void useSkill(){
            skillReady=false; skillUntil=System.currentTimeMillis()+900;
            enemyHp-=damage*2.5f;
            burst(enemyX,enemyY,35); shake=.9f;
            if(enemyHp<=0) defeatEnemy(System.currentTimeMillis());
        }

        void reset(){
            playerHp=100; xp=0; level=1; enemyLevel=1; damage=18; defense=0; kills=0;
            enemyHp=100; playerX=w*.30f; enemyX=w*.72f; gameOver=false; particles.clear();
        }

        static class Particle {
            float x,y,vx,vy,life,r;
        }
    }
}
