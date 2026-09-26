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

    static class TrucoView extends View{
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); Random rnd=new Random();
        ArrayList<Card> deck=new ArrayList<>(),me=new ArrayList<>(),ai=new ArrayList<>();
        ArrayList<Card> seen=new ArrayList<>();
        Card vira; int myPoints=0,aiPoints=0,stake=1,round=1;
        boolean myTurn=true,finished=false;
        String status="Sua vez — escolha uma carta";
        RectF[] cardRects=new RectF[3]; RectF trucoRect=new RectF(),restartRect=new RectF();

        TrucoView(Context c){super(c);p.setTypeface(Typeface.create("sans",Typeface.BOLD));startMatch();}

        void startMatch(){
            deck.clear();me.clear();ai.clear();seen.clear();
            myPoints=aiPoints=0;stake=1;round=1;finished=false;
            buildDeck();Collections.shuffle(deck,rnd);
            for(int i=0;i<3;i++){me.add(deck.remove(0));ai.add(deck.remove(0));}
            vira=deck.remove(0);seen.add(vira);
            myTurn=true;status="Sua vez — escolha uma carta";invalidate();
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
        boolean isManilha(Card c){return c.rank.equals(nextRank(vira.rank));}

        int unseenCountOfRank(String rank){
            int n=0;
            String[] suits={"♣","♥","♠","♦"};
            for(String s:suits){
                boolean found=false;
                for(Card c:seen)if(c.rank.equals(rank)&&c.suit.equals(s)){found=true;break;}
                if(!found)n++;
            }
            return n;
        }

        // IA avançada: conta cartas já vistas e escolhe a menor carta capaz de vencer.
        Card chooseCard(){
            if(ai.isEmpty())return null;

            ArrayList<Card> sorted=new ArrayList<>(ai);
            Collections.sort(sorted,(a,b)->Integer.compare(strength(a),strength(b)));

            Card best=sorted.get(sorted.size()-1), weakest=sorted.get(0);

            // Se a IA precisa ganhar a mão, preserva a maior carta e usa a menor que resolve a situação.
            if(myTurn==false){
                return weakest;
            }

            // Quando responde à carta do jogador, procura a menor carta que ainda vence.
            Card last=seen.isEmpty()?null:seen.get(seen.size()-1);
            if(last!=null && last!=vira){
                int target=strength(last);
                for(Card c:sorted)if(strength(c)>target)return c;
                return weakest;
            }

            // Mão forte: joga uma carta intermediária para preservar a melhor.
            if(sorted.size()>=3 && strength(best)>=8){
                return sorted.get(1);
            }

            // Mão fraca: descarta a menor e guarda recursos.
            return weakest;
        }

        boolean aiShouldTruco(){
            int top=0,strong=0,manilhas=0;
            for(Card c:ai){
                top=Math.max(top,strength(c));
                if(strength(c)>=8)strong++;
                if(isManilha(c))manilhas++;
            }
            if(manilhas>=1 && stake==1)return true;
            if(strong>=2 && stake==1)return true;
            if(aiPoints>=10 && top>=7 && stake==1)return true;
            if(myPoints>=10 && top>=8 && stake==1)return true;
            return false;
        }

        boolean aiAcceptTruco(){
            int top=0,strong=0,manilhas=0;
            for(Card c:ai){
                top=Math.max(top,strength(c));
                if(strength(c)>=8)strong++;
                if(isManilha(c))manilhas++;
            }
            if(manilhas>=1)return true;
            if(strong>=2)return true;
            if(top>=9)return true;
            if(aiPoints>myPoints && strong>=1)return true;
            return rnd.nextInt(100)<10;
        }

        void play(int idx){
            if(!myTurn||finished||idx>=me.size())return;
            Card mine=me.remove(idx);seen.add(mine);

            Card enemy=chooseCard();
            if(enemy==null)return;
            ai.remove(enemy);seen.add(enemy);

            int a=strength(mine),b=strength(enemy);
            boolean win=a>b;
            if(a==b)status="Empate — desempate pela ordem da rodada.";
            else status=win?"Você levou a rodada!":"🤖 IA levou a rodada!";

            if(win)myPoints+=stake;else if(a<b)aiPoints+=stake;

            if(myPoints>=12||aiPoints>=12){
                finished=true;status=myPoints>=12?"🏆 Você venceu a partida!":"🤖 A IA venceu a partida!";
                invalidate();return;
            }

            if(me.isEmpty()||ai.isEmpty()){
                round++;stake=1;
                if(deck.size()<7){buildDeck();Collections.shuffle(deck,rnd);}
                for(int i=0;i<3;i++){me.add(deck.remove(0));ai.add(deck.remove(0));}
                vira=deck.remove(0);seen.add(vira);
                myTurn=win;
                status=win?"Nova mão — você começa!":"Nova mão — IA começa!";
            }else{
                // Na versão atual, o vencedor da rodada joga primeiro na próxima.
                myTurn=win;
                if(!myTurn)aiAutoTurn();
            }
            invalidate();
        }

        void aiAutoTurn(){
            if(finished||myTurn||ai.isEmpty())return;
            // A IA joga automaticamente quando ganha a vez.
            Card c=chooseCard(); if(c!=null){ai.remove(c);seen.add(c);}
            myTurn=true;
            status="🤖 IA jogou. Sua vez!";
        }

        void askTruco(){
            if(finished||!myTurn)return;
            if(aiAcceptTruco()){
                stake=Math.min(stake*3,12);
                status="🤖 IA aceitou o TRUCO! Vale "+stake+" pontos.";
            }else{
                myPoints+=stake;status="🤖 IA correu! Você ganhou "+stake+" ponto(s).";
                if(myPoints>=12){finished=true;status="🏆 Você venceu a partida!";}
            }
            invalidate();
        }

        @Override protected void onDraw(Canvas c){
            super.onDraw(c);float w=getWidth(),h=getHeight();c.drawColor(Color.rgb(22,96,62));
            p.setColor(Color.WHITE);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(26);c.drawText("TRUCO IA",w/2,42,p);
            p.setTextSize(16);c.drawText("Você  "+myPoints+"  ×  "+aiPoints+"  IA",w/2,70,p);
            p.setTextSize(15);c.drawText("Mão "+round+"   •   Vira: "+vira.label()+"   •   Vale "+stake,w/2,98,p);
            p.setTextSize(16);c.drawText(status,w/2,126,p);
            drawAi(c,w/2,170);drawVira(c,w/2,245);drawHand(c,w/2,h-170);
            trucoRect.set(w/2-85,h-110,w/2+85,h-60);p.setColor(Color.rgb(196,45,45));c.drawRoundRect(trucoRect,16,16,p);
            p.setColor(Color.WHITE);p.setTextSize(19);c.drawText("TRUCO!",w/2,h-77,p);
            if(finished){restartRect.set(w/2-100,h-52,w/2+100,h-10);p.setColor(Color.rgb(40,55,80));c.drawRoundRect(restartRect,14,14,p);p.setColor(Color.WHITE);p.setTextSize(17);c.drawText("NOVA PARTIDA",w/2,h-24,p);}
        }

        void drawAi(Canvas c,float x,float y){p.setColor(Color.argb(120,0,0,0));c.drawOval(x-42,y-22,x+42,y+62,p);p.setColor(Color.WHITE);p.setTextSize(15);c.drawText("🤖 IA",x,y+22,p);p.setTextSize(12);c.drawText(ai.size()+" cartas",x,y+42,p);}
        void drawVira(Canvas c,float x,float y){drawCard(c,vira,x-34,y-35,68,92);}
        void drawHand(Canvas c,float x,float y){
            float start=x-(me.size()*72)/2f;
            for(int i=0;i<me.size();i++){float left=start+i*72;cardRects[i]=new RectF(left,y,left+68,y+92);drawCard(c,me.get(i),left,y,68,92);}
            p.setColor(Color.WHITE);p.setTextSize(13);c.drawText("SUAS CARTAS",x,y+112,p);
        }
        void drawCard(Canvas c,Card card,float x,float y,float cw,float ch){
            p.setColor(Color.WHITE);c.drawRoundRect(x,y,x+cw,y+ch,12,12,p);
            p.setColor(card.suit.equals("♥")||card.suit.equals("♦")?Color.rgb(190,35,45):Color.rgb(25,25,25));
            p.setTextAlign(Paint.Align.CENTER);p.setTextSize(22);c.drawText(card.rank,x+cw/2,y+32,p);c.drawText(card.suit,x+cw/2,y+62,p);
            p.setStyle(Paint.Style.STROKE);p.setColor(Color.LTGRAY);c.drawRoundRect(x,y,x+cw,y+ch,12,12,p);p.setStyle(Paint.Style.FILL);
        }
        @Override public boolean onTouchEvent(MotionEvent e){
            if(e.getAction()!=MotionEvent.ACTION_UP)return true;float x=e.getX(),y=e.getY();
            if(finished&&restartRect.contains(x,y)){startMatch();return true;}
            if(trucoRect.contains(x,y)){askTruco();return true;}
            if(myTurn&&!finished)for(int i=0;i<me.size();i++)if(cardRects[i]!=null&&cardRects[i].contains(x,y)){play(i);return true;}
            return true;
        }
    }
}