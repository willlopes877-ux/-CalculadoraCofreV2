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
        String name; boolean human,teamA; ArrayList<Card> hand=new ArrayList<>();
        Player(String n,boolean h,boolean t){name=n;human=h;teamA=t;}
    }

    static class TrucoView extends View{
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); Random rnd=new Random();
        ArrayList<Card> deck=new ArrayList<>(),seen=new ArrayList<>();
        Player[] players=new Player[4];
        Card vira; int teamAPoints=0,teamBPoints=0,stake=1,current=0,round=1;
        boolean finished=false,roundActive=true;
        String status="Sua vez — escolha uma carta";
        RectF[] cardRects=new RectF[3],trucoRect=new RectF(),restartRect=new RectF();

        TrucoView(Context c){super(c);p.setTypeface(Typeface.create("sans",Typeface.BOLD));startMatch();}

        void startMatch(){
            players[0]=new Player("Você",true,true);
            players[1]=new Player("IA Parceira",false,true);
            players[2]=new Player("IA Rival 1",false,false);
            players[3]=new Player("IA Rival 2",false,false);
            teamAPoints=teamBPoints=0;stake=1;round=1;finished=false;
            newHand(0);status="Você e IA Parceira vs 2 IAs. Sua vez!";invalidate();
        }

        void newHand(int first){
            deck.clear();seen.clear();stake=1;roundActive=true;
            for(Player pl:players)pl.hand.clear();
            buildDeck();Collections.shuffle(deck,rnd);
            for(int i=0;i<3;i++)for(Player pl:players)pl.hand.add(deck.remove(0));
            vira=deck.remove(0);seen.add(vira);current=first;
            if(!players[current].human)runAITurn();
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

            int top=strength(sorted.get(sorted.size()-1));
            int strong=0,man=0;
            for(Card c:pl.hand){if(strength(c)>=8)strong++;if(manilha(c))man++;}

            // Personalidade competitiva: quanto mais perto de perder, maior o risco.
            boolean desperate=(pl.teamA?teamAPoints:teamBPoints) <= 0 && (pl.teamA?teamBPoints:teamAPoints)>=8;
            if(man>0 && (stake==1 || desperate))return sorted.get(sorted.size()-1);
            if(strong>=2 && stake==1 && rnd.nextInt(100)<75)return sorted.get(sorted.size()-1);
            if(desperate)return sorted.get(sorted.size()-1);

            // Preserva a carta mais forte e descarta a menor quando não precisa forçar.
            if(sorted.size()>=3 && top>=8)return sorted.get(1);
            return sorted.get(0);
        }

        void playHuman(int idx){
            if(finished||!roundActive||current!=0||idx<0||idx>=players[0].hand.size())return;
            Card c=players[0].hand.remove(idx);seen.add(c);
            status="Você jogou "+c.label()+".";
            resolveTurn(c);
        }

        void resolveTurn(Card played){
            // A carta é registrada; os demais jogadores jogam na sequência.
            int next=(current+1)%4;
            current=next;
            if(!players[current].human){runAITurn();}
            else {status="Sua vez!";invalidate();}
        }

        void runAITurn(){
            if(finished||!roundActive)return;
            Player pl=players[current];
            if(pl.human){invalidate();return;}
            Card c=chooseAI(pl);
            if(c==null){endHand();return;}
            pl.hand.remove(c);seen.add(c);
            status=pl.name+" jogou "+c.label()+".";
            int next=(current+1)%4;
            current=next;
            if(players[current].human)invalidate();else postDelayedAI();
        }

        void postDelayedAI(){
            // Pequena pausa visual sem bloquear a interface.
            postDelayed(()->runAITurn(),220);
        }

        void endHand(){
            // Nesta V1, a mão termina quando os quatro jogadores jogaram suas três cartas.
            round++;
            int a=0,b=0;
            // O placar da mão usa a carta mais forte vista pela equipe em cada ciclo.
            for(Card c:seen)if(strength(c)>=8){if((c==vira))continue; if(a==0)a=strength(c); else b=Math.max(b,strength(c));}
            if(a>=b)teamAPoints+=stake;else teamBPoints+=stake;
            if(teamAPoints>=12||teamBPoints>=12){
                finished=true;status=teamAPoints>=12?"🏆 Sua dupla venceu!":"🤖 As IAs rivais venceram!";
                invalidate();return;
            }
            int first=winnerStartsNextHand();
            newHand(first);
            status=(first==0?"Sua dupla começa!":"Nova mão — "+players[first].name+" começa.");
            invalidate();
        }

        int winnerStartsNextHand(){
            return rnd.nextInt(4);
        }

        boolean aiAcceptTruco(){
            Player rival=players[2];
            int top=0,strong=0,man=0;
            for(Card c:rival.hand){top=Math.max(top,strength(c));if(strength(c)>=8)strong++;if(manilha(c))man++;}
            if(man>0||strong>=2||top>=9)return true;
            return rnd.nextInt(100)<15;
        }

        void askTruco(){
            if(finished||current!=0)return;
            if(aiAcceptTruco()){
                stake=Math.min(stake*3,12);
                status="🤖 As IAs aceitaram! Vale "+stake+" pontos.";
            }else{
                teamAPoints+=stake;
                status="😤 As IAs correram! Sua dupla ganhou "+stake+" ponto(s).";
                if(teamAPoints>=12){finished=true;status="🏆 Sua dupla venceu!";}
            }
            invalidate();
        }

        @Override protected void onDraw(Canvas c){
            super.onDraw(c);float w=getWidth(),h=getHeight();c.drawColor(Color.rgb(22,96,62));
            p.setColor(Color.WHITE);p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(25);c.drawText("TRUCO IA — 4 JOGADORES",w/2,38,p);
            p.setTextSize(15);c.drawText("VOCÊ + IA  "+teamAPoints+"  ×  "+teamBPoints+"  2 IAs",w/2,64,p);
            p.setTextSize(14);c.drawText("Mão "+round+"  •  Vira: "+vira.label()+"  •  Vale "+stake,w/2,89,p);
            p.setTextSize(15);c.drawText(status,w/2,116,p);

            drawOpponent(c,players[2],w/2,154);
            drawVira(c,w/2,245);
            drawTeammate(c,players[1],65,245);
            drawOpponent(c,players[3],w-65,245);
            drawHand(c,w/2,h-155);

            trucoRect.set(w/2-85,h-92,w/2+85,h-42);
            p.setColor(Color.rgb(196,45,45));c.drawRoundRect(trucoRect,16,16,p);
            p.setColor(Color.WHITE);p.setTextSize(18);c.drawText("TRUCO!",w/2,h-59,p);
            if(finished){
                restartRect.set(w/2-105,h-36,w/2+105,h-4);
                p.setColor(Color.rgb(40,55,80));c.drawRoundRect(restartRect,12,12,p);
                p.setColor(Color.WHITE);p.setTextSize(15);c.drawText("NOVA PARTIDA",w/2,h-14,p);
            }
        }

        void drawOpponent(Canvas c,Player pl,float x,float y){
            p.setColor(Color.argb(130,0,0,0));c.drawOval(x-45,y-22,x+45,y+40,p);
            p.setColor(Color.WHITE);p.setTextSize(13);c.drawText("🤖 "+pl.name,x,y+5,p);
            p.setTextSize(11);c.drawText(pl.hand.size()+" cartas",x,y+23,p);
        }
        void drawTeammate(Canvas c,Player pl,float x,float y){
            p.setColor(Color.argb(100,0,0,0));c.drawOval(x-50,y-24,x+50,y+38,p);
            p.setColor(Color.WHITE);p.setTextSize(11);c.drawText("🤖 Parceira",x,y+2,p);
            p.setTextSize(10);c.drawText(pl.hand.size()+" cartas",x,y+18,p);
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
            if(e.getAction()!=MotionEvent.ACTION_UP)return true;
            float x=e.getX(),y=e.getY();
            if(finished&&restartRect.contains(x,y)){startMatch();return true;}
            if(trucoRect.contains(x,y)){askTruco();return true;}
            if(current==0&&!finished)for(int i=0;i<players[0].hand.size();i++)if(cardRects[i]!=null&&cardRects[i].contains(x,y)){playHuman(i);return true;}
            return true;
        }
    }
}