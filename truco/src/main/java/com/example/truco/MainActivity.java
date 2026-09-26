package com.example.truco;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import android.content.Context;
import java.util.*;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle b){super.onCreate(b);setContentView(new TrucoView(this));}

    static class Card{
        String rank,suit; int value;
        Card(String r,String s,int v){rank=r;suit=s;value=v;}
        String label(){return rank+suit;}
    }

    static class Player{
        String name; boolean human,teamA; ArrayList<Card> hand=new ArrayList<>(); int mood=50;
        Player(String n,boolean h,boolean t){name=n;human=h;teamA=t;}
    }
    static class Played{
        int player; Card card;
        Played(int p,Card c){player=p;card=c;}
    }

    static class TrucoView extends View{
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); Random rnd=new Random();
        ArrayList<Card> deck=new ArrayList<>(); ArrayList<Played> trick=new ArrayList<>(); ArrayList<Integer> trickWinners=new ArrayList<>();
        Player[] players=new Player[4]; Card vira;
        int teamAPoints=0,teamBPoints=0,stake=1,current=0,round=1,tricksA=0,tricksB=0;
        boolean finished=false,waiting=false,elevenDecision=false;
        String status="Sua vez — escolha uma carta";
        RectF[] cardRects=new RectF[3],trucoRect=new RectF(),restartRect=new RectF(),play11Rect=new RectF(),run11Rect=new RectF();

        TrucoView(Context c){super(c);p.setTypeface(Typeface.create("sans",Typeface.BOLD));startMatch();}

        void startMatch(){
            players[0]=new Player("Você",true,true);
            players[1]=new Player("IA Parceira",false,true);
            players[2]=new Player("IA Rival 1",false,false);
            players[3]=new Player("IA Rival 2",false,false);
            teamAPoints=teamBPoints=0;round=1;finished=false;newHand(0);
        }

        void newHand(int first){
            deck.clear();trick.clear();trickWinners.clear();stake=1;tricksA=tricksB=0;waiting=false;elevenDecision=false;
            for(Player pl:players)pl.hand.clear();
            buildDeck();Collections.shuffle(deck,rnd);
            for(int i=0;i<3;i++)for(Player pl:players)pl.hand.add(deck.remove(0));
            vira=deck.remove(0);current=first;
            if(teamAPoints==11){
                elevenDecision=true;status="🃏 MÃO DE 11 — você vê as cartas da parceira!";invalidate();return;
            }
            status=current==0?"Sua vez!":players[current].name+" começa.";
            invalidate();if(!players[current].human)runAITurn();
        }

        void buildDeck(){
            String[] s={"♣","♥","♠","♦"},r={"4","5","6","7","Q","J","K","A","2","3"};
            int[] v={1,2,3,4,5,6,7,8,9,10};
            for(int a=0;a<4;a++)for(int i=0;i<r.length;i++)deck.add(new Card(r[i],s[a],v[i]));
        }

        String nextRank(String r){
            String[] rs={"4","5","6","7","Q","J","K","A","2","3"};
            for(int i=0;i<rs.length;i++)if(rs[i].equals(r))return rs[(i+1)%rs.length];
            return "4";
        }
        int suitPower(String s){return s.equals("♣")?4:s.equals("♥")?3:s.equals("♠")?2:1;}
        int strength(Card c){return c.rank.equals(nextRank(vira.rank))?100+suitPower(c.suit):c.value;}
        boolean manilha(Card c){return c.rank.equals(nextRank(vira.rank));}

        Card chooseAI(Player pl){
            ArrayList<Card> sorted=new ArrayList<>(pl.hand);
            Collections.sort(sorted,(a,b)->Integer.compare(strength(a),strength(b)));
            if(sorted.isEmpty())return null;
            int top=strength(sorted.get(sorted.size()-1)),strong=0,man=0;
            int own=pl.teamA?teamAPoints:teamBPoints,opp=pl.teamA?teamBPoints:teamAPoints;
            for(Card c:pl.hand){if(strength(c)>=8)strong++;if(manilha(c))man++;}
            if(opp-own>=5)pl.mood=Math.min(100,pl.mood+20);
            if(own-opp>=4)pl.mood=Math.max(0,pl.mood-8);
            if(man>0&&(stake==1||pl.mood>70))return sorted.get(sorted.size()-1);
            if(strong>=2&&stake==1&&rnd.nextInt(100)<82)return sorted.get(sorted.size()-1);
            if(sorted.size()>=3&&top>=8)return sorted.get(1);
            return sorted.get(0);
        }

        void playHuman(int idx){
            if(finished||waiting||elevenDecision||current!=0||idx<0||idx>=players[0].hand.size())return;
            Card c=players[0].hand.remove(idx);trick.add(new Played(0,c));status="Você jogou "+c.label()+".";advanceTurn();
        }
        void advanceTurn(){
            if(trick.size()==4){resolveTrick();return;}
            current=(current+1)%4;
            if(players[current].human){status="Sua vez!";invalidate();}else runAITurn();
        }

        void runAITurn(){
            if(finished||waiting||elevenDecision)return;
            Player pl=players[current]; if(pl.human){invalidate();return;}
            waiting=true;
            postDelayed(()->{
                waiting=false;Card c=chooseAI(pl);if(c==null)return;
                pl.hand.remove(c);trick.add(new Played(current,c));
                String sig=current==1?partnerSignal(pl):rivalSignal(pl);
                status=sig.length()>0?pl.name+" fez o sinal "+sig:pl.name+" jogou uma carta.";
                advanceTurn();
            },380);
        }
        String partnerSignal(Player pl){
            if(pl.hand.isEmpty())return "";
            Card best=Collections.max(pl.hand,(a,b)->Integer.compare(strength(a),strength(b)));
            if(manilha(best)){
                if(best.suit.equals("♣"))return "😉";
                if(best.suit.equals("♥"))return "😙";
                if(best.suit.equals("♠"))return "😬";
                return "👄";
            }
            return strength(best)>=9&&rnd.nextInt(100)<45?"🔥":"";
        }
        String rivalSignal(Player pl){
            if(rnd.nextInt(100)<32)return new String[]{"😉","😙","😬","👄"}[rnd.nextInt(4)];
            return "";
        }

        void resolveTrick(){
            int best=-1,bestStrength=-1;boolean tie=false;
            for(Played x:trick){int s=strength(x.card);if(s>bestStrength){bestStrength=s;best=x.player;tie=false;}else if(s==bestStrength)tie=true;}
            int winner;
            if(tie)winner=trickWinners.size()>0?trickWinners.get(trickWinners.size()-1):current;
            else winner=best;
            trickWinners.add(winner);
            if(players[winner].teamA)tricksA++;else tricksB++;
            status=(players[winner].teamA?"🤝 Sua dupla":"🤖 Rivais")+" ganharam a vaza!";
            trick.clear();
            if(tricksA>=2||tricksB>=2){endHand(players[winner].teamA);return;}
            current=winner;
            postDelayed(()->{if(current==0){status="Sua vez — próxima vaza.";invalidate();}else runAITurn();},550);
        }
        void endHand(boolean teamAWon){
            if(teamAWon)teamAPoints+=stake;else teamBPoints+=stake;
            if(teamAPoints>=12||teamBPoints>=12){
                finished=true;status=teamAPoints>=12?"🏆 VOCÊ + PARCEIRA VENCERAM!":"🤖 AS IAs RIVAIS VENCERAM!";invalidate();return;
            }
            round++;newHand(teamAWon?0:2);
        }

        boolean aiAcceptTruco(){
            Player rival=players[2];int top=0,strong=0,man=0;
            for(Card c:rival.hand){top=Math.max(top,strength(c));if(strength(c)>=8)strong++;if(manilha(c))man++;}
            if(man>0||strong>=2||top>=9)return true;
            return rnd.nextInt(100)<(stake>=6?45:28);
        }

        void askTruco(){
            if(finished||waiting||elevenDecision||current!=0)return;
            int next=stake==1?3:stake==3?6:stake==6?9:12;
            if(stake>=12){status="Já vale 12!";invalidate();return;}
            if(aiAcceptTruco()){stake=next;status="😈 Rivais aceitaram! Vale "+stake+".";}
            else{teamAPoints+=stake;status="🏃 Rivais correram! Sua dupla +"+stake+".";if(teamAPoints>=12)finished=true;}
            invalidate();
        }
        void play11(){elevenDecision=false;stake=3;status="🔥 VAMOS! Mão de 11 valendo 3.";invalidate();}
        void run11(){elevenDecision=false;teamBPoints++;status="🏃 CORRE! Rivais +1.";if(teamBPoints>=12){finished=true;invalidate();return;}round++;newHand(2);}

        @Override protected void onDraw(Canvas c){
            super.onDraw(c);float w=getWidth(),h=getHeight();c.drawColor(Color.rgb(18,92,58));
            p.setColor(Color.WHITE);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(24);c.drawText("TRUCO IA — 2 × 2",w/2,34,p);
            p.setTextSize(16);c.drawText("VOCÊ + PARCEIRA  "+teamAPoints+" × "+teamBPoints+"  RIVAIS",w/2,60,p);
            p.setTextSize(13);c.drawText("Mão "+round+" • Vira: "+vira.label()+" • Vale "+stake+" • Vazadas "+tricksA+"×"+tricksB,w/2,84,p);
            p.setTextSize(14);c.drawText(status,w/2,110,p);
            drawOpponent(c,players[2],w/2,145);drawTeammate(c,players[1],70,215);drawOpponent(c,players[3],w-70,215);drawVira(c,w/2,285);drawHand(c,w/2,h-170);
            trucoRect.set(w/2-80,h-105,w/2+80,h-55);p.setColor(Color.rgb(190,42,42));c.drawRoundRect(trucoRect,14,14,p);p.setColor(Color.WHITE);p.setTextSize(17);c.drawText("TRUCO!",w/2,h-72,p);
            if(elevenDecision)draw11(c,w,h);
            if(finished){restartRect.set(w/2-105,h-48,w/2+105,h-12);p.setColor(Color.rgb(38,54,78));c.drawRoundRect(restartRect,12,12,p);p.setColor(Color.WHITE);p.setTextSize(14);c.drawText("NOVA PARTIDA",w/2,h-25,p);}
        }

        void drawOpponent(Canvas c,Player pl,float x,float y){
            p.setColor(Color.argb(130,0,0,0));c.drawOval(x-45,y-22,x+45,y+40,p);
            p.setColor(Color.WHITE);p.setTextSize(13);c.drawText("🤖 "+pl.name,x,y+5,p);
            p.setTextSize(11);c.drawText(pl.hand.size()+" cartas",x,y+23,p);
        }
        void drawTeammate(Canvas c,Player pl,float x,float y){
            p.setColor(Color.argb(120,0,0,0));c.drawRoundRect(x-62,y-24,x+62,y+36,12,12,p);p.setColor(Color.WHITE);p.setTextSize(11);c.drawText("🤖 PARCEIRA",x,y-6,p);
            if(teamAPoints==11){c.drawText("👀 CARTAS VISÍVEIS",x,y+10,p);for(int i=0;i<pl.hand.size();i++)c.drawText(pl.hand.get(i).label(),x,y+25+i*12,p);}
            else c.drawText("🂠 "+pl.hand.size()+" cartas",x,y+12,p);
        }
        void draw11(Canvas c,float w,float h){
            p.setColor(Color.argb(240,8,18,12));c.drawRoundRect(16,h/2-95,w-16,h/2+100,20,20,p);p.setColor(Color.WHITE);p.setTextSize(22);c.drawText("🃏 MÃO DE 11",w/2,h/2-55,p);
            p.setTextSize(13);c.drawText("Você vê as 3 cartas da parceira.",w/2,h/2-30,p);c.drawText("Decidam: jogar valendo 3 ou correr.",w/2,h/2-8,p);
            play11Rect.set(w/2-145,h/2+18,w/2-10,h/2+66);run11Rect.set(w/2+10,h/2+18,w/2+145,h/2+66);
            p.setColor(Color.rgb(35,135,75));c.drawRoundRect(play11Rect,12,12,p);p.setColor(Color.rgb(150,45,45));c.drawRoundRect(run11Rect,12,12,p);p.setColor(Color.WHITE);p.setTextSize(16);c.drawText("VAMOS! 3",w/2-77,h/2+48,p);c.drawText("CORRE",w/2+77,h/2+48,p);
        }
        void drawVira(Canvas c,float x,float y){drawCard(c,vira,x-32,y-32,64,86);}

        void drawHand(Canvas c,float x,float y){
            float start=x-(players[0].hand.size()*72)/2f;
            for(int i=0;i<players[0].hand.size();i++){
                float left=start+i*72;cardRects[i]=new RectF(left,y,left+68,y+92);
                drawCard(c,players[0].hand.get(i),left,y,68,92);
            }
            p.setColor(Color.WHITE);p.setTextSize(12);c.drawText("SUAS CARTAS",x,y+108,p);
        }

        void drawCard(Canvas c,Card card,float x,float y,float cw,float ch){
            p.setColor(Color.WHITE);c.drawRoundRect(x,y,x+cw,y+ch,12,12,p);
            p.setColor(card.suit.equals("♥")||card.suit.equals("♦")?Color.rgb(190,35,45):Color.rgb(25,25,25));
            p.setTextAlign(Paint.Align.CENTER);p.setTextSize(21);
            c.drawText(card.rank,x+cw/2,y+31,p);c.drawText(card.suit,x+cw/2,y+60,p);
            p.setStyle(Paint.Style.STROKE);p.setColor(Color.LTGRAY);c.drawRoundRect(x,y,x+cw,y+ch,12,12,p);p.setStyle(Paint.Style.FILL);
        }

        @Override public boolean onTouchEvent(MotionEvent e){
            if(e.getAction()!=MotionEvent.ACTION_UP)return true;float x=e.getX(),y=e.getY();
            if(finished&&restartRect.contains(x,y)){startMatch();return true;}
            if(elevenDecision){if(play11Rect.contains(x,y))play11();else if(run11Rect.contains(x,y))run11();return true;}
            if(trucoRect.contains(x,y)){askTruco();return true;}
            if(current==0&&!waiting)for(int i=0;i<players[0].hand.size();i++)if(cardRects[i]!=null&&cardRects[i].contains(x,y)){playHuman(i);return true;}
            return true;
        }
    }
}