package com.example.calculatorvault;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import android.content.Context;
import java.util.*;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        setContentView(new GameView(this));
    }

    static class GameView extends View {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        Random random = new Random();
        int screen=0, level=1, xp=0, maxHp=100, hp=100, enemiesDefeated=0;
        float playerX=540, playerY=1120;
        boolean left=false,right=false, attacking=false, skillReady=true, boss=false, victory=false;
        long attackUntil=0, skillUntil=0, lastSpawn=0, lastFrame=0;
        ArrayList<Enemy> enemies=new ArrayList<>();
        ArrayList<Particle> particles=new ArrayList<>();

        GameView(Context c){ super(c); p.setTypeface(Typeface.create("sans",Typeface.BOLD)); }

        protected void onDraw(Canvas c){
            super.onDraw(c);
            float sx=getWidth()/1080f, sy=getHeight()/1920f;
            c.save(); c.scale(sx,sy);
            if(screen==0) drawMenu(c); else drawGame(c);
            c.restore();
            invalidate();
        }

        void txt(Canvas c,String s,float x,float y,float size,int color,Paint.Align align){
            p.setTextSize(size); p.setColor(color); p.setTextAlign(align); p.setStyle(Paint.Style.FILL); c.drawText(s,x,y,p);
        }
        void rect(Canvas c,int color,float l,float t,float r,float b,float rad){
            p.setColor(color); p.setStyle(Paint.Style.FILL); c.drawRoundRect(l,t,r,b,rad,rad,p);
        }

        void drawMenu(Canvas c){
            p.setShader(new LinearGradient(0,0,1080,1920,Color.rgb(8,8,16),Color.rgb(35,10,45),Shader.TileMode.CLAMP));
            c.drawRect(0,0,1080,1920,p); p.setShader(null);
            txt(c,"ASCENSION",540,520,104,Color.WHITE,Paint.Align.CENTER);
            txt(c,"LORD OF SHADOWS",540,625,62,Color.rgb(190,130,255),Paint.Align.CENTER);
            txt(c,"A DARK RPG OF POWER AND EVOLUTION",540,705,25,Color.LTGRAY,Paint.Align.CENTER);
            p.setColor(Color.rgb(120,45,180)); c.drawCircle(540,920,150,p);
            txt(c,"☠",540,985,150,Color.BLACK,Paint.Align.CENTER);
            rect(c,Color.rgb(110,35,170),260,1250,820,1400,35);
            txt(c,"START ASCENSION",540,1345,38,Color.WHITE,Paint.Align.CENTER);
            txt(c,"V1 • THE FIRST AWAKENING",540,1510,24,Color.GRAY,Paint.Align.CENTER);
        }

        void drawGame(Canvas c){
            long now=System.currentTimeMillis();
            p.setShader(new LinearGradient(0,0,0,1920,Color.rgb(12,12,22),Color.rgb(28,14,35),Shader.TileMode.CLAMP));
            c.drawRect(0,0,1080,1920,p); p.setShader(null);
            p.setColor(Color.rgb(48,38,60)); c.drawRect(0,820,1080,1500,p);
            txt(c,"AREA 1 — SHADOW OUTSKIRTS",40,70,30,Color.LTGRAY,Paint.Align.LEFT);
            txt(c,"LV "+level,40,125,42,Color.WHITE,Paint.Align.LEFT);
            p.setColor(Color.DKGRAY);c.drawRoundRect(40,150,420,178,14,14,p);
            p.setColor(Color.rgb(55,205,105));c.drawRoundRect(40,150,40+380*hp/(float)maxHp,178,14,14,p);
            txt(c,hp+" / "+maxHp,230,174,20,Color.WHITE,Paint.Align.CENTER);
            p.setColor(Color.DKGRAY);c.drawRoundRect(40,195,500,218,12,12,p);
            p.setColor(Color.rgb(170,95,240));c.drawRoundRect(40,195,40+460*xp/(float)(level*100),218,12,12,p);
            txt(c,"XP "+xp+" / "+(level*100),520,218,20,Color.LTGRAY,Paint.Align.LEFT);
            drawPlayer(c);
            for(Enemy e:enemies) drawEnemy(c,e);
            for(Particle q:particles){p.setColor(q.color);c.drawCircle(q.x,q.y,q.r,p);}
            if(boss) txt(c,"BOSS",540,350,32,Color.rgb(255,90,110),Paint.Align.CENTER);
            rect(c,Color.argb(150,20,20,30),45,1580,250,1785,28);
            rect(c,Color.argb(150,20,20,30),270,1580,475,1785,28);
            txt(c,"◀",148,1710,85,Color.WHITE,Paint.Align.CENTER);
            txt(c,"▶",372,1710,85,Color.WHITE,Paint.Align.CENTER);
            rect(c,Color.rgb(125,35,170),720,1570,900,1750,90);
            txt(c,"⚔",810,1685,75,Color.WHITE,Paint.Align.CENTER);
            rect(c,Color.rgb(45,95,170),905,1620,1045,1760,70);
            txt(c,"✦",975,1710,55,Color.WHITE,Paint.Align.CENTER);
            if(victory){
                p.setColor(Color.argb(235,5,5,12));c.drawRect(0,0,1080,1920,p);
                txt(c,"BOSS DEFEATED",540,700,58,Color.WHITE,Paint.Align.CENTER);
                txt(c,"NEW CHARACTER UNLOCKED",540,790,32,Color.rgb(205,140,255),Paint.Align.CENTER);
                txt(c,"SHADOW WARDEN",540,870,48,Color.WHITE,Paint.Align.CENTER);
                rect(c,Color.rgb(110,35,170),260,1040,820,1190,30);
                txt(c,"CONTINUE",540,1135,36,Color.WHITE,Paint.Align.CENTER);
            }
            update(now);
        }

        void drawPlayer(Canvas c){
            p.setColor(Color.argb(45,150,70,230)); c.drawCircle(playerX,playerY-55,95+level*8,p);
            p.setColor(Color.rgb(35,35,45));c.drawCircle(playerX,playerY-100,35,p);
            p.setColor(level>=3?Color.rgb(105,40,150):Color.rgb(55,55,70));c.drawRoundRect(playerX-42,playerY-65,playerX+42,playerY+65,22,22,p);
            p.setColor(Color.rgb(150,150,165));c.drawRect(playerX-32,playerY+65,playerX-8,playerY+120,p);c.drawRect(playerX+8,playerY+65,playerX+32,playerY+120,p);
            p.setColor(Color.rgb(210,210,220));c.drawRect(playerX+35,playerY-10,playerX+105,playerY+2,p);
            if(System.currentTimeMillis()<attackUntil){p.setColor(Color.argb(180,210,150,255));c.drawCircle(playerX+95,playerY-5,55,p);}
        }

        void drawEnemy(Canvas c,Enemy e){
            p.setColor(e.boss?Color.rgb(120,25,45):Color.rgb(105,35,45));
            c.drawCircle(e.x,e.y-30,e.boss?70:42,p);
            p.setColor(Color.rgb(35,20,25));c.drawRect(e.x-(e.boss?55:30),e.y,e.x+(e.boss?55:30),e.y+85,p);
            txt(c,e.boss?"☠":"•",e.x,e.y-18,e.boss?60:42,Color.WHITE,Paint.Align.CENTER);
            p.setColor(Color.DKGRAY);c.drawRoundRect(e.x-45,e.y-105,e.x+45,e.y-94,8,8,p);
            p.setColor(Color.rgb(220,60,70));c.drawRoundRect(e.x-45,e.y-105,e.x-45+90*e.hp/(float)e.maxHp,e.y-94,8,8,p);
        }

        void update(long now){
            if(lastFrame==0){lastFrame=now;return;}
            float dt=Math.min(0.04f,(now-lastFrame)/1000f); lastFrame=now;
            if(screen!=1||victory)return;
            if(left)playerX-=420*dt;if(right)playerX+=420*dt;
            playerX=Math.max(80,Math.min(1000,playerX));
            if(!boss && enemiesDefeated>=8){boss=true; enemies.clear(); enemies.add(new Enemy(540,1050,true));}
            if(!boss && now-lastSpawn>1400 && enemies.size()<5){enemies.add(new Enemy(100+random.nextInt(880),880+random.nextInt(250),false));lastSpawn=now;}
            for(int i=enemies.size()-1;i>=0;i--){
                Enemy e=enemies.get(i);
                float dx=playerX-e.x,dy=(playerY-e.y),d=(float)Math.sqrt(dx*dx+dy*dy);
                if(d>95){e.x+=dx/d*70*dt;e.y+=dy/d*70*dt;}
                else if(now-e.lastHit>900){hp-=e.boss?10:5;e.lastHit=now;}
                if(attacking && now<attackUntil && d<230 && now-e.lastHitPlayer>300){
                    e.hp-=level*35;e.lastHitPlayer=now;particles.add(new Particle(e.x,e.y,Color.rgb(220,140,255)));
                }
                if(e.hp<=0){enemies.remove(i);enemiesDefeated++;gainXp(e.boss?150:25);}
            }
            if(hp<=0){hp=maxHp;xp=0;level=1;enemiesDefeated=0;boss=false;enemies.clear();}
            if(now>attackUntil)attacking=false;
            if(now>skillUntil)skillReady=true;
            for(int i=particles.size()-1;i>=0;i--){Particle q=particles.get(i);q.r-=80*dt;if(q.r<=0)particles.remove(i);}
        }

        void gainXp(int n){
            xp+=n;
            while(xp>=level*100){xp-=level*100;level++;maxHp+=25;hp=maxHp;}
            if(boss && enemies.isEmpty()) victory=true;
        }

        public boolean onTouchEvent(MotionEvent e){
            float x=e.getX()*1080f/getWidth(), y=e.getY()*1920f/getHeight();
            if(e.getAction()==MotionEvent.ACTION_DOWN){
                if(screen==0 && x>240&&x<840&&y>1200&&y<1450){screen=1;lastFrame=0;return true;}
                if(victory && y>1000&&y<1230){victory=false;boss=false;enemiesDefeated=0;level=Math.max(level,3);return true;}
                if(y>1550&&x<500){if(x<250)left=true;else right=true;return true;}
                if(y>1500&&x>680&&x<920){attacking=true;attackUntil=System.currentTimeMillis()+260;return true;}
                if(y>1580&&x>900){if(skillReady){skillReady=false;skillUntil=System.currentTimeMillis()+3000;for(Enemy q:enemies)q.hp-=level*20;particles.add(new Particle(playerX,playerY,Color.rgb(90,170,255)));}return true;}
            }
            if(e.getAction()==MotionEvent.ACTION_UP){left=false;right=false;attacking=false;return true;}
            return true;
        }

        static class Enemy{
            float x,y; int hp,maxHp; boolean boss; long lastHit,lastHitPlayer;
            Enemy(float x,float y,boolean b){this.x=x;this.y=y;boss=b;maxHp=b?500:70;hp=maxHp;}
        }
        static class Particle{float x,y,r=35;int color;Particle(float x,float y,int c){this.x=x;this.y=y;color=c;}}
    }
}
